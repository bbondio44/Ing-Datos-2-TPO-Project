<#
  Carga clientes, proveedores y operadores en distintas situaciones para probar facturacion a mano.
  Uso, desde la raiz del repo y con las bases cargadas:
      powershell -ExecutionPolicy Bypass -File .\scripts\cargar-datos-facturacion.ps1
  Se puede correr dos veces sin duplicar. Conviene cargarlos despues de las pruebas automaticas,
  porque esas pruebas comparan los totales del dataset original.
#>
$ErrorActionPreference = 'Continue'
$c = docker compose ps -q postgres
if (-not $c) { 'No encontre el contenedor de postgres. Corre docker compose up -d'; exit 1 }
docker cp database/referencia-postgresql/09_datos_facturacion.sql "${c}:/tmp/09_datos_facturacion.sql" 2>$null | Out-Null
docker exec $c sh -c 'psql -q -v ON_ERROR_STOP=1 -U $POSTGRES_USER -d $POSTGRES_DB -f /tmp/09_datos_facturacion.sql'
if ($LASTEXITCODE -ne 0) { 'ERROR al cargar los datos de facturacion'; exit 1 }
$consulta = @'
select coalesce(cl.razon_social, p.razon_social, o.razon_social), c.naturaleza, c.saldo
  from tp_logistica.cuenta_corriente c
  left join tp_logistica.cliente cl using(id_cliente)
  left join tp_logistica.proveedor p using(id_proveedor)
  left join tp_logistica.operador_logistico o using(id_operador)
 where coalesce(cl.razon_social, p.razon_social, o.razon_social) like 'Prueba Fact%'
 order by 1;
'@
$consulta | docker exec -i $c sh -c 'psql -q -U $POSTGRES_USER -d $POSTGRES_DB -At'
