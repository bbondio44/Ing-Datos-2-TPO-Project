# Probar las consultas a mano (Persona 2)

Consultas sueltas para copiar y pegar en la consola de cada base, ordenadas por requisito, con el
resultado que tienen que dar. Sirven para practicar la defensa y para sacar capturas de evidencia.
Asumen que ya corriste `cargar_todo.bat`.

> Si probás algo que modifica datos y querés volver al estado inicial, corré `cargar_todo.bat` de nuevo.

## Abrir las consolas

| Base | Comando (PowerShell) |
|---|---|
| Cassandra | `docker exec -it tp4-cassandra cqlsh` y adentro `USE logistica_iot;` |
| MongoDB (docker-compose del repo) | `docker exec -it tp4-mongo mongosh logistica_docs` |
| MongoDB (contenedor con usuario) | `docker exec -it <contenedor> mongosh -u <usuario> -p --authenticationDatabase admin logistica_docs` |

Si tus contenedores tienen otro nombre, buscalo con `docker ps`. Se sale con `exit`.

## UUID que se usan

| Código | Qué es | UUID |
|---|---|---|
| SEN-001 | sensor multiparámetro (lectura tardía, lectura sin GPS) | `101231a8-af8f-53ec-bfb0-f4d579ba7455` |
| SEN-002 | sensor de MSKU0000002 | `d9e437b3-519e-5123-badd-eabc293e132b` |
| SEN-003 | sensor con excursión de frío | `12330766-e3ec-52b9-b9c0-b400c16274f6` |
| SEN-005 | sensor con pico anómalo | `94858666-0f1b-5d7a-8f8d-0118791421e9` |
| SEN-007 | sensor que dejó de reportar | `ee32d9a8-5a2d-5bb2-b703-370676b20446` |
| SEN-012 | sensor con batería baja | `5f9d963d-5c94-598d-9f99-83906ed03b7b` |
| SEN-025 / SEN-040 | sensores de MSKU0000025 (SEN-040 nunca reportó) | `83006645-c8d5-5362-8e16-4f25460e5efe` / `1a41b97f-b75c-5365-8111-d65ac5ec9d46` |
| MSKU0000001 | contenedor (envío E001) | `42d867d9-d7a4-5248-9681-33e2c0cdb123` |
| MSKU0000002 | contenedor (envío E001) | `5ce5419b-847b-5ecc-b70d-03b18c5c0003` |
| MSKU0000003 | contenedor refrigerado (envío E002) | `96f3d043-00e2-5c9e-8a9d-d0810f2f5995` |
| E002 | envío con retraso y alerta de frío | `ec22d634-96cd-5267-b7bd-accdce959d92` |
| C01 / C02 | clientes | `aef2750c-1592-5ed4-b7a7-ab5771efc165` / `270823a9-7978-5eda-80f0-9ae673331b0b` |
| O01 | operador (naviera) | `c6d39b10-a95f-5346-90b9-0de9d2f1f771` |
| P02 | proveedor (FrioTech) | `ec12ca06-a78a-5605-ab40-31988a417f61` |
| CONV-001 | chat privado C02 – O01 | `61fd042c-9a1f-5796-ae0b-b4919e5cb427` |
| CONV-002 | grupo de trabajo cadena de frío | `eb3ed64f-c916-579d-8d26-48511d1cb78e` |

---

# Cassandra (keyspace `logistica_iot`)

### R18 · Registro de lecturas IoT (y lectura tardía)

Una lectura se escribe en las 5 tablas. Esta llega **tarde**: es de las 12:15 del 22/10, pero el
sensor ya había mandado una de las 23:30.

