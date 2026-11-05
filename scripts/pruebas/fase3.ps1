param([string]$Scratch)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -aq --filter name=postgres); $CA = (docker ps -aq --filter name=cassandra); $NJ = (docker ps -aq --filter name=neo4j); $MO = (docker ps -aq --filter name=mongo)
$base = 'http://localhost:8080'
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'; $env:APP_BOOTSTRAP_ADMIN_EMAIL = "smoke-f3-$([guid]::NewGuid().ToString('N').Substring(0,8))@ejemplo.test"; $env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N'); $env:APP_OUTBOX_INTERVALO_MS = '2000'
function Sql($q) { ($q | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At') }
function Cy($q) {
    [IO.File]::WriteAllText("$Scratch\q.cypher", $q, (New-Object Text.UTF8Encoding $false))
    docker cp "$Scratch\q.cypher" "${NJ}:/tmp/q.cypher" 2>$null | Out-Null
    docker exec $NJ sh -c 'cypher-shell -u ${NEO4J_AUTH%%/*} -p ${NEO4J_AUTH#*/} --format plain -f /tmp/q.cypher' 2>$null | Select-Object -Skip 1
}
function Ver($t, $e, $o) { "$(if ("$e" -eq "$o") { 'OK   ' } else { 'FALLA' }) ${t}: esperado=$e obtenido=$o" }
function Autorizacion { @{ Authorization = "Bearer $script:token" } }
function Lista($p) { $r = Invoke-RestMethod "$base$p" -Headers (Autorizacion) -TimeoutSec 60; ,@($r | ForEach-Object { $_ }) }
function Obj($m, $p, $b) { $a = @{ Uri = "$base$p"; Method = $m; Headers = (Autorizacion); TimeoutSec = 60; ContentType = 'application/json' }; if ($b -ne $null) { $a.Body = ($b | ConvertTo-Json -Depth 5) }; Invoke-RestMethod @a }
function Req($m, $p, $b) { try { $a = @{ Uri = "$base$p"; Method = $m; Headers = (Autorizacion); UseBasicParsing = $true; TimeoutSec = 60; ContentType = 'application/json' }; if ($b -ne $null) { $a.Body = ($b | ConvertTo-Json -Depth 5) }; [int](Invoke-WebRequest @a).StatusCode } catch { if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 'sin respuesta' } } }
function Pendientes { [int](Sql "select count(*) from tp_logistica.outbox_evento where estado <> 'PROCESADO';") }
function Esperar($seg) { for ($i = 0; $i -lt $seg; $i += 2) { if ((Pendientes) -eq 0) { return }; Start-Sleep -Seconds 2 } }
function Conteos { Cy "MATCH (n) RETURN labels(n)[0] AS l, count(n) AS c ORDER BY l;" | ForEach-Object { "$_".Replace('"', '').Replace(', ', '=') } }
function Rels { [int](Cy "MATCH ()-[r]->() RETURN count(r) AS c;") }
$cat = Import-Csv tp-grupo4-mongo-cassandra/catalogo_ids.csv
function Id($c) { ($cat | Where-Object codigo -eq $c).uuid }
$proc = $null; $demo = '0bca0b45-e7f6-59b2-a455-02191ee367f6'
try {
    "pendientes al inicio: $(Pendientes)"
    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\f3.out.log" -RedirectStandardError "$Scratch\f3.err.log"
    for ($i = 0; $i -lt 45; $i++) { if ($proc.HasExited) { 'backend termino'; exit 1 }; try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { if ($_.Exception.Response) { break } }; Start-Sleep -Seconds 2 }
    $script:token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body (@{ email = $env:APP_BOOTSTRAP_ADMIN_EMAIL; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json)).token

    '=== R30 worker del outbox: Postgres -> Neo4j'
    Esperar 90
    Ver 'outbox vacio' 0 (Pendientes)
    $c1 = Conteos; "     $($c1 -join ' ')"
    foreach ($par in @(@('Cliente', 'cliente'), @('Envio', 'envio'), @('Tramo', 'tramo'), @('Contenedor', 'contenedor'), @('Operador', 'operador_logistico'), @('Incidente', 'incidente'))) {
        $esperado = Sql "select count(*) from tp_logistica.$($par[1]);"
        Ver "$($par[0]) igual que Postgres" "$($par[0])=$esperado" ($c1 | Where-Object { $_ -like "$($par[0])=*" })
    }
    '=== R30 reprocesar todo no duplica'
    $r1 = Rels
    Sql "update tp_logistica.outbox_evento set estado='PENDIENTE', fecha_procesamiento=null;" | Out-Null
    Esperar 90
    Ver 'outbox vacio tras reprocesar' 0 (Pendientes)
    Ver 'mismos nodos' ($c1 -join ' ') ((Conteos) -join ' ')
    Ver 'mismas relaciones' $r1 (Rels)

    '=== R30 falla parcial: Neo4j caido'
    docker stop $NJ 2>$null | Out-Null
    $cli = Id 'C03'
    Sql "update tp_logistica.cliente set telefono='111' where id_cliente='$cli';" | Out-Null
    Start-Sleep -Seconds 12
    "     con Neo4j caido: $(Sql "select estado, intentos, left(coalesce(ultimo_error,''),50) from tp_logistica.outbox_evento where estado <> 'PROCESADO' limit 2;")"
    Sql "update tp_logistica.cliente set estado='INACTIVO' where id_cliente='$cli';" | Out-Null
    docker start $NJ 2>$null | Out-Null
    for ($i = 0; $i -lt 30; $i++) { if ("$(Cy 'RETURN 1 AS x;')" -match '1') { break }; Start-Sleep -Seconds 3 }
    Esperar 120
    Ver 'se pone al dia solo' 0 (Pendientes)
    Ver 'cambio aplicado en el grafo' '"INACTIVO"' "$(Cy "MATCH (c:Cliente {id_cliente: '$cli'}) RETURN c.estado AS e;")"
    Sql "update tp_logistica.cliente set estado='ACTIVO', telefono=null where id_cliente='$cli';" | Out-Null

    '=== R30 red de un cliente'
    $c02 = Id 'C02'
    $red = Obj GET "/api/v1/grafo/clientes/$c02/red" $null
    "     $($red.cliente.razon_social): envios=$(@($red.envios).Count) tramos=$(@($red.tramos).Count) operadores=$(@($red.operadores).Count) contenedores=$(@($red.contenedores).Count)"
    Ver 'envios igual que Postgres' (Sql "select count(*) from tp_logistica.envio where id_cliente='$c02';") @($red.envios).Count
    Ver 'tramos igual que Postgres' (Sql "select count(*) from tp_logistica.tramo t join tp_logistica.envio e using(id_envio) where e.id_cliente='$c02';") @($red.tramos).Count
    Ver 'operadores igual que Postgres' (Sql "select count(distinct t.id_operador) from tp_logistica.tramo t join tp_logistica.envio e using(id_envio) where e.id_cliente='$c02';") @($red.operadores).Count
    Ver 'cliente inexistente' 404 (Req GET "/api/v1/grafo/clientes/$([guid]::NewGuid())/red" $null)

    '=== R28 ranking de clientes contra calculo manual'
    $rk = Lista '/api/v1/analitica/ranking-clientes'
    $m = "select round(sum(volumen_m3),3) from tp_logistica.envio where id_cliente='$($rk[0].id_cliente)' and estado<>'CANCELADO';"
    "     1ro: $($rk[0].razon_social) $($rk[0].volumen_total_m3) m3"
    Ver 'volumen del primero' ([double](Sql $m)) ([double]$rk[0].volumen_total_m3)
    $vols = @($rk | ForEach-Object { [double]$_.volumen_total_m3 })
    Ver 'orden descendente' (($vols | Sort-Object -Descending) -join ',') ($vols -join ',')
    '=== R27 ranking de rutas'
    $rr = Lista '/api/v1/analitica/ranking-rutas'
    "     $($rr.Count) rutas con incidentes; 1ra: $($rr[0].origen) -> $($rr[0].destino) ($($rr[0].medio_transporte)) = $($rr[0].cantidad_incidentes)"
    Ver 'total contra incidentes con tramo' (Sql "select count(*) from tp_logistica.incidente i join tp_logistica.tramo t using(id_tramo) where t.estado<>'CANCELADO';") (($rr | Measure-Object cantidad_incidentes -Sum).Sum)
    '=== R31 desempeno por periodo'
    $d = Lista '/api/v1/analitica/desempeno'
    $d | ForEach-Object { "     $($_.periodo): creados=$($_.envios_creados) entregados=$($_.entregados) fuera_plazo=$($_.entregados_fuera_plazo) horas=$($_.horas_promedio_entrega)" }
    Ver 'mas de un periodo' True ($d.Count -gt 1)
    Ver 'creados igual que Postgres' (Sql "select count(*) from tp_logistica.envio where estado<>'CANCELADO';") (($d | Measure-Object envios_creados -Sum).Sum)

    '=== R29 prediccion'
    $g1 = (Obj POST '/api/v1/analitica/predicciones/generar' $null).generadas
    $g2 = (Obj POST '/api/v1/analitica/predicciones/generar' $null).generadas
    "     primera corrida genero $g1"
    Ver 'segunda corrida del dia no repite' 0 $g2
    $p = Lista '/api/v1/analitica/predicciones'
    Ver 'una prediccion por envio en transito' (Sql "select count(*) from tp_logistica.envio where estado in ('EN_TRANSITO','DEMORADO');") $p.Count
    "     ejemplo: $($p[0].origen) -> $($p[0].destino) eta=$($p[0].eta_predicha) riesgo=$($p[0].probabilidad_atraso)"
    $v = Lista '/api/v1/analitica/predicciones/validacion'
    "     validacion: evaluados=$($v[0].envios_evaluados) promedio=$($v[0].horas_promedio_reales)h error_medio=$($v[0].error_absoluto_medio_horas)h"

    '=== R11, R34, R33 reportes'
    $rep = Obj POST '/api/v1/reportes' @{ tipo = 'PREDICTIVO'; idCliente = $demo }
    Ver 'reporte generado' 'GENERADO' $rep.estado
    "     precio=$($rep.precio)"
    $res = Lista "/api/v1/reportes/$($rep.idSolicitud)/resultado"
    "     resultado en Mongo: $($res.Count) filas; riesgo=$($res[0].probabilidad_atraso)"
    Ver 'filas del cliente' (Sql "select count(*) from tp_logistica.envio where id_cliente='$demo' and estado in ('EN_TRANSITO','DEMORADO');") $res.Count
    $f1 = Obj POST "/api/v1/reportes/$($rep.idSolicitud)/facturar" $null
    $f2 = Obj POST "/api/v1/reportes/$($rep.idSolicitud)/facturar" $null
    Ver 'estado facturado' 'FACTURADO' $f1.estado
    Ver 'facturar dos veces da la misma factura' $f1.idFactura $f2.idFactura
    Ver 'una sola factura' 1 (Sql "select count(*) from tp_logistica.factura where clave_idempotencia='reporte:$($rep.idSolicitud)';")
    Ver 'otro tipo: desempeno' 'GENERADO' (Obj POST '/api/v1/reportes' @{ tipo = 'DESEMPENO'; idCliente = $demo; desde = '2026-09-01'; hasta = '2026-10-31' }).estado
    Ver 'tipo invalido' 400 (Req POST '/api/v1/reportes' @{ tipo = 'X'; idCliente = $demo })
    Ver 'sin cliente' 400 (Req POST '/api/v1/reportes' @{ tipo = 'DESEMPENO' })
    Ver 'reporte inexistente' 404 (Req GET "/api/v1/reportes/$([guid]::NewGuid())/resultado" $null)
    $sinCuenta = Obj POST '/api/v1/reportes' @{ tipo = 'RANKING_RUTAS'; idCliente = (Id 'C01') }
    "     facturar cliente del catalogo: $(Req POST "/api/v1/reportes/$($sinCuenta.idSolicitud)/facturar" $null)"
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
    docker start $NJ 2>$null | Out-Null
    # Sus reportes lo referencian: no se puede borrar, queda inactivo.
    Sql "update tp_logistica.usuario set estado='INACTIVO' where email='$($env:APP_BOOTSTRAP_ADMIN_EMAIL)';" | Out-Null
}
