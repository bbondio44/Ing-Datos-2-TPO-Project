param([string]$Scratch)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres); $CA = (docker ps -q --filter name=cassandra); $NJ = (docker ps -q --filter name=neo4j); $MO = (docker ps -q --filter name=mongo)
$base = 'http://localhost:8080'
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'smoke-f2@ejemplo.test'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
$env:APP_ALERTAS_INTERVALO_MS = '4000'
function Sql($q) { ($q | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At') }
function Ver($t, $e, $o) { "$(if ("$e" -eq "$o") { 'OK   ' } else { 'FALLA' }) ${t}: esperado=$e obtenido=$o" }
function Autorizacion { @{ Authorization = "Bearer $script:token" } }
function Get1($p) { $r = Invoke-RestMethod "$base$p" -Headers (Autorizacion) -TimeoutSec 30; $r | ForEach-Object { $_ } }
function Req($m, $p, $b) {
    try {
        $a = @{ Uri = "$base$p"; Method = $m; Headers = (Autorizacion); UseBasicParsing = $true; TimeoutSec = 30; ContentType = 'application/json' }
        if ($b -ne $null) { $a.Body = [Text.Encoding]::UTF8.GetBytes(($b | ConvertTo-Json)) }
        $r = Invoke-WebRequest @a; [int]$r.StatusCode
    } catch { if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "sin respuesta" } }
}
function Fecha($t) { $t.ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ') }
function Lectura($sensor, $t, $extra, $id) {
    if (-not $id) { $id = [guid]::NewGuid() }
    $b = @{ idLectura = "$id"; idSensor = "$sensor"; timestamp = (Fecha $t) }
    foreach ($k in $extra.Keys) { $b[$k] = $extra[$k] }
    $b
}
function AlertasDe($sensor, $idRegla) { ,@(Get1 '/api/v1/alertas' | Where-Object { $_.idSensor -eq $sensor -and (-not $idRegla -or $_.idRegla -eq $idRegla) }) }

$proc = $null; $regla = $null; $inact = $null
try {
    $filas = @(Sql "select id_sensor, id_contenedor, id_envio from tp_logistica.v_envio_contenedor_sensor_activo where id_sensor is not null order by id_sensor;")
    $s = @(); foreach ($f in $filas) { $s += ,("$f".Trim() -split '\|') }
    $s1 = $s[0][0]; $s2 = $s[1][0]; $s3 = $s[2][0]; $envio1 = $s[0][2]
    "sensores: $s1 / $s2 / $s3"
    $inc0 = [int](Sql "select count(*) from tp_logistica.incidente;")
    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\f2.out.log" -RedirectStandardError "$Scratch\f2.err.log"
    for ($i = 0; $i -lt 45; $i++) { if ($proc.HasExited) { 'backend termino'; exit 1 }; try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { if ($_.Exception.Response) { break } }; Start-Sleep -Seconds 2 }
    $login = @{ email = $env:APP_BOOTSTRAP_ADMIN_EMAIL; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json
    $script:token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login).token
    $ahora = (Get-Date).ToUniversalTime()

    '=== R09 ABM de reglas'
    $cuerpo = @{ idSensor = $s1; nombre = 'Smoke temperatura'; tipoRegla = 'TEMPERATURA'; operador = '>'; umbral = 50.0; severidad = 'ALTA'; activa = $true }
    $r = Invoke-RestMethod "$base/api/v1/reglas" -Method Post -Headers (Autorizacion) -ContentType 'application/json' -Body ($cuerpo | ConvertTo-Json)
    $regla = $r.idRegla; "regla creada $regla"
    Ver 'regla invalida (INACTIVIDAD con umbral)' 400 (Req POST '/api/v1/reglas' @{ nombre = 'x'; tipoRegla = 'INACTIVIDAD'; operador = '>'; umbral = 1.0; severidad = 'ALTA'; activa = $true })
    $cuerpo.umbral = 55.0
    Ver 'editar regla' 200 (Req PUT "/api/v1/reglas/$regla" $cuerpo)
    Ver 'regla inexistente' 404 (Req PUT "/api/v1/reglas/$([guid]::NewGuid())" $cuerpo)

    '=== R09/R26/R25 lectura que supera la regla'
    $l1 = [guid]::NewGuid()
    Ver 'POST lectura 60 C' 201 (Req POST '/api/v1/iot/lecturas' (Lectura $s1 $ahora.AddSeconds(-50) @{ temperatura = 60.0 } $l1))
    $a = AlertasDe $s1 $regla
    Ver 'una alerta para la regla' 1 $a.Count
    Ver 'guarda la lectura que la causo' "$l1" $a[0].idLectura
    Ver 'estado ABIERTA' 'ABIERTA' $a[0].estado
    Ver 'alerta ALTA con incidente' True ([bool]$a[0].idIncidente)
    Ver 'incidente existe en Postgres' 1 ([int](Sql "select count(*) from tp_logistica.incidente where id_incidente='$($a[0].idIncidente)';"))
    $ev = @((Get1 "/api/v1/monitoreo/envios/$envio1").eventos | Where-Object { $_.idAlerta -eq $a[0].id })
    Ver 'evento ALERTA_GENERADA en Mongo' 1 $ev.Count

    '=== R26 deduplicacion (D04)'
    $idA = $a[0].id
    Ver 'segunda lectura' 201 (Req POST '/api/v1/iot/lecturas' (Lectura $s1 $ahora.AddSeconds(-40) @{ temperatura = 61.0 } $null))
    $a = AlertasDe $s1 $regla
    Ver 'sigue una sola alerta' 1 $a.Count
    Ver 'ocurrencias suma' 2 $a[0].ocurrencias
    $l3 = [guid]::NewGuid(); $b3 = Lectura $s1 $ahora.AddSeconds(-30) @{ temperatura = 62.0 } $l3
    Req POST '/api/v1/iot/lecturas' $b3 | Out-Null
    Req POST '/api/v1/iot/lecturas' $b3 | Out-Null
    Ver 'reenvio no suma ocurrencia' 3 (AlertasDe $s1 $regla)[0].ocurrencias

    '=== R26 ciclo de estados'
    $c = @{ comentario = 'revisado' }
    Ver 'reconocer' 204 (Req POST "/api/v1/alertas/$idA/reconocer" $c)
    Ver 'reconocer de nuevo' 409 (Req POST "/api/v1/alertas/$idA/reconocer" $c)
    Ver 'cerrar' 204 (Req POST "/api/v1/alertas/$idA/cerrar" $c)
    Ver 'cerrar de nuevo' 409 (Req POST "/api/v1/alertas/$idA/cerrar" $c)
    Ver 'alerta inexistente' 404 (Req POST "/api/v1/alertas/no-existe/cerrar" $c)
    Req POST '/api/v1/iot/lecturas' (Lectura $s1 $ahora.AddSeconds(-20) @{ temperatura = 63.0 } $null) | Out-Null
    $a = AlertasDe $s1 $regla
    Ver 'reapertura tras cerrar' 2 $a.Count
    Ver 'una sola activa' 1 @($a | Where-Object { $_.activa }).Count

    '=== R32 anomalia de temperatura'
    $previas = AlertasDe $s2 $null
    $previas | Where-Object { $_.tipoRegla -eq 'ANOMALIA_TEMPERATURA' -and $_.activa } | ForEach-Object { Req POST "/api/v1/alertas/$($_.id)/cerrar" @{ comentario = 'limpieza de la prueba' } | Out-Null }
    foreach ($i in 1..12) { Req POST '/api/v1/iot/lecturas' (Lectura $s2 $ahora.AddMinutes(-30 + $i) @{ temperatura = 5.0 + ($i % 3) * 0.5 } $null) | Out-Null }
    $lA = [guid]::NewGuid()
    Req POST '/api/v1/iot/lecturas' (Lectura $s2 $ahora.AddSeconds(-5) @{ temperatura = 25.0 } $lA) | Out-Null
    $todas = AlertasDe $s2 $null
    $an = @($todas | Where-Object { $_.tipoRegla -eq 'ANOMALIA_TEMPERATURA' -and $_.activa })
    Ver 'anomalia detectada' 1 $an.Count
    Ver 'con la lectura que la causo' "$lA" $an[0].idLectura
    "     z=$($an[0].valorObservado) umbral=$($an[0].umbral) severidad=$($an[0].severidad)"

    '=== R09 regla limitada a un tipo de contenedor'
    $tipo = Sql "select tipo from tp_logistica.contenedor where id_contenedor=(select id_contenedor from tp_logistica.v_envio_contenedor_sensor_activo where id_sensor='$s2');"
    $otra = (Invoke-RestMethod "$base/api/v1/reglas" -Method Post -Headers (Autorizacion) -ContentType 'application/json' -Body (@{ idSensor = $s2; nombre = 'Prueba otro tipo'; tipoRegla = 'HUMEDAD'; operador = '>'; umbral = 1.0; severidad = 'BAJA'; activa = $true; tipoContenedor = 'TIPO_INEXISTENTE' } | ConvertTo-Json)).idRegla
    $igual = (Invoke-RestMethod "$base/api/v1/reglas" -Method Post -Headers (Autorizacion) -ContentType 'application/json' -Body (@{ idSensor = $s2; nombre = 'Prueba mismo tipo'; tipoRegla = 'HUMEDAD'; operador = '>'; umbral = 1.0; severidad = 'BAJA'; activa = $true; tipoContenedor = "$tipo" } | ConvertTo-Json)).idRegla
    Req POST '/api/v1/iot/lecturas' (Lectura $s2 (Get-Date).ToUniversalTime().AddSeconds(-2) @{ humedad = 50.0 } $null) | Out-Null
    Ver 'no aplica a otro tipo' 0 (AlertasDe $s2 $otra).Count
    Ver 'aplica a su tipo' 1 (AlertasDe $s2 $igual).Count
    Sql "delete from tp_logistica.regla_alerta where id_regla in ('$otra','$igual');" | Out-Null
    '=== R23 tarea periodica'
    $inact = (Invoke-RestMethod "$base/api/v1/reglas" -Method Post -Headers (Autorizacion) -ContentType 'application/json' -Body (@{ idSensor = $s3; nombre = 'Prueba inactividad'; tipoRegla = 'INACTIVIDAD'; operador = '>'; minutosInactividad = 1; severidad = 'ALTA'; activa = $true } | ConvertTo-Json)).idRegla
    Req POST '/api/v1/iot/lecturas' (Lectura $s3 (Get-Date).ToUniversalTime().AddMinutes(-3) @{ temperatura = 5.0; bateria = 10.0 } $null) | Out-Null
    # Un contenedor vigente sin sensor no puede frenar la tarea periodica de los demas sensores.
    $cli = Sql "select id_cliente from tp_logistica.cliente where estado='ACTIVO' limit 1;"
    $sinSensor = Invoke-RestMethod "$base/api/v1/envios" -Method Post -Headers (Autorizacion) -ContentType 'application/json' -Body (@{ clientId = "$cli".Trim(); origin = 'Sin sensor'; destination = 'Destino'; originCountry = 'Argentina'; destinationCountry = 'Brasil'; priority = 'NORMAL'; volumeM3 = 1 } | ConvertTo-Json)
    $vacio = Invoke-RestMethod "$base/api/v1/contenedores" -Method Post -Headers (Autorizacion) -ContentType 'application/json' -Body (@{ internationalCode = "VACIO$([guid]::NewGuid().ToString('N').Substring(0,8))"; type = 'DRY'; capacity = 10; state = 'DISPONIBLE' } | ConvertTo-Json)
    Ver 'contenedor vigente sin sensor' 201 (Req POST "/api/v1/envios/$($sinSensor.id)/contenedores" @{ containerId = $vacio.id })
    $e = @(); for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 5
        $e = AlertasDe $s3 $null
        if ($e | Where-Object tipoRegla -eq 'INACTIVIDAD') { break }
    }
    "     alertas del sensor: $(($e | ForEach-Object { "$($_.tipoRegla)x$($_.ocurrencias)" }) -join ' ')"
    Ver 'alerta de inactividad' True ([bool]($e | Where-Object tipoRegla -eq 'INACTIVIDAD'))
    Ver 'alerta de bateria' True ([bool]($e | Where-Object tipoRegla -eq 'BATERIA'))
    "     mensaje: $(($e | Where-Object tipoRegla -eq 'INACTIVIDAD' | Select-Object -First 1).mensaje)"

    '=== R17 monitoreo: un envio con datos de los tres motores'
    $e2 = 'ec22d634-96cd-5267-b7bd-accdce959d92'
    $m = Get1 "/api/v1/monitoreo/envios/$e2"
    "     Postgres: $($m.origen) -> $($m.destino), $($m.estado), contenedores=$(@($m.contenedores).Count)"
    "     Cassandra: lecturas=$(@($m.ultimasLecturas).Count) posiciones=$(@($m.ultimasPosiciones).Count)"
    "     Mongo: eventos=$(@($m.eventos).Count) alertas=$(@($m.alertas).Count); Postgres incidentes=$(@($m.incidentes).Count)"
    Ver 'trae lecturas' True (@($m.ultimasLecturas).Count -gt 0)
    Ver 'trae eventos' True (@($m.eventos).Count -gt 0)
    Ver 'envio inexistente' 404 (Req GET "/api/v1/monitoreo/envios/$([guid]::NewGuid())" $null)

    '=== Control R25: alertas ALTA o CRITICA nuevas sin incidente'
    $sin = @(Get1 '/api/v1/alertas' | Where-Object { $_.severidad -in 'ALTA', 'CRITICA' -and $_.idEnvio -and -not $_.idIncidente })
    Ver 'ALTA/CRITICA sin incidente' 0 $sin.Count

    '=== R09 baja logica y Mongo caido'
    $lista = AlertasDe $s1 $regla
    $antes = @($lista | Where-Object { $_.activa })[0].ocurrencias
    $lCaida = Lectura $s1 (Get-Date) @{ temperatura = 70.0 } $null
    docker stop $MO 2>$null | Out-Null
    Ver 'pide reintentar con Mongo caido' 503 (Req POST '/api/v1/iot/lecturas' $lCaida)
    Ver 'listar alertas con Mongo caido' 503 (Req GET '/api/v1/alertas' $null)
    docker start $MO 2>$null | Out-Null
    $res = $null; for ($i = 0; $i -lt 20; $i++) { Start-Sleep -Seconds 3; $res = Req POST '/api/v1/iot/lecturas' $lCaida; if ($res -eq 201) { break } }
    Ver 'el reintento funciona al volver Mongo' 201 $res
    $lista = AlertasDe $s1 $regla
    Ver 'la alerta no se perdio' ($antes + 1) (@($lista | Where-Object { $_.activa })[0].ocurrencias)
    Ver 'desactivar regla' 204 (Req DELETE "/api/v1/reglas/$regla" $null)
    Ver 'regla inactiva' 'f' (Sql "select activa from tp_logistica.regla_alerta where id_regla='$regla';")
    "incidentes nuevos: $([int](Sql 'select count(*) from tp_logistica.incidente;') - $inc0)"
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
    docker start $MO 2>$null | Out-Null
    if ($regla) { Sql "delete from tp_logistica.regla_alerta where id_regla='$regla';" | Out-Null }
    if ($inact) { Sql "delete from tp_logistica.regla_alerta where id_regla='$inact';" | Out-Null }
    $n = Sql "delete from tp_logistica.usuario where email='smoke-f2@ejemplo.test' returning 1;"
    "usuario temporal borrado: $(@($n).Count)"
}

