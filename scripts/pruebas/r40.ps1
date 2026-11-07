param([string]$Scratch)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres); $CA = (docker ps -q --filter name=cassandra); $NJ = (docker ps -q --filter name=neo4j); $MO = (docker ps -q --filter name=mongo)
$base = 'http://localhost:8080'
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'; $env:APP_BOOTSTRAP_ADMIN_EMAIL = 'smoke-r40@ejemplo.test'; $env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
function Sql($q) { ($q | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At') }
function Ver($t, $e, $o) { "$(if ("$e" -eq "$o") { 'OK   ' } else { 'FALLA' }) ${t}: esperado=$e obtenido=$o" }
function Autorizacion { @{ Authorization = "Bearer $script:token" } }
function Lista($p) { $r = Invoke-RestMethod "$base$p" -Headers (Autorizacion) -TimeoutSec 30; ,@($r | ForEach-Object { $_ }) }
function Req($m, $p, $b) {
    try { $a = @{ Uri = "$base$p"; Method = $m; Headers = (Autorizacion); UseBasicParsing = $true; TimeoutSec = 30; ContentType = 'application/json' }
        if ($b -ne $null) { $a.Body = [Text.Encoding]::UTF8.GetBytes(($b | ConvertTo-Json -Depth 5)) }
        [int](Invoke-WebRequest @a).StatusCode
    } catch { if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 'sin respuesta' } }
}
function Ser($col, $id) { Sql "update tp_logistica.usuario set id_cliente=null, id_proveedor=null, id_operador=null where email='smoke-r40@ejemplo.test';" | Out-Null; if ($col) { Sql "update tp_logistica.usuario set $col='$id' where email='smoke-r40@ejemplo.test';" | Out-Null } }
$cat = Import-Csv tp-grupo4-mongo-cassandra/catalogo_ids.csv
function Id($c) { ($cat | Where-Object codigo -eq $c).uuid }
$c01 = Id 'C01'; $c02 = Id 'C02'; $o01 = Id 'O01'; $p02 = Id 'P02'
$proc = $null
try {
    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\r40.out.log" -RedirectStandardError "$Scratch\r40.err.log"
    for ($i = 0; $i -lt 45; $i++) { if ($proc.HasExited) { 'backend termino'; exit 1 }; try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { if ($_.Exception.Response) { break } }; Start-Sleep -Seconds 2 }
    # El administrador inicial se crea apenas despues de que responde el health.
    Start-Sleep 6
    $script:token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body (@{ email = $env:APP_BOOTSTRAP_ADMIN_EMAIL; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json)).token

    '=== Bandejas segun quien es el usuario'
    Ser 'id_cliente' $c02; $b2 = Lista '/api/v1/mensajeria/conversaciones'; "C02 ve $($b2.Count) conversaciones"
    Ser 'id_cliente' $c01; $b1 = Lista '/api/v1/mensajeria/conversaciones'; "C01 ve $($b1.Count) conversaciones"
    $soloC02 = @($b2 | Where-Object { $_.id -notin $b1.id })[0]
    Ver 'C02 tiene una conversacion que C01 no ve' True ([bool]$soloC02)
    '=== Autorizacion: un ajeno no ve nada'
    Ver 'C01 no lee mensajes ajenos' 404 (Req GET "/api/v1/mensajeria/conversaciones/$($soloC02.id)/mensajes" $null)
    Ver 'C01 no escribe en conversacion ajena' 404 (Req POST "/api/v1/mensajeria/conversaciones/$($soloC02.id)/mensajes" @{ idMensaje = "$([guid]::NewGuid())"; contenido = 'hola' })
    Ser 'id_cliente' $c02
    Ver 'C02 si lee su conversacion' 200 (Req GET "/api/v1/mensajeria/conversaciones/$($soloC02.id)/mensajes" $null)

    '=== Conversacion privada cliente - operador'
    Ser 'id_cliente' $c01
    $conv = Invoke-RestMethod "$base/api/v1/mensajeria/conversaciones" -Method Post -Headers (Autorizacion) -ContentType 'application/json' -Body (@{ tipo = 'PRIVADA'; asunto = 'Smoke privada'; participantes = @(@{ tipo = 'OPERADOR'; idEntidad = $o01 }) } | ConvertTo-Json -Depth 5)
    "conversacion $($conv.id); participantes: $($conv.participantes -join ', ')"
    $m = @{ idMensaje = "$([guid]::NewGuid())"; contenido = 'Hola, consulto por el envio' }
    Ver 'cliente envia' 201 (Req POST "/api/v1/mensajeria/conversaciones/$($conv.id)/mensajes" $m)
    Ver 'reintento mismo id' 201 (Req POST "/api/v1/mensajeria/conversaciones/$($conv.id)/mensajes" $m)
    Ver 'no duplica el mensaje' 1 (Lista "/api/v1/mensajeria/conversaciones/$($conv.id)/mensajes").Count
    Ser 'id_operador' $o01
    Ver 'operador responde' 201 (Req POST "/api/v1/mensajeria/conversaciones/$($conv.id)/mensajes" @{ idMensaje = "$([guid]::NewGuid())"; contenido = 'Te respondo' })
    $msjs = Lista "/api/v1/mensajeria/conversaciones/$($conv.id)/mensajes"
    Ver 'operador ve 2 mensajes' 2 $msjs.Count
    Ver 'mas nuevo primero' 'Te respondo' $msjs[0].contenido
    Ver 'paginado de a 1' 1 (Lista "/api/v1/mensajeria/conversaciones/$($conv.id)/mensajes?tamanio=1").Count
    $bo = Lista '/api/v1/mensajeria/conversaciones'
    $mine = @($bo | Where-Object { $_.id -eq $conv.id })[0]
    Ver 'contador y extracto en la bandeja' '2|Te respondo' "$($mine.cantidadMensajes)|$($mine.ultimoMensajeExtracto)"
    Ser 'id_proveedor' $p02
    Ver 'proveedor ajeno no la ve' 404 (Req GET "/api/v1/mensajeria/conversaciones/$($conv.id)/mensajes" $null)
    '=== Grupal y validaciones'
    Ver 'grupo proveedor con cliente y operador' 201 (Req POST '/api/v1/mensajeria/conversaciones' @{ tipo = 'GRUPAL'; asunto = 'Smoke grupo'; nombreGrupo = 'GT'; participantes = @(@{ tipo = 'CLIENTE'; idEntidad = $c01 }, @{ tipo = 'OPERADOR'; idEntidad = $o01 }) })
    Ver 'privada con dos destinatarios' 400 (Req POST '/api/v1/mensajeria/conversaciones' @{ tipo = 'PRIVADA'; asunto = 'x'; participantes = @(@{ tipo = 'CLIENTE'; idEntidad = $c01 }, @{ tipo = 'OPERADOR'; idEntidad = $o01 }) })
    Ver 'participante inexistente' 404 (Req POST '/api/v1/mensajeria/conversaciones' @{ tipo = 'GRUPAL'; asunto = 'x'; participantes = @(@{ tipo = 'CLIENTE'; idEntidad = "$([guid]::NewGuid())" }) })
    Ver 'mensaje vacio' 400 (Req POST "/api/v1/mensajeria/conversaciones/$($conv.id)/mensajes" @{ idMensaje = "$([guid]::NewGuid())"; contenido = '' })
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
    $n = Sql "delete from tp_logistica.usuario where email='smoke-r40@ejemplo.test' returning 1;"; "usuario temporal borrado: $(@($n).Count)"
}
