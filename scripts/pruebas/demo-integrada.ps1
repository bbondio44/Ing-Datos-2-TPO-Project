<#
  Recorrido continuo de la defensa contra las cuatro bases, sin pasos manuales:
  crear envio -> asignar contenedor y sensor -> lecturas IoT/GPS -> historico y recorrido -> temperatura
  fuera de rango -> alerta e incidente -> evento trazado -> grafo -> reporte -> factura -> pago -> cierre.
  Usa un cliente propio, marcado "Demo integrada". Con el backend apagado y mvn -B -DskipTests install hecho.
#>
param([string]$Scratch = $env:TEMP)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres); $NJ = (docker ps -q --filter name=neo4j); $MO = (docker ps -q --filter name=mongo)
$base = 'http://localhost:8080'
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'
Remove-Item Env:PG_JDBC_URL -ErrorAction SilentlyContinue
$marca = [guid]::NewGuid().ToString('N').Substring(0, 6)
$env:APP_BOOTSTRAP_ADMIN_EMAIL = "smoke-demo-$marca@ejemplo.test"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')

function Sql($q) { ($q | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At') }
function Cy($q) {
    [IO.File]::WriteAllText("$Scratch\q.cypher", $q, (New-Object Text.UTF8Encoding $false))
    docker cp "$Scratch\q.cypher" "${NJ}:/tmp/q.cypher" 2>$null | Out-Null
    docker exec $NJ sh -c 'cypher-shell -u ${NEO4J_AUTH%%/*} -p ${NEO4J_AUTH#*/} --format plain -f /tmp/q.cypher' 2>$null | Select-Object -Skip 1
}
function Ver($t, $e, $o) { "$(if ("$e" -eq "$o") { 'OK   ' } else { 'FALLA' }) ${t}: esperado=$e obtenido=$o" }
function Num($v) { [double][math]::Round([decimal]$v, 2) }
function Api($m, $p, $b = $null) {
    $q = @{ Uri = "$base$p"; Method = $m; UseBasicParsing = $true; TimeoutSec = 30; Headers = @{ Authorization = "Bearer $script:token" } }
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
function NuevoCuit {
    $pesos = 5, 4, 3, 2, 7, 6, 5, 4, 3, 2
    while ($true) {
        $n = '30' + (Get-Random -Minimum 10000000 -Maximum 99999999)
        $s = 0; for ($i = 0; $i -lt 10; $i++) { $s += [int]"$($n[$i])" * $pesos[$i] }
        $d = 11 - ($s % 11); if ($d -eq 11) { $d = 0 }
        if ($d -lt 10) { return "$n$d" }
    }
}
function Fecha($t) { $t.ToString('yyyy-MM-ddTHH:mm:ss.fffZ') }
function Lectura($t, $extra) {
    $b = @{ idLectura = "$([guid]::NewGuid())"; idSensor = "$($script:sensor.id)"; timestamp = (Fecha $t) }
    foreach ($k in $extra.Keys) { $b[$k] = $extra[$k] }
    $b
}

$proc = $null
try {
    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\demo.out.log" -RedirectStandardError "$Scratch\demo.err.log"
    for ($i = 0; $i -lt 60; $i++) {
        if ($proc.HasExited) { 'backend termino'; exit 1 }
        try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { Start-Sleep 2 }
    }
    $login = @{ email = $env:APP_BOOTSTRAP_ADMIN_EMAIL; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json
    $script:token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login).token
    $ahora = (Get-Date).ToUniversalTime()

    '=== Demo 1 crear envio, contenedor y sensor'
    $cliente = (Api POST '/api/v1/clientes' @{ businessName = "Demo integrada $marca"; cuit = (NuevoCuit); email = "demo-$marca@ejemplo.test"
        phone = '1100000000'; address = 'Calle 1'; country = 'Argentina' }).d
    $r = Api POST '/api/v1/envios' @{ clientId = $cliente.id; origin = 'Buenos Aires'; destination = 'Santos'; originCountry = 'Argentina'
        destinationCountry = 'Brasil'; estimatedDelivery = (Fecha $ahora.AddDays(10)); priority = 'ALTA'; volumeM3 = 22.5 }
    Ver 'envio creado' 201 $r.s; $envio = $r.d
    $cont = (Api POST '/api/v1/contenedores' @{ internationalCode = "DEMO$marca"; type = 'REEFER'; capacity = 30; state = 'DISPONIBLE' }).d
    $script:sensor = (Api POST '/api/v1/sensores' @{ type = 'MULTISENSOR'; manufacturer = 'Acme'; installationDate = (Fecha $ahora.AddDays(-30)); state = 'ACTIVO' }).d
    $inicio = $ahora.AddHours(-2)
    Ver 'contenedor asignado' 201 (Api POST "/api/v1/envios/$($envio.id)/contenedores" @{ containerId = $cont.id; startAt = (Fecha $inicio) }).s
    Ver 'sensor asignado' 201 (Api POST '/api/v1/asignaciones-sensor' @{ sensorId = $sensor.id; containerId = $cont.id; startAt = (Fecha $inicio) }).s
    foreach ($estado in 'EN_PREPARACION', 'EN_TRANSITO') { Ver "envio pasa a $estado" 200 (Api PATCH "/api/v1/envios/$($envio.id)/estado" @{ state = $estado }).s }

    '=== R18 R20 R21 R22 lecturas con GPS'
    $puntos = @(@(-34.60, -58.37), @(-34.20, -57.90), @(-33.90, -56.50), @(-33.20, -54.90), @(-32.50, -53.30))
    for ($i = 0; $i -lt 5; $i++) {
        Ver "lectura $i" 201 (Api POST '/api/v1/iot/lecturas' (Lectura $ahora.AddMinutes(-60 + $i * 10) @{ temperatura = 5.0 + $i * 0.1; humedad = 60.0; bateria = 80.0 - $i; latitud = $puntos[$i][0]; longitud = $puntos[$i][1] })).s
    }
    $rango = "?desde=$(Fecha $inicio)&hasta=$(Fecha $ahora.AddMinutes(1))"
    Ver 'R19 historico del sensor' 5 @((Api GET "/api/v1/iot/sensores/$($sensor.id)/lecturas$rango").d).Count
    Ver 'R19 historico del contenedor' 5 @((Api GET "/api/v1/iot/contenedores/$($cont.id)/lecturas$rango").d).Count
    $rec = @((Api GET "/api/v1/iot/contenedores/$($cont.id)/recorrido$rango").d)
    Ver 'R20 cantidad de posiciones' 5 $rec.Count
    Ver 'R20 orden cronologico y mismos puntos' (($puntos | ForEach-Object { $_[0] }) -join ',') (($rec.latitud) -join ',')
    Ver 'R21 ultima posicion' -32.5 (Api GET "/api/v1/iot/contenedores/$($cont.id)/ultima-posicion").d.latitud
    Ver 'R20 lectura sin GPS' 201 (Api POST '/api/v1/iot/lecturas' (Lectura $ahora.AddMinutes(-5) @{ temperatura = 5.5; bateria = 75.0 })).s
    Ver 'R21 sin GPS no mueve la ultima posicion' -32.5 (Api GET "/api/v1/iot/contenedores/$($cont.id)/ultima-posicion").d.latitud
    Ver 'R20 lectura tardia' 201 (Api POST '/api/v1/iot/lecturas' (Lectura $ahora.AddMinutes(-70) @{ temperatura = 5.0; latitud = -34.70; longitud = -58.40 })).s
    Ver 'R21 la tardia no pisa la ultima posicion' -32.5 (Api GET "/api/v1/iot/contenedores/$($cont.id)/ultima-posicion").d.latitud
    Ver 'R20 la tardia queda primera en el recorrido' -34.7 @((Api GET "/api/v1/iot/contenedores/$($cont.id)/recorrido$rango").d)[0].latitud
    $est = @((Api GET '/api/v1/iot/sensores/estado').d) | Where-Object idSensor -eq $sensor.id
    Ver 'R22 sensor en linea' 'ONLINE' $est.conexion
    Ver 'R22 bateria de la ultima lectura' 75 (Num $est.bateria)
    Ver 'R22 contenedor del sensor' $cont.id $est.idContenedor

    '=== R09 R25 R26 temperatura fuera de rango'
    $regla = (Api POST '/api/v1/reglas' @{ idSensor = $sensor.id; nombre = "Demo $marca"; tipoRegla = 'TEMPERATURA'; operador = '>'; umbral = 8.0; severidad = 'ALTA'; activa = $true }).d
    $lectura = Lectura $ahora.AddMinutes(-1) @{ temperatura = 14.0 }
    Ver 'lectura fuera de rango' 201 (Api POST '/api/v1/iot/lecturas' $lectura).s
    $alerta = @((Api GET '/api/v1/alertas').d) | Where-Object { $_.idRegla -eq $regla.idRegla }
    Ver 'alerta generada' 'ABIERTA' $alerta.estado
    Ver 'con la lectura que la causo' $lectura.idLectura $alerta.idLectura
    Ver 'incidente en PostgreSQL' 1 (Sql "select count(*) from tp_logistica.incidente where id_envio='$($envio.id)' and id_incidente='$($alerta.idIncidente)';")

    '=== R17 monitoreo integrado'
    $m = (Api GET "/api/v1/monitoreo/envios/$($envio.id)").d
    Ver 'PostgreSQL: estado del envio' 'EN_TRANSITO' $m.estado
    Ver 'Cassandra: ultima lectura del sensor' $lectura.idLectura @($m.ultimasLecturas)[0].idLectura
    Ver 'Cassandra: ultima posicion del contenedor' -32.5 @($m.ultimasPosiciones)[0].latitud
    Ver 'MongoDB: evento de la alerta' 1 @($m.eventos | Where-Object { $_.idAlerta -eq $alerta.id }).Count
    Ver 'MongoDB: alerta' 1 @($m.alertas | Where-Object { $_.id -eq $alerta.id }).Count
    Ver 'PostgreSQL: incidente de la alerta' 1 @($m.incidentes | Where-Object { $_.idIncidente -eq $alerta.idIncidente -or $_.id -eq $alerta.idIncidente }).Count
    Ver 'R26 reconocer' 204 (Api POST "/api/v1/alertas/$($alerta.id)/reconocer" @{ comentario = 'demo' }).s
    Ver 'R26 cerrar' 204 (Api POST "/api/v1/alertas/$($alerta.id)/cerrar" @{ comentario = 'demo' }).s

    '=== R17 R24 trazabilidad de los cambios de estado'
    $tramo = @{ shipmentId = $envio.id; order = 1; origin = 'Buenos Aires'; destination = 'Santos'; transportType = 'MARITIMO'
        departureAt = (Fecha $ahora.AddHours(-1)); estimatedArrivalAt = (Fecha $ahora.AddDays(5)); estimatedCost = 100; state = 'PENDIENTE'; suppliers = @() }
    $t = Api POST '/api/v1/trayectos' $tramo; Ver 'tramo creado' 201 $t.s
    $tramo.state = 'EN_CURSO'
    Ver 'tramo en curso' 200 (Api PUT "/api/v1/trayectos/$($t.d.id)" $tramo).s
    Ver 'guardar de nuevo el tramo' 200 (Api PUT "/api/v1/trayectos/$($t.d.id)" $tramo).s
    $tipos = @(); for ($i = 0; $i -lt 15; $i++) {
        $tipos = @((Api GET "/api/v1/monitoreo/envios/$($envio.id)").d.eventos.tipo)
        if ($tipos -contains 'SALIDA') { break }; Start-Sleep 2
    }
    foreach ($tipo in 'CARGA_INICIADA', 'EN_TRANSITO', 'SALIDA') { Ver "evento $tipo una sola vez" 1 @($tipos | Where-Object { $_ -eq $tipo }).Count }

    '=== R29 R34 riesgos de la prediccion'
    Ver 'generar predicciones' 200 (Api POST '/api/v1/analitica/predicciones/generar').s
    $p = @((Api GET '/api/v1/analitica/predicciones').d) | Where-Object id_envio -eq $envio.id
    Ver 'riesgo climatico por medio maritimo' 0.5 (Num $p.riesgo_climatico)
    $abiertos = [int](Sql "select count(*) from tp_logistica.incidente where id_envio='$($envio.id)' and estado<>'CERRADO';")
    $tasa = [double]::Parse((Sql "select coalesce(count(i.id_incidente)::numeric/nullif(count(distinct e.id_envio),0),0) from tp_logistica.envio e left join tp_logistica.incidente i on i.id_envio=e.id_envio where e.pais_origen='Argentina' and e.pais_destino='Brasil' and e.id_envio<>'$($envio.id)' and e.estado<>'CANCELADO';"), [Globalization.CultureInfo]::InvariantCulture)
    Ver 'riesgo operativo contra calculo manual' ([math]::Round(0.6 * [math]::Min($abiertos, 3) / 3 + 0.4 * [math]::Min($tasa, 1), 2)) (Num $p.riesgo_operativo)
    Ver 'todas las predicciones tienen riesgos' 0 @(@((Api GET '/api/v1/analitica/predicciones').d) | Where-Object { $null -eq $_.riesgo_operativo -or $null -eq $_.riesgo_climatico }).Count
    $rp = (Api POST '/api/v1/reportes' @{ tipo = 'PREDICTIVO'; idCliente = $cliente.id }).d
    $res = @((Api GET "/api/v1/reportes/$($rp.idSolicitud)/resultado").d)
    Ver 'R34 el reporte incluye los riesgos' (Num $p.riesgo_operativo) (Num $res[0].riesgo_operativo)

    '=== R30 sincronizacion con el grafo'
    $n = 0; for ($i = 0; $i -lt 15; $i++) { $n = Cy "MATCH (:Cliente {id_cliente: '$($cliente.id)'})-[:SOLICITA]->(e:Envio {id_envio: '$($envio.id)'}) RETURN count(e);"; if ("$n" -eq '1') { break }; Start-Sleep 2 }
    Ver 'cliente y envio nuevos en Neo4j' 1 $n

    '=== R11 R33 reporte facturable, factura y cierre'
    $rep = (Api POST '/api/v1/reportes' @{ tipo = 'DESEMPENO'; idCliente = $cliente.id; desde = '2026-09-01'; hasta = '2026-11-30' }).d
    Ver 'reporte generado' 'GENERADO' $rep.estado
    $f = Api POST "/api/v1/reportes/$($rep.idSolicitud)/facturar"; Ver 'reporte facturado' 200 $f.s
    $factura = (Api GET "/api/v1/finanzas/facturas/$($f.d.idFactura)").d
    Ver 'factura a nombre del cliente' $cliente.id $factura.ownerId
    Ver 'pago total' 201 (Api POST "/api/v1/finanzas/facturas/$($factura.id)/pagos" @{ amount = $factura.pending; paymentMethod = 'TRANSFERENCIA'; idempotencyKey = "DEMO-$marca" }).s
    $cuenta = (Api GET "/api/v1/finanzas/cuentas/$($factura.accountId)").d
    Ver 'saldo en cero' 0 (Num $cuenta.balance)
    $c = Api POST "/api/v1/finanzas/cuentas/$($cuenta.id)/cierres" @{ period = $ahora.ToString('yyyy-MM-01') }
    Ver 'cierre del mes' 200 $c.s
    Ver 'debitos del cierre igual al total de la factura' (Num $factura.total) (Num $c.d.debits)

    '=== R25 integridad entre MongoDB y PostgreSQL'
    [IO.File]::WriteAllText("$Scratch\ids.js", 'var s={}; db.alertas.distinct("id_incidente",{id_incidente:{$ne:null}}).concat(db.eventos_logisticos.distinct("id_incidente",{id_incidente:{$ne:null}})).forEach(function(x){s[x]=1}); print(Object.keys(s).join(","));', (New-Object Text.UTF8Encoding $false))
    docker cp "$Scratch\ids.js" "${MO}:/tmp/ids.js" 2>$null | Out-Null
    $ids = @((docker exec $MO sh -c 'mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin logistica_docs /tmp/ids.js') -split ',' | Where-Object { $_ })
    $lista = ($ids | ForEach-Object { "'$_'" }) -join ','
    Ver 'incidentes referenciados desde Mongo que existen en PostgreSQL' $ids.Count (Sql "select count(*) from tp_logistica.incidente where id_incidente in ($lista);")

    '=== R27 R28 R31 analitica contra calculo manual'
    $rutas = @((Api GET '/api/v1/analitica/ranking-rutas').d)
    Ver 'R27 orden descendente' (($rutas.cantidad_incidentes | Sort-Object -Descending) -join ',') (($rutas.cantidad_incidentes) -join ',')
    $p = $rutas[0]
    Ver 'R27 primera ruta contra incidentes' (Sql "select count(*) from tp_logistica.incidente i join tp_logistica.tramo t using(id_tramo) where t.origen='$($p.origen)' and t.destino='$($p.destino)' and t.medio_transporte='$($p.medio_transporte)' and t.estado<>'CANCELADO';") $p.cantidad_incidentes
    $clientes = @((Api GET '/api/v1/analitica/ranking-clientes').d)
    $u = $clientes | Where-Object id_cliente -eq $cliente.id
    Ver 'R28 volumen del cliente de la demo' 22.5 (Num $u.volumen_total_m3)
    $d = @((Api GET '/api/v1/analitica/desempeno').d)
    Ver 'R31 hay dos periodos distintos para comparar' True (@($d.envios_creados | Select-Object -Unique).Count -ge 2)
    $mes = $ahora.ToString('yyyy-MM-01')
    Ver 'R31 envios creados del mes' (Sql "select count(*) from tp_logistica.envio where date_trunc('month',fecha_creacion at time zone 'UTC')='$mes';") (($d | Where-Object { "$($_.periodo)".StartsWith($mes) }).envios_creados)
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force; Start-Sleep 2 }
    # Su reporte lo referencia: no se puede borrar, queda inactivo.
    "update tp_logistica.usuario set estado='INACTIVO' where email='$($env:APP_BOOTSTRAP_ADMIN_EMAIL)';" | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At' | Out-Null
    'usuario temporal desactivado'
}