```sql
-- antes: 25 lecturas de SEN-002 ese día
SELECT COUNT(*) FROM lecturas_por_sensor_dia WHERE id_sensor = d9e437b3-519e-5123-badd-eabc293e132b AND dia = '2026-10-22';

BEGIN BATCH
  INSERT INTO lecturas_por_sensor_dia (id_sensor, dia, fecha_hora, id_lectura, id_contenedor, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (d9e437b3-519e-5123-badd-eabc293e132b, '2026-10-22', '2026-10-22 12:15:00.000+0000', 41483774-e0e2-5cb8-8b0b-481c804bd0a3, 5ce5419b-847b-5ecc-b70d-03b18c5c0003, 4.55, 77.0, 0.3, -30.1529, -53.34712, 93.6);
  INSERT INTO lecturas_por_contenedor_dia (id_contenedor, dia, fecha_hora, id_sensor, id_lectura, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, '2026-10-22', '2026-10-22 12:15:00.000+0000', d9e437b3-519e-5123-badd-eabc293e132b, 41483774-e0e2-5cb8-8b0b-481c804bd0a3, 4.55, 77.0, 0.3, -30.1529, -53.34712, 93.6);
  INSERT INTO posiciones_por_contenedor_dia (id_contenedor, dia, fecha_hora, id_sensor, id_lectura, latitud, longitud) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, '2026-10-22', '2026-10-22 12:15:00.000+0000', d9e437b3-519e-5123-badd-eabc293e132b, 41483774-e0e2-5cb8-8b0b-481c804bd0a3, -30.1529, -53.34712);
  INSERT INTO ultima_lectura_por_sensor (id_sensor, id_contenedor, id_lectura, fecha_hora, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (d9e437b3-519e-5123-badd-eabc293e132b, 5ce5419b-847b-5ecc-b70d-03b18c5c0003, 41483774-e0e2-5cb8-8b0b-481c804bd0a3, '2026-10-22 12:15:00.000+0000', 4.55, 77.0, 0.3, -30.1529, -53.34712, 93.6) USING TIMESTAMP 1792671300000000;
  INSERT INTO ultima_posicion_por_contenedor (id_contenedor, id_sensor, id_lectura, fecha_hora, latitud, longitud) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, d9e437b3-519e-5123-badd-eabc293e132b, 41483774-e0e2-5cb8-8b0b-481c804bd0a3, '2026-10-22 12:15:00.000+0000', -30.1529, -53.34712) USING TIMESTAMP 1792671300000000;
APPLY BATCH;

-- después: 26 (la lectura quedó en el historial)
SELECT COUNT(*) FROM lecturas_por_sensor_dia WHERE id_sensor = d9e437b3-519e-5123-badd-eabc293e132b AND dia = '2026-10-22';

-- pero la última lectura sigue siendo la de las 23:30: la tardía no la pisó
SELECT fecha_hora, temperatura FROM ultima_lectura_por_sensor WHERE id_sensor = d9e437b3-519e-5123-badd-eabc293e132b;
```

**Esperado:** 25 → 26, y la última lectura sigue en `2026-10-22 23:30`. Si corrés el `BATCH` otra
vez, el conteo sigue en 26: no duplica, porque un INSERT con la misma clave reemplaza la fila.

Para dejarlo como estaba:

```sql
DELETE FROM lecturas_por_sensor_dia WHERE id_sensor = d9e437b3-519e-5123-badd-eabc293e132b AND dia = '2026-10-22' AND fecha_hora = '2026-10-22 12:15:00.000+0000' AND id_lectura = 41483774-e0e2-5cb8-8b0b-481c804bd0a3;
DELETE FROM lecturas_por_contenedor_dia WHERE id_contenedor = 5ce5419b-847b-5ecc-b70d-03b18c5c0003 AND dia = '2026-10-22' AND fecha_hora = '2026-10-22 12:15:00.000+0000' AND id_sensor = d9e437b3-519e-5123-badd-eabc293e132b AND id_lectura = 41483774-e0e2-5cb8-8b0b-481c804bd0a3;
DELETE FROM posiciones_por_contenedor_dia WHERE id_contenedor = 5ce5419b-847b-5ecc-b70d-03b18c5c0003 AND dia = '2026-10-22' AND fecha_hora = '2026-10-22 12:15:00.000+0000' AND id_sensor = d9e437b3-519e-5123-badd-eabc293e132b AND id_lectura = 41483774-e0e2-5cb8-8b0b-481c804bd0a3;
```

### R19 · Histórico por sensor y por contenedor

