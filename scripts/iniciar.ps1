<#
  Arranque de un solo paso: bases, carga inicial si estan vacias, backend y aplicacion JavaFX.
  Se usa con iniciar.bat, desde la raiz del repo. Al cerrar la aplicacion apaga el backend;
  las bases quedan prendidas (se apagan con: docker compose stop).
#>
$ErrorActionPreference = 'Continue'
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
$base = 'http://localhost:8080'
$mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else { '.\mvnw.cmd' }

function Salir($texto) { ''; "ERROR: $texto"; ''; exit 1 }
function Responde { try { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 3 | Out-Null; $true } catch { $false } }

'[1/5] Revisando lo necesario'
if (-not (Test-Path .env)) {
    # Primera vez en esta compu: las claves se generan aca y quedan solo en .env, que no se sube al repo.
    $secreto = [guid]::NewGuid().ToString('N')
    $claveAdmin = [guid]::NewGuid().ToString('N').Substring(0, 16)
    $texto = [IO.File]::ReadAllText((Join-Path $raiz '.env.example'))
    $texto = $texto.Replace('reemplazar-con-secreto-local', $secreto).Replace('change-me', $secreto)
    $texto = $texto.Replace('APP_BOOTSTRAP_ADMIN_EMAIL=', 'APP_BOOTSTRAP_ADMIN_EMAIL=admin@grupo4.local')
    $texto = $texto.Replace('APP_BOOTSTRAP_ADMIN_PASSWORD=', "APP_BOOTSTRAP_ADMIN_PASSWORD=$claveAdmin")
    [IO.File]::WriteAllText((Join-Path $raiz '.env'), $texto, (New-Object Text.UTF8Encoding $false))
    ''
    'Primera vez: cree el archivo .env. Para entrar a la aplicacion:'
    '   Email:      admin@grupo4.local'
    "   Contrasena: $claveAdmin   (queda guardada en .env)"
    ''
}
if (-not (Select-String -Path .env -Pattern '^APP_BOOTSTRAP_ADMIN_EMAIL=.+' -Quiet)) {
    'Aviso: .env no define APP_BOOTSTRAP_ADMIN_EMAIL y APP_BOOTSTRAP_ADMIN_PASSWORD. Sin eso no hay usuario para entrar.'
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Salir 'No encontre Java. Hace falta el JDK 21.' }
docker info *> $null
if ($LASTEXITCODE -ne 0) { Salir 'Docker no responde. Abri Docker Desktop y volve a intentar.' }

'[2/5] Levantando las cuatro bases (Cassandra tarda hasta un minuto)'
docker compose up -d --wait
if ($LASTEXITCODE -ne 0) { Salir 'Las bases no arrancaron. Si hay otro compose prendido usa los mismos puertos: apagalo y reintenta.' }

'[3/5] Revisando los datos'
$pg = docker compose ps -q postgres
$hay = "select count(*) from information_schema.tables where table_schema='tp_logistica' and table_name='cliente';" | docker exec -i $pg sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At'
if ("$hay".Trim() -eq '0') {
    'Base vacia: cargando los datos por primera vez'
    powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\cargar-postgres.ps1
    if ($LASTEXITCODE -ne 0) { Salir 'No se pudo cargar PostgreSQL.' }
    $mo = docker compose ps -q mongo
    $usuario = docker exec $mo printenv MONGO_INITDB_ROOT_USERNAME
    $clave = docker exec $mo printenv MONGO_INITDB_ROOT_PASSWORD
    powershell -NoProfile -ExecutionPolicy Bypass -File .\tp-grupo4-mongo-cassandra\cargar_todo.ps1 -MongoUser $usuario -MongoPass $clave
    Set-Location $raiz
} else {
    'Los datos ya estan cargados'
}

'[4/5] Arrancando el backend'
$jar = Get-ChildItem backend\target\backend-*.jar -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
# Despues de un git pull el compilado queda viejo: se compara con la fuente mas nueva.
$fuente = Get-ChildItem backend\src, desktop\src\main, shared-contracts\src -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar -or -not (Test-Path desktop\target\classes) -or $fuente.LastWriteTime -gt $jar.LastWriteTime) {
    if (Responde) { Salir 'Hay una version vieja corriendo. Cerra la aplicacion y su ventana negra, y volve a abrir iniciar.bat.' }
    'Compilando la ultima version del proyecto (puede tardar unos minutos)'
    & $mvn -B -q -DskipTests install
    if ($LASTEXITCODE -ne 0) { Salir 'No se pudo compilar el proyecto.' }
    $jar = Get-ChildItem backend\target\backend-*.jar | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
}
$backend = $null
if (Responde) {
    'Ya hay un backend corriendo en el puerto 8080: uso ese'
} else {
    $env:SPRING_PROFILES_ACTIVE = 'dev,polyglot'
    New-Item -ItemType Directory -Force logs | Out-Null
    $backend = Start-Process java -ArgumentList '-jar', $jar.FullName -PassThru -WindowStyle Hidden -RedirectStandardOutput logs\backend.log -RedirectStandardError logs\backend-error.log
    $listo = $false
    for ($i = 0; $i -lt 60; $i++) {
        if ($backend.HasExited) { Salir 'El backend se cerro al arrancar. Mira logs\backend.log.' }
        if (Responde) { $listo = $true; break }
        Start-Sleep 2
    }
    if (-not $listo) { Stop-Process -Id $backend.Id -Force; Salir 'El backend no respondio en 2 minutos. Mira logs\backend.log.' }
}

'[5/5] Abriendo la aplicacion. Para terminar, cerra la ventana.'
try {
    & $mvn -B -q -pl desktop javafx:run
} finally {
    if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force; 'Backend apagado' }
}
'Las bases siguen prendidas. Para apagarlas: docker compose stop'
