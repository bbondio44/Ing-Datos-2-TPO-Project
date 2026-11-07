<#
  Prueba de un viaje simulado de punta a punta: envio con ruta, contenedor, sensor, Iniciar viaje,
  posiciones en Cassandra y llegada a destino. Corre con el backend apagado: levanta el suyo con
  2 segundos por punto para no esperar el viaje completo.
#>
param([string]$Scratch = $env:TEMP)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres)
$puerto = if ($env:API_PORT) { $env:API_PORT } else { '8080' }
$base = "http://localhost:$puerto"
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$marca = [guid]::NewGuid().ToString('N').Substring(0, 8)
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'
$env:APP_VIAJES_SEGUNDOS = '2'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = "viaje-admin-$marca@ejemplo.test"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
function Ver($texto, $esperado, $obtenido) {
    "$(if ("$esperado" -eq "$obtenido") { 'OK   ' } else { 'FALLA' }) ${texto}: esperado=$esperado obtenido=$obtenido"
}
function Verificar($texto, $condicion) { "$(if ($condicion) { 'OK   ' } else { 'FALLA' }) $texto" }
function Estado($bloque) {
    try { & $bloque | Out-Null; 200 } catch { [int]$_.Exception.Response.StatusCode }
}

$proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\viaje.out.log" -RedirectStandardError "$Scratch\viaje.err.log"
try {
    for ($i = 0; $i -lt 60; $i++) {
        if ($proc.HasExited) { 'FALLA el backend termino'; exit 1 }
        try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { Start-Sleep 2 }
    }
    Start-Sleep 8
    $login = @{ email = $env:APP_BOOTSTRAP_ADMIN_EMAIL; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json
    $token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login).token
    $h = @{ Authorization = "Bearer $token" }
    function Lista($ruta) { $r = Invoke-RestMethod "$base$ruta" -Headers $h; ,@($r | ForEach-Object { $_ }) }
    function Saldo { $cuenta = (Lista '/api/v1/finanzas/cuentas') | Where-Object { $_.ownerId -eq $cliente.id }; [decimal]$cuenta.balance }
    function Post($ruta, $cuerpo) {
        Invoke-RestMethod "$base$ruta" -Method Post -Headers $h -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes(($cuerpo | ConvertTo-Json)))
    }

    '=== R20 R24 ruta por ciudades y precio'
    $cotizacion = Invoke-RestMethod "$base/api/v1/rutas/cotizacion?origen=BUE&destino=PSS&volumen=10" -Headers $h
    Ver 'Buenos Aires a Posadas pasa por 4 tramos' 4 @($cotizacion.tramos).Count
    Ver 'el total es km por m3 por precio' ([int]($cotizacion.km * 10 * 15)) ([int]$cotizacion.total)
    $cliente = @((Invoke-RestMethod "$base/api/v1/clientes?buscar=&pagina=0&tamanio=100" -Headers $h).content | Where-Object state -eq 'ACTIVO')[0]
    $envio = Post '/api/v1/rutas/viajes' @{ clientId = $cliente.id; origen = 'BUE'; destino = 'SFE'; aereo = $false; priority = 'NORMAL'; volumeM3 = 10 }
    $tramos = Lista "/api/v1/trayectos?envioId=$($envio.id)"
    Ver 'el envio se crea con sus tramos' 2 $tramos.Count
    Ver 'el primer tramo llega a Rosario' 'Rosario' $tramos[0].destination

    '=== R20 sin sensor no se puede iniciar'
    Ver 'iniciar sin sensor se rechaza' 409 (Estado { Post "/api/v1/rutas/viajes/$($envio.id)/iniciar" @{} })

    '=== R18 R20 R21 viaje simulado'
    $contenedor = Post '/api/v1/contenedores' @{ internationalCode = "VJ$marca"; type = 'REEFER'; capacity = 30; state = 'DISPONIBLE' }
    $sensor = Post '/api/v1/sensores' @{ type = 'TEMPERATURA'; manufacturer = 'Prueba'; state = 'ACTIVO' }
    Post "/api/v1/envios/$($envio.id)/contenedores" @{ containerId = $contenedor.id } | Out-Null
    Post '/api/v1/asignaciones-sensor' @{ sensorId = $sensor.id; containerId = $contenedor.id } | Out-Null
    $inicio = (Get-Date).ToUniversalTime().AddMinutes(-1).ToString('yyyy-MM-ddTHH:mm:ssZ')
    $iniciado = Post "/api/v1/rutas/viajes/$($envio.id)/iniciar" @{}
    Ver 'al iniciar queda EN_TRANSITO' 'EN_TRANSITO' $iniciado.state
    $estado = ''
    for ($i = 0; $i -lt 45; $i++) {
        Start-Sleep 2
        $estado = (Invoke-RestMethod "$base/api/v1/envios/$($envio.id)" -Headers $h).shipment.state
        if ($estado -eq 'ENTREGADO') { break }
    }
    Ver 'al llegar queda ENTREGADO' 'ENTREGADO' $estado
    $fin = (Get-Date).ToUniversalTime().AddMinutes(1).ToString('yyyy-MM-ddTHH:mm:ssZ')
    $libres = Lista '/api/v1/sensores/libres'
    Verificar 'al llegar el sensor queda libre' (@($libres | Where-Object { $_.id -eq $sensor.id }).Count -eq 1)
    $catalogo = (Invoke-RestMethod "$base/api/v1/contenedores?buscar=VJ$marca&pagina=0&tamanio=10" -Headers $h).content
    Ver 'al llegar el contenedor queda disponible' 'DISPONIBLE' @($catalogo)[0].state
    $tramos = Lista "/api/v1/trayectos?envioId=$($envio.id)"
    Ver 'todos los tramos terminan' 2 @($tramos | Where-Object state -eq 'FINALIZADO').Count
    Ver 'el costo real es el calculado' ([int]($tramos | Measure-Object estimatedCost -Sum).Sum) ([int]($tramos | Measure-Object actualCost -Sum).Sum)
    $posiciones = Lista "/api/v1/iot/contenedores/$($contenedor.id)/recorrido?desde=$inicio&hasta=$fin"
    Verificar "el contenedor registra posiciones en Cassandra ($($posiciones.Count))" ($posiciones.Count -ge 2)
    $fuera = @($posiciones | Where-Object { $_.latitud -lt -34.7 -or $_.latitud -gt -31.5 -or $_.longitud -lt -60.9 -or $_.longitud -gt -58.3 })
    Ver 'todas las posiciones quedan entre origen y destino' 0 $fuera.Count
    Ver 'un viaje terminado no se puede reiniciar' 409 (Estado { Post "/api/v1/rutas/viajes/$($envio.id)/iniciar" @{} })

    '=== R12 R13 factura y cobro mensual'
    $mes = @{ period = (Get-Date).ToUniversalTime().ToString('yyyy-MM-01') }
    $saldoAntes = Saldo
    $factura = Post '/api/v1/finanzas/facturacion-mensual' $mes
    Verificar "la corrida crea facturas ($($factura.created)) para los envios ($($factura.processed))" ($factura.created -ge 1 -and $factura.processed -ge 1)
    $saldoDespues = Saldo
    Verificar "el saldo del cliente sube al menos el viaje ($saldoAntes a $saldoDespues)" (($saldoDespues - $saldoAntes) -ge 79650)
    Ver 'repetir la facturacion no crea nada' 0 (Post '/api/v1/finanzas/facturacion-mensual' $mes).created
    $cobro = Post '/api/v1/finanzas/cobro-mensual' $mes
    Verificar "el cobro registra pagos ($($cobro.collected))" ($cobro.collected -ge 1)
    $saldoFinal = Saldo
    Ver 'el cobro devuelve el saldo al anterior' ([int]$saldoAntes) ([int]$saldoFinal)
    Ver 'repetir el cobro no cobra nada' 0 (Post '/api/v1/finanzas/cobro-mensual' $mes).collected
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force; Start-Sleep 2 }
    "update tp_logistica.usuario set estado='INACTIVO' where email like 'viaje-admin-%';" | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At' | Out-Null
    'admin temporal desactivado'
}