```sql
-- sensor, rango que cruza la medianoche (consulta las 2 particiones diarias)
SELECT dia, fecha_hora, temperatura, bateria FROM lecturas_por_sensor_dia
 WHERE id_sensor = 12330766-e3ec-52b9-b9c0-b400c16274f6 AND dia IN ('2026-10-20', '2026-10-21')
   AND fecha_hora >= '2026-10-20 18:00:00+0000' AND fecha_hora <= '2026-10-21 06:00:00+0000';

-- últimas 5 lecturas de un sensor
SELECT fecha_hora, temperatura, bateria FROM lecturas_por_sensor_dia
 WHERE id_sensor = 12330766-e3ec-52b9-b9c0-b400c16274f6 AND dia = '2026-10-22' LIMIT 5;

-- todos los sensores de un contenedor juntos (MSKU0000003, durante la falla de frío)
SELECT fecha_hora, id_sensor, temperatura FROM lecturas_por_contenedor_dia
 WHERE id_contenedor = 96f3d043-00e2-5c9e-8a9d-d0810f2f5995 AND dia = '2026-10-21'
   AND fecha_hora >= '2026-10-21 14:00:00+0000' AND fecha_hora <= '2026-10-21 17:00:00+0000';
```

**Esperado:** 13 filas · 5 filas de 23:00 a 19:00 · 8 filas: un sensor marca 10.8–11.6 °C y el otro
del mismo contenedor ~4–5 °C.

### R20 y R21 · Recorrido y posición GPS

```sql
-- recorrido del 21/10 en orden cronológico (los puntos del mapa)
SELECT fecha_hora, latitud, longitud FROM posiciones_por_contenedor_dia
 WHERE id_contenedor = 42d867d9-d7a4-5248-9681-33e2c0cdb123 AND dia = '2026-10-21' ORDER BY fecha_hora ASC;

-- última posición
SELECT fecha_hora, latitud, longitud FROM ultima_posicion_por_contenedor
 WHERE id_contenedor = 42d867d9-d7a4-5248-9681-33e2c0cdb123;
```

**Esperado:** 24 posiciones, una por hora · última: `2026-10-22 23:00`, (-29.2729, -52.35262).

### R22 y R23 · Estado de sensores, batería e inactividad

```sql
-- última lectura de 3 sensores: SEN-001 al día, SEN-007 frenado el 21/10, SEN-040 no aparece
SELECT id_sensor, fecha_hora, bateria FROM ultima_lectura_por_sensor
 WHERE id_sensor IN (101231a8-af8f-53ec-bfb0-f4d579ba7455, ee32d9a8-5a2d-5bb2-b703-370676b20446, 1a41b97f-b75c-5365-8111-d65ac5ec9d46);

-- batería de SEN-012 y lecturas por debajo del 15 %
SELECT fecha_hora, bateria FROM ultima_lectura_por_sensor WHERE id_sensor = 5f9d963d-5c94-598d-9f99-83906ed03b7b;
SELECT fecha_hora, bateria FROM lecturas_por_sensor_dia
 WHERE id_sensor = 5f9d963d-5c94-598d-9f99-83906ed03b7b AND dia = '2026-10-21' AND bateria < 15.0 ALLOW FILTERING;
```

**Esperado:** 2 filas (SEN-001 `2026-10-22 23:00`, SEN-007 `2026-10-21 10:00`; SEN-040 no tiene
fila porque nunca reportó) · batería actual 8.4 · 6 lecturas bajo 15 % desde las 18:00.
El `ALLOW FILTERING` está acotado a una sola partición (un sensor, un día), por eso es aceptable.

### R32 · Anomalías de temperatura

```sql
-- lecturas sobre el umbral de 8 °C
SELECT fecha_hora, temperatura FROM lecturas_por_sensor_dia
 WHERE id_sensor = 12330766-e3ec-52b9-b9c0-b400c16274f6 AND dia = '2026-10-21' AND temperatura > 8.0 ALLOW FILTERING;

-- estadísticas por día de SEN-005: el 22/10 aparece un máximo de 7.9 °C
SELECT COUNT(*), MIN(temperatura), MAX(temperatura), AVG(temperatura) FROM lecturas_por_sensor_dia
 WHERE id_sensor = 94858666-0f1b-5d7a-8f8d-0118791421e9 AND dia = '2026-10-22';
```

**Esperado:** 4 lecturas (10.8, 11.4, 11.6, 10.9) · 24 lecturas, mínima 3.56, máxima 7.9,
promedio ≈ 4.38. Ese 7.9 no supera 8 °C, pero se aleja más de 3 desvíos del promedio del sensor
(z = 6.73): por eso se detecta como anomalía (la alerta está en MongoDB, R32 más abajo).

---

# MongoDB (base `logistica_docs`)

### Vista general

```js
show collections
db.alertas.find({}, {_id: 0, tipo_regla: 1, severidad: 1, estado: 1, ocurrencias: 1}).sort({fecha: 1})
```

