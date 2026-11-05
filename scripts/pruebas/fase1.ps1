param([string]$Scratch)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -aq --filter name=postgres); $CA = (docker ps -aq --filter name=cassandra); $NJ = (docker ps -aq --filter name=neo4j); $MO = (docker ps -aq --filter name=mongo)
$base = 'http://localhost:8080'
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'smoke-f1@ejemplo.test'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
function Ver($t, $e, $o) { "$(if ("$e" -eq "$o") { 'OK   ' } else { 'FALLA' }) ${t}: esperado=$e obtenido=$o" }
function Get1($path) { $r = Invoke-RestMethod "$base$path" -Headers @{ Authorization = "Bearer $script:token" } -TimeoutSec 30; $r | ForEach-Object { $_ } }
function Status($path) { try { [int](Invoke-WebRequest "$base$path" -Headers @{ Authorization = "Bearer $script:token" } -UseBasicParsing -TimeoutSec 30).StatusCode } catch { [int]$_.Exception.Response.StatusCode } }
$proc = $null
try {
    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\f1.out.log" -RedirectStandardError "$Scratch\f1.err.log"
    for ($i = 0; $i -lt 45; $i++) { if ($proc.HasExited) { 'backend termino'; exit 1 }; try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { if ($_.Exception.Response) { break } }; Start-Sleep -Seconds 2 }
    $login = @{ email = $env:APP_BOOTSTRAP_ADMIN_EMAIL; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json
    $script:token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login).token

    $sen = '101231a8-af8f-53ec-bfb0-f4d579ba7455'; $con = '42d867d9-d7a4-5248-9681-33e2c0cdb123'
    $rango = '?desde=2026-10-20T22:00:00Z&hasta=2026-10-21T02:00:00Z'
    '=== R19 historico cruzando medianoche'
    $l = @(Get1 "/api/v1/iot/sensores/$sen/lecturas$rango")
    Ver 'lecturas de SEN-001' 5 $l.Count
    "     primera=$($l[0].fechaHora) ultima=$($l[-1].fechaHora)"
    $ordenado = ($l.fechaHora -join '|') -eq (($l.fechaHora | Sort-Object) -join '|')
    Ver 'ordenadas por fecha' True $ordenado
    $c = @(Get1 "/api/v1/iot/contenedores/$con/lecturas$rango")
    Ver 'lecturas del contenedor (2 sensores)' 10 $c.Count
    Ver 'rango invertido' 400 (Status "/api/v1/iot/sensores/$sen/lecturas?desde=2026-10-21T02:00:00Z&hasta=2026-10-20T22:00:00Z")
    Ver 'rango de mas de 31 dias' 400 (Status "/api/v1/iot/sensores/$sen/lecturas?desde=2026-01-01T00:00:00Z&hasta=2026-10-20T22:00:00Z")
    '=== R20 recorrido'
    $r = @(Get1 "/api/v1/iot/contenedores/$con/recorrido$rango")
    Ver 'posiciones (solo con GPS)' 5 $r.Count
    "     primera=($($r[0].latitud),$($r[0].longitud)) ultima=($($r[-1].latitud),$($r[-1].longitud))"
    '=== R21 ultima posicion'
    $u = Get1 "/api/v1/iot/contenedores/$con/ultima-posicion"
    "     fecha=$($u.fechaHora) lat=$($u.latitud) lon=$($u.longitud)"
    Ver 'contenedor sin posicion' 404 (Status "/api/v1/iot/contenedores/$([guid]::NewGuid())/ultima-posicion")
    '=== R22 estado de sensores'
    $e = @(Get1 '/api/v1/iot/sensores/estado')
    Ver 'sensores en la lista' 43 $e.Count
    "     $(($e | Group-Object conexion | ForEach-Object { "$($_.Name)=$($_.Count)" }) -join ' ')"
    Ver 'SEN-040 nunca reporto' 'SIN_DATOS' (($e | Where-Object { $_.idSensor -eq (Import-Csv tp-grupo4-mongo-cassandra/catalogo_ids.csv | Where-Object codigo -eq 'SEN-040').uuid }).conexion)
    Ver 'sensor demo con lecturas recientes' 'ONLINE' (($e | Where-Object { $_.idSensor -eq 'cb6a673c-14a5-5469-a365-30fe9700c80b' }).conexion)
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
    $n = "delete from tp_logistica.usuario where email='smoke-f1@ejemplo.test' returning 1;" | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At'
    "usuario temporal borrado: $(@($n).Count)"
}
