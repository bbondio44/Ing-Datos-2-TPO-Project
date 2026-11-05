<#
  Pruebas de aceptacion de Persona 1 (R01-R08, R10, R12-R16, R24, R35-R39) contra PostgreSQL real.
  Corre sobre una COPIA de la base (CREATE DATABASE ... TEMPLATE) que se borra al final, porque la
  facturacion automatica y el cierre mensual no se pueden deshacer y cambiarian los datos de la demo.
  Uso, con el backend apagado: mvn -B -DskipTests install, despues correr-todo.ps1 o este archivo.
#>
param([string]$Scratch = $env:TEMP)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres); $NJ = (docker ps -q --filter name=neo4j)
$base = 'http://localhost:8080'
$copia = 'logistica_p1'
$origen = (docker exec $PG printenv POSTGRES_DB)
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:PG_JDBC_URL = "jdbc:postgresql://localhost:5432/$copia"
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'smoke-p1@ejemplo.test'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
$clave = [guid]::NewGuid().ToString('N')
$marca = [guid]::NewGuid().ToString('N').Substring(0, 6)

function SqlEn($db, $q) { ($q | docker exec -i $PG sh -c "psql -q -U `$POSTGRES_USER -d $db -At") }
function Sql($q) { SqlEn $copia $q }
function Ver($t, $e, $o) { "$(if ("$e" -eq "$o") { 'OK   ' } else { 'FALLA' }) ${t}: esperado=$e obtenido=$o" }
function Num($v) { [double][math]::Round([decimal]$v, 2) }
function Cy($q) {
    [IO.File]::WriteAllText("$Scratch\q.cypher", $q, (New-Object Text.UTF8Encoding $false))
    docker cp "$Scratch\q.cypher" "${NJ}:/tmp/q.cypher" 2>$null | Out-Null
    docker exec $NJ sh -c 'cypher-shell -u ${NEO4J_AUTH%%/*} -p ${NEO4J_AUTH#*/} --format plain -f /tmp/q.cypher' 2>$null | Select-Object -Skip 1
}
function Api($m, $p, $b = $null, $t = $script:token) {
    $q = @{ Uri = "$base$p"; Method = $m; UseBasicParsing = $true; TimeoutSec = 30; Headers = @{} }
    if ($t) { $q.Headers.Authorization = "Bearer $t" }
    if ($null -ne $b) {
        $q.ContentType = 'application/json; charset=utf-8'
        $q.Body = [Text.Encoding]::UTF8.GetBytes(($b | ConvertTo-Json -Depth 6))
    }
    try {
        $r = Invoke-WebRequest @q
        $d = $null; if ($r.Content) { $d = $r.Content | ConvertFrom-Json }
        [pscustomobject]@{ s = [int]$r.StatusCode; d = $d }
    } catch {
        $c = 0; if ($_.Exception.Response) { $c = [int]$_.Exception.Response.StatusCode }
        [pscustomobject]@{ s = $c; d = $null }
    }
}
function Login($correo, $pass) { (Api POST '/api/v1/auth/login' @{ email = $correo; password = $pass } '').d.token }
function NuevoCuit {
    $pesos = 5, 4, 3, 2, 7, 6, 5, 4, 3, 2
    while ($true) {
        $n = '30' + (Get-Random -Minimum 10000000 -Maximum 99999999)
        $s = 0; for ($i = 0; $i -lt 10; $i++) { $s += [int]"$($n[$i])" * $pesos[$i] }
        $d = 11 - ($s % 11); if ($d -eq 11) { $d = 0 }
        if ($d -lt 10) { return "$n$d" }
    }
}
function Org($nombre) {
    @{ businessName = "$nombre $marca"; cuit = (NuevoCuit); email = "$nombre-$marca@ejemplo.test".ToLower(); phone = '1100000000'
       address = 'Calle 1'; country = 'Argentina'; serviceType = 'TRANSPORTE' }
}
function Momento($horas) { (Get-Date).ToUniversalTime().AddHours($horas).ToString('yyyy-MM-ddTHH:mm:ssZ') }