**Esperado:** 5 colecciones · 8 alertas (2 de vibración, 2 de inactividad, 1 de temperatura,
1 de batería, 2 de anomalía).

### R09 · ABM de alertas (alta, duplicado, cambio de estado, baja, validación)

Copiá estas líneas de a una y mirá qué responde cada una:

```js
var a = db.alertas.findOne({tipo_regla: 'TEMPERATURA'});
a._id = 'prueba-r09'; a.clave_dedup = 'prueba|r09'; a.estado = 'ABIERTA'; a.activa = true;
a.ocurrencias = NumberInt(1); a.historial_estados = [{estado: 'ABIERTA', fecha: new Date(), id_usuario: null, comentario: null}];

db.alertas.insertOne(a)                                   // 1) alta: acknowledged: true
a._id = 'prueba-r09-bis'; db.alertas.insertOne(a)         // 2) ERROR E11000: ya hay una alerta ACTIVA con esa clave
db.alertas.updateOne({_id: 'prueba-r09'}, {$inc: {ocurrencias: NumberInt(1)}})   // 3) la repetición suma una ocurrencia
db.alertas.updateOne({_id: 'prueba-r09', estado: 'ABIERTA'},
  {$set: {estado: 'CERRADA', activa: false}, $push: {historial_estados: {estado: 'CERRADA', fecha: new Date(), id_usuario: null, comentario: 'prueba'}}})   // 4) baja: modifiedCount 1
db.alertas.updateOne({_id: 'prueba-r09', estado: 'ABIERTA'}, {$set: {estado: 'RECONOCIDA'}})   // 5) transición inválida: modifiedCount 0
db.alertas.insertOne(a)                                   // 6) reapertura: ahora sí entra (la anterior está cerrada)
a._id = 'prueba-r09-mal'; a.clave_dedup = 'x'; a.estado = 'INVENTADA'; db.alertas.insertOne(a)   // 7) ERROR: Document failed validation
db.alertas.deleteMany({_id: {$in: ['prueba-r09', 'prueba-r09-bis']}})   // limpiar: deletedCount 2
```

### R17 · Monitoreo de un envío (eventos + alertas en una sola línea de tiempo)

```js
db.eventos_logisticos.aggregate([
  {$match: {id_envio: 'ec22d634-96cd-5267-b7bd-accdce959d92'}},
  {$project: {_id: 0, fecha: 1, fuente: {$literal: 'EVENTO'}, tipo: 1}},
  {$unionWith: {coll: 'alertas', pipeline: [
    {$match: {id_envio: 'ec22d634-96cd-5267-b7bd-accdce959d92'}},
    {$project: {_id: 0, fecha: 1, fuente: {$literal: 'ALERTA'}, tipo: '$tipo_regla', estado: 1}}]}},
  {$sort: {fecha: 1}}
])
```

**Esperado:** 12 filas: 10 eventos (carga, salida, en tránsito, trasbordo, salida, retraso,
incidentes, alertas generadas) y 2 alertas (TEMPERATURA reconocida, ANOMALIA_TEMPERATURA abierta).

### R23 · Alertas de estado de sensores

```js
db.alertas.find({tipo_regla: {$in: ['BATERIA', 'INACTIVIDAD']}, activa: true},
                {_id: 0, tipo_regla: 1, ocurrencias: 1, mensaje: 1})
```

**Esperado:** 3: batería en 14.8 % (30 ocurrencias), sensor sin lecturas hace 150 minutos
(36 ocurrencias), sensor sin lecturas desde su instalación (46 ocurrencias).

### R24 · Seguimiento por tramo y estado de cada envío

```js
// eventos de E002 agrupados por tramo, en orden
db.eventos_logisticos.aggregate([
  {$match: {id_envio: 'ec22d634-96cd-5267-b7bd-accdce959d92', id_tramo: {$type: 'string'}}},
  {$sort: {fecha: 1}},
  {$group: {_id: '$id_tramo', desde: {$first: '$fecha'}, eventos: {$push: '$tipo'}}},
  {$sort: {desde: 1}}
])

// estado actual de los 20 envíos (último evento logístico de cada uno)
db.eventos_logisticos.aggregate([
  {$match: {tipo: {$nin: ['ALERTA_GENERADA', 'INCIDENTE_DETECTADO']}}},
  {$sort: {fecha: 1}},
  {$group: {_id: '$id_envio', ultimo: {$last: '$tipo'}}},
  {$group: {_id: '$ultimo', envios: {$sum: 1}}}
])

// eventos a menos de 5 km del Puerto de Buenos Aires (índice 2dsphere)
db.eventos_logisticos.countDocuments({'ubicacion.geo': {$geoWithin: {$centerSphere: [[-58.37, -34.5875], 5 / 6378.1]}}})
```

