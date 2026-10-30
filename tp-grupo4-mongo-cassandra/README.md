# MongoDB y Cassandra · TP Persistencia Políglota · Grupo 4

Bases de datos de **Persona 2** (telemetría IoT, eventos, alertas y mensajería): esquema, índices,
restricciones, datos de prueba y consultas que respaldan los requisitos
**R09, R17–R26, R32 y R40**. La aplicación (Spring Boot) y las otras bases (PostgreSQL, Neo4j) las
hace el resto del grupo; este paquete les deja el contrato de IDs (`catalogo_ids.csv`).

Cada integrante tiene **su propia copia** de las bases en su PC, así lo que cambia uno no afecta a
los demás. No se comparten las bases: se comparten estos scripts, que las crean idénticas en
cualquier máquina (mismos datos, mismos UUID).

## Instructivo: levantar las bases en tu PC

**Necesitás:** Docker Desktop instalado y abierto, con 4 GB de memoria o más asignados
(*Settings → Resources*; Cassandra consume bastante).

**1. Bajar el repo** (o descomprimir el zip; no ejecutes nada desde adentro del zip).

**2. Crear los contenedores** (solo la primera vez). Abrí PowerShell en esta carpeta y corré:

```powershell
docker compose up -d
```

Crea `tp4-mongo` (MongoDB 7, puerto 27017) y `tp4-cassandra` (Cassandra 4.1, puerto 9042).
Esperá 1–2 minutos a que Cassandra termine de arrancar. Si ya tenés tus propios contenedores de
Mongo y Cassandra corriendo, salteá este paso.

**3. Cargar las bases:** doble clic en **`cargar_todo.bat`**.

Crea las bases, carga los datos de prueba, corre las consultas y guarda la salida en
`evidencias\`. Tarda 2–4 minutos. Al final tiene que decir:

- MongoDB: **`RESUMEN: 37 de 37 verificaciones OK`**
- Cassandra: **`filas en lecturas_por_sensor_dia: 2773`**

La carpeta `evidencias\` no se sube al repo (está en `.gitignore`): cada uno genera la suya al
correr el script.

**4. Listo.** Conexión desde la app o desde una herramienta:

| Base | Dirección | Nombre |
|---|---|---|
| MongoDB | `mongodb://localhost:27017` | base `logistica_docs` |
| Cassandra | `localhost:9042`, datacenter `datacenter1` | keyspace `logistica_iot` |

**Volver al estado inicial:** repetí el paso 3 cuando quieras (por ejemplo, después de probar un
cambio). No duplica datos: deja todo como recién cargado.

**Ver los datos:** MongoDB Compass para Mongo. Para Cassandra:
`docker exec -it tp4-cassandra cqlsh` y adentro `USE logistica_iot;`.

**Si algo falla**

| Problema | Solución |
|---|---|
| "Docker no responde" | Abrí Docker Desktop y esperá a que diga *Running* |
| No encuentra los contenedores | Corré `docker ps` y pasá los nombres: `.\cargar_todo.ps1 -Mongo <nombre> -Cassandra <nombre>` |
| Mongo pide usuario | `.\cargar_todo.ps1 -SoloMongo -MongoUser <usuario> -MongoPass <clave>` |
| Cassandra no arranca o se cae | Subí la memoria de Docker a 4 GB o más y reiniciá Docker Desktop |
| Windows bloquea el `.bat` | "Más información" → "Ejecutar de todas formas" |

**Si cambiás el esquema:** guardá el cambio como un script nuevo numerado (por ejemplo
`cassandra/04_agrega_columna.cql`), subilo al repo y avisá: los demás hacen pull y lo aplican.
No edites `02_datos_prueba.*` a mano: se generan con `python generador/generar.py`.

<details><summary>Cargarlo a mano (comandos sueltos)</summary>

```bash
# Cassandra
docker cp cassandra/. <contenedor_cassandra>:/tmp/tp_grupo4/
docker exec <contenedor_cassandra> cqlsh -f /tmp/tp_grupo4/01_schema.cql
docker exec <contenedor_cassandra> cqlsh --request-timeout=60 -f /tmp/tp_grupo4/02_datos_prueba.cql
docker exec <contenedor_cassandra> cqlsh -f /tmp/tp_grupo4/03_consultas.cql

# MongoDB
docker cp mongodb/. <contenedor_mongo>:/tmp/tp_grupo4/
docker exec <contenedor_mongo> mongosh logistica_docs /tmp/tp_grupo4/01_schema.js
docker exec <contenedor_mongo> mongosh logistica_docs /tmp/tp_grupo4/02_datos_prueba.js
docker exec <contenedor_mongo> mongosh logistica_docs /tmp/tp_grupo4/03_consultas.js
```
En Linux/macOS también está `./cargar_todo.sh`. Si alguien no tiene los contenedores:
`docker compose up -d` (usa `docker-compose.yml`, MongoDB 7 + Cassandra 4.1).
</details>

