"""Genera mongodb/03_consultas.js: una sección por requisito con verificación OK/FALLA.

Cada paso define:
  * el código mongosh que se ejecuta contra la base real,
  * el valor esperado (calculado acá a partir del dataset), y
  * cuando mongomock lo soporta, una función Python equivalente que verificar.py corre
    para comprobar que la consulta devuelve exactamente lo esperado.
"""

from __future__ import annotations

import math
from collections import Counter
from dataclasses import dataclass
from datetime import timedelta
from typing import Callable

from catalogo import Catalogo, t
from documentos import Documentos
from ids import id_derivado, id_estable
from js import jsc


@dataclass
class Paso:
    req: str
    descripcion: str
    codigo: str               # JS que deja el resultado en la variable `obtenido`
    esperado: object
    py: Callable | None = None  # verificación en mongomock (opcional)


def _km(lat1, lon1, lat2, lon2):
    dlat, dlon = math.radians(lat2 - lat1), math.radians(lon2 - lon1)
    h = math.sin(dlat / 2) ** 2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2) ** 2
    return 2 * 6371.0088 * math.asin(math.sqrt(h))


def construir(cat: Catalogo, docs: Documentos) -> tuple[list[Paso], dict]:
    ID = {
        "E002": cat.envios["E002"].id,
        "C01": cat.clientes["C01"][0], "C02": cat.clientes["C02"][0],
        "O01": cat.operadores["O01"][0], "P02": cat.proveedores["P02"][0],
        "SEN026": cat.sensores["SEN-026"].id, "CONT001": cat.contenedores["MSKU0000001"].id,
        "ENV001": cat.envios["E001"].id,
        "REG06": cat.reglas["REG-06"].id,
        "CONV001": id_estable("conversacion", "CONV-001"), "CONV002": id_estable("conversacion", "CONV-002"),
        "U_OPS": cat.usuarios["U-OPS"]["id"], "U_C02": cat.usuarios["U-C02"]["id"],
        "PRUEBA_R09_A": id_derivado("prueba", "R09", "A"), "PRUEBA_R09_B": id_derivado("prueba", "R09", "B"),
        "PRUEBA_R09_C": id_derivado("prueba", "R09", "C"), "PRUEBA_R40": id_derivado("prueba", "R40", "mensaje"),
    }
    ev, al, msgs, convs = docs.eventos, docs.alertas, docs.mensajes, docs.conversaciones
    pasos: list[Paso] = []
    P = lambda *a, **k: pasos.append(Paso(*a, **k))

    # ================================================================ R09 · ABM de alertas
    alerta_base = {
        "id_regla": ID["REG06"], "tipo_regla": "HUMEDAD", "id_sensor": ID["SEN026"],
        "id_contenedor": ID["CONT001"], "id_envio": ID["ENV001"], "id_cliente": ID["C01"],
        "id_lectura": None, "id_evento": None, "id_incidente": None,
        "operador": ">", "umbral": 90.0, "valor_observado": 93.5, "severidad": "BAJA",
        "estado": "ABIERTA", "activa": True, "mensaje": "Humedad 93.5 % supera 90 % (dato de prueba R09)",
        "detalle": {"prueba": True}, "fecha": t("2026-10-22T23:50:00"), "fecha_lectura": t("2026-10-22T23:50:00"),
        "ultima_ocurrencia": t("2026-10-22T23:50:00"), "ocurrencias": 1,
        "clave_dedup": f"{ID['REG06']}|{ID['SEN026']}",
        "historial_estados": [{"estado": "ABIERTA", "fecha": t("2026-10-22T23:50:00"), "id_usuario": None, "comentario": None}],
    }
    a = {"_id": ID["PRUEBA_R09_A"], **alerta_base}
    b = {"_id": ID["PRUEBA_R09_B"], **alerta_base}
    c = {"_id": ID["PRUEBA_R09_C"], **alerta_base, "fecha": t("2026-10-23T02:00:00")}
    invalida = {"_id": "prueba-invalida", **alerta_base, "estado": "INVENTADA", "clave_dedup": "prueba-invalida"}
    ids_prueba = jsc([a["_id"], b["_id"], c["_id"], "prueba-invalida"])
    P("R09", "Alta de alerta (regla REG-06 definida en PostgreSQL)",
      f"db.alertas.deleteMany({{_id: {{$in: {ids_prueba}}}}});  // limpia corridas anteriores\n"
      f"obtenido = db.alertas.insertOne({jsc(a)}).acknowledged;", True)
    P("R09", "Segunda alerta ACTIVA para la misma regla+sensor es rechazada (índice único parcial)",
      f"try {{ db.alertas.insertOne({jsc(b)}); obtenido = 'insertó (MAL)'; }}\n"
      "catch (e) { obtenido = e.code === 11000 ? 'E11000 duplicate key' : String(e.code); }",
      "E11000 duplicate key")
    P("R09", "La repetición se registra sumando ocurrencias (no se duplica la alerta)",
      f"db.alertas.updateOne({{clave_dedup: {jsc(a['clave_dedup'])}, activa: true}},\n"
      f"  {{$inc: {{ocurrencias: NumberInt(1)}}, $set: {{ultima_ocurrencia: ISODate(\"2026-10-23T00:10:00Z\")}}}});\n"
      f"obtenido = db.alertas.findOne({{_id: {jsc(a['_id'])}}}).ocurrencias;", 2)
    P("R09", "El validador $jsonSchema rechaza un estado inexistente",
      f"try {{ db.alertas.insertOne({jsc(invalida)}); obtenido = 'insertó (MAL)'; }}\n"
      "catch (e) { obtenido = e.code === 121 ? 'DocumentValidationFailure' : String(e.code); }",
      "DocumentValidationFailure")
    P("R09", "Modificación: ABIERTA -> RECONOCIDA (con historial)",
      f"obtenido = db.alertas.updateOne({{_id: {jsc(a['_id'])}, estado: 'ABIERTA'}},\n"
      f"  {{$set: {{estado: 'RECONOCIDA'}}, $push: {{historial_estados: {{estado: 'RECONOCIDA', fecha: ISODate(\"2026-10-23T00:20:00Z\"), id_usuario: {jsc(ID['U_OPS'])}, comentario: 'Revisando'}}}}}}).modifiedCount;",
      1)
    P("R09", "Transición inválida: volver a 'reconocer' no modifica nada",
      f"obtenido = db.alertas.updateOne({{_id: {jsc(a['_id'])}, estado: 'ABIERTA'}}, {{$set: {{estado: 'RECONOCIDA'}}}}).modifiedCount;",
      0)
    P("R09", "Baja lógica: cerrar la alerta (activa = false)",
      f"obtenido = db.alertas.updateOne({{_id: {jsc(a['_id'])}, estado: {{$in: ['ABIERTA', 'RECONOCIDA']}}}},\n"
      f"  {{$set: {{estado: 'CERRADA', activa: false}}, $push: {{historial_estados: {{estado: 'CERRADA', fecha: ISODate(\"2026-10-23T01:00:00Z\"), id_usuario: {jsc(ID['U_OPS'])}, comentario: 'Normalizado'}}}}}}).modifiedCount;",
      1)
    P("R09", "Reapertura: una nueva violación tras el cierre crea otra alerta",
      f"obtenido = db.alertas.insertOne({jsc(c)}).acknowledged;", True)
    P("R09", "Se eliminan los documentos de prueba (el dataset queda como estaba)",
      f"obtenido = db.alertas.deleteMany({{_id: {{$in: {ids_prueba}}}}}).deletedCount;", 2)

    # ================================================================ R17 · Monitoreo integrado
    e002 = ID["E002"]
    ev_e002 = [e for e in ev if e["id_envio"] == e002]
    al_e002 = [x for x in al if x["id_envio"] == e002]
    timeline = sorted([(e["fecha"], "EVENTO", e["tipo"]) for e in ev_e002] +
                      [(x["fecha"], "ALERTA", x["tipo_regla"]) for x in al_e002])
    P("R17", "Línea de tiempo del envío E002: eventos + alertas ($unionWith)",
      "const linea = db.eventos_logisticos.aggregate([\n"
      f"  {{$match: {{id_envio: {jsc(e002)}}}}},\n"
      "  {$project: {_id: 0, fecha: 1, fuente: {$literal: 'EVENTO'}, tipo: 1, descripcion: 1}},\n"
      f"  {{$unionWith: {{coll: 'alertas', pipeline: [{{$match: {{id_envio: {jsc(e002)}}}}},\n"
      "     {$project: {_id: 0, fecha: 1, fuente: {$literal: 'ALERTA'}, tipo: '$tipo_regla', descripcion: '$mensaje', estado: 1}}]}},\n"
      "  {$sort: {fecha: 1}}\n"
      "]).toArray();\n"
      "linea.forEach(x => print('   ', x.fecha.toISOString().slice(0, 16), x.fuente.padEnd(6), x.tipo.padEnd(20), x.descripcion));\n"
      "obtenido = linea.length;", len(timeline))
    ultimo = max((e for e in ev_e002 if e["tipo"] not in ("ALERTA_GENERADA", "INCIDENTE_DETECTADO")), key=lambda e: e["fecha"])
    P("R17", "Estado de seguimiento actual de E002 (último evento logístico)",
      f"obtenido = db.eventos_logisticos.find({{id_envio: {jsc(e002)}, tipo: {{$nin: ['ALERTA_GENERADA', 'INCIDENTE_DETECTADO']}}}})\n"
      "  .sort({fecha: -1}).limit(1).toArray()[0].tipo;", ultimo["tipo"],
      py=lambda db: db.eventos_logisticos.find({"id_envio": e002, "tipo": {"$nin": ["ALERTA_GENERADA", "INCIDENTE_DETECTADO"]}}).sort("fecha", -1).limit(1)[0]["tipo"])
    activas_e002 = sum(1 for x in al_e002 if x["activa"])
    P("R17", "Alertas activas del envío E002",
      f"obtenido = db.alertas.countDocuments({{id_envio: {jsc(e002)}, activa: true}});", activas_e002,
      py=lambda db: db.alertas.count_documents({"id_envio": e002, "activa": True}))

    # ================================================================ R23 · Alertas de estado de sensores
    estado = sorted(x["mensaje"] for x in al if x["tipo_regla"] in ("BATERIA", "INACTIVIDAD") and x["activa"])
    P("R23", "Alertas activas de estado de sensor (batería baja / sin conexión)",
      "const est = db.alertas.find({tipo_regla: {$in: ['BATERIA', 'INACTIVIDAD']}, activa: true},\n"
      "  {_id: 0, tipo_regla: 1, severidad: 1, fecha: 1, valor_observado: 1, ocurrencias: 1, mensaje: 1}).sort({mensaje: 1}).toArray();\n"
      "est.forEach(x => print('   ', x.tipo_regla.padEnd(12), x.severidad.padEnd(6), x.fecha.toISOString().slice(0, 16), 'ocurrencias=' + x.ocurrencias, '|', x.mensaje));\n"
      "obtenido = est.length;", len(estado),
      py=lambda db: db.alertas.count_documents({"tipo_regla": {"$in": ["BATERIA", "INACTIVIDAD"]}, "activa": True}))

    # ================================================================ R24 · Gestión de trayectos (seguimiento)
    tramos = {}
    for e in sorted(ev_e002, key=lambda e: e["fecha"]):
        if e["id_tramo"]:
            tramos.setdefault(e["id_tramo"], []).append(e["tipo"])
    orden = [cat.envios["E002"].tramos[0].id, cat.envios["E002"].tramos[1].id]
    esperado_tramos = [len(tramos.get(i, [])) for i in orden]
    P("R24", "Seguimiento por tramo del envío E002 (eventos agrupados por tramo, en orden)",
      "const porTramo = db.eventos_logisticos.aggregate([\n"
      f"  {{$match: {{id_envio: {jsc(e002)}, id_tramo: {{$type: 'string'}}}}}},\n"
      "  {$sort: {fecha: 1}},\n"
      "  {$group: {_id: '$id_tramo', desde: {$first: '$fecha'}, hasta: {$last: '$fecha'}, eventos: {$push: '$tipo'}}},\n"
      "  {$sort: {desde: 1}}\n"
      "]).toArray();\n"
      "porTramo.forEach((x, i) => print('    tramo', i + 1, x.desde.toISOString().slice(0, 16), '->', x.hasta.toISOString().slice(0, 16), x.eventos.join(' > ')));\n"
      "obtenido = porTramo.map(x => x.eventos.length);", esperado_tramos,
      py=lambda db: [len(x["eventos"]) for x in db.eventos_logisticos.aggregate([
          {"$match": {"id_envio": e002, "id_tramo": {"$type": "string"}}}, {"$sort": {"fecha": 1}},
          {"$group": {"_id": "$id_tramo", "desde": {"$first": "$fecha"}, "eventos": {"$push": "$tipo"}}},
          {"$sort": {"desde": 1}}])])
    ultimos: dict[str, tuple] = {}
    for e in ev:
        if e["tipo"] in ("ALERTA_GENERADA", "INCIDENTE_DETECTADO"):
            continue
        if e["id_envio"] not in ultimos or e["fecha"] > ultimos[e["id_envio"]][0]:
            ultimos[e["id_envio"]] = (e["fecha"], e["tipo"])
    conteo = dict(sorted(Counter(x[1] for x in ultimos.values()).items()))
    P("R24", "Estado de seguimiento de todos los envíos (último evento de cada uno)",
      "obtenido = {};\n"
      "db.eventos_logisticos.aggregate([\n"
      "  {$match: {tipo: {$nin: ['ALERTA_GENERADA', 'INCIDENTE_DETECTADO']}}},\n"
      "  {$sort: {fecha: 1}},\n"
      "  {$group: {_id: '$id_envio', ultimo: {$last: '$tipo'}}},\n"
      "  {$group: {_id: '$ultimo', envios: {$sum: 1}}},\n"
      "  {$sort: {_id: 1}}\n"
      "]).forEach(x => { obtenido[x._id] = x.envios; });", conteo,
      py=lambda db: {x["_id"]: x["envios"] for x in db.eventos_logisticos.aggregate([
          {"$match": {"tipo": {"$nin": ["ALERTA_GENERADA", "INCIDENTE_DETECTADO"]}}}, {"$sort": {"fecha": 1}},
          {"$group": {"_id": "$id_envio", "ultimo": {"$last": "$tipo"}}},
          {"$group": {"_id": "$ultimo", "envios": {"$sum": 1}}}, {"$sort": {"_id": 1}}])})
    santos = cat_lugar = None
    from catalogo import LUGARES
    santos = LUGARES["PBA"]
    cerca = sum(1 for e in ev if e["ubicacion"] and _km(santos.lat, santos.lon,
                e["ubicacion"]["geo"]["coordinates"][1], e["ubicacion"]["geo"]["coordinates"][0]) <= 5)
    P("R24", "Eventos registrados a menos de 5 km del Puerto de Buenos Aires (índice 2dsphere)",
      "obtenido = db.eventos_logisticos.countDocuments({'ubicacion.geo': {$geoWithin: {$centerSphere: "
      f"[[{santos.lon}, {santos.lat}], 5 / 6378.1]}}}}}});\n"
      "// Variante ordenada por cercanía (usa el índice ix_ubicacion_geo):\n"
      "db.eventos_logisticos.find({'ubicacion.geo': {$near: {$geometry: {type: 'Point', coordinates: "
      f"[{santos.lon}, {santos.lat}]}}, $maxDistance: 5000}}}}}}, {{_id: 0, tipo: 1, fecha: 1, 'ubicacion.nombre': 1}})\n"
      "  .limit(3).forEach(x => print('   ', x.fecha.toISOString().slice(0, 16), x.tipo, x.ubicacion.nombre));",
      cerca)

    # ================================================================ R25 · Detección de incidentes
    P("R25", "Eventos RETRASO/DESVIO vinculados a su incidente (PostgreSQL)",
      "obtenido = db.eventos_logisticos.countDocuments({tipo: {$in: ['RETRASO', 'DESVIO']}, id_incidente: {$type: 'string'}});",
      sum(1 for e in ev if e["tipo"] in ("RETRASO", "DESVIO") and e["id_incidente"]),
      py=lambda db: db.eventos_logisticos.count_documents({"tipo": {"$in": ["RETRASO", "DESVIO"]}, "id_incidente": {"$type": "string"}}))
    origenes = dict(sorted(Counter(e["payload"]["origen"] for e in ev if e["tipo"] == "INCIDENTE_DETECTADO").items()))
    P("R25", "Incidentes detectados por origen (evento operativo o alerta de sensor)",
      "obtenido = {};\n"
      "db.eventos_logisticos.aggregate([{$match: {tipo: 'INCIDENTE_DETECTADO'}},\n"
      "  {$group: {_id: '$payload.origen', n: {$sum: 1}}}, {$sort: {_id: 1}}]).forEach(x => { obtenido[x._id] = x.n; });",
      origenes,
      py=lambda db: {x["_id"]: x["n"] for x in db.eventos_logisticos.aggregate([
          {"$match": {"tipo": "INCIDENTE_DETECTADO"}}, {"$group": {"_id": "$payload.origen", "n": {"$sum": 1}}}, {"$sort": {"_id": 1}}])})
    P("R25", "Incidente <-> alerta que lo originó ($lookup)",
      "const inc = db.eventos_logisticos.aggregate([\n"
      "  {$match: {tipo: 'INCIDENTE_DETECTADO', 'payload.origen': 'ALERTA'}},\n"
      "  {$lookup: {from: 'alertas', localField: 'payload.id_origen', foreignField: '_id', as: 'alerta'}},\n"
      "  {$unwind: '$alerta'},\n"
      "  {$project: {_id: 0, id_incidente: 1, fecha: 1, regla: '$alerta.tipo_regla', severidad: '$alerta.severidad'}}\n"
      "]).toArray();\n"
      "inc.forEach(x => print('   ', x.fecha.toISOString().slice(0, 16), x.regla.padEnd(12), x.severidad, 'incidente', x.id_incidente));\n"
      "obtenido = inc.length;", origenes.get("ALERTA", 0),
      py=lambda db: len(list(db.eventos_logisticos.aggregate([
          {"$match": {"tipo": "INCIDENTE_DETECTADO", "payload.origen": "ALERTA"}},
          {"$lookup": {"from": "alertas", "localField": "payload.id_origen", "foreignField": "_id", "as": "alerta"}},
          {"$unwind": "$alerta"}]))))
    P("R25", "Control de consistencia: alertas ALTA/CRÍTICA sin incidente asociado",
      "obtenido = db.alertas.countDocuments({severidad: {$in: ['ALTA', 'CRITICA']}, id_incidente: null});", 0,
      py=lambda db: db.alertas.count_documents({"severidad": {"$in": ["ALTA", "CRITICA"]}, "id_incidente": None}))

    # ================================================================ R26 · Generación de alertas
    por_estado = dict(sorted(Counter(x["estado"] for x in al).items()))
    P("R26", "Alertas por estado",
      "obtenido = {};\n"
      "db.alertas.aggregate([{$group: {_id: '$estado', n: {$sum: 1}}}, {$sort: {_id: 1}}]).forEach(x => { obtenido[x._id] = x.n; });",
      por_estado,
      py=lambda db: {x["_id"]: x["n"] for x in db.alertas.aggregate([{"$group": {"_id": "$estado", "n": {"$sum": 1}}}, {"$sort": {"_id": 1}}])})
    por_tipo = dict(sorted(Counter(x["tipo_regla"] for x in al if x["activa"]).items()))
    P("R26", "Alertas activas por tipo de regla",
      "obtenido = {};\n"
      "db.alertas.aggregate([{$match: {activa: true}}, {$group: {_id: '$tipo_regla', n: {$sum: 1}}}, {$sort: {_id: 1}}])\n"
      "  .forEach(x => { obtenido[x._id] = x.n; });", por_tipo,
      py=lambda db: {x["_id"]: x["n"] for x in db.alertas.aggregate([{"$match": {"activa": True}}, {"$group": {"_id": "$tipo_regla", "n": {"$sum": 1}}}, {"$sort": {"_id": 1}}])})
    P("R26", "Nunca hay dos alertas activas con la misma clave (deduplicación)",
      "obtenido = db.alertas.aggregate([{$match: {activa: true}}, {$group: {_id: '$clave_dedup', n: {$sum: 1}}}, {$match: {n: {$gt: 1}}}]).toArray().length;",
      0,
      py=lambda db: len(list(db.alertas.aggregate([{"$match": {"activa": True}}, {"$group": {"_id": "$clave_dedup", "n": {"$sum": 1}}}, {"$match": {"n": {"$gt": 1}}}]))))
    c02_abiertas = sum(1 for x in al if x["id_cliente"] == ID["C02"] and x["estado"] in ("ABIERTA", "RECONOCIDA"))
    P("R26", "Alertas abiertas/reconocidas del cliente C02 (usa índice ix_cliente_estado_fecha)",
      f"const filtroC02 = {{id_cliente: {jsc(ID['C02'])}, estado: {{$in: ['ABIERTA', 'RECONOCIDA']}}}};\n"
      "const plan = JSON.stringify(db.alertas.find(filtroC02).sort({fecha: -1}).explain('queryPlanner').queryPlanner.winningPlan);\n"
      "print('    plan usa ix_cliente_estado_fecha:', plan.includes('ix_cliente_estado_fecha'));\n"
      "obtenido = db.alertas.countDocuments(filtroC02);", c02_abiertas,
      py=lambda db: db.alertas.count_documents({"id_cliente": ID["C02"], "estado": {"$in": ["ABIERTA", "RECONOCIDA"]}}))
    P("R26", "Alerta con su evento ALERTA_GENERADA (trazabilidad Alerta -> Evento del enunciado)",
      "obtenido = db.alertas.aggregate([\n"
      "  {$lookup: {from: 'eventos_logisticos', localField: 'id_evento', foreignField: '_id', as: 'ev'}},\n"
      "  {$match: {'ev.tipo': 'ALERTA_GENERADA'}}\n"
      "]).toArray().length;", len(al),
      py=lambda db: len(list(db.alertas.aggregate([
          {"$lookup": {"from": "eventos_logisticos", "localField": "id_evento", "foreignField": "_id", "as": "ev"}},
          {"$match": {"ev.tipo": "ALERTA_GENERADA"}}]))))

    # ================================================================ R32 · Anomalías de temperatura
    anom = [x for x in al if x["tipo_regla"] == "ANOMALIA_TEMPERATURA"]
    lect_anom = sum(len(x["detalle"]["lecturas"]) for x in anom)
    P("R32", "Lecturas anómalas de temperatura detectadas (|z| > 3)",
      "const anom = db.alertas.aggregate([\n"
      "  {$match: {tipo_regla: 'ANOMALIA_TEMPERATURA'}},\n"
      "  {$unwind: '$detalle.lecturas'},\n"
      "  {$project: {_id: 0, id_sensor: 1, fecha: '$detalle.lecturas.fecha_hora', temp: '$detalle.lecturas.temperatura', z: '$detalle.lecturas.z'}},\n"
      "  {$sort: {fecha: 1}}\n"
      "]).toArray();\n"
      "anom.forEach(x => print('   ', x.fecha.toISOString().slice(0, 16), 'temp', x.temp, 'z', x.z));\n"
      "obtenido = anom.length;", lect_anom,
      py=lambda db: len(list(db.alertas.aggregate([{"$match": {"tipo_regla": "ANOMALIA_TEMPERATURA"}}, {"$unwind": "$detalle.lecturas"}]))))
    P("R32", "Sensores con anomalía de temperatura",
      "obtenido = db.alertas.distinct('id_sensor', {tipo_regla: 'ANOMALIA_TEMPERATURA'}).length;", len(anom),
      py=lambda db: len(db.alertas.distinct("id_sensor", {"tipo_regla": "ANOMALIA_TEMPERATURA"})))

    # ================================================================ R40 · Mensajería
    k_c02, k_c01, k_o01, k_p02 = f"CLIENTE:{ID['C02']}", f"CLIENTE:{ID['C01']}", f"OPERADOR:{ID['O01']}", f"PROVEEDOR:{ID['P02']}"
    bandeja_c02 = [x["asunto"] for x in sorted((x for x in convs if k_c02 in x["ids_participantes"]),
                                                key=lambda x: x["ultimo_mensaje"]["fecha"], reverse=True)]
    P("R40", "Bandeja del cliente C02 (solo sus conversaciones, más reciente primero)",
      f"obtenido = db.conversaciones.find({{ids_participantes: {jsc(k_c02)}}}, {{asunto: 1}})\n"
      "  .sort({'ultimo_mensaje.fecha': -1}).toArray().map(x => x.asunto);", bandeja_c02,
      py=lambda db: [x["asunto"] for x in db.conversaciones.find({"ids_participantes": k_c02}).sort("ultimo_mensaje.fecha", -1)])
    P("R40", "Aislamiento: el cliente C01 NO puede abrir la conversación privada de C02",
      f"obtenido = db.conversaciones.findOne({{_id: {jsc(ID['CONV001'])}, ids_participantes: {jsc(k_c01)}}}) === null ? 'DENEGADO' : 'PERMITIDO';",
      "DENEGADO")
    P("R40", "El participante sí puede abrirla",
      f"obtenido = db.conversaciones.findOne({{_id: {jsc(ID['CONV001'])}, ids_participantes: {jsc(k_c02)}}}) === null ? 'DENEGADO' : 'PERMITIDO';",
      "PERMITIDO")
    grupales = sum(1 for x in convs if x["tipo"] == "GRUPAL")
    P("R40", "Mensajería grupal: grupos de trabajo existentes",
      "const grupos = db.conversaciones.find({tipo: 'GRUPAL'}, {nombre_grupo: 1, participantes: 1}).toArray();\n"
      "grupos.forEach(g => print('   ', g.nombre_grupo, '->', g.participantes.map(p => p.tipo + ' ' + p.nombre).join(', ')));\n"
      "obtenido = grupos.length;", grupales,
      py=lambda db: db.conversaciones.count_documents({"tipo": "GRUPAL"}))
    hist = sorted((m for m in msgs if m["id_conversacion"] == ID["CONV002"]), key=lambda m: (m["fecha_envio"], m["_id"]), reverse=True)
    P("R40", "Historial de la conversación grupal, paginado (3 por página)",
      f"const pag1 = db.mensajes.find({{id_conversacion: {jsc(ID['CONV002'])}}}).sort({{fecha_envio: -1, _id: -1}}).limit(3).toArray();\n"
      "const corte = pag1[pag1.length - 1].fecha_envio;\n"
      f"const pag2 = db.mensajes.find({{id_conversacion: {jsc(ID['CONV002'])}, fecha_envio: {{$lt: corte}}}}).sort({{fecha_envio: -1, _id: -1}}).limit(3).toArray();\n"
      "pag1.concat(pag2).forEach(m => print('   ', m.fecha_envio.toISOString().slice(0, 16), m.remitente.nombre.padEnd(32), m.contenido.slice(0, 60)));\n"
      "obtenido = [pag1.length, pag2.length];", [min(3, len(hist)), min(3, max(0, len(hist) - 3))])
    filtro_no_leidos = lambda k, eid: {"id_conversacion": {"$in": [x["_id"] for x in convs if k in x["ids_participantes"]]},
                                       "remitente.id_entidad": {"$ne": eid}, "leido_por": {"$ne": k}}
    no_leidos_o01 = sum(1 for m in msgs if m["id_conversacion"] in [x["_id"] for x in convs if k_o01 in x["ids_participantes"]]
                        and m["remitente"]["id_entidad"] != ID["O01"] and k_o01 not in m["leido_por"])
    P("R40", "Mensajes no leídos del operador O01",
      f"db.mensajes.deleteOne({{_id: {jsc(ID['PRUEBA_R40'])}}});  // limpia el mensaje de prueba de una corrida anterior\n"
      f"var convsO01 = db.conversaciones.find({{ids_participantes: {jsc(k_o01)}}}).toArray().map(x => x._id);\n"
      f"var filtroNoLeidos = {{id_conversacion: {{$in: convsO01}}, 'remitente.id_entidad': {{$ne: {jsc(ID['O01'])}}}, leido_por: {{$ne: {jsc(k_o01)}}}}};\n"
      "obtenido = db.mensajes.countDocuments(filtroNoLeidos);", no_leidos_o01,
      py=lambda db: db.mensajes.count_documents(filtro_no_leidos(k_o01, ID["O01"])))
    c02p = next(p for p in next(x for x in convs if x["_id"] == ID["CONV001"])["participantes"] if p["tipo"] == "CLIENTE")
    o01p = next(p for p in next(x for x in convs if x["_id"] == ID["CONV001"])["participantes"] if p["tipo"] == "OPERADOR")
    nuevo = {"_id": ID["PRUEBA_R40"], "id_conversacion": ID["CONV001"], "remitente": c02p,
             "destinatario": {"tipo": o01p["tipo"], "id_entidad": o01p["id_entidad"]},
             "fecha_envio": t("2026-10-22T23:55:00"), "contenido": "¿Confirmaron el turno de atraque en Santos?",
             "tipo": "TEXTO", "estado": "ENVIADO", "leido_por": [], "adjunto": None}
    P("R40", "Enviar mensaje privado (C02 -> O01): se guarda y se actualiza la conversación",
      f"const antes = db.conversaciones.findOne({{_id: {jsc(ID['CONV001'])}}}).cantidad_mensajes;\n"
      f"// Control de acceso ANTES de escribir: el remitente debe participar de la conversación\n"
      f"if (db.conversaciones.findOne({{_id: {jsc(ID['CONV001'])}, ids_participantes: {jsc(k_c02)}}}) === null) throw new Error('ACCESO_DENEGADO');\n"
      f"const r = db.mensajes.updateOne({{_id: {jsc(ID['PRUEBA_R40'])}}}, {{$setOnInsert: {jsc({k: v for k, v in nuevo.items() if k != '_id'})}}}, {{upsert: true}});\n"
      "if (r.upsertedCount === 1) {\n"
      f"  db.conversaciones.updateOne({{_id: {jsc(ID['CONV001'])}}}, {{$inc: {{cantidad_mensajes: NumberInt(1)}},\n"
      f"    $set: {{ultimo_mensaje: {{fecha: ISODate(\"2026-10-22T23:55:00Z\"), remitente: {jsc(k_c02)}, extracto: {jsc(nuevo['contenido'][:60])}}}}}}});\n"
      "}\n"
      f"obtenido = db.conversaciones.findOne({{_id: {jsc(ID['CONV001'])}}}).cantidad_mensajes - antes;", 1)
    P("R40", "El operador O01 ve el nuevo mensaje como no leído y luego los marca leídos",
      "const pendientes = db.mensajes.countDocuments(filtroNoLeidos);\n"
      f"db.mensajes.updateMany(filtroNoLeidos, {{$addToSet: {{leido_por: {jsc(k_o01)}}}, $set: {{estado: 'LEIDO'}}}});\n"
      "obtenido = [pendientes, db.mensajes.countDocuments(filtroNoLeidos)];", [no_leidos_o01 + 1, 0])
    conv001 = next(x for x in convs if x["_id"] == ID["CONV001"])
    no_leidos_ids = [m["_id"] for m in msgs if m["id_conversacion"] in [x["_id"] for x in convs if k_o01 in x["ids_participantes"]]
                     and m["remitente"]["id_entidad"] != ID["O01"] and k_o01 not in m["leido_por"]]
    P("R40", "Se revierte la prueba (mensaje de prueba y lecturas) para poder repetir el script",
      f"db.mensajes.updateMany({{_id: {{$in: {jsc(no_leidos_ids)}}}}}, {{$pull: {{leido_por: {jsc(k_o01)}}}, $set: {{estado: 'ENVIADO'}}}});\n"
      f"const borrado = db.mensajes.deleteOne({{_id: {jsc(ID['PRUEBA_R40'])}}}).deletedCount;\n"
      f"db.conversaciones.updateOne({{_id: {jsc(ID['CONV001'])}}}, {{$set: {{cantidad_mensajes: NumberInt({conv001['cantidad_mensajes']}), ultimo_mensaje: {jsc(conv001['ultimo_mensaje'])}}}}});\n"
      "obtenido = [borrado, db.mensajes.countDocuments(filtroNoLeidos)];", [1, no_leidos_o01])
    P("R40", "El proveedor P02 solo ve el grupo de trabajo donde participa",
      f"obtenido = db.conversaciones.countDocuments({{ids_participantes: {jsc(k_p02)}}});",
      sum(1 for x in convs if k_p02 in x["ids_participantes"]),
      py=lambda db: db.conversaciones.count_documents({"ids_participantes": k_p02}))

    resumen = {"pasos": len(pasos), "ids": ID}
    return pasos, resumen