**Esperado:** tramo 1 con 4 eventos (carga, salida, en tránsito, trasbordo) y tramo 2 con 6 ·
ENTREGADO 15, SALIDA 3, EN_TRANSITO 1, RETRASO 1 · 39 eventos.

### R25 · Detección de incidentes

```js
// eventos de retraso y desvío, cada uno con su incidente de PostgreSQL
db.eventos_logisticos.find({tipo: {$in: ['RETRASO', 'DESVIO']}}, {_id: 0, tipo: 1, 'payload.motivo': 1, id_incidente: 1})

// incidentes generados por alertas, con la alerta que los originó
db.eventos_logisticos.aggregate([
  {$match: {tipo: 'INCIDENTE_DETECTADO', 'payload.origen': 'ALERTA'}},
  {$lookup: {from: 'alertas', localField: 'payload.id_origen', foreignField: '_id', as: 'alerta'}},
  {$project: {_id: 0, fecha: 1, id_incidente: 1, regla: {$first: '$alerta.tipo_regla'}}}
])

// control: alertas graves sin incidente
db.alertas.countDocuments({severidad: {$in: ['ALTA', 'CRITICA']}, id_incidente: null})
```

**Esperado:** 2 (congestión en Puerto de Santos; corte de ruta) · 3 incidentes por alerta
(1 de TEMPERATURA y 2 de INACTIVIDAD) · 0.

### R26 · Generación de alertas

```js
db.alertas.aggregate([{$group: {_id: '$estado', n: {$sum: 1}}}])
// nunca dos alertas activas con la misma regla y sensor
db.alertas.aggregate([{$match: {activa: true}}, {$group: {_id: '$clave_dedup', n: {$sum: 1}}}, {$match: {n: {$gt: 1}}}])
db.alertas.getIndexes()
```

**Esperado:** ABIERTA 6, RECONOCIDA 1, CERRADA 1 · vacío · aparece `ux_dedup_activa` con
`unique: true` y `partialFilterExpression: {activa: true}`.

### R32 · Anomalías de temperatura

```js
db.alertas.aggregate([
  {$match: {tipo_regla: 'ANOMALIA_TEMPERATURA'}},
  {$unwind: '$detalle.lecturas'},
  {$project: {_id: 0, fecha: '$detalle.lecturas.fecha_hora', temp: '$detalle.lecturas.temperatura', z: '$detalle.lecturas.z'}},
  {$sort: {fecha: 1}}
])
```

**Esperado:** 5 lecturas: las 4 de la falla de frío (z entre 3.74 y 4.26) y el pico de 7.9 °C
(z = 6.73).

### R40 · Mensajería con control de acceso

```js
// bandeja del cliente C02 (solo sus conversaciones)
db.conversaciones.find({ids_participantes: 'CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b'}, {_id: 0, asunto: 1})

// el cliente C01 intenta abrir el chat privado de C02 con la naviera
db.conversaciones.findOne({_id: '61fd042c-9a1f-5796-ae0b-b4919e5cb427', ids_participantes: 'CLIENTE:aef2750c-1592-5ed4-b7a7-ab5771efc165'})

// grupos de trabajo
db.conversaciones.find({tipo: 'GRUPAL'}, {_id: 0, nombre_grupo: 1, 'participantes.nombre': 1})

// historial del chat privado, en orden
db.mensajes.find({id_conversacion: '61fd042c-9a1f-5796-ae0b-b4919e5cb427'}, {_id: 0, fecha_envio: 1, 'remitente.nombre': 1, contenido: 1}).sort({fecha_envio: 1})
```

**Esperado:** 2 conversaciones (Demora del envío E002; Excursión de temperatura contenedor
MSKU0000003) · `null`: no puede verla · 2 grupos (GT Cadena de frío E002; GT Mantenimiento IoT) ·
6 mensajes entre el cliente y la naviera.
