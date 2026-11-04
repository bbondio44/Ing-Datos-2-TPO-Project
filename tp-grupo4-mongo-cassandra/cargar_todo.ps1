<#
  Ingeniería de Datos II · Grupo 4 · Carga de MongoDB y Cassandra en Docker (Windows)

  Uso (desde esta carpeta, en PowerShell):
      powershell -ExecutionPolicy Bypass -File .\cargar_todo.ps1
  o doble clic en cargar_todo.bat

  Detecta solos los contenedores por imagen (mongo / cassandra). Si tus contenedores
  tienen otra imagen o querés elegirlos a mano:
      .\cargar_todo.ps1 -Mongo mi_mongo -Cassandra mi_cassandra
  Si Mongo tiene usuario y contraseña:
      .\cargar_todo.ps1 -MongoUser admin -MongoPass secreto
  Solo una de las dos bases:
      .\cargar_todo.ps1 -SoloMongo      /      .\cargar_todo.ps1 -SoloCassandra

  Es re-ejecutable: vuelve a dejar las bases con el dataset de prueba sin duplicar.
  La salida de las consultas queda en .\evidencias\ (sirve como evidencia por requisito).
#>
param(
    [string]$Mongo = "",
    [string]$Cassandra = "",
    [string]$MongoDb = "logistica_docs",
    [string]$MongoUser = "",
    [string]$MongoPass = "",
    [string]$CassandraUser = "",
    [string]$CassandraPass = "",
    [switch]$SoloMongo,
    [switch]$SoloCassandra
)

# "Continue": cqlsh/mongosh escriben avisos por stderr; los errores se detectan por código de salida.
$ErrorActionPreference = "Continue"
$aqui = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $aqui
$evid = Join-Path $aqui "evidencias"
New-Item -ItemType Directory -Force -Path $evid | Out-Null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Titulo($t) { Write-Host ""; Write-Host "==== $t" -ForegroundColor Cyan }
function Fallar($t) { Write-Host "ERROR: $t" -ForegroundColor Red; Read-Host "Enter para salir"; exit 1 }

function Buscar-Contenedor($patron, $nombreDado) {
    if ($nombreDado) { return $nombreDado }
    $filas = docker ps --format "{{.Names}}|{{.Image}}" 2>$null
    if ($LASTEXITCODE -ne 0) { Fallar "No pude hablar con Docker. ¿Está abierto Docker Desktop?" }
    $hallados = @($filas | Where-Object { ($_ -split '\|')[1] -match $patron } | ForEach-Object { ($_ -split '\|')[0] })
    if ($hallados.Count -eq 0) { return $null }
    if ($hallados.Count -gt 1) { Write-Host "Hay varios contenedores de $patron ($($hallados -join ', ')); uso $($hallados[0]). Elegí otro con -Mongo/-Cassandra." -ForegroundColor Yellow }
    return $hallados[0]
}

function Ejecutar($descripcion, [scriptblock]$bloque, $archivoSalida) {
    Write-Host "-> $descripcion"
    $salida = & $bloque 2>&1 | ForEach-Object { "$_" }
    $codigo = $LASTEXITCODE
    if ($archivoSalida) { $salida | Out-File -Encoding utf8 (Join-Path $evid $archivoSalida) }
    $errores = @($salida | Where-Object { $_ -match 'SyntaxException|InvalidRequest|ConfigurationException|MongoServerError|requires authentication|Authentication failed|SyntaxError|ReferenceError' })
    if ($codigo -ne 0 -or $errores.Count -gt 0) {
        ($errores + ($salida | Select-Object -Last 15)) | Select-Object -First 25 | ForEach-Object { Write-Host "   $_" }
        if (($salida -join ' ') -match 'requires authentication|Authentication failed|Unauthorized') {
            Write-Host "   Tu MongoDB pide usuario y no lo encontré en el contenedor. Corré:" -ForegroundColor Yellow
            Write-Host "   .\cargar_todo.ps1 -SoloMongo -MongoUser <usuario> -MongoPass <clave>" -ForegroundColor Yellow
        }
        Fallar "$descripcion falló (código $codigo)"
    }
    return $salida
}

docker version --format "{{.Server.Version}}" *> $null
if ($LASTEXITCODE -ne 0) { Fallar "Docker no responde. Abrí Docker Desktop y volvé a correr el script." }