def a_js(pasos: list[Paso]) -> str:
    o = [
        "// =====================================================================",
        "// MongoDB · CONSULTAS POR REQUISITO (Persona 2: R09, R17, R23, R24, R25, R26, R32, R40)",
        "// GENERADO por generador/generar.py con los IDs reales del dataset.",
        "// Uso: mongosh \"mongodb://localhost:27017/logistica_docs\" mongodb/03_consultas.js",
        "// Correr después de 01_schema.js y 02_datos_prueba.js. Cada paso imprime OK/FALLA",
        "// comparando el resultado obtenido contra el esperado (sirve como evidencia).",
        "// FUERA DE CLASE: aggregate ($match, $group, $lookup), $setOnInsert, $addToSet, $pull. Solo en estas",
        "// consultas de verificación, para contar y cruzar colecciones; la aplicación no los usa.",
        "// FUERA DE CLASE: también $in, $project, $unwind, $near y $geoWithin, solo en estas consultas.",
        "// Las pruebas de escritura (R09, R40) limpian lo que crean, se puede repetir.",
        "// =====================================================================",
        "",
        "const resultados = [];",
        "function verificar(req, descripcion, esperado, obtenido) {",
        "  const ok = JSON.stringify(esperado) === JSON.stringify(obtenido);",
        "  resultados.push({req, descripcion, ok});",
        "  print((ok ? 'OK    ' : 'FALLA ') + req + ' · ' + descripcion);",
        "  if (!ok) print('       esperado: ' + JSON.stringify(esperado) + '\\n       obtenido: ' + JSON.stringify(obtenido));",
        "  else print('       resultado: ' + JSON.stringify(obtenido));",
        "}",
        "let obtenido;",
        "",
    ]
    req_actual = None
    for i, p in enumerate(pasos, 1):
        if p.req != req_actual:
            req_actual = p.req
            o += ["", f"// ===================================================================== {p.req}",
                  f"print('\\n==================== {p.req} ====================');"]
        o.append(f"// --- {p.req}.{i} · {p.descripcion}")
        o.append(f"// Esperado: {jsc(p.esperado) if not isinstance(p.esperado, str) else p.esperado}")
        o.append("{")
        o += ["  " + linea for linea in p.codigo.split("\n")]
        o.append(f"  verificar({jsc(p.req)}, {jsc(p.descripcion)}, {jsc(p.esperado)}, obtenido);")
        o.append("}")
    o += [
        "",
        "const fallas = resultados.filter(r => !r.ok);",
        "print('\\n=====================================================================');",
        "print('RESUMEN: ' + (resultados.length - fallas.length) + ' de ' + resultados.length + ' verificaciones OK');",
        "fallas.forEach(f => print('  FALLA ' + f.req + ' · ' + f.descripcion));",
    ]
    return "\n".join(o) + "\n"
