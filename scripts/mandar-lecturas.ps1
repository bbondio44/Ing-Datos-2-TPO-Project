<#
  Manda lecturas de un sensor al backend, para probar a mano la ingesta, las alertas y las anomalias.
  Con la aplicacion abierta, desde la raiz del repo:
      .\scripts\mandar-lecturas.ps1 -Email tu@correo -Clave tuclave -Cantidad 12
      .\scripts\mandar-lecturas.ps1 -Email tu@correo -Clave tuclave -Temperatura 45
      .\scripts\mandar-lecturas.ps1 -Email tu@correo -Clave tuclave -Bateria 10
  -Cantidad manda una serie, una lectura por minuto hasta ahora, con la temperatura casi fija.
#>
param(
    [Parameter(Mandatory = $true)][string]$Email,
    [Parameter(Mandatory = $true)][string]$Clave,
    [double]$Temperatura = 5,
    [double]$Bateria = 80,
    [int]$Cantidad = 1,
    [string]$Sensor = 'cb6a673c-14a5-5469-a365-30fe9700c80b',
    [string]$Base = 'http://localhost:8080'
)
$ErrorActionPreference = 'Stop'
$login = @{ email = $Email; password = $Clave } | ConvertTo-Json
try {
    $token = (Invoke-RestMethod "$Base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login).token
} catch {
    'No pude entrar: revisa que la aplicacion este abierta y que el email y la clave sean correctos.'
    exit 1
}
$ahora = (Get-Date).ToUniversalTime()
for ($i = 0; $i -lt $Cantidad; $i++) {
    $momento = $ahora.AddMinutes($i - $Cantidad + 1)
    $valor = if ($Cantidad -gt 1) { $Temperatura + ($i % 3) * 0.5 } else { $Temperatura }
    $lectura = @{
        idLectura = "$([guid]::NewGuid())"; idSensor = $Sensor; timestamp = $momento.ToString('yyyy-MM-ddTHH:mm:ss.fffZ')
        temperatura = $valor; humedad = 60.0; vibracion = 0.2; bateria = $Bateria
        latitud = -34.60 + $i * 0.02; longitud = -58.37 + $i * 0.02
    } | ConvertTo-Json
    try {
        Invoke-RestMethod "$Base/api/v1/iot/lecturas" -Method Post -ContentType 'application/json' -Body $lectura -Headers @{ Authorization = "Bearer $token" } | Out-Null
        "Lectura enviada: $valor grados, bateria $Bateria"
    } catch {
        "No se pudo enviar la lectura: $($_.Exception.Message)"
        exit 1
    }
}
