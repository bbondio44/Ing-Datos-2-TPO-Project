// =====================================================================
// MongoDB · CONSULTAS POR REQUISITO (Persona 2: R09, R17, R23, R24, R25, R26, R32, R40)
// GENERADO por generador/generar.py con los IDs reales del dataset.
// Uso: mongosh "mongodb://localhost:27017/logistica_docs" mongodb/03_consultas.js
// Correr después de 01_schema.js y 02_datos_prueba.js. Cada paso imprime OK/FALLA
// comparando el resultado obtenido contra el esperado (sirve como evidencia).
// FUERA DE CLASE: aggregate ($match, $group, $lookup), $setOnInsert, $addToSet, $pull. Solo en estas
// consultas de verificación, para contar y cruzar colecciones; la aplicación no los usa.
// Las pruebas de escritura (R09, R40) limpian lo que crean, se puede repetir.
// =====================================================================

const resultados = [];
function verificar(req, descripcion, esperado, obtenido) {
  const ok = JSON.stringify(esperado) === JSON.stringify(obtenido);
  resultados.push({req, descripcion, ok});
  print((ok ? 'OK    ' : 'FALLA ') + req + ' · ' + descripcion);
  if (!ok) print('       esperado: ' + JSON.stringify(esperado) + '\n       obtenido: ' + JSON.stringify(obtenido));
  else print('       resultado: ' + JSON.stringify(obtenido));
}
let obtenido;


