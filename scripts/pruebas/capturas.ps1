<#
  Prueba las pantallas JavaFX de verdad: abre la aplicacion, la maneja como un usuario y guarda capturas en
  evidencias/RXX, mas el detalle en evidencias/pantallas-javafx.txt. Hace falta una sesion de Windows activa:
  la aplicacion aparece en pantalla mientras corre, sin tocarla. Tarda unos 3 minutos.
  Uso, con el backend apagado: mvn -B -DskipTests install y despues este script.
#>
param([string]$Scratch = $env:TEMP)
$ErrorActionPreference = 'Continue'
$PG = (docker ps -q --filter name=postgres)
$base = 'http://localhost:8080'
$jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
$marca = [guid]::NewGuid().ToString('N').Substring(0, 8)
$env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'
Remove-Item Env:PG_JDBC_URL -ErrorAction SilentlyContinue
$env:APP_BOOTSTRAP_ADMIN_EMAIL = "captura-admin-$marca@ejemplo.test"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
$env:CAPTURA_ADMIN_EMAIL = $env:APP_BOOTSTRAP_ADMIN_EMAIL
$env:CAPTURA_ADMIN_PASSWORD = $env:APP_BOOTSTRAP_ADMIN_PASSWORD
$env:CAPTURA_CLIENTE_EMAIL = "captura-cliente-$marca@ejemplo.test"
$env:CAPTURA_CLIENTE_PASSWORD = [guid]::NewGuid().ToString('N')

function Limpiar {
    Get-ChildItem evidencias -Recurse -Filter 'captura-*.png' | Remove-Item -Force
}

$proc = $null
try {
    Limpiar
    $cp = "$Scratch\desktop.cp"
    cmd /c "mvn -B -q -pl desktop test-compile dependency:build-classpath -Dmdep.outputFile=$cp -Dmdep.includeScope=test 2>&1" | Out-Null
    $proc = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput "$Scratch\cap.out.log" -RedirectStandardError "$Scratch\cap.err.log"
    for ($i = 0; $i -lt 60; $i++) {
        if ($proc.HasExited) { 'backend termino'; exit 1 }
        try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; break } catch { Start-Sleep 2 }
    }
    $login = @{ email = $env:CAPTURA_ADMIN_EMAIL; password = $env:CAPTURA_ADMIN_PASSWORD } | ConvertTo-Json
    $token = (Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $login).token
    $h = @{ Authorization = "Bearer $token" }
    $rol = ((Invoke-RestMethod "$base/api/v1/admin/roles" -Headers $h) | ForEach-Object { $_ } | Where-Object description -eq 'CLIENTE').id
    $usuario = @{ clientId = '270823a9-7978-5eda-80f0-9ae673331b0b'; firstName = 'Captura'; lastName = 'Cliente'
        email = $env:CAPTURA_CLIENTE_EMAIL; password = $env:CAPTURA_CLIENTE_PASSWORD; roleIds = @($rol) } | ConvertTo-Json
    Invoke-RestMethod "$base/api/v1/admin/usuarios" -Method Post -Headers $h -ContentType 'application/json' -Body $usuario | Out-Null

    $clases = "desktop\target\test-classes;desktop\target\classes;$(Get-Content $cp -Raw)".Trim()
    & java -cp $clases 'ar.edu.uade.logistica.desktop.CapturasPantallas$Lanzador' 2>&1 | ForEach-Object { "$_" }
    "codigo de salida de la aplicacion: $LASTEXITCODE"
}
finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force; Start-Sleep 2 }
    "update tp_logistica.usuario set estado='INACTIVO' where email like 'captura-%$marca@ejemplo.test';" | docker exec -i $PG sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At' | Out-Null
    'usuarios temporales desactivados'
}
