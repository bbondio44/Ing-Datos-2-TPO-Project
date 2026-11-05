# Neo4j: respaldo e integración

## Fuente de datos

El único respaldo incluido en el repositorio es [82cf1a53-2026-10-30T19-12-16-82cf1a53.backup](82cf1a53-2026-10-30T19-12-16-82cf1a53.backup). Se conserva sin modificar. Huella registrada al revisar este documento: SHA-256 `58EFAB4DDBE8290023002B6FCCDE7D08D60F1BC53EEC886C7103183C391AE690` (10,590 bytes).

Neo4j Admin identificó el archivo como `Neo4j Full Backup`, base `82cf1a53`, formato `Block`, 77 archivos y 1,502,097 bytes descomprimidos. No es un `.dump` convencional. El formato requiere Neo4j Enterprise; la versión 5.26.5 era anterior al store y no pudo cargarlo. La restauración aislada se completó con Enterprise 2026.10.1. No hay un archivo `init_and_seed.cypher` en el repositorio. `neo4jcode.txt` contiene una carga distinta con identificadores de ejemplo y una operación `DETACH DELETE`; no ejecutarla sobre el respaldo ni sobre una base existente.

El respaldo queda como referencia histórica y no se carga en el entorno de desarrollo. No se deben crear grafos de demostración a mano: el grafo se llena con el worker de `outbox_evento`, usando los UUID de PostgreSQL. Las consultas existentes en documentación anterior se retiraron porque no fueron comprobadas contra el contenido real del archivo. Para volver a consultar el respaldo hace falta abrirlo con la versión/herramienta compatible, preservando el original.

## Conexión de la aplicación

Decisión del grupo (04/11/2026): en desarrollo se usa el servicio `neo4j` del `docker-compose.yml` de la raíz, en edición Community. Escucha en Browser `http://localhost:7474` y Bolt `bolt://localhost:7687`, y la base es `neo4j`, que arranca vacía. El compose necesita `NEO4J_USER` y `NEO4J_PASSWORD` en `.env`. Para conectar la aplicación se debe activar Spring `dev,polyglot`; `NEO4J_URI` y `NEO4J_DATABASE` usan esos valores por defecto. Si un `.env` anterior define `NEO4J_DATABASE=82cf1a53`, hay que cambiarlo, porque esa base no existe en Community. Community no puede abrir el respaldo; para eso haría falta una versión Enterprise compatible.

Con Neo4j configurado, `GET /api/v1/integracion/estado` comprueba la conectividad Bolt. La consulta de red de solo lectura está en `GET /api/v1/grafo/clientes/{clientId}/red` y requiere `LOGISTICA_LEER`; los usuarios cliente solo pueden consultar su propio grafo y usuarios con `ENVIOS_LEER_TODOS` pueden consultar cualquier identificador. El grafo lo escribe `OutboxNeo4jWorker`, que lee `outbox_evento` cada 5 segundos y aplica cada evento con `MERGE`, así reprocesar no duplica. Crea las restricciones de unicidad al arrancar. Además de las relaciones del contrato, agrega `OPERADO_POR` entre tramo y operador, porque un tramo puede tener operador sin vehículo cargado.

## Validación histórica del respaldo

Esta sección describe una restauración anterior en una instancia Enterprise aislada (contenedor `neo4j-demo-restore-82cf1a53`, Browser `http://localhost:7475`, Bolt `bolt://localhost:7688`, base `82cf1a53`). Ese contenedor no forma parte del entorno de desarrollo del grupo. El respaldo usa IDs como `id_cliente="cli-001"`, que no son UUID.

La copia aislada se restauró y registró online. El backup original continúa sin modificar con SHA-256 `58EFAB4DDBE8290023002B6FCCDE7D08D60F1BC53EEC886C7103183C391AE690`. Se aceptó la licencia Enterprise únicamente para esta instancia local por autorización expresa del usuario. La prueba Cypher de R24 devolvió el trayecto real; R30 devolvió el grafo de `cli-001` y un `inc-901` sin duplicados. El backend compila y sus pruebas pasan; aún no se ejecutó el endpoint HTTP porque el backend también requiere PostgreSQL.