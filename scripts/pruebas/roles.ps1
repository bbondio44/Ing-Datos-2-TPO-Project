param([string]$Scratch)
# Comprueba con un usuario de rol CLIENTE real que cada uno ve solo lo suyo. Usa el cliente C02.
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres); $base = 'http://localhost:8080'
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$correo = "prueba-rol-$([guid]::NewGuid().ToString('N').Substring(0,8))@ejemplo.test"
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'; $env:APP_BOOTSTRAP_ADMIN_EMAIL = $correo; $env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
function Sql($q) { ($q | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At') }
function Ver($t, $e, $o) { "$(if ("$e" -eq "$o") { 'OK   ' } else { 'FALLA' }) ${t}: esperado=$e obtenido=$o" }
function Autorizacion { @{ Authorization = "Bearer $script:token" } }
function Lista($p) { $r = Invoke-RestMethod "$base$p" -Headers (Autorizacion) -TimeoutSec 30; ,@($r | ForEach-Object { $_ }) }
function Req($m, $p, $b) { try { $a = @{ Uri = "$base$p"; Method = $m; Headers = (Autorizacion); UseBasicParsing = $true; TimeoutSec = 30; ContentType = 'application/json' }; if ($b -ne $null) { $a.Body = ($b | ConvertTo-Json -Depth 5) }; [int](Invoke-WebRequest @a).StatusCode } catch { if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 'sin respuesta' } } }
$cat = Import-Csv tp-grupo4-mongo-cassandra/catalogo_ids.csv
function Id($c) { ($cat | Where-Object codigo -eq $c).uuid }
$c02 = Id 'C02'; $e002 = Id 'E002'; $e001 = Id 'E001'
$rango = '?desde=2026-10-20T00:00:00Z&hasta=2026-10-22T23:00:00Z'
$propio = Id 'MSKU0000002'; $ajeno = Id 'MSKU0000001'
$proc = $null
try {
    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\roles.out.log" -RedirectStandardError "$Scratch\roles.err.log"
    for ($i = 0; $i -lt 45; $i++) { if ($proc.HasExited) { 'backend termino'; exit 1 }; try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { if ($_.Exception.Response) { break } }; Start-Sleep -Seconds 2 }
    # El administrador inicial se crea apenas despues de que responde el health.
    Start-Sleep 6
    $script:token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body (@{ email = $correo; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json)).token

    Sql "update tp_logistica.usuario set id_cliente='$c02' where email='$correo'; delete from tp_logistica.usuario_rol where id_usuario=(select id_usuario from tp_logistica.usuario where email='$correo'); insert into tp_logistica.usuario_rol(id_usuario,id_rol) select u.id_usuario, r.id_rol from tp_logistica.usuario u, tp_logistica.rol r where u.email='$correo' and r.descripcion='CLIENTE';" | Out-Null

    '=== R40 R17 R26 usuario con rol CLIENTE: ve lo suyo'
    $alertas = Lista '/api/v1/alertas'
    Ver 'todas sus alertas son suyas' 0 @($alertas | Where-Object { $_.idCliente -ne $c02 }).Count
    "     alertas visibles: $($alertas.Count)"
    Ver 'monitoreo de su envio' 200 (Req GET "/api/v1/monitoreo/envios/$e002" $null)
    Ver 'su red en el grafo' 200 (Req GET "/api/v1/grafo/clientes/$c02/red" $null)
    Ver 'sus predicciones' 200 (Req GET '/api/v1/analitica/predicciones' $null)
    Ver 'su desempeno' 200 (Req GET '/api/v1/analitica/desempeno' $null)
    Ver 'pedir un reporte propio' 201 (Req POST '/api/v1/reportes' @{ tipo = 'DESEMPENO' })
    "     sensores visibles: $((Lista '/api/v1/iot/sensores/estado').Count)"

    '=== R19 R30 usuario con rol CLIENTE: no ve lo ajeno'
    Ver 'envio de otro cliente' 404 (Req GET "/api/v1/monitoreo/envios/$e001" $null)
    Ver 'grafo de otro cliente' 404 (Req GET "/api/v1/grafo/clientes/$(Id 'C01')/red" $null)
    Ver 'contenedor de otro cliente' 404 (Req GET "/api/v1/iot/contenedores/$ajeno/recorrido$rango" $null)
    Ver 'ranking global' 403 (Req GET '/api/v1/analitica/ranking-clientes' $null)
    Ver 'crear una regla' 403 (Req POST '/api/v1/reglas' @{ nombre = 'x'; tipoRegla = 'HUMEDAD'; operador = '>'; umbral = 90.0; severidad = 'BAJA'; activa = $true })
    Ver 'registrar una lectura' 403 (Req POST '/api/v1/iot/lecturas' @{ idLectura = "$([guid]::NewGuid())"; idSensor = "$([guid]::NewGuid())"; timestamp = '2026-10-21T00:00:00Z'; temperatura = 5.0 })
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
    Sql "update tp_logistica.usuario set estado='INACTIVO' where email='$correo';" | Out-Null
}
