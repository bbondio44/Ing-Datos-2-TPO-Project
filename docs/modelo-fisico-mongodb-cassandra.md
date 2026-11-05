# Modelo físico · MongoDB y Cassandra (Persona 2)

Responsabilidad de cada motor según la primera entrega y el Plan Maestro:

| Motor | Guarda | No guarda |
|---|---|---|
| **Cassandra** (tabular / columnar) | Telemetría IoT: lecturas, posiciones GPS, último estado | Nada transaccional ni relaciones |
| **MongoDB** (documental) | Eventos logísticos, alertas, conversaciones, mensajes, resultados de reportes | Saldos, facturas, reglas, solicitudes (son de PostgreSQL) |
| PostgreSQL (Persona 1) | Maestros, asignaciones con vigencia, `regla_alerta`, `incidente`, finanzas | |
| Neo4j (Persona 3) | Red cliente–envío–tramo–vehículo–operador | |

**Identidad compartida (D01):** toda referencia es el UUID lógico de PostgreSQL. En Cassandra el tipo
es `uuid`; en MongoDB se guarda como `String`. Los UUID del dataset están en `catalogo_ids.csv`
(generados con `uuid5` sobre el código legible, así los cuatro motores usan los mismos).
**Fechas:** siempre UTC (`timestamp` en Cassandra, `Date` en MongoDB).

---

## 1. Cassandra · keyspace `logistica_iot`

```sql
CREATE KEYSPACE logistica_iot WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
```
Demo de un nodo. En producción: `NetworkTopologyStrategy {'dc1': 3}` y `LOCAL_QUORUM` en lectura y
escritura (R + W > RF ⇒ lectura consistente con lo último escrito).

### Tablas (una por patrón de consulta, metodología Chebotko)

| Tabla | Partition key | Clustering (orden) | Consultas |
|---|---|---|---|
| `lecturas_por_sensor_dia` | `(id_sensor, dia)` | `fecha_hora DESC, id_lectura ASC` | Q02, Q03, Q09, Q10, Q12 |
| `lecturas_por_contenedor_dia` | `(id_contenedor, dia)` | `fecha_hora DESC, id_sensor ASC, id_lectura ASC` | Q04 |
| `posiciones_por_contenedor_dia` | `(id_contenedor, dia)` | `fecha_hora DESC, id_sensor ASC, id_lectura ASC` | Q07 |
| `ultima_lectura_por_sensor` | `id_sensor` | — | Q05, Q06, Q10, Q11 |
| `ultima_posicion_por_contenedor` | `id_contenedor` | — | Q08 |

Columnas de medición (todas `double`, opcionales): `temperatura, humedad, vibracion, latitud, longitud, bateria`.
Además `id_lectura uuid`, `fecha_hora timestamp`, `dia date` y la referencia cruzada
(`id_contenedor` en las tablas por sensor, `id_sensor` en las tablas por contenedor).

### Particionamiento

- **Bucket diario** (`dia` en la partition key): una lectura por minuto son 1.440 filas por partición y
  por día; acota el tamaño (recomendado < 100 MB / 100k filas) y evita hot partitions que crecen sin fin.
- Consultas de varios días: se consulta cada partición (`dia IN (...)` o una consulta por día) y la
  aplicación fusiona el resultado; **nunca `ALLOW FILTERING` sobre toda la tabla** (C02).
  El único `ALLOW FILTERING` (Q09, Q10) está acotado a una partición y solo audita; la aplicación no lo usa.
- `fecha_hora DESC`: la consulta más común es "lo más reciente primero" (Q03 con `LIMIT`). Para el
  recorrido cronológico se usa `ORDER BY fecha_hora ASC` (Q07).
- `id_lectura` en el clustering: dos lecturas en el mismo instante no se pisan.
- Compactación `UnifiedCompactionStrategy` (UCS) en las tablas de historial: es la estrategia de Cassandra 5.0.
- Retención (C05): sin TTL por ahora, porque las anomalías (R32) y los reportes usan el histórico.
  Se puede agregar `default_time_to_live` cuando se acuerde la ventana.

### Restricciones e integridad (Cassandra no tiene FK ni CHECK)

