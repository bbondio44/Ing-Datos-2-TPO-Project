# R30 — Red de relaciones Neo4j

Estado: `PROBADO` mediante Cypher de solo lectura contra el backup real restaurado.

Fuente: `neo4jDB/82cf1a53-2026-10-30T19-12-16-82cf1a53.backup`, SHA-256 `58EFAB4DDBE8290023002B6FCCDE7D08D60F1BC53EEC886C7103183C391AE690`. Restaurado como base `82cf1a53` en Neo4j Enterprise 2026.10.1 dentro del volumen aislado `neo4j_demo_enterprise_2026_82cf1a53`.

Cliente real consultado: `cli-001`. La consulta recorrió `Cliente-SOLICITA-Envio-COMPUESTO_POR-Tramo`, incluyó origen/destino, vehículo, operador e incidentes. Resultado observado: `cli-001` → `env-101` → `trm-01`, origen `Puerto Buenos Aires`, destino `Centro Logístico Mendoza`, vehículo `veh-801`, operador `op-501`, incidente `inc-901` una sola vez.

La ejecución fue directa con `cypher-shell` contra la base restaurada; el endpoint HTTP de la aplicación no se probó. No se crearon nodos ni relaciones y el archivo de backup quedó intacto.

## Capturas

- `captura-01-red-cliente.png`
