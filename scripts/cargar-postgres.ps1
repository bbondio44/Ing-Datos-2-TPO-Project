<#
  Carga PostgreSQL desde cero: esquema, datos demo, pruebas de aceptacion y catalogo compartido.
  Uso, desde la raiz del repo y con los contenedores arriba:
      powershell -ExecutionPolicy Bypass -File .\scripts\cargar-postgres.ps1
  Solo corre sobre una base nueva: si el esquema tp_logistica ya existe, no hace nada.
#>
param([string]$Base = '')
$ErrorActionPreference = 'Continue'
$c = docker ps -q --filter name=postgres
if (-not $c) { 'No encontre el contenedor de postgres. Corre docker compose up -d'; exit 1 }
$db = if ($Base) { $Base } else { '$POSTGRES_DB' }
$psql = "psql -v ON_ERROR_STOP=1 -q -U `$POSTGRES_USER -d $db"

$existe = "select count(*) from information_schema.schemata where schema_name='tp_logistica';" | docker exec -i $c sh -c "$psql -At"
$esquema = '00_esquema.sql', '01_reglas_y_procesos.sql', '02_vistas_y_analitica.sql', '03_seguridad.sql'
if ("$existe".Trim() -ne '0') {
    # infra/docker-compose.yml carga el esquema solo al crear el volumen: ahi faltan solo los datos.
    $clientes = "select count(*) from tp_logistica.cliente;" | docker exec -i $c sh -c "$psql -At"
    if ("$clientes".Trim() -ne '0') { 'La base ya tiene datos: no cargo nada para no duplicarlos.'; exit 1 }
    $esquema = @()
}

docker exec $c mkdir -p /tmp/tp_pg
docker cp database/init/. "${c}:/tmp/tp_pg/" 2>$null | Out-Null
foreach ($f in '03_datos_demo.sql', '04_pruebas_aceptacion.sql', '06_datos_catalogo.sql', '07_ajustes_catalogo.sql', '08_incidentes_catalogo.sql') {
    docker cp "database/referencia-postgresql/$f" "${c}:/tmp/tp_pg/" 2>$null | Out-Null
}
# 04 va antes de 06 porque verifica los totales del dataset chico.
foreach ($f in ($esquema + '03_datos_demo.sql', '04_pruebas_aceptacion.sql', '06_datos_catalogo.sql', '07_ajustes_catalogo.sql', '08_incidentes_catalogo.sql')) {
    docker exec $c sh -c "$psql -f /tmp/tp_pg/$f" *> $null
    if ($LASTEXITCODE -ne 0) { "ERROR al cargar $f"; exit 1 }
    "OK $f"
}
"select 'clientes', count(*) from tp_logistica.cliente union all select 'envios', count(*) from tp_logistica.envio union all select 'sensores', count(*) from tp_logistica.sensor_iot;" | docker exec -i $c sh -c "$psql -At"