| Regla | Cómo se garantiza |
|---|---|
| Lectura duplicada (reenvío) no duplica (D04) | `INSERT` = upsert por clave primaria; `id_lectura` determinístico. Q01 lo demuestra |
| Lectura tardía no pisa la última (C03) | Tablas `ultima_*` escritas con `USING TIMESTAMP` = instante de la lectura (µs): last write wins por **tiempo de evento**. La sintaxis está fuera de clase; el concepto de timestamp por columna se vio. Q05 lo demuestra |
| Sin GPS no se inventa posición (C04) | Solo se escribe en `posiciones_*` / `ultima_posicion_*` si hay latitud y longitud |
| Las 5 escrituras de una lectura | `INSERT` separados. Si uno falla, las tablas quedan distintas hasta reintentar: se reenvía la lectura con el mismo `id_lectura` y el upsert no duplica (Q01) |
| Sensor / contenedor válidos y asignación vigente | Lo valida la aplicación contra PostgreSQL antes de escribir |
| Columnas nulas | No se escriben (evita tombstones) |

### Relaciones (desnormalizadas, sin JOIN)

`lecturas_por_contenedor_dia` y `posiciones_por_contenedor_dia` son copias de la misma lectura
indexadas por contenedor. Cuál sensor está en cuál contenedor (y desde cuándo) es una relación de
PostgreSQL (`asignacion_sensor_contenedor`, D05); Cassandra guarda el `id_contenedor` vigente en el
momento de la lectura, así el histórico no cambia si el sensor se mueve después.

---

## 2. MongoDB · base `logistica_docs`

Todas las colecciones tienen validador `$jsonSchema` con `validationLevel: strict` y
`validationAction: error` (un documento inválido se rechaza, código 121).

### `eventos_logisticos` — historial de cada envío (trazabilidad)

```js
{ _id: "<uuid evento>", id_envio, id_contenedor?, id_tramo?,
  tipo: "CARGA_INICIADA|SALIDA|EN_TRANSITO|ADUANA|RETRASO|DESVIO|TRASBORDO|DESCARGA|ENTREGADO|INCIDENTE_DETECTADO|ALERTA_GENERADA",
  fecha, ubicacion: { nombre, pais, geo: { type: "Point", coordinates: [lon, lat] } },
  descripcion, payload: { ...variable según tipo... }, origen: "OPERADOR|SENSOR|SISTEMA",
  id_alerta?, id_incidente?, id_correlacion, registrado_por?, creado_en }
```
Por qué documental: el `payload` cambia con el tipo (aduana: canal y documentos; retraso: motivo y
nueva ETA; desvío: rutas y km). Índices: `{id_envio, fecha}`, `{id_contenedor, fecha}` (parcial),
`{tipo, fecha}`, `{ubicacion.geo: 2dsphere}`.

### `alertas` — resultado de evaluar reglas sobre la telemetría

```js
{ _id, id_regla, tipo_regla, id_sensor, id_contenedor, id_envio, id_cliente, id_lectura?, id_evento, id_incidente?,
  operador, umbral, valor_observado, severidad: "BAJA|MEDIA|ALTA|CRITICA",
  estado: "ABIERTA|RECONOCIDA|CERRADA", activa: bool, mensaje, detalle: {...variable...},
  fecha, fecha_lectura, ultima_ocurrencia, ocurrencias: int, clave_dedup: "<id_regla>|<id_sensor>",
  historial_estados: [ { estado, fecha, id_usuario, comentario } ] }
```
- **Deduplicación (D04):** índice **único parcial** `{clave_dedup: 1}` con
  `partialFilterExpression: {activa: true}` ⇒ a lo sumo una alerta activa por regla y sensor. Las lecturas
  que repiten la violación hacen `$inc: {ocurrencias: 1}`. Al cerrarla (`activa: false`), una violación
  nueva abre otra alerta (reapertura). Probado en R09. `ultima_id_lectura` guarda la última lectura contada: si se reenvía la misma, no suma otra ocurrencia.
- **Transiciones válidas:** ABIERTA → RECONOCIDA → CERRADA (o ABIERTA → CERRADA). Se aplican con
  filtro por estado actual (`updateOne({_id, estado: 'ABIERTA'}, ...)`) para que una transición inválida
  no modifique nada; cada cambio queda en `historial_estados`.
- Las reglas (`id_regla`) se definen en PostgreSQL (`regla_alerta`). Alertas ALTA/CRITICA generan un
  incidente en PostgreSQL y guardan su `id_incidente`.