# =============================================================================== Cassandra
if (-not $SoloMongo) {
    Titulo "Cassandra"
    $c = Buscar-Contenedor "cassandra" $Cassandra
    if (-not $c) { Fallar "No encontré un contenedor de Cassandra corriendo (docker ps). Arrancalo o pasá -Cassandra <nombre>." }
    Write-Host "Contenedor: $c"
    $auth = @()
    if ($CassandraUser) { $auth = @("-u", $CassandraUser, "-p", $CassandraPass) }

    Write-Host "Esperando a que Cassandra acepte conexiones (puede tardar 1-2 minutos)..."
    $listo = $false
    for ($i = 0; $i -lt 60; $i++) {
        docker exec $c cqlsh @auth -e "SELECT release_version FROM system.local;" *> $null
        if ($LASTEXITCODE -eq 0) { $listo = $true; break }
        Start-Sleep -Seconds 5
    }
    if (-not $listo) { Fallar "Cassandra no respondió en 5 minutos." }

    docker exec $c mkdir -p /tmp/tp_grupo4 | Out-Null
    docker cp "$aqui/cassandra/." "${c}:/tmp/tp_grupo4/"
    if ($LASTEXITCODE -ne 0) { Fallar "No pude copiar los scripts al contenedor $c." }

    Ejecutar "01_schema.cql (keyspace logistica_iot + 5 tablas)" { docker exec $c cqlsh @auth -f /tmp/tp_grupo4/01_schema.cql } "cassandra_01_schema.txt" | Out-Null
    Write-Host "-> 02_datos_prueba.cql (2.772 lecturas en 5 tablas; tarda 1-3 minutos)"
    Ejecutar "02_datos_prueba.cql" { docker exec $c cqlsh @auth --request-timeout=60 -f /tmp/tp_grupo4/02_datos_prueba.cql } "cassandra_02_datos.txt" | Out-Null
    Ejecutar "03_consultas.cql (Q01-Q12)" { docker exec $c cqlsh @auth -f /tmp/tp_grupo4/03_consultas.cql } "cassandra_03_consultas.txt" | Out-Null
    $conteo = Ejecutar "Conteo de control" { docker exec $c cqlsh @auth -e "SELECT COUNT(*) FROM logistica_iot.lecturas_por_sensor_dia;" } $null
    Write-Host ("   filas en lecturas_por_sensor_dia: " + (($conteo | Where-Object { $_ -match '^\s*\d+\s*$' }) -join '').Trim() + "   (esperado: 2773, incluye la lectura de la prueba Q01)")
    Write-Host "Salida de las consultas: evidencias\cassandra_03_consultas.txt" -ForegroundColor Green
}

# =============================================================================== MongoDB
if (-not $SoloCassandra) {
    Titulo "MongoDB"
    $m = Buscar-Contenedor "mongo" $Mongo
    if (-not $m) { Fallar "No encontré un contenedor de MongoDB corriendo (docker ps). Arrancalo o pasá -Mongo <nombre>." }
    Write-Host "Contenedor: $m"

    # Si no se pasó usuario, se toma el que se usó al crear el contenedor
    # (variables MONGO_INITDB_ROOT_USERNAME / MONGO_INITDB_ROOT_PASSWORD de la imagen oficial).
    if (-not $MongoUser) {
        $envs = docker inspect $m --format "{{range .Config.Env}}{{println .}}{{end}}" 2>$null
        foreach ($linea in $envs) {
            if ($linea -match '^MONGO_INITDB_ROOT_USERNAME=(.+)$') { $MongoUser = $Matches[1].Trim() }
            if ($linea -match '^MONGO_INITDB_ROOT_PASSWORD=(.+)$') { $MongoPass = $Matches[1].Trim() }
        }
        if ($MongoUser) { Write-Host "Usando el usuario de Mongo configurado en el contenedor: $MongoUser" }
    }

    docker exec $m sh -c "command -v mongosh" *> $null
    if ($LASTEXITCODE -eq 0) { $shell = "mongosh" } else {
        docker exec $m sh -c "command -v mongo" *> $null
        if ($LASTEXITCODE -ne 0) { Fallar "El contenedor $m no tiene mongosh ni mongo." }
        $shell = "mongo"
        Write-Host "Aviso: la imagen usa el shell viejo 'mongo'. Se recomienda MongoDB 6 o 7 (\$unionWith requiere 4.4+)." -ForegroundColor Yellow
    }
    $args_m = @("--quiet")
    if ($MongoUser) { $args_m += @("-u", $MongoUser, "-p", $MongoPass, "--authenticationDatabase", "admin") }

    docker exec $m mkdir -p /tmp/tp_grupo4 | Out-Null
    docker cp "$aqui/mongodb/." "${m}:/tmp/tp_grupo4/"
    if ($LASTEXITCODE -ne 0) { Fallar "No pude copiar los scripts al contenedor $m." }

    Ejecutar "01_schema.js (5 colecciones con validador e índices)" { docker exec $m $shell @args_m $MongoDb /tmp/tp_grupo4/01_schema.js } "mongodb_01_schema.txt" | ForEach-Object { Write-Host "   $_" }
    Ejecutar "02_datos_prueba.js (207 documentos)" { docker exec $m $shell @args_m $MongoDb /tmp/tp_grupo4/02_datos_prueba.js } "mongodb_02_datos.txt" | ForEach-Object { Write-Host "   $_" }
    $res = Ejecutar "03_consultas.js (verificación por requisito)" { docker exec $m $shell @args_m $MongoDb /tmp/tp_grupo4/03_consultas.js } "mongodb_03_consultas.txt"
    $res | Where-Object { $_ -match '^(OK|FALLA|RESUMEN)' } | ForEach-Object {
        if ($_ -match '^FALLA') { Write-Host "   $_" -ForegroundColor Red } elseif ($_ -match '^RESUMEN') { Write-Host "   $_" -ForegroundColor Green } else { Write-Host "   $_" }
    }
    Write-Host "Salida completa: evidencias\mongodb_03_consultas.txt" -ForegroundColor Green
}

Titulo "Listo"
Write-Host "Conexión para la app Spring Boot:"
Write-Host "  spring.data.mongodb.uri=mongodb://localhost:27017/$MongoDb"
Write-Host "  spring.cassandra.contact-points=localhost:9042"
Write-Host "  spring.cassandra.keyspace-name=logistica_iot"
Write-Host "  spring.cassandra.local-datacenter=datacenter1"
Read-Host "Enter para cerrar"