// ===================================================================== R09
print('\n==================== R09 ====================');
// --- R09.1 · Alta de alerta (regla REG-06 definida en PostgreSQL)
// Esperado: true
{
  db.alertas.deleteMany({_id: {$in: ["546326c8-9a9b-5fe6-81c7-87630c4c5b59", "3e4cdbe5-8d8b-5c65-b182-c617bc1900d3", "1001bf39-f2bc-549c-ad11-1b8a2dbe0f0d", "prueba-invalida"]}});  // limpia corridas anteriores
  obtenido = db.alertas.insertOne({_id: "546326c8-9a9b-5fe6-81c7-87630c4c5b59", id_regla: "8682a474-dea2-5b6e-af19-2862d4840b65", tipo_regla: "HUMEDAD", id_sensor: "458d6508-e6b4-5322-b376-9e3a8333f438", id_contenedor: "42d867d9-d7a4-5248-9681-33e2c0cdb123", id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add", id_cliente: "aef2750c-1592-5ed4-b7a7-ab5771efc165", id_lectura: null, id_evento: null, id_incidente: null, operador: ">", umbral: 90.0, valor_observado: 93.5, severidad: "BAJA", estado: "ABIERTA", activa: true, mensaje: "Humedad 93.5 % supera 90 % (dato de prueba R09)", detalle: {prueba: true}, fecha: ISODate("2026-10-22T23:50:00.000Z"), fecha_lectura: ISODate("2026-10-22T23:50:00.000Z"), ultima_ocurrencia: ISODate("2026-10-22T23:50:00.000Z"), ocurrencias: NumberInt(1), clave_dedup: "8682a474-dea2-5b6e-af19-2862d4840b65|458d6508-e6b4-5322-b376-9e3a8333f438", historial_estados: [{estado: "ABIERTA", fecha: ISODate("2026-10-22T23:50:00.000Z"), id_usuario: null, comentario: null}]}).acknowledged;
  verificar("R09", "Alta de alerta (regla REG-06 definida en PostgreSQL)", true, obtenido);
}
// --- R09.2 · Segunda alerta ACTIVA para la misma regla+sensor es rechazada (índice único parcial)
// Esperado: E11000 duplicate key
{
  try { db.alertas.insertOne({_id: "3e4cdbe5-8d8b-5c65-b182-c617bc1900d3", id_regla: "8682a474-dea2-5b6e-af19-2862d4840b65", tipo_regla: "HUMEDAD", id_sensor: "458d6508-e6b4-5322-b376-9e3a8333f438", id_contenedor: "42d867d9-d7a4-5248-9681-33e2c0cdb123", id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add", id_cliente: "aef2750c-1592-5ed4-b7a7-ab5771efc165", id_lectura: null, id_evento: null, id_incidente: null, operador: ">", umbral: 90.0, valor_observado: 93.5, severidad: "BAJA", estado: "ABIERTA", activa: true, mensaje: "Humedad 93.5 % supera 90 % (dato de prueba R09)", detalle: {prueba: true}, fecha: ISODate("2026-10-22T23:50:00.000Z"), fecha_lectura: ISODate("2026-10-22T23:50:00.000Z"), ultima_ocurrencia: ISODate("2026-10-22T23:50:00.000Z"), ocurrencias: NumberInt(1), clave_dedup: "8682a474-dea2-5b6e-af19-2862d4840b65|458d6508-e6b4-5322-b376-9e3a8333f438", historial_estados: [{estado: "ABIERTA", fecha: ISODate("2026-10-22T23:50:00.000Z"), id_usuario: null, comentario: null}]}); obtenido = 'insertó (MAL)'; }
  catch (e) { obtenido = e.code === 11000 ? 'E11000 duplicate key' : String(e.code); }
  verificar("R09", "Segunda alerta ACTIVA para la misma regla+sensor es rechazada (índice único parcial)", "E11000 duplicate key", obtenido);
}
// --- R09.3 · La repetición se registra sumando ocurrencias (no se duplica la alerta)
// Esperado: 2
{
  db.alertas.updateOne({clave_dedup: "8682a474-dea2-5b6e-af19-2862d4840b65|458d6508-e6b4-5322-b376-9e3a8333f438", activa: true},
    {$inc: {ocurrencias: NumberInt(1)}, $set: {ultima_ocurrencia: ISODate("2026-10-23T00:10:00Z")}});
  obtenido = db.alertas.findOne({_id: "546326c8-9a9b-5fe6-81c7-87630c4c5b59"}).ocurrencias;
  verificar("R09", "La repetición se registra sumando ocurrencias (no se duplica la alerta)", 2, obtenido);
}
// --- R09.4 · El validador $jsonSchema rechaza un estado inexistente
// Esperado: DocumentValidationFailure
{
  try { db.alertas.insertOne({_id: "prueba-invalida", id_regla: "8682a474-dea2-5b6e-af19-2862d4840b65", tipo_regla: "HUMEDAD", id_sensor: "458d6508-e6b4-5322-b376-9e3a8333f438", id_contenedor: "42d867d9-d7a4-5248-9681-33e2c0cdb123", id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add", id_cliente: "aef2750c-1592-5ed4-b7a7-ab5771efc165", id_lectura: null, id_evento: null, id_incidente: null, operador: ">", umbral: 90.0, valor_observado: 93.5, severidad: "BAJA", estado: "INVENTADA", activa: true, mensaje: "Humedad 93.5 % supera 90 % (dato de prueba R09)", detalle: {prueba: true}, fecha: ISODate("2026-10-22T23:50:00.000Z"), fecha_lectura: ISODate("2026-10-22T23:50:00.000Z"), ultima_ocurrencia: ISODate("2026-10-22T23:50:00.000Z"), ocurrencias: NumberInt(1), clave_dedup: "prueba-invalida", historial_estados: [{estado: "ABIERTA", fecha: ISODate("2026-10-22T23:50:00.000Z"), id_usuario: null, comentario: null}]}); obtenido = 'insertó (MAL)'; }
  catch (e) { obtenido = e.code === 121 ? 'DocumentValidationFailure' : String(e.code); }
  verificar("R09", "El validador $jsonSchema rechaza un estado inexistente", "DocumentValidationFailure", obtenido);
}
// --- R09.5 · Modificación: ABIERTA -> RECONOCIDA (con historial)
// Esperado: 1
{
  obtenido = db.alertas.updateOne({_id: "546326c8-9a9b-5fe6-81c7-87630c4c5b59", estado: 'ABIERTA'},
    {$set: {estado: 'RECONOCIDA'}, $push: {historial_estados: {estado: 'RECONOCIDA', fecha: ISODate("2026-10-23T00:20:00Z"), id_usuario: "6810f791-1878-5de4-b0f6-7e63d996d4ac", comentario: 'Revisando'}}}).modifiedCount;
  verificar("R09", "Modificación: ABIERTA -> RECONOCIDA (con historial)", 1, obtenido);
}
// --- R09.6 · Transición inválida: volver a 'reconocer' no modifica nada
// Esperado: 0
{
  obtenido = db.alertas.updateOne({_id: "546326c8-9a9b-5fe6-81c7-87630c4c5b59", estado: 'ABIERTA'}, {$set: {estado: 'RECONOCIDA'}}).modifiedCount;
  verificar("R09", "Transición inválida: volver a 'reconocer' no modifica nada", 0, obtenido);
}
// --- R09.7 · Baja lógica: cerrar la alerta (activa = false)
// Esperado: 1
{
  obtenido = db.alertas.updateOne({_id: "546326c8-9a9b-5fe6-81c7-87630c4c5b59", estado: {$in: ['ABIERTA', 'RECONOCIDA']}},
    {$set: {estado: 'CERRADA', activa: false}, $push: {historial_estados: {estado: 'CERRADA', fecha: ISODate("2026-10-23T01:00:00Z"), id_usuario: "6810f791-1878-5de4-b0f6-7e63d996d4ac", comentario: 'Normalizado'}}}).modifiedCount;
  verificar("R09", "Baja lógica: cerrar la alerta (activa = false)", 1, obtenido);
}
// --- R09.8 · Reapertura: una nueva violación tras el cierre crea otra alerta
// Esperado: true
{
  obtenido = db.alertas.insertOne({_id: "1001bf39-f2bc-549c-ad11-1b8a2dbe0f0d", id_regla: "8682a474-dea2-5b6e-af19-2862d4840b65", tipo_regla: "HUMEDAD", id_sensor: "458d6508-e6b4-5322-b376-9e3a8333f438", id_contenedor: "42d867d9-d7a4-5248-9681-33e2c0cdb123", id_envio: "d72d0543-b3be-54c5-9248-ceb758d30add", id_cliente: "aef2750c-1592-5ed4-b7a7-ab5771efc165", id_lectura: null, id_evento: null, id_incidente: null, operador: ">", umbral: 90.0, valor_observado: 93.5, severidad: "BAJA", estado: "ABIERTA", activa: true, mensaje: "Humedad 93.5 % supera 90 % (dato de prueba R09)", detalle: {prueba: true}, fecha: ISODate("2026-10-23T02:00:00.000Z"), fecha_lectura: ISODate("2026-10-22T23:50:00.000Z"), ultima_ocurrencia: ISODate("2026-10-22T23:50:00.000Z"), ocurrencias: NumberInt(1), clave_dedup: "8682a474-dea2-5b6e-af19-2862d4840b65|458d6508-e6b4-5322-b376-9e3a8333f438", historial_estados: [{estado: "ABIERTA", fecha: ISODate("2026-10-22T23:50:00.000Z"), id_usuario: null, comentario: null}]}).acknowledged;
  verificar("R09", "Reapertura: una nueva violación tras el cierre crea otra alerta", true, obtenido);
}
// --- R09.9 · Se eliminan los documentos de prueba (el dataset queda como estaba)
// Esperado: 2
{
  obtenido = db.alertas.deleteMany({_id: {$in: ["546326c8-9a9b-5fe6-81c7-87630c4c5b59", "3e4cdbe5-8d8b-5c65-b182-c617bc1900d3", "1001bf39-f2bc-549c-ad11-1b8a2dbe0f0d", "prueba-invalida"]}}).deletedCount;
  verificar("R09", "Se eliminan los documentos de prueba (el dataset queda como estaba)", 2, obtenido);
}

// ===================================================================== R17
print('\n==================== R17 ====================');
// --- R17.10 · Línea de tiempo del envío E002: eventos + alertas ($unionWith)
// Esperado: 12
{
  const linea = db.eventos_logisticos.aggregate([
    {$match: {id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92"}},
    {$project: {_id: 0, fecha: 1, fuente: {$literal: 'EVENTO'}, tipo: 1, descripcion: 1}},
    {$unionWith: {coll: 'alertas', pipeline: [{$match: {id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92"}},
       {$project: {_id: 0, fecha: 1, fuente: {$literal: 'ALERTA'}, tipo: '$tipo_regla', descripcion: '$mensaje', estado: 1}}]}},
    {$sort: {fecha: 1}}
  ]).toArray();
  linea.forEach(x => print('   ', x.fecha.toISOString().slice(0, 16), x.fuente.padEnd(6), x.tipo.padEnd(20), x.descripcion));
  obtenido = linea.length;
  verificar("R17", "Línea de tiempo del envío E002: eventos + alertas ($unionWith)", 12, obtenido);
}
// --- R17.11 · Estado de seguimiento actual de E002 (último evento logístico)
// Esperado: RETRASO
{
  obtenido = db.eventos_logisticos.find({id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92", tipo: {$nin: ['ALERTA_GENERADA', 'INCIDENTE_DETECTADO']}})
    .sort({fecha: -1}).limit(1).toArray()[0].tipo;
  verificar("R17", "Estado de seguimiento actual de E002 (último evento logístico)", "RETRASO", obtenido);
}
// --- R17.12 · Alertas activas del envío E002
// Esperado: 2
{
  obtenido = db.alertas.countDocuments({id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92", activa: true});
  verificar("R17", "Alertas activas del envío E002", 2, obtenido);
}

// ===================================================================== R23
print('\n==================== R23 ====================');
// --- R23.13 · Alertas activas de estado de sensor (batería baja / sin conexión)
// Esperado: 3
{
  const est = db.alertas.find({tipo_regla: {$in: ['BATERIA', 'INACTIVIDAD']}, activa: true},
    {_id: 0, tipo_regla: 1, severidad: 1, fecha: 1, valor_observado: 1, ocurrencias: 1, mensaje: 1}).sort({mensaje: 1}).toArray();
  est.forEach(x => print('   ', x.tipo_regla.padEnd(12), x.severidad.padEnd(6), x.fecha.toISOString().slice(0, 16), 'ocurrencias=' + x.ocurrencias, '|', x.mensaje));
  obtenido = est.length;
  verificar("R23", "Alertas activas de estado de sensor (batería baja / sin conexión)", 3, obtenido);
}

// ===================================================================== R24
print('\n==================== R24 ====================');
// --- R24.14 · Seguimiento por tramo del envío E002 (eventos agrupados por tramo, en orden)
// Esperado: [4, 6]
{
  const porTramo = db.eventos_logisticos.aggregate([
    {$match: {id_envio: "ec22d634-96cd-5267-b7bd-accdce959d92", id_tramo: {$type: 'string'}}},
    {$sort: {fecha: 1}},
    {$group: {_id: '$id_tramo', desde: {$first: '$fecha'}, hasta: {$last: '$fecha'}, eventos: {$push: '$tipo'}}},
    {$sort: {desde: 1}}
  ]).toArray();
  porTramo.forEach((x, i) => print('    tramo', i + 1, x.desde.toISOString().slice(0, 16), '->', x.hasta.toISOString().slice(0, 16), x.eventos.join(' > ')));
  obtenido = porTramo.map(x => x.eventos.length);
  verificar("R24", "Seguimiento por tramo del envío E002 (eventos agrupados por tramo, en orden)", [4, 6], obtenido);
}
// --- R24.15 · Estado de seguimiento de todos los envíos (último evento de cada uno)
// Esperado: {ENTREGADO: 15, EN_TRANSITO: 1, RETRASO: 1, SALIDA: 3}
{
  obtenido = {};
  db.eventos_logisticos.aggregate([
    {$match: {tipo: {$nin: ['ALERTA_GENERADA', 'INCIDENTE_DETECTADO']}}},
    {$sort: {fecha: 1}},
    {$group: {_id: '$id_envio', ultimo: {$last: '$tipo'}}},
    {$group: {_id: '$ultimo', envios: {$sum: 1}}},
    {$sort: {_id: 1}}
  ]).forEach(x => { obtenido[x._id] = x.envios; });
  verificar("R24", "Estado de seguimiento de todos los envíos (último evento de cada uno)", {ENTREGADO: 15, EN_TRANSITO: 1, RETRASO: 1, SALIDA: 3}, obtenido);
}
// --- R24.16 · Eventos registrados a menos de 5 km del Puerto de Buenos Aires (índice 2dsphere)
// Esperado: 39
{
  obtenido = db.eventos_logisticos.countDocuments({'ubicacion.geo': {$geoWithin: {$centerSphere: [[-58.37, -34.5875], 5 / 6378.1]}}});
  // Variante ordenada por cercanía (usa el índice ix_ubicacion_geo):
  db.eventos_logisticos.find({'ubicacion.geo': {$near: {$geometry: {type: 'Point', coordinates: [-58.37, -34.5875]}, $maxDistance: 5000}}}, {_id: 0, tipo: 1, fecha: 1, 'ubicacion.nombre': 1})
    .limit(3).forEach(x => print('   ', x.fecha.toISOString().slice(0, 16), x.tipo, x.ubicacion.nombre));
  verificar("R24", "Eventos registrados a menos de 5 km del Puerto de Buenos Aires (índice 2dsphere)", 39, obtenido);
}

// ===================================================================== R25
print('\n==================== R25 ====================');
// --- R25.17 · Eventos RETRASO/DESVIO vinculados a su incidente (PostgreSQL)
// Esperado: 2
{
  obtenido = db.eventos_logisticos.countDocuments({tipo: {$in: ['RETRASO', 'DESVIO']}, id_incidente: {$type: 'string'}});
  verificar("R25", "Eventos RETRASO/DESVIO vinculados a su incidente (PostgreSQL)", 2, obtenido);
}
// --- R25.18 · Incidentes detectados por origen (evento operativo o alerta de sensor)
// Esperado: {ALERTA: 3, EVENTO: 2}
{
  obtenido = {};
  db.eventos_logisticos.aggregate([{$match: {tipo: 'INCIDENTE_DETECTADO'}},
    {$group: {_id: '$payload.origen', n: {$sum: 1}}}, {$sort: {_id: 1}}]).forEach(x => { obtenido[x._id] = x.n; });
  verificar("R25", "Incidentes detectados por origen (evento operativo o alerta de sensor)", {ALERTA: 3, EVENTO: 2}, obtenido);
}
// --- R25.19 · Incidente <-> alerta que lo originó ($lookup)
// Esperado: 3
{
  const inc = db.eventos_logisticos.aggregate([
    {$match: {tipo: 'INCIDENTE_DETECTADO', 'payload.origen': 'ALERTA'}},
    {$lookup: {from: 'alertas', localField: 'payload.id_origen', foreignField: '_id', as: 'alerta'}},
    {$unwind: '$alerta'},
    {$project: {_id: 0, id_incidente: 1, fecha: 1, regla: '$alerta.tipo_regla', severidad: '$alerta.severidad'}}
  ]).toArray();
  inc.forEach(x => print('   ', x.fecha.toISOString().slice(0, 16), x.regla.padEnd(12), x.severidad, 'incidente', x.id_incidente));
  obtenido = inc.length;
  verificar("R25", "Incidente <-> alerta que lo originó ($lookup)", 3, obtenido);
}
// --- R25.20 · Control de consistencia: alertas ALTA/CRÍTICA sin incidente asociado
// Esperado: 0
{
  obtenido = db.alertas.countDocuments({severidad: {$in: ['ALTA', 'CRITICA']}, id_incidente: null});
  verificar("R25", "Control de consistencia: alertas ALTA/CRÍTICA sin incidente asociado", 0, obtenido);
}

// ===================================================================== R26
print('\n==================== R26 ====================');
// --- R26.21 · Alertas por estado
// Esperado: {ABIERTA: 6, CERRADA: 1, RECONOCIDA: 1}
{
  obtenido = {};
  db.alertas.aggregate([{$group: {_id: '$estado', n: {$sum: 1}}}, {$sort: {_id: 1}}]).forEach(x => { obtenido[x._id] = x.n; });
  verificar("R26", "Alertas por estado", {ABIERTA: 6, CERRADA: 1, RECONOCIDA: 1}, obtenido);
}
// --- R26.22 · Alertas activas por tipo de regla
// Esperado: {ANOMALIA_TEMPERATURA: 2, BATERIA: 1, INACTIVIDAD: 2, TEMPERATURA: 1, VIBRACION: 1}
{
  obtenido = {};
  db.alertas.aggregate([{$match: {activa: true}}, {$group: {_id: '$tipo_regla', n: {$sum: 1}}}, {$sort: {_id: 1}}])
    .forEach(x => { obtenido[x._id] = x.n; });
  verificar("R26", "Alertas activas por tipo de regla", {ANOMALIA_TEMPERATURA: 2, BATERIA: 1, INACTIVIDAD: 2, TEMPERATURA: 1, VIBRACION: 1}, obtenido);
}
// --- R26.23 · Nunca hay dos alertas activas con la misma clave (deduplicación)
// Esperado: 0
{
  obtenido = db.alertas.aggregate([{$match: {activa: true}}, {$group: {_id: '$clave_dedup', n: {$sum: 1}}}, {$match: {n: {$gt: 1}}}]).toArray().length;
  verificar("R26", "Nunca hay dos alertas activas con la misma clave (deduplicación)", 0, obtenido);
}
// --- R26.24 · Alertas abiertas/reconocidas del cliente C02 (usa índice ix_cliente_estado_fecha)
// Esperado: 2
{
  const filtroC02 = {id_cliente: "270823a9-7978-5eda-80f0-9ae673331b0b", estado: {$in: ['ABIERTA', 'RECONOCIDA']}};
  const plan = JSON.stringify(db.alertas.find(filtroC02).sort({fecha: -1}).explain('queryPlanner').queryPlanner.winningPlan);
  print('    plan usa ix_cliente_estado_fecha:', plan.includes('ix_cliente_estado_fecha'));
  obtenido = db.alertas.countDocuments(filtroC02);
  verificar("R26", "Alertas abiertas/reconocidas del cliente C02 (usa índice ix_cliente_estado_fecha)", 2, obtenido);
}
// --- R26.25 · Alerta con su evento ALERTA_GENERADA (trazabilidad Alerta -> Evento del enunciado)
// Esperado: 8
{
  obtenido = db.alertas.aggregate([
    {$lookup: {from: 'eventos_logisticos', localField: 'id_evento', foreignField: '_id', as: 'ev'}},
    {$match: {'ev.tipo': 'ALERTA_GENERADA'}}
  ]).toArray().length;
  verificar("R26", "Alerta con su evento ALERTA_GENERADA (trazabilidad Alerta -> Evento del enunciado)", 8, obtenido);
}

// ===================================================================== R32
print('\n==================== R32 ====================');
// --- R32.26 · Lecturas anómalas de temperatura detectadas (|z| > 3)
// Esperado: 5
{
  const anom = db.alertas.aggregate([
    {$match: {tipo_regla: 'ANOMALIA_TEMPERATURA'}},
    {$unwind: '$detalle.lecturas'},
    {$project: {_id: 0, id_sensor: 1, fecha: '$detalle.lecturas.fecha_hora', temp: '$detalle.lecturas.temperatura', z: '$detalle.lecturas.z'}},
    {$sort: {fecha: 1}}
  ]).toArray();
  anom.forEach(x => print('   ', x.fecha.toISOString().slice(0, 16), 'temp', x.temp, 'z', x.z));
  obtenido = anom.length;
  verificar("R32", "Lecturas anómalas de temperatura detectadas (|z| > 3)", 5, obtenido);
}
// --- R32.27 · Sensores con anomalía de temperatura
// Esperado: 2
{
  obtenido = db.alertas.distinct('id_sensor', {tipo_regla: 'ANOMALIA_TEMPERATURA'}).length;
  verificar("R32", "Sensores con anomalía de temperatura", 2, obtenido);
}

// ===================================================================== R40
print('\n==================== R40 ====================');
// --- R40.28 · Bandeja del cliente C02 (solo sus conversaciones, más reciente primero)
// Esperado: ["Demora del envío E002", "Excursión de temperatura contenedor MSKU0000003"]
{
  obtenido = db.conversaciones.find({ids_participantes: "CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b"}, {asunto: 1})
    .sort({'ultimo_mensaje.fecha': -1}).toArray().map(x => x.asunto);
  verificar("R40", "Bandeja del cliente C02 (solo sus conversaciones, más reciente primero)", ["Demora del envío E002", "Excursión de temperatura contenedor MSKU0000003"], obtenido);
}
// --- R40.29 · Aislamiento: el cliente C01 NO puede abrir la conversación privada de C02
// Esperado: DENEGADO
{
  obtenido = db.conversaciones.findOne({_id: "61fd042c-9a1f-5796-ae0b-b4919e5cb427", ids_participantes: "CLIENTE:aef2750c-1592-5ed4-b7a7-ab5771efc165"}) === null ? 'DENEGADO' : 'PERMITIDO';
  verificar("R40", "Aislamiento: el cliente C01 NO puede abrir la conversación privada de C02", "DENEGADO", obtenido);
}
// --- R40.30 · El participante sí puede abrirla
// Esperado: PERMITIDO
{
  obtenido = db.conversaciones.findOne({_id: "61fd042c-9a1f-5796-ae0b-b4919e5cb427", ids_participantes: "CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b"}) === null ? 'DENEGADO' : 'PERMITIDO';
  verificar("R40", "El participante sí puede abrirla", "PERMITIDO", obtenido);
}
// --- R40.31 · Mensajería grupal: grupos de trabajo existentes
// Esperado: 2
{
  const grupos = db.conversaciones.find({tipo: 'GRUPAL'}, {nombre_grupo: 1, participantes: 1}).toArray();
  grupos.forEach(g => print('   ', g.nombre_grupo, '->', g.participantes.map(p => p.tipo + ' ' + p.nombre).join(', ')));
  obtenido = grupos.length;
  verificar("R40", "Mensajería grupal: grupos de trabajo existentes", 2, obtenido);
}
// --- R40.32 · Historial de la conversación grupal, paginado (3 por página)
// Esperado: [3, 3]
{
  const pag1 = db.mensajes.find({id_conversacion: "eb3ed64f-c916-579d-8d26-48511d1cb78e"}).sort({fecha_envio: -1, _id: -1}).limit(3).toArray();
  const corte = pag1[pag1.length - 1].fecha_envio;
  const pag2 = db.mensajes.find({id_conversacion: "eb3ed64f-c916-579d-8d26-48511d1cb78e", fecha_envio: {$lt: corte}}).sort({fecha_envio: -1, _id: -1}).limit(3).toArray();
  pag1.concat(pag2).forEach(m => print('   ', m.fecha_envio.toISOString().slice(0, 16), m.remitente.nombre.padEnd(32), m.contenido.slice(0, 60)));
  obtenido = [pag1.length, pag2.length];
  verificar("R40", "Historial de la conversación grupal, paginado (3 por página)", [3, 3], obtenido);
}
// --- R40.33 · Mensajes no leídos del operador O01
// Esperado: 1
{
  db.mensajes.deleteOne({_id: "640ad5d3-816a-5e11-ae36-660fa4f14759"});  // limpia el mensaje de prueba de una corrida anterior
  var convsO01 = db.conversaciones.find({ids_participantes: "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"}).toArray().map(x => x._id);
  var filtroNoLeidos = {id_conversacion: {$in: convsO01}, 'remitente.id_entidad': {$ne: "c6d39b10-a95f-5346-90b9-0de9d2f1f771"}, leido_por: {$ne: "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"}};
  obtenido = db.mensajes.countDocuments(filtroNoLeidos);
  verificar("R40", "Mensajes no leídos del operador O01", 1, obtenido);
}
// --- R40.34 · Enviar mensaje privado (C02 -> O01): se guarda y se actualiza la conversación
// Esperado: 1
{
  const antes = db.conversaciones.findOne({_id: "61fd042c-9a1f-5796-ae0b-b4919e5cb427"}).cantidad_mensajes;
  // Control de acceso ANTES de escribir: el remitente debe participar de la conversación
  if (db.conversaciones.findOne({_id: "61fd042c-9a1f-5796-ae0b-b4919e5cb427", ids_participantes: "CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b"}) === null) throw new Error('ACCESO_DENEGADO');
  const r = db.mensajes.updateOne({_id: "640ad5d3-816a-5e11-ae36-660fa4f14759"}, {$setOnInsert: {id_conversacion: "61fd042c-9a1f-5796-ae0b-b4919e5cb427", remitente: {tipo: "CLIENTE", id_entidad: "270823a9-7978-5eda-80f0-9ae673331b0b", id_usuario: "a841cc7a-599e-59a5-9a94-aca73ccae354", nombre: "Lácteos Pampeanos S.A."}, destinatario: {tipo: "OPERADOR", id_entidad: "c6d39b10-a95f-5346-90b9-0de9d2f1f771"}, fecha_envio: ISODate("2026-10-22T23:55:00.000Z"), contenido: "¿Confirmaron el turno de atraque en Santos?", tipo: "TEXTO", estado: "ENVIADO", leido_por: [], adjunto: null}}, {upsert: true});
  if (r.upsertedCount === 1) {
    db.conversaciones.updateOne({_id: "61fd042c-9a1f-5796-ae0b-b4919e5cb427"}, {$inc: {cantidad_mensajes: NumberInt(1)},
      $set: {ultimo_mensaje: {fecha: ISODate("2026-10-22T23:55:00Z"), remitente: "CLIENTE:270823a9-7978-5eda-80f0-9ae673331b0b", extracto: "¿Confirmaron el turno de atraque en Santos?"}}});
  }
  obtenido = db.conversaciones.findOne({_id: "61fd042c-9a1f-5796-ae0b-b4919e5cb427"}).cantidad_mensajes - antes;
  verificar("R40", "Enviar mensaje privado (C02 -> O01): se guarda y se actualiza la conversación", 1, obtenido);
}
// --- R40.35 · El operador O01 ve el nuevo mensaje como no leído y luego los marca leídos
// Esperado: [2, 0]
{
  const pendientes = db.mensajes.countDocuments(filtroNoLeidos);
  db.mensajes.updateMany(filtroNoLeidos, {$addToSet: {leido_por: "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"}, $set: {estado: 'LEIDO'}});
  obtenido = [pendientes, db.mensajes.countDocuments(filtroNoLeidos)];
  verificar("R40", "El operador O01 ve el nuevo mensaje como no leído y luego los marca leídos", [2, 0], obtenido);
}
// --- R40.36 · Se revierte la prueba (mensaje de prueba y lecturas) para poder repetir el script
// Esperado: [1, 1]
{
  db.mensajes.updateMany({_id: {$in: ["e3c8ea3b-4bb7-5fec-8b03-5e02bd16bbc9"]}}, {$pull: {leido_por: "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771"}, $set: {estado: 'ENVIADO'}});
  const borrado = db.mensajes.deleteOne({_id: "640ad5d3-816a-5e11-ae36-660fa4f14759"}).deletedCount;
  db.conversaciones.updateOne({_id: "61fd042c-9a1f-5796-ae0b-b4919e5cb427"}, {$set: {cantidad_mensajes: NumberInt(6), ultimo_mensaje: {fecha: ISODate("2026-10-21T22:30:00.000Z"), remitente: "OPERADOR:c6d39b10-a95f-5346-90b9-0de9d2f1f771", extracto: "Les avisamos apenas tengamos turno de atraque."}}});
  obtenido = [borrado, db.mensajes.countDocuments(filtroNoLeidos)];
  verificar("R40", "Se revierte la prueba (mensaje de prueba y lecturas) para poder repetir el script", [1, 1], obtenido);
}
// --- R40.37 · El proveedor P02 solo ve el grupo de trabajo donde participa
// Esperado: 1
{
  obtenido = db.conversaciones.countDocuments({ids_participantes: "PROVEEDOR:ec12ca06-a78a-5605-ab40-31988a417f61"});
  verificar("R40", "El proveedor P02 solo ve el grupo de trabajo donde participa", 1, obtenido);
}

const fallas = resultados.filter(r => !r.ok);
print('\n=====================================================================');
print('RESUMEN: ' + (resultados.length - fallas.length) + ' de ' + resultados.length + ' verificaciones OK');
fallas.forEach(f => print('  FALLA ' + f.req + ' · ' + f.descripcion));