- Índices: `ux_dedup_activa`, `{id_sensor, fecha}`, `{id_envio, fecha}`, `{estado, severidad, fecha}`,
  `{id_cliente, estado, fecha}`.

### `conversaciones` y `mensajes` — mensajería privada y grupal

```js
// conversaciones
{ _id, tipo: "PRIVADA|GRUPAL", asunto, nombre_grupo?, id_envio?,
  participantes: [ { tipo: "CLIENTE|PROVEEDOR|OPERADOR|INTERNO", id_entidad, id_usuario, nombre } ],
  ids_participantes: [ "CLIENTE:<uuid>", ... ], creada_por, fecha_creacion, estado,
  cantidad_mensajes: int, ultimo_mensaje: { fecha, remitente, extracto } }
// mensajes
{ _id, id_conversacion, remitente: {tipo, id_entidad, ...}, destinatario?, fecha_envio,
  contenido (1..4000), tipo: "TEXTO|ARCHIVO|SISTEMA", estado: "ENVIADO|LEIDO", leido_por: [...], adjunto? }
```
- La conversación grupal es el **GrupoDeTrabajo** del enunciado (integrantes = `participantes`).
- **Mensajes referenciados, no embebidos:** una conversación puede crecer sin límite (16 MB por
  documento) y así se paginan y auditan. En la conversación se desnormaliza solo `ultimo_mensaje` y el
  contador para listar bandejas sin leer los mensajes.
- **Control de acceso:** toda consulta filtra por `ids_participantes: "<TIPO>:<uuid>"` (índice multikey);
  un cliente no puede leer conversaciones ajenas (R40 lo prueba con C01 vs C02).
- Índices: `{ids_participantes, ultimo_mensaje.fecha}`, `{id_envio}` (parcial),
  `{id_conversacion, fecha_envio, _id}` (paginado por cursor).

### `reportes_generados` — resultados de reportes

`{ _id, id_solicitud, id_cliente, solicitado_por, tipo, parametros, formato, estado, resultado | ubicacion, generado_en }`.
La solicitud, el precio y la factura viven en PostgreSQL (`solicitud_reporte`); acá solo el resultado,
cuya forma depende del tipo. Índice **único** `{id_solicitud}` ⇒ un resultado por solicitud
(reintentar no duplica). Otros: `{id_cliente, generado_en}`, `{tipo, generado_en}`.

### Relaciones entre colecciones y con otros motores

```
alertas.id_evento        ──► eventos_logisticos._id         (evento ALERTA_GENERADA)
alertas.id_incidente     ──► PostgreSQL incidente           (alertas ALTA / CRITICA)
eventos.id_incidente     ──► PostgreSQL incidente           (RETRASO, DESVIO, INCIDENTE_DETECTADO)
mensajes.id_conversacion ──► conversaciones._id
reportes.id_solicitud    ──► PostgreSQL solicitud_reporte
id_envio, id_tramo, id_contenedor, id_sensor, id_cliente, id_regla ──► PostgreSQL (catalogo_ids.csv)
alertas.id_lectura       ──► Cassandra lecturas_* (id_lectura)
```

---

## 3. Sincronización y consistencia

- **Fuente de verdad única por dato:** maestros, reglas, incidentes y dinero en PostgreSQL; Mongo y
  Cassandra solo referencian por UUID y nunca son fuente competidora de un estado crítico.
- **Escrituras idempotentes:** IDs determinísticos (`id_lectura`, `id_evento`), upserts por `_id`
  (`replaceOne`/`$setOnInsert`), índice único parcial en alertas y único en reportes ⇒ reintentar un
  evento procesado dos veces no duplica.
- **Outbox (PostgreSQL → Mongo/Neo4j):** el incidente se escribe en la transacción relacional junto
  con un evento outbox; un worker lo lleva a MongoDB con reintentos. Si Mongo está caído, no se pierde.
- **Problemas posibles:** lectura que llega tarde (resuelto con `USING TIMESTAMP`), alerta repetida
  (dedup), alerta sin incidente (control R25: alertas ALTA/CRITICA con `id_incidente: null` debe dar 0),
  referencias a IDs inexistentes (se valida contra PostgreSQL antes de escribir).
