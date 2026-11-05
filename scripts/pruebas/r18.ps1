param([string]$Scratch)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres); $CA = (docker ps -q --filter name=cassandra); $NJ = (docker ps -q --filter name=neo4j); $MO = (docker ps -q --filter name=mongo)
$base = 'http://localhost:8080'
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'smoke-r18@ejemplo.test'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')

function Sql($q) { ($q | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At') }
function Cql($q) { docker exec $CA cqlsh -e $q 2>$null }
function Fecha($t) { $t.ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ') }

function Contar($tabla, $col, $id) {
    $total = 0
    foreach ($d in $script:dias) {
        $r = Cql "SELECT COUNT(*) FROM logistica_iot.$tabla WHERE $col = $id AND dia = '$d';"
        $total += [int](($r | Where-Object { $_ -match '^\s*\d+\s*$' } | Select-Object -First 1).Trim())
    }
    $total
}

function Post($body, $tok) {
    $h = @{}; if ($tok) { $h.Authorization = "Bearer $tok" }
    try {
        $r = Invoke-WebRequest "$base/api/v1/iot/lecturas" -Method Post -Headers $h -ContentType 'application/json' -UseBasicParsing -TimeoutSec 30 -Body ($body | ConvertTo-Json)
        [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "sin respuesta: $($_.Exception.Message)" }
    }
}

function Lectura($id, $t, $extra) {
    $b = @{ idLectura = "$id"; idSensor = "$script:sensor"; timestamp = (Fecha $t) }
    foreach ($k in $extra.Keys) { $b[$k] = $extra[$k] }
    $b
}

function Ver($titulo, $esperado, $obtenido) {
    $ok = "$esperado" -eq "$obtenido"
    "$(if ($ok) { 'OK   ' } else { 'FALLA' }) ${titulo}: esperado=$esperado obtenido=$obtenido"
}

$proc = $null
try {
    $fila = Sql "select id_sensor, id_contenedor from tp_logistica.v_envio_contenedor_sensor_activo order by id_sensor desc limit 1;"
    $script:sensor, $cont = ("$fila").Trim() -split '\|'
    "sensor demo = $script:sensor"; "contenedor  = $cont"
    $ahora = (Get-Date).ToUniversalTime()
    $script:dias = @($ahora.AddDays(-1).ToString('yyyy-MM-dd'), $ahora.ToString('yyyy-MM-dd'), $ahora.AddDays(1).ToString('yyyy-MM-dd'))

    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput "$Scratch\r18.out.log" -RedirectStandardError "$Scratch\r18.err.log"
    for ($i = 0; $i -lt 45; $i++) {
        if ($proc.HasExited) { 'backend termino al arrancar'; exit 1 }
        try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { if ($_.Exception.Response) { break } }
        Start-Sleep -Seconds 2
    }
    $login = @{ email = $env:APP_BOOTSTRAP_ADMIN_EMAIL; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json
    $token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login).token

    '=== 1. Serie de 3 lecturas consecutivas con GPS'
    $s0 = Contar 'lecturas_por_sensor_dia' 'id_sensor' $script:sensor
    $c0 = Contar 'lecturas_por_contenedor_dia' 'id_contenedor' $cont
    $p0 = Contar 'posiciones_por_contenedor_dia' 'id_contenedor' $cont
    $ultimoId = $null; $ultimaFecha = $null; $ultimoCuerpo = $null
    foreach ($m in 3, 2, 1) {
        $ultimoId = [guid]::NewGuid(); $ultimaFecha = $ahora.AddMinutes(-$m)
        $ultimoCuerpo = Lectura $ultimoId $ultimaFecha @{ temperatura = 5.0 + $m; humedad = 50.5; vibracion = 0.2; bateria = 90.0; latitud = -34.6; longitud = -58.4 }
        Ver "POST lectura -$m min" 201 (Post $ultimoCuerpo $token)
    }
    Ver 'lecturas_por_sensor_dia +3' ($s0 + 3) (Contar 'lecturas_por_sensor_dia' 'id_sensor' $script:sensor)
    Ver 'lecturas_por_contenedor_dia +3' ($c0 + 3) (Contar 'lecturas_por_contenedor_dia' 'id_contenedor' $cont)
    Ver 'posiciones_por_contenedor_dia +3' ($p0 + 3) (Contar 'posiciones_por_contenedor_dia' 'id_contenedor' $cont)

    '=== 2. Reenvio de la misma lectura'
    $s1 = Contar 'lecturas_por_sensor_dia' 'id_sensor' $script:sensor
    Ver 'POST reenvio' 201 (Post $ultimoCuerpo $token)
    Ver 'no duplica' $s1 (Contar 'lecturas_por_sensor_dia' 'id_sensor' $script:sensor)

    '=== 3. Lectura tardia, 2 horas atras'
    $u0 = Cql "SELECT id_lectura FROM logistica_iot.ultima_lectura_por_sensor WHERE id_sensor = $script:sensor;"
    $idU = (($u0 | Select-String -Pattern '[0-9a-f]{8}-[0-9a-f-]{27}').Matches | Select-Object -First 1).Value
    Ver 'ultima = la mas nueva antes de la tardia' "$ultimoId" $idU
    Ver 'POST tardia' 201 (Post (Lectura ([guid]::NewGuid()) $ahora.AddHours(-2) @{ temperatura = 99.0; latitud = 10.0; longitud = 10.0 }) $token)
    Ver 'historial +1' ($s1 + 1) (Contar 'lecturas_por_sensor_dia' 'id_sensor' $script:sensor)
    $u1 = Cql "SELECT id_lectura, temperatura FROM logistica_iot.ultima_lectura_por_sensor WHERE id_sensor = $script:sensor;"
    $idU = (($u1 | Select-String -Pattern '[0-9a-f]{8}-[0-9a-f-]{27}').Matches | Select-Object -First 1).Value
    Ver 'tardia no pisa ultima_lectura' "$ultimoId" $idU
    "     fila: $(($u1 | Where-Object { $_ -match '[0-9a-f]{8}-' }))"
    $pos = Cql "SELECT id_lectura, latitud FROM logistica_iot.ultima_posicion_por_contenedor WHERE id_contenedor = $cont;"
    $idP = (($pos | Select-String -Pattern '[0-9a-f]{8}-[0-9a-f-]{27}').Matches | Select-Object -First 1).Value
    Ver 'tardia no pisa ultima_posicion' "$ultimoId" $idP

    '=== 4. Lectura sin GPS'
    $p1 = Contar 'posiciones_por_contenedor_dia' 'id_contenedor' $cont
    $idSinGps = [guid]::NewGuid()
    Ver 'POST sin GPS' 201 (Post (Lectura $idSinGps $ahora.AddSeconds(-20) @{ temperatura = 4.0 }) $token)
    Ver 'posiciones sin cambios' $p1 (Contar 'posiciones_por_contenedor_dia' 'id_contenedor' $cont)
    $pos = Cql "SELECT id_lectura FROM logistica_iot.ultima_posicion_por_contenedor WHERE id_contenedor = $cont;"
    Ver 'ultima_posicion sin cambios' "$ultimoId" ((($pos | Select-String -Pattern '[0-9a-f]{8}-[0-9a-f-]{27}').Matches | Select-Object -First 1).Value)
    $u2 = Cql "SELECT id_lectura FROM logistica_iot.ultima_lectura_por_sensor WHERE id_sensor = $script:sensor;"
    Ver 'ultima_lectura se actualiza' "$idSinGps" ((($u2 | Select-String -Pattern '[0-9a-f]{8}-[0-9a-f-]{27}').Matches | Select-Object -First 1).Value)

    '=== 5. Errores'
    $b = Lectura ([guid]::NewGuid()) $ahora @{ temperatura = 5.0 }
    Ver 'sin token' 401 (Post $b $null)
    $x = Lectura ([guid]::NewGuid()) $ahora @{ temperatura = 5.0 }; $x.idSensor = "$([guid]::NewGuid())"
    Ver 'sensor inexistente' 404 (Post $x $token)
    $x = Lectura ([guid]::NewGuid()) $ahora @{ temperatura = 5.0 }; $x.idSensor = '101231a8-af8f-53ec-bfb0-f4d579ba7455'
    Ver 'sensor sin asignacion vigente (SEN-001 hoy)' 409 (Post $x $token)
    Ver 'bateria 120' 400 (Post (Lectura ([guid]::NewGuid()) $ahora @{ bateria = 120.0 }) $token)
    Ver 'latitud sin longitud' 400 (Post (Lectura ([guid]::NewGuid()) $ahora @{ latitud = -34.6 }) $token)
    Ver 'fecha futura' 400 (Post (Lectura ([guid]::NewGuid()) $ahora.AddHours(2) @{ temperatura = 5.0 }) $token)
    Ver 'sin mediciones' 400 (Post (Lectura ([guid]::NewGuid()) $ahora @{}) $token)

    '=== 6. Cassandra caida y reintento'
    $idCaida = [guid]::NewGuid(); $bCaida = Lectura $idCaida $ahora.AddSeconds(-10) @{ temperatura = 4.5 }
    docker stop $CA 2>$null | Out-Null
    Ver 'POST con Cassandra caida' 503 (Post $bCaida $token)
    docker start $CA 2>$null | Out-Null
    for ($i = 0; $i -lt 40; $i++) { if ((docker inspect $CA --format '{{.State.Health.Status}}') -eq 'healthy') { break }; Start-Sleep -Seconds 5 }
    $res = $null
    for ($i = 0; $i -lt 12; $i++) { $res = Post $bCaida $token; if ($res -eq 201) { break }; Start-Sleep -Seconds 5 }
    Ver 'reintento con el mismo id' 201 $res

    '=== 7. Simulador'
    $cp = (Get-Content "$Scratch\sim.cp" -Raw).Trim()
    $s2 = Contar 'lecturas_por_sensor_dia' 'id_sensor' $script:sensor
    $env:SENSOR_ID = $script:sensor; $env:API_TOKEN = $token; $env:SIM_INTERVAL_MS = '1000'
    $sim = Start-Process java -ArgumentList '-cp', "`"simulator\target\classes;$cp`"", 'ar.edu.uade.logistica.simulator.IoTSimulator' -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput "$Scratch\sim.out.log" -RedirectStandardError "$Scratch\sim.err.log"
    Start-Sleep -Seconds 8
    if (-not $sim.HasExited) { Stop-Process -Id $sim.Id -Force }
    Start-Sleep -Seconds 1
    $env:API_TOKEN = $null
    $lineas = @(Get-Content "$Scratch\sim.out.log" -ErrorAction SilentlyContinue)
    "simulador envio $($lineas.Count) lecturas; estados: $(($lineas | ForEach-Object { ($_ -split 'estado=')[1] } | Sort-Object -Unique) -join ',')"
    Ver 'lecturas del simulador guardadas' ($s2 + $lineas.Count) (Contar 'lecturas_por_sensor_dia' 'id_sensor' $script:sensor)
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
    docker start $CA 2>$null | Out-Null
    $n = Sql "delete from tp_logistica.usuario where email = 'smoke-r18@ejemplo.test' returning 1;"
    "usuario de prueba borrado: $(@($n).Count)"
}