'DROP DATABASE IF EXISTS ' + $copia + ';' | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d postgres' | Out-Null
'CREATE DATABASE ' + $copia + ' TEMPLATE ' + $origen + ';' | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d postgres' | Out-Null
$proc = $null
try {
    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\p1.out.log" -RedirectStandardError "$Scratch\p1.err.log"
    for ($i = 0; $i -lt 60; $i++) {
        if ($proc.HasExited) { 'backend termino'; exit 1 }
        try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { Start-Sleep 2 }
    }
    $script:token = Login $env:APP_BOOTSTRAP_ADMIN_EMAIL $env:APP_BOOTSTRAP_ADMIN_PASSWORD

    '=== R01 ABM clientes'
    $bx = Org 'ClienteBaja'
    $r = Api POST '/api/v1/clientes' $bx; Ver 'alta' 201 $r.s; $cx = $r.d
    Ver 'CUIT duplicado' 409 (Api POST '/api/v1/clientes' $bx).s
    $mal = Org 'ClienteMal'; $mal.cuit = '30000000001'
    Ver 'CUIT con digito verificador invalido' 409 (Api POST '/api/v1/clientes' $mal).s
    $mal.cuit = '123'; Ver 'CUIT mal formado' 400 (Api POST '/api/v1/clientes' $mal).s
    $bx.businessName = "ClienteEditado $marca"
    Ver 'edicion' 200 (Api PUT "/api/v1/clientes/$($cx.id)" $bx).s
    Ver 'listado por busqueda' 1 @((Api GET "/api/v1/clientes?buscar=ClienteEditado%20$marca").d.content).Count
    Ver 'baja logica' 204 (Api DELETE "/api/v1/clientes/$($cx.id)").s
    Ver 'estado despues de la baja' 'INACTIVO' (Api GET "/api/v1/clientes/$($cx.id)").d.state

    $cA = (Api POST '/api/v1/clientes' (Org 'ClienteA')).d
    $cB = (Api POST '/api/v1/clientes' (Org 'ClienteB')).d

    '=== R02 usuarios y roles'
    $roles = @((Api GET '/api/v1/admin/roles').d)
    $rolCliente = ($roles | Where-Object description -eq 'CLIENTE').id
    function Usuario($correo, $cliente) {
        Api POST '/api/v1/admin/usuarios' @{ clientId = $cliente; firstName = 'Prueba'; lastName = 'P1'; email = $correo; password = $clave; roleIds = @($rolCliente) }
    }
    $correoA = "p1-a-$marca@ejemplo.test"; $correoB = "p1-b-$marca@ejemplo.test"
    $r = Usuario $correoA $cA.id; Ver 'alta de usuario con rol' 201 $r.s; $uA = $r.d
    Ver 'email repetido' 409 (Usuario $correoA $cA.id).s
    Usuario $correoB $cB.id | Out-Null
    $tA = Login $correoA $clave; $tB = Login $correoB $clave
    Ver 'admin lista usuarios' 200 (Api GET '/api/v1/admin/usuarios').s
    Ver 'CLIENTE no lista usuarios' 403 (Api GET '/api/v1/admin/usuarios' $null $tA).s
    Ver 'CLIENTE no crea clientes' 403 (Api POST '/api/v1/clientes' (Org 'Intruso') $tA).s
    $r = Api POST '/api/v1/admin/roles' @{ description = "ROL_$marca"; level = 10; permissions = @('REPORTES_LEER') }
    Ver 'alta de rol' 201 $r.s
    Ver 'edicion de rol' 200 (Api PUT "/api/v1/admin/roles/$($r.d.id)" @{ description = "ROL2_$marca"; level = 11; permissions = @('REPORTES_LEER') }).s
    Ver 'baja de rol' 204 (Api DELETE "/api/v1/admin/roles/$($r.d.id)").s
    $correoC = "p1-c-$marca@ejemplo.test"; $uC = (Usuario $correoC $cB.id).d; $tC = Login $correoC $clave
    Ver 'usuario activo accede' 200 (Api GET '/api/v1/auth/sessions' $null $tC).s
    Ver 'bloquear usuario' 200 (Api PATCH "/api/v1/admin/usuarios/$($uC.id)/estado" @{ state = 'BLOQUEADO' }).s
    Ver 'bloquear revoca la sesion' 401 (Api GET '/api/v1/auth/sessions' $null $tC).s
    Ver 'bloqueado no puede entrar' 401 (Api POST '/api/v1/auth/login' @{ email = $correoC; password = $clave } '').s

    '=== R03 sesiones'
    $s1 = Login $correoA $clave; $s2 = Login $correoA $clave
    $vivas = @((Api GET '/api/v1/auth/sessions' $null $s1).d | Where-Object state -eq 'ACTIVA')
    Ver 'lista sesiones activas' True ($vivas.Count -ge 2)
    $ultima = ($vivas | Sort-Object startedAt | Select-Object -Last 1).id
    Ver 'revocar' 204 (Api DELETE "/api/v1/auth/sessions/$ultima" $null $s1).s
    Ver 'sesion revocada no accede' 401 (Api GET '/api/v1/auth/sessions' $null $s2).s
    Ver 'sesion activa accede' 200 (Api GET '/api/v1/auth/sessions' $null $s1).s
    Ver 'logout' 204 (Api POST '/api/v1/auth/logout' $null $s1).s
    Ver 'despues del logout no accede' 401 (Api GET '/api/v1/auth/sessions' $null $s1).s
    $s3 = Login $correoA $clave
    Sql "update tp_logistica.sesion set fecha_inicio=now()-interval '2 days', fecha_expiracion=now()-interval '1 day' where id_usuario='$($uA.id)' and estado='ACTIVA' and fecha_inicio=(select max(fecha_inicio) from tp_logistica.sesion where id_usuario='$($uA.id)');" | Out-Null
    Ver 'sesion expirada no accede' 401 (Api GET '/api/v1/auth/sessions' $null $s3).s
    $tA = Login $correoA $clave

    '=== R04 proveedores'
    $bp = Org 'Proveedor'
    $r = Api POST '/api/v1/proveedores' $bp; Ver 'alta' 201 $r.s; $p1 = $r.d
    Ver 'CUIT duplicado' 409 (Api POST '/api/v1/proveedores' $bp).s
    $bp.serviceType = 'ADUANA'; Ver 'edicion' 200 (Api PUT "/api/v1/proveedores/$($p1.id)" $bp).s
    $p2 = (Api POST '/api/v1/proveedores' (Org 'Proveedor2')).d
    $pb = (Api POST '/api/v1/proveedores' (Org 'ProveedorBaja')).d
    Ver 'baja logica' 204 (Api DELETE "/api/v1/proveedores/$($pb.id)").s
    Ver 'estado despues de la baja' 'INACTIVO' (Api GET "/api/v1/proveedores/$($pb.id)").d.state
    Ver 'CLIENTE no da de alta proveedores' 403 (Api POST '/api/v1/proveedores' (Org 'Intruso') $tA).s

    '=== R05 operadores y vehiculos'
    $r = Api POST '/api/v1/operadores' (Org 'Operador'); Ver 'alta' 201 $r.s; $o1 = $r.d
    $o2 = (Api POST '/api/v1/operadores' (Org 'Operador2')).d
    $ob = (Api POST '/api/v1/operadores' (Org 'OperadorBaja')).d
    Ver 'baja logica' 204 (Api DELETE "/api/v1/operadores/$($ob.id)").s
    function Vehiculo($op, $id) { Api POST '/api/v1/vehiculos' @{ operatorId = $op; type = 'CAMION'; identification = "$id$marca"; state = 'ACTIVO' } }
    $r = Vehiculo $o1.id 'V1'; Ver 'alta de vehiculo' 201 $r.s; $v1 = $r.d
    $v2 = (Vehiculo $o2.id 'V2').d
    Ver 'vehiculo de operador inactivo' 409 (Vehiculo $ob.id 'V3').s
    $enPg = Sql "select count(*) from tp_logistica.operador_logistico where razon_social not like '%$marca';"
    Ver 'operadores de la demo presentes en Neo4j' $enPg (Cy 'MATCH (o:Operador) RETURN count(o);')
    Ver 'tramos con operador en Neo4j' (Sql "select count(*) from tp_logistica.tramo where id_operador is not null;") (Cy 'MATCH (:Tramo)-[r:OPERADO_POR]-(:Operador) RETURN count(r);')

    '=== R06 envios, contenedores y sensores'
    function Envio($cliente) {
        Api POST '/api/v1/envios' @{ clientId = $cliente; origin = 'Buenos Aires'; destination = 'Santos'; originCountry = 'Argentina'
            destinationCountry = 'Brasil'; estimatedDelivery = (Momento 240); priority = 'NORMAL'; volumeM3 = 10.5 }
    }
    $r = Envio $cA.id; Ver 'alta de envio' 201 $r.s; $e1 = $r.d
    $e2 = (Envio $cA.id).d
    function Contenedor($c) { Api POST '/api/v1/contenedores' @{ internationalCode = "$c$marca"; type = 'DRY'; capacity = 30; state = 'DISPONIBLE' } }
    $r = Contenedor 'K1'; Ver 'alta de contenedor' 201 $r.s; $k1 = $r.d
    $r = Api POST '/api/v1/sensores' @{ type = 'TEMPERATURA'; manufacturer = 'Acme'; installationDate = (Momento -720); state = 'ACTIVO' }
    Ver 'alta de sensor' 201 $r.s; $n1 = $r.d
    Ver 'CLIENTE no ve envio ajeno' 404 (Api GET "/api/v1/envios/$($e1.id)" $null $tB).s

    '=== R07 contenedores de un envio'
    $ini = Momento -48; $fin = Momento -24
    Ver 'asociar' 201 (Api POST "/api/v1/envios/$($e1.id)/contenedores" @{ containerId = $k1.id; startAt = $ini }).s
    Ver 'mismo contenedor en otro envio, superpuesto' 409 (Api POST "/api/v1/envios/$($e2.id)/contenedores" @{ containerId = $k1.id; startAt = (Momento -30) }).s
    Ver 'desasociar' 204 (Api DELETE "/api/v1/envios/$($e1.id)/contenedores/$($k1.id)?inicio=$ini&fin=$fin").s
    $h = @((Api GET "/api/v1/envios/$($e1.id)/contenedores").d)
    Ver 'queda en el historico con fin' True ($h.Count -eq 1 -and $null -ne $h[0].endAt -and -not $h[0].active)
    Ver 'asociar despues del cierre' 201 (Api POST "/api/v1/envios/$($e1.id)/contenedores" @{ containerId = $k1.id; startAt = (Momento -12) }).s
    $h = @((Api GET "/api/v1/envios/$($e1.id)/contenedores").d)
    Ver 'una historica y una vigente' '1/1' "$(@($h | Where-Object { -not $_.active }).Count)/$(@($h | Where-Object active).Count)"

    '=== R08 R06 sensores de un contenedor'
    $ini = Momento -11; $fin = Momento -6
    $r = Api POST '/api/v1/asignaciones-sensor' @{ sensorId = $n1.id; containerId = $k1.id; startAt = $ini }
    Ver 'asociar' 201 $r.s; $as = $r.d
    Ver 'superposicion por fecha' 409 (Api POST '/api/v1/asignaciones-sensor' @{ sensorId = $n1.id; containerId = $k1.id; startAt = (Momento -9) }).s
    Ver 'desasociar' 204 (Api DELETE "/api/v1/asignaciones-sensor/$($as.assignmentId)?fin=$fin").s
    Ver 'sensor visible dentro de la vigencia' 1 @((Api GET "/api/v1/contenedores/$($k1.id)/sensores?fecha=$(Momento -8)").d).Count
    Ver 'sensor no visible fuera de la vigencia' 0 @((Api GET "/api/v1/contenedores/$($k1.id)/sensores?fecha=$(Momento -2)").d).Count
    Ver 'asociar de nuevo' 201 (Api POST '/api/v1/asignaciones-sensor' @{ sensorId = $n1.id; containerId = $k1.id; startAt = (Momento -5) }).s
    $det = (Api GET "/api/v1/envios/$($e1.id)").d
    Ver 'R06 el envio muestra contenedores' 2 @($det.containers).Count
    Ver 'R06 el envio muestra sensores' True (@($det.sensors).Count -ge 1)

    '=== R24 R04 R05 trayectos con operador y proveedor'
    function Tramo($orden, $extra = @{}) {
        $b = @{ shipmentId = $e1.id; order = $orden; origin = "Punto $orden"; destination = "Punto $($orden + 1)"; transportType = 'TERRESTRE'
                vehicleId = $v1.id; operatorId = $o1.id; departureAt = (Momento (24 * $orden)); estimatedArrivalAt = (Momento (24 * $orden + 12))
                estimatedCost = 100; state = 'PENDIENTE'; suppliers = @() }
        foreach ($k in $extra.Keys) { $b[$k] = $extra[$k] }
        $b
    }
    $r = Api POST '/api/v1/trayectos' (Tramo 2); Ver 'alta del tramo 2' 201 $r.s
    $r = Api POST '/api/v1/trayectos' (Tramo 1 @{ suppliers = @(@{ supplierId = $p1.id; service = 'ADUANA'; cost = 50 }) }); Ver 'alta del tramo 1' 201 $r.s; $t1 = $r.d
    $l = @((Api GET "/api/v1/trayectos?envioId=$($e1.id)").d)
    Ver 'tramos en secuencia' '1,2' (($l.order) -join ',')
    Ver 'R05 operador visible en el tramo' $o1.businessName $l[0].operator
    Ver 'R04 proveedor visible en el tramo' $p1.businessName $l[0].suppliers[0].supplierName
    Ver 'vehiculo de otro operador' 409 (Api POST '/api/v1/trayectos' (Tramo 3 @{ vehicleId = $v2.id })).s
    Ver 'fechas invertidas' 409 (Api POST '/api/v1/trayectos' (Tramo 3 @{ estimatedArrivalAt = (Momento -100) })).s
    Ver 'finalizado sin llegada real' 409 (Api POST '/api/v1/trayectos' (Tramo 3 @{ state = 'FINALIZADO' })).s
    Ver 'numero de tramo repetido' 409 (Api POST '/api/v1/trayectos' (Tramo 2)).s
    Ver 'proveedor inactivo' 409 (Api POST '/api/v1/trayectos' (Tramo 3 @{ suppliers = @(@{ supplierId = $pb.id; service = 'X'; cost = 1 }) })).s
    $dos = @(@{ supplierId = $p1.id; service = 'ADUANA'; cost = 50 }, @{ supplierId = $p2.id; service = 'DEPOSITO'; cost = 70 })
    Ver 'edicion con dos proveedores' 200 (Api PUT "/api/v1/trayectos/$($t1.id)" (Tramo 1 @{ suppliers = $dos })).s
    Ver 'el tramo conserva los dos proveedores' 2 @((Api GET "/api/v1/trayectos/$($t1.id)").d.suppliers).Count
    $r3 = (Api POST '/api/v1/trayectos' (Tramo 3)).d
    Ver 'baja de tramo' 204 (Api DELETE "/api/v1/trayectos/$($r3.id)").s
    Ver 'CLIENTE no crea tramos' 403 (Api POST '/api/v1/trayectos' (Tramo 4) $tA).s

    '=== R10 factura y cobros'
    function Factura($tipo, $id, $clave2, $precio) {
        Api POST '/api/v1/finanzas/facturas' @{ ownerType = $tipo; ownerId = $id; dueDate = (Get-Date).AddDays(30).ToString('yyyy-MM-dd'); idempotencyKey = $clave2
            lines = @(@{ concept = 'Servicio de prueba'; quantity = 1; unitPrice = $precio }) }
    }
    $r = Factura 'CLIENTE' $cA.id "P1-$marca-1" 1000; Ver 'factura borrador' 201 $r.s; $f1 = $r.d
    Ver 'estado inicial' 'BORRADOR' $f1.state
    $r = Api POST "/api/v1/finanzas/facturas/$($f1.id)/emitir"; Ver 'emitir' 200 $r.s
    $cta = (Api GET "/api/v1/finanzas/facturas/$($f1.id)").d.accountId
    Ver 'saldo despues de emitir' 1000 (Num (Api GET "/api/v1/finanzas/cuentas/$cta").d.balance)
    function Pago($f, $monto, $k) { Api POST "/api/v1/finanzas/facturas/$f/pagos" @{ amount = $monto; paymentMethod = 'TRANSFERENCIA'; idempotencyKey = $k } }
    $r = Pago $f1.id 400 "PG-$marca-1"; Ver 'cobro parcial' 201 $r.s
    Ver 'saldo despues del cobro parcial' 600 (Num (Api GET "/api/v1/finanzas/cuentas/$cta").d.balance)
    Ver 'estado parcial' 'PARCIAL' (Api GET "/api/v1/finanzas/facturas/$($f1.id)").d.state

    '=== R13 R10 pagos'
    Ver 'misma clave y datos no duplica' True ((Pago $f1.id 400 "PG-$marca-1").s -lt 300)
    Ver 'pagos registrados' 1 @((Api GET "/api/v1/finanzas/facturas/$($f1.id)/pagos").d).Count
    Ver 'misma clave con otro monto' 409 (Pago $f1.id 300 "PG-$marca-1").s
    Ver 'sobrepago' 409 (Pago $f1.id 9999 "PG-$marca-2").s
    Ver 'cliente ajeno no ve la factura' True ((Api GET "/api/v1/finanzas/facturas/$($f1.id)" $null $tB).s -in 403, 404)
    Ver 'pago total' 201 (Pago $f1.id 600 "PG-$marca-3").s
    $f = (Api GET "/api/v1/finanzas/facturas/$($f1.id)").d
    Ver 'R10 factura pagada' 'PAGADA' $f.state
    Ver 'R10 pendiente en cero' 0 (Num $f.pending)

    '=== R14 cuenta corriente'
    $c = (Api GET "/api/v1/finanzas/cuentas/$cta").d
    Ver 'saldo igual al calculado' (Num $c.calculatedBalance) (Num $c.balance)
    Ver 'conciliada' True $c.reconciled
    $s = 0; foreach ($m in $c.movements) { if ($m.type -eq 'CREDITO' -or $m.movementType -eq 'CREDITO') { $s -= $m.amount } else { $s += $m.amount } }
    Ver 'saldo igual a la suma de movimientos' (Num $c.balance) (Num $s)
    Ver 'movimientos' 3 @($c.movements).Count
    Ver 'desconciliadas en toda la base' 0 (Sql "select count(*) from tp_logistica.v_conciliacion_cuentas where saldo_conciliado is not true;")

    '=== R15 cierre mensual'
    $f2 = (Factura 'CLIENTE' $cA.id "P1-$marca-2" 250).d
    Api POST "/api/v1/finanzas/facturas/$($f2.id)/emitir" | Out-Null
    $periodo = (Get-Date).ToUniversalTime().ToString('yyyy-MM-01')
    $r = Api POST "/api/v1/finanzas/cuentas/$cta/cierres" @{ period = $periodo }; Ver 'cierre' 200 $r.s
    Ver 'el cierre reproduce el saldo' (Num (Api GET "/api/v1/finanzas/cuentas/$cta").d.balance) (Num $r.d.finalBalance)
    Ver 'repetir el periodo' 409 (Api POST "/api/v1/finanzas/cuentas/$cta/cierres" @{ period = $periodo }).s
    $anterior = (Get-Date).ToUniversalTime().AddMonths(-1).ToString('yyyy-MM-01')
    Ver 'cierre retroactivo' 409 (Api POST "/api/v1/finanzas/cuentas/$cta/cierres" @{ period = $anterior }).s
    Ver 'dia que no es primero de mes' 409 (Api POST "/api/v1/finanzas/cuentas/$cta/cierres" @{ period = (Get-Date).ToUniversalTime().ToString('yyyy-MM-15') }).s
    Ver 'cierres registrados' 1 @((Api GET "/api/v1/finanzas/cuentas/$cta/cierres").d).Count

    '=== R16 terceros'
    foreach ($t in @(@('PROVEEDOR', $p1.id), @('OPERADOR', $o1.id))) {
        $r = Factura $t[0] $t[1] "P1-$marca-$($t[0])" 500; Ver "$($t[0]) factura" 201 $r.s
        Api POST "/api/v1/finanzas/facturas/$($r.d.id)/emitir" | Out-Null
        $x = (Api GET "/api/v1/finanzas/facturas/$($r.d.id)").d
        Ver "$($t[0]) naturaleza a pagar" 'PAGAR' $x.nature
        Pago $x.id 200 "PG-$marca-$($t[0])" | Out-Null
        $ct = (Api GET "/api/v1/finanzas/cuentas/$($x.accountId)").d
        Ver "$($t[0]) cuenta del tercero" $t[1] $ct.ownerId
        Ver "$($t[0]) saldo refleja factura y pago" 300 (Num $ct.balance)
    }

    '=== R12 facturacion automatica'
    $tarifa = (@((Api GET '/api/v1/finanzas/tarifas').d) | Where-Object serviceType -eq 'TRANSPORTE' | Select-Object -First 1).id
    Ver 'transicion de estado invalida' 409 (Api PATCH "/api/v1/envios/$($e2.id)/estado" @{ state = 'EN_TRANSITO' }).s
    $e3 = (Envio $cB.id).d
    foreach ($x in @($e2.id, $e3.id)) {
        Api PATCH "/api/v1/envios/$x/estado" @{ state = 'EN_PREPARACION' } | Out-Null
        Api PATCH "/api/v1/envios/$x/estado" @{ state = 'EN_TRANSITO' } | Out-Null
    }
    $r = Api POST '/api/v1/finanzas/facturacion-automatica' @{ tariffId = $tarifa }; Ver 'primera corrida' 200 $r.s
    Ver 'crea facturas' True ($r.d.created -ge 1)
    Ver 'saltea la cuenta con el mes cerrado' 1 $r.d.skippedClosed
    Ver 'el envio de la cuenta cerrada no se factura' 0 (Sql "select count(*) from tp_logistica.factura where id_envio='$($e2.id)';")
    $antes = Sql 'select count(*) from tp_logistica.factura;'
    $r = Api POST '/api/v1/finanzas/facturacion-automatica' @{ tariffId = $tarifa }
    Ver 'segunda corrida no crea nada' 0 $r.d.created
    Ver 'misma cantidad de facturas' $antes (Sql 'select count(*) from tp_logistica.factura;')
    Ver 'una factura por envio' 1 (Sql "select count(*) from tp_logistica.factura where id_envio='$($e3.id)';")

    '=== R15 cierre periodico de todas las cuentas'
    $total = Sql 'select count(*) from tp_logistica.cuenta_corriente;'
    $r = Api POST '/api/v1/finanzas/cierre-mensual' @{ period = $anterior }; Ver 'cierre de todas' 200 $r.s
    Ver 'cerradas mas salteadas igual a cuentas' $total ($r.d.closed + $r.d.skipped)
    Ver 'la cuenta ya cerrada despues se saltea' True ($r.d.skipped -ge 1)
    Ver 'repetir no cierra nada' 0 (Api POST '/api/v1/finanzas/cierre-mensual' @{ period = $anterior }).d.closed
    Ver 'CLIENTE no cierra cuentas' 403 (Api POST '/api/v1/finanzas/cierre-mensual' @{ period = $anterior } $tA).s

    '=== R07 historial de movimientos de un contenedor'
    Ver 'dos pasos por el mismo envio' 2 @((Api GET "/api/v1/contenedores/$($k1.id)/historial").d).Count
    Ver 'el duenio ve su historial' 2 @((Api GET "/api/v1/contenedores/$($k1.id)/historial" $null $tA).d).Count
    Ver 'otro cliente no ve nada' 0 @((Api GET "/api/v1/contenedores/$($k1.id)/historial" $null $tB).d | Where-Object { $_ }).Count

    '=== R35 reporte de envios'
    $dato = Sql "select id_cliente from tp_logistica.envio where estado='ENTREGADO' group by 1 order by count(*) desc limit 1;"
    Ver 'filtro por cliente' (Sql "select count(*) from tp_logistica.envio where id_cliente='$dato';") @((Api GET "/api/v1/reportes/envios?clienteId=$dato").d).Count
    Ver 'filtro por estado' (Sql "select count(*) from tp_logistica.envio where estado='ENTREGADO';") @((Api GET '/api/v1/reportes/envios?estado=ENTREGADO').d).Count
    Ver 'filtro por pais' (Sql "select count(*) from tp_logistica.envio where lower(pais_destino)='brasil';") @((Api GET '/api/v1/reportes/envios?pais=Brasil').d).Count
    Ver 'cliente y estado combinados' (Sql "select count(*) from tp_logistica.envio where id_cliente='$dato' and estado='ENTREGADO';") @((Api GET "/api/v1/reportes/envios?clienteId=$dato&estado=ENTREGADO").d).Count
    Ver 'demorados' (Sql "select count(*) from tp_logistica.envio where estado not in ('ENTREGADO','CANCELADO') and fecha_entrega_estimada<now();") @((Api GET '/api/v1/reportes/envios?demorado=true').d).Count
    Ver 'incidentes abiertos' (Sql "select count(distinct e.id_envio) from tp_logistica.envio e join tp_logistica.incidente i using(id_envio) where i.estado<>'CERRADO';") @((Api GET '/api/v1/reportes/envios?incidentesAbiertos=true').d).Count
    Ver 'CLIENTE ve solo lo suyo' 2 @((Api GET '/api/v1/reportes/envios' $null $tA).d).Count
    Ver 'CLIENTE filtra su propio cliente' 2 @((Api GET "/api/v1/reportes/envios?clienteId=$($cA.id)" $null $tA).d).Count
    Ver 'CLIENTE no pide otro cliente' 403 (Api GET "/api/v1/reportes/envios?clienteId=$($cB.id)" $null $tA).s

    '=== R36 maximo y minimo por cliente'
    $fila = @((Api GET "/api/v1/reportes/tiempos-entrega-clientes?clienteId=$dato").d)[0]
    $m = (Sql "select count(*), round(min(extract(epoch from fecha_entrega_real-fecha_creacion)/3600),2), round(max(extract(epoch from fecha_entrega_real-fecha_creacion)/3600),2) from tp_logistica.envio where id_cliente='$dato' and estado='ENTREGADO' and fecha_entrega_real is not null;") -split '\|'
    Ver 'entregados' $m[0] $fila.deliveredShipments
    Ver 'minimo' (Num ([decimal]::Parse($m[1], [Globalization.CultureInfo]::InvariantCulture))) (Num $fila.minimumHours)
    Ver 'maximo' (Num ([decimal]::Parse($m[2], [Globalization.CultureInfo]::InvariantCulture))) (Num $fila.maximumHours)
    Ver 'cliente sin entregas no aparece' 0 @((Api GET "/api/v1/reportes/tiempos-entrega-clientes?clienteId=$($cA.id)").d).Count

    '=== R37 promedio de incidentes'
    $g = @((Api GET '/api/v1/reportes/promedio-incidentes').d)
    Ver 'envios del denominador' (Sql "select count(*) from tp_logistica.envio where estado<>'CANCELADO';") (($g | Measure-Object shipments -Sum).Sum)
    Ver 'incidentes del numerador' (Sql "select count(*) from tp_logistica.incidente i join tp_logistica.envio e using(id_envio) where e.estado<>'CANCELADO';") (($g | Measure-Object incidents -Sum).Sum)
    $x = $g | Sort-Object incidents -Descending | Select-Object -First 1
    Ver 'promedio del grupo mayor' (Num ([decimal]$x.incidents / $x.shipments)) (Num $x.incidentsPerShipment)

    '=== R38 tiempo promedio de entrega'
    $a = (Api GET '/api/v1/reportes/tiempo-promedio-entrega').d
    $m = (Sql "select count(*), round(avg(extract(epoch from fecha_entrega_real-fecha_creacion)/3600),2) from tp_logistica.envio where estado='ENTREGADO' and fecha_entrega_real is not null;") -split '\|'
    Ver 'entregados' $m[0] $a.deliveredShipments
    Ver 'promedio manual' (Num ([decimal]::Parse($m[1], [Globalization.CultureInfo]::InvariantCulture))) (Num $a.averageHours)

    '=== R39 costos por transporte'
    foreach ($fila in @((Api GET '/api/v1/reportes/costos-transporte').d)) {
        $m = Sql "select coalesce(sum(costo_real),0) from tp_logistica.tramo where medio_transporte='$($fila.transportType)';"
        Ver "total $($fila.transportType)" (Num ([decimal]::Parse($m, [Globalization.CultureInfo]::InvariantCulture))) (Num $fila.totalActualCost)
    }
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force; Start-Sleep 2 }
    'DROP DATABASE IF EXISTS ' + $copia + ';' | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d postgres' | Out-Null
    "copia de la base borrada: $copia"
}
