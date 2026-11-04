# Neo4j: respaldo e integración

## Fuente de datos

El único respaldo incluido en el repositorio es [82cf1a53-2026-10-30T19-12-16-82cf1a53.backup](82cf1a53-2026-10-30T19-12-16-82cf1a53.backup). Se conserva sin modificar. Huella registrada al revisar este documento: SHA-256 `58EFAB4DDBE8290023002B6FCCDE7D08D60F1BC53EEC886C7103183C391AE690` (10,590 bytes).

Neo4j Admin identificó el archivo como `Neo4j Full Backup`, base `82cf1a53`, formato `Block`, 77 archivos y 1,502,097 bytes descomprimidos. No es un `.dump` convencional. El formato requiere Neo4j Enterprise; la versión 5.26.5 era anterior al store y no pudo cargarlo. La restauración aislada se completó con Enterprise 2026.10.1. No hay un archivo `init_and_seed.cypher` en el repositorio. `neo4jcode.txt` contiene una carga distinta con identificadores de ejemplo y una operación `DETACH DELETE`; no ejecutarla sobre el respaldo ni sobre una base existente.

No se deben crear datos, restricciones ni grafos de demostración para sustituir este respaldo. Las consultas existentes en documentación anterior se retiraron porque no fueron comprobadas contra el contenido real del archivo. Para validar el esquema y los requisitos, primero hace falta abrir el respaldo con la versión/herramienta compatible, preservando el original.

## Conexión de la aplicación

El contenedor aislado restaurado se llama `neo4j-demo-restore-82cf1a53`, usa el volumen `neo4j_demo_enterprise_2026_82cf1a53` y escucha solo en localhost: Browser `http://localhost:7475`, Bolt `bolt://localhost:7688`. La base es `82cf1a53`. Para conectar la aplicación se debe activar Spring `dev,polyglot` y configurar `NEO4J_URI=bolt://localhost:7688`; `NEO4J_DATABASE` usa `82cf1a53` por defecto. El servicio de Compose sigue usando Community y no sirve para abrir este backup sin cambiarlo a una versión Enterprise compatible.

Con Neo4j configurado, `GET /api/v1/integracion/estado` comprueba la conectividad Bolt. La consulta de red de solo lectura está en `GET /api/v1/grafo/clientes/{clientId}/red` y requiere `LOGISTICA_LEER`; los usuarios cliente solo pueden consultar su propio grafo y usuarios con `ENVIOS_LEER_TODOS` pueden consultar cualquier identificador. El backup usa `id_cliente="cli-001"` (no UUID), por lo que este dato demo debe consultarse con permisos globales. La integración no escribe en el grafo.

## Estado de validación

La copia aislada se restauró y registró online. El backup original continúa sin modificar con SHA-256 `58EFAB4DDBE8290023002B6FCCDE7D08D60F1BC53EEC886C7103183C391AE690`. Se aceptó la licencia Enterprise únicamente para esta instancia local por autorización expresa del usuario. La prueba Cypher de R24 devolvió el trayecto real; R30 devolvió el grafo de `cli-001` y un `inc-901` sin duplicados. El backend compila y sus pruebas pasan; aún no se ejecutó el endpoint HTTP porque el backend también requiere PostgreSQL.