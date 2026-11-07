<#
  Comprueba que activar y desactivar tengan efecto real: con el registro inactivo la operacion se rechaza
  y, al activarlo, vuelve a funcionar. Corre con el backend apagado: levanta el suyo.
#>
param([string]$Scratch = $env:TEMP)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres)
$puerto = if ($env:API_PORT) { $env:API_PORT } else { '8080' }
$base = "http://localhost:$puerto"
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$marca = [guid]::NewGuid().ToString('N').Substring(0, 8)
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = "activar-admin-$marca@ejemplo.test"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
function Ver($texto, $esperado, $obtenido) {
    "$(if ("$esperado" -eq "$obtenido") { 'OK   ' } else { 'FALLA' }) ${texto}: esperado=$esperado obtenido=$obtenido"
}

$proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\activar.out.log" -RedirectStandardError "$Scratch\activar.err.log"
try {
    for ($i = 0; $i -lt 60; $i++) {
        if ($proc.HasExited) { 'FALLA el backend termino'; exit 1 }
        try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { Start-Sleep 2 }
    }
    Start-Sleep 6
    $login = @{ email = $env:APP_BOOTSTRAP_ADMIN_EMAIL; password = $env:APP_BOOTSTRAP_ADMIN_PASSWORD } | ConvertTo-Json
    $token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login).token
    $h = @{ Authorization = "Bearer $token" }
    function Llamar($metodo, $ruta, $cuerpo) {
        $datos = @{ Uri = "$base$ruta"; Method = $metodo; Headers = $h; ContentType = 'application/json; charset=utf-8' }
        if ($null -ne $cuerpo) { $datos.Body = [Text.Encoding]::UTF8.GetBytes(($cuerpo | ConvertTo-Json -Depth 5)) }
        Invoke-RestMethod @datos
    }
    function Estado($metodo, $ruta, $cuerpo) {
        try { Llamar $metodo $ruta $cuerpo | Out-Null; 'acepta' } catch { [int]$_.Exception.Response.StatusCode }
    }
    function Viaje($id) { @{ clientId = $id; origen = 'BUE'; destino = 'ROS'; aereo = $false; priority = 'NORMAL'; volumeM3 = 1 } }
    function Tramo($envio, $orden, $extra) {
        $t = @{ shipmentId = $envio; order = $orden; origin = 'A'; destination = 'B'; transportType = 'TERRESTRE'; state = 'PENDIENTE' }
        foreach ($k in $extra.Keys) { $t[$k] = $extra[$k] }
        $t
    }
    function Factura($tipo, $id, $clave) {
        @{ ownerType = $tipo; ownerId = $id; dueDate = (Get-Date).AddDays(30).ToString('yyyy-MM-dd'); idempotencyKey = $clave
           lines = @(@{ concept = 'Prueba'; quantity = 1; unitPrice = 10 }) }
    }

    '=== R01 activar un cliente'
    $cliente = Llamar POST '/api/v1/clientes' @{ businessName = "Activar $marca"; country = 'Argentina' }
    Llamar DELETE "/api/v1/clientes/$($cliente.id)" | Out-Null
    Ver 'cliente inactivo: no se le crea un envio' 409 (Estado POST '/api/v1/rutas/viajes' (Viaje $cliente.id))
    Ver 'cliente inactivo: no se le crea una factura' 409 (Estado POST '/api/v1/finanzas/facturas' (Factura 'CLIENTE' $cliente.id "ACT-C1-$marca"))
    Llamar PATCH "/api/v1/clientes/$($cliente.id)/activar" | Out-Null
    $envio = Llamar POST '/api/v1/rutas/viajes' (Viaje $cliente.id)
    Ver 'cliente activado: se le crea un envio' 'CREADO' $envio.state
    Ver 'cliente activado: se le crea una factura' 'acepta' (Estado POST '/api/v1/finanzas/facturas' (Factura 'CLIENTE' $cliente.id "ACT-C2-$marca"))

    '=== R04 activar un proveedor'
    $proveedor = Llamar POST '/api/v1/proveedores' @{ businessName = "Proveedor $marca"; country = 'Argentina' }
    $servicio = @{ suppliers = @(@{ supplierId = $proveedor.id; service = 'ADUANA'; cost = 10 }) }
    Llamar DELETE "/api/v1/proveedores/$($proveedor.id)" | Out-Null
    Ver 'proveedor inactivo: no se usa en un tramo' 409 (Estado POST '/api/v1/trayectos' (Tramo $envio.id 10 $servicio))
    Ver 'proveedor inactivo: no se le crea una factura' 409 (Estado POST '/api/v1/finanzas/facturas' (Factura 'PROVEEDOR' $proveedor.id "ACT-P1-$marca"))
    Llamar PATCH "/api/v1/proveedores/$($proveedor.id)/activar" | Out-Null
    Ver 'proveedor activado: se usa en un tramo' 'acepta' (Estado POST '/api/v1/trayectos' (Tramo $envio.id 10 $servicio))
    Ver 'proveedor activado: se le crea una factura' 'acepta' (Estado POST '/api/v1/finanzas/facturas' (Factura 'PROVEEDOR' $proveedor.id "ACT-P2-$marca"))

    '=== R05 activar un operador y un vehiculo'
    $operador = Llamar POST '/api/v1/operadores' @{ businessName = "Operador $marca"; country = 'Argentina' }
    Llamar DELETE "/api/v1/operadores/$($operador.id)" | Out-Null
    $pedido = @{ operatorId = $operador.id; type = 'CAMION'; identification = "AC$marca"; state = 'ACTIVO' }
    Ver 'operador inactivo: no se le crea un vehiculo' 409 (Estado POST '/api/v1/vehiculos' $pedido)
    Llamar PATCH "/api/v1/operadores/$($operador.id)/activar" | Out-Null
    $vehiculo = Llamar POST '/api/v1/vehiculos' $pedido
    Ver 'operador activado: se le crea un vehiculo' 'ACTIVO' $vehiculo.state
    $conVehiculo = @{ vehicleId = $vehiculo.id; operatorId = $operador.id; suppliers = @() }
    Llamar DELETE "/api/v1/vehiculos/$($vehiculo.id)" | Out-Null
    Ver 'vehiculo inactivo: no se usa en un tramo' 409 (Estado POST '/api/v1/trayectos' (Tramo $envio.id 11 $conVehiculo))
    Llamar PUT "/api/v1/vehiculos/$($vehiculo.id)" $pedido | Out-Null
    Ver 'vehiculo activado: se usa en un tramo' 'acepta' (Estado POST '/api/v1/trayectos' (Tramo $envio.id 11 $conVehiculo))

    '=== R09 activar una regla'
    $contenedor = Llamar POST '/api/v1/contenedores' @{ internationalCode = "AC$marca"; type = 'DRY'; capacity = 10; state = 'DISPONIBLE' }
    $sensor = Llamar POST '/api/v1/sensores' @{ type = 'TEMPERATURA'; manufacturer = 'Prueba'; state = 'ACTIVO' }
    Llamar POST "/api/v1/envios/$($envio.id)/contenedores" @{ containerId = $contenedor.id } | Out-Null
    Llamar POST '/api/v1/asignaciones-sensor' @{ sensorId = $sensor.id; containerId = $contenedor.id } | Out-Null
    Start-Sleep 1
    $pedidoRegla = @{ idSensor = $sensor.id; nombre = "Regla $marca"; tipoRegla = 'TEMPERATURA'; operador = '>'; umbral = 50; severidad = 'BAJA'; activa = $true }
    $regla = Llamar POST '/api/v1/reglas' $pedidoRegla
    function Lectura { @{ idLectura = [guid]::NewGuid().ToString(); idSensor = $sensor.id; timestamp = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ'); temperatura = 60; bateria = 90 } }
    function Alertas { $r = Llamar GET '/api/v1/alertas'; @($r | ForEach-Object { $_ } | Where-Object { $_.idRegla -eq $regla.idRegla }).Count }
    Llamar DELETE "/api/v1/reglas/$($regla.idRegla)" | Out-Null
    Llamar POST '/api/v1/iot/lecturas' (Lectura) | Out-Null
    Ver 'regla inactiva: la lectura no genera alerta' 0 (Alertas)
    Llamar PUT "/api/v1/reglas/$($regla.idRegla)" $pedidoRegla | Out-Null
    Llamar POST '/api/v1/iot/lecturas' (Lectura) | Out-Null
    Ver 'regla activada: la lectura genera alerta' 1 (Alertas)
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force; Start-Sleep 2 }
    "update tp_logistica.usuario set estado='INACTIVO' where email like 'activar-admin-%';" | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At' | Out-Null
    'admin temporal desactivado'
}