## Qué hay en la carpeta

| Archivo | Qué es |
|---|---|
| `cassandra/01_schema.cql` | Keyspace `logistica_iot` y las 5 tablas del drawio (modelo Chebotko) |
| `cassandra/02_datos_prueba.cql` | 2.772 lecturas de 39 sensores (20–22/10/2026) con casos límite |
| `cassandra/03_consultas.cql` | Q01–Q12 del flujo del drawio, con UUID reales y **resultado esperado** |
| `mongodb/01_schema.js` | Base `logistica_docs`: 5 colecciones con validador `$jsonSchema` e índices |
| `mongodb/02_datos_prueba.js` | 207 documentos coherentes con la telemetría |
| `mongodb/03_consultas.js` | 37 verificaciones por requisito que imprimen OK/FALLA |
| `catalogo_ids.csv` | UUID de clientes, envíos, tramos, contenedores, sensores, reglas, usuarios (para PostgreSQL y Neo4j) |
| `incidentes_para_postgres.csv` | Los 5 incidentes que referencian los documentos de Mongo (la tabla vive en PostgreSQL) |
| `docs/MODELO_FISICO.md` | Modelo físico: tablas/colecciones, claves, índices, restricciones, particionamiento, relaciones |
| `docs/DATOS_DE_PRUEBA.md` | Qué casos límite tiene el dataset y qué debería dar cada requisito |
| `generador/` | Python que genera todos los scripts y los verifica (ver abajo) |

## Requisito → dónde se resuelve

| Req. | Punto de promoción | Cassandra | MongoDB |
|---|---|---|---|
| R09 | ABM reglas y alertas | Q09 (auditoría de umbral) | alta / cambio de estado / cierre / reapertura de alertas, dedup e índice único parcial (las reglas están en PostgreSQL `regla_alerta`) |
| R17 | Monitoreo envíos–sensores–contenedores–eventos | Q04 | línea de tiempo eventos + alertas (`$unionWith`) |
| R18 | Registro continuo de lecturas IoT | Q01 (escritura en 5 tablas, reenvío sin duplicar) | — |
| R19 | Histórico por contenedor / sensor | Q02 (cruza medianoche), Q03, Q04 | — |
| R20 | Reconstrucción del recorrido | Q07 | — |
| R21 | Visualización GPS | Q07, Q08 | — |
| R22 | Control de estado de sensores | Q05 (lectura tardía), Q06, Q10, Q11 | — |
| R23 | Alertas de estado de sensores | Q10, Q11 | alertas BATERIA / INACTIVIDAD |
| R24 | Gestión de trayectos (seguimiento) | — | eventos por tramo, estado de cada envío, búsqueda geográfica (2dsphere) |
| R25 | Detección de incidentes | — | eventos RETRASO/DESVIO y alertas graves → incidente (`$lookup`) |
| R26 | Generación de alertas | — | por estado, por tipo, deduplicación, por cliente |
| R32 | Anomalías de temperatura | Q09, Q12 (estadísticas) | alertas ANOMALIA_TEMPERATURA (z-score > 3) |
| R40 | Mensajería con las partes | — | bandejas, aislamiento entre clientes, grupos, paginado, no leídos, envío |

## Para la app Spring Boot

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/logistica_docs
spring.cassandra.contact-points=localhost:9042
spring.cassandra.keyspace-name=logistica_iot
spring.cassandra.local-datacenter=datacenter1
spring.cassandra.schema-action=none
```

- Cassandra: `uuid` → `java.util.UUID`, `date` → `LocalDate`, `timestamp` → `Instant`, `double` → `Double`
  (usar `Double`, no `double`: hay mediciones nulas, p. ej. sensores sin GPS). Para las tablas `ultima_*`
  escribir con `USING TIMESTAMP` = instante de la lectura (ver Q05) para que una lectura atrasada no pise la nueva.
- MongoDB: todos los `_id` y referencias son `String` (UUID). `ocurrencias` y `cantidad_mensajes`
  son `Integer` (el validador exige int32). Las fechas son `Instant`/`Date`.
- `schema-action=none`: el esquema lo crean estos scripts, no Spring.

## Regenerar o verificar (opcional)

```bash
python generador/generar.py      # vuelve a generar todos los scripts (determinista: mismos UUID)
pip install mongomock
python generador/verificar.py    # chequeo offline: INSERTs contra el esquema, conteos, casos límite
```
