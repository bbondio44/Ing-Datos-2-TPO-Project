"""Auto-verificación offline de los scripts generados (no necesita Docker).

    pip install mongomock        # solo para la parte de MongoDB
    python generador/verificar.py

1. Cassandra: parsea 01_schema.cql y 02_datos_prueba.cql, controla que cada INSERT use
   columnas existentes y la clave primaria completa, y simula la carga con semántica de
   Cassandra (upsert por PK + last-write-wins por USING TIMESTAMP) para comprobar los
   conteos y los casos límite que 03_consultas.cql declara como ESPERADO.
2. MongoDB: carga los documentos en mongomock con los mismos índices (incluido el único
   parcial de alertas) y ejecuta el equivalente Python de cada consulta de 03_consultas.js.
"""

from __future__ import annotations

import json
import os
import re
import sys
from collections import defaultdict

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(AQUI)
sys.path.insert(0, AQUI)

fallas: list[str] = []


def chequear(nombre: str, esperado, obtenido) -> None:
    ok = esperado == obtenido
    print(("OK    " if ok else "FALLA ") + nombre + ("" if ok else f"\n       esperado {esperado!r}\n       obtenido {obtenido!r}"))
    if not ok:
        fallas.append(nombre)


# =========================================================================== Cassandra
def esquema_cql() -> dict[str, dict]:
    texto = open(os.path.join(RAIZ, "cassandra/01_schema.cql"), encoding="utf-8").read()
    tablas = {}
    for m in re.finditer(r"CREATE TABLE IF NOT EXISTS (\w+) \((.*?)\n\)", texto, re.S):
        nombre, cuerpo = m.group(1), m.group(2)
        columnas = dict(re.findall(r"^\s+(\w+)\s+(uuid|date|timestamp|double)\s*,", cuerpo, re.M))
        pk = re.search(r"PRIMARY KEY \((.*)\)", cuerpo).group(1)
        part = re.match(r"\(([^)]*)\)", pk)
        if part:
            particion = [c.strip() for c in part.group(1).split(",")]
            clustering = [c.strip() for c in pk[part.end():].split(",") if c.strip()]
        else:
            partes = [c.strip() for c in pk.split(",")]
            particion, clustering = partes[:1], partes[1:]
        tablas[nombre] = {"columnas": columnas, "particion": particion, "clustering": clustering}
    return tablas


INSERT = re.compile(r"INSERT INTO (\w+) \(([^)]*)\) VALUES \((.*)\)(?: USING TIMESTAMP (\d+))?;")


def simular_cassandra(esperado: dict) -> None:
    print("\n=== Cassandra")
    tablas = esquema_cql()
    chequear("01_schema.cql define las 5 tablas del drawio", sorted(tablas), sorted([
        "lecturas_por_contenedor_dia", "lecturas_por_sensor_dia", "posiciones_por_contenedor_dia",
        "ultima_lectura_por_sensor", "ultima_posicion_por_contenedor"]))
    datos: dict[str, dict] = defaultdict(dict)
    errores = []
    lineas = open(os.path.join(RAIZ, "cassandra/02_datos_prueba.cql"), encoding="utf-8").read().splitlines()
    batch_abierto = False
    for n, linea in enumerate(lineas, 1):
        s = linea.strip()
        if s == "BEGIN UNLOGGED BATCH":
            if batch_abierto:
                errores.append(f"línea {n}: batch anidado")
            batch_abierto = True
            continue
        if s == "APPLY BATCH;":
            batch_abierto = False
            continue
        m = INSERT.match(s)
        if not m:
            continue
        tabla, cols, vals, ts = m.group(1), [c.strip() for c in m.group(2).split(",")], m.group(3), m.group(4)
        valores = [v.strip() for v in re.split(r",(?=(?:[^']*'[^']*')*[^']*$)", vals)]
        if len(cols) != len(valores):
            errores.append(f"línea {n}: {len(cols)} columnas y {len(valores)} valores")
            continue
        definicion = tablas[tabla]
        desconocidas = set(cols) - set(definicion["columnas"])
        if desconocidas:
            errores.append(f"línea {n}: columnas inexistentes {desconocidas}")
        faltan = set(definicion["particion"] + definicion["clustering"]) - set(cols)
        if faltan:
            errores.append(f"línea {n}: falta la clave primaria {faltan}")
        fila = dict(zip(cols, valores))
        for c, v in fila.items():
            tipo = definicion["columnas"].get(c)
            ok = {"uuid": re.fullmatch(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", v),
                  "date": re.fullmatch(r"'\d{4}-\d{2}-\d{2}'", v),
                  "timestamp": re.fullmatch(r"'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.000\+0000'", v),
                  "double": re.fullmatch(r"-?\d+\.\d+(e-?\d+)?", v)}.get(tipo)
            if tipo and not ok:
                errores.append(f"línea {n}: valor {v} no es {tipo}")
        clave = tuple(fila[c] for c in definicion["particion"] + definicion["clustering"])
        marca = int(ts) if ts else n  # sin USING TIMESTAMP: gana la escritura posterior
        previa = datos[tabla].get(clave)
        if previa is None or marca >= previa[0]:
            datos[tabla][clave] = (marca, fila)
    chequear("02_datos_prueba.cql: INSERTs válidos contra el esquema (columnas, PK, tipos)", [], errores[:5])
    chequear("batches cerrados correctamente", False, batch_abierto)

    exp = esperado["cassandra"]
    chequear("filas en lecturas_por_sensor_dia", exp["filas_lecturas_por_sensor_dia"], len(datos["lecturas_por_sensor_dia"]))
    chequear("filas en lecturas_por_contenedor_dia", exp["filas_lecturas_por_contenedor_dia"], len(datos["lecturas_por_contenedor_dia"]))
    chequear("filas en posiciones_por_contenedor_dia", exp["filas_posiciones_por_contenedor_dia"], len(datos["posiciones_por_contenedor_dia"]))
    chequear("ultima_lectura_por_sensor: 39 sensores (SEN-040 nunca reportó)", 39, len(datos["ultima_lectura_por_sensor"]))

    from catalogo import Catalogo
    cat = Catalogo()
    sen001 = cat.sensores["SEN-001"].id
    fila = next(f for k, (_, f) in datos["ultima_lectura_por_sensor"].items() if k[0] == sen001)
    chequear("C03 lectura tardía NO pisa la última lectura de SEN-001",
             "'2026-10-22 23:00:00.000+0000'", fila["fecha_hora"])
    tardia = [k for k in datos["lecturas_por_sensor_dia"] if k[0] == sen001 and k[2] == "'2026-10-22 05:30:00.000+0000'"]
    chequear("C03 lectura tardía SÍ queda en el historial", 1, len(tardia))
    cont1 = cat.contenedores["MSKU0000001"].id
    sin_gps = [k for k in datos["posiciones_por_contenedor_dia"] if k[0] == cont1 and k[2] == "'2026-10-22 06:00:00.000+0000'"]
    chequear("C04 lectura sin GPS no genera posición", 0, len(sin_gps))
    q = exp["consultas"]
    sen003 = cat.sensores["SEN-003"].id
    q02 = [k for k in datos["lecturas_por_sensor_dia"] if k[0] == sen003 and
           "'2026-10-20 18:00:00.000+0000'" <= k[2] <= "'2026-10-21 06:00:00.000+0000'"]
    chequear("Q02 historial cruzando medianoche", q["Q02"], len(q02))
    q09 = [f for k, (_, f) in datos["lecturas_por_sensor_dia"].items()
           if k[0] == sen003 and k[1] == "'2026-10-21'" and float(f["temperatura"]) > 8]
    chequear("Q09 lecturas > 8 °C de SEN-003", q["Q09"], len(q09))
    sen012 = cat.sensores["SEN-012"].id
    q10 = [f for k, (_, f) in datos["lecturas_por_sensor_dia"].items() if k[0] == sen012 and float(f["bateria"]) < 15]
    chequear("Q10 lecturas con batería < 15 % de SEN-012", q["Q10"], len(q10))
    inactivos = sorted(c for c, s in cat.sensores.items()
                       for k, (_, f) in datos["ultima_lectura_por_sensor"].items()
                       if k[0] == s.id and f["fecha_hora"] < "'2026-10-22 21:30:00.000+0000'")
    chequear("Q11 sensores inactivos", q["Q11"]["inactivos"], inactivos)
    # Reglas CQL básicas sobre 03_consultas.cql
    consultas = open(os.path.join(RAIZ, "cassandra/03_consultas.cql"), encoding="utf-8").read()
    selects = re.findall(r"^SELECT.*?;", consultas, re.S | re.M)
    malas = []
    for s in selects:
        tabla = re.search(r"FROM (\w+)", s).group(1)
        where = s.split("WHERE", 1)[1] if "WHERE" in s else ""
        for c in tablas[tabla]["particion"]:
            if not re.search(rf"\b{c}\s*(=|IN)", where):
                malas.append(f"{tabla}: no restringe la partition key {c}")
        if "ALLOW FILTERING" in s and not all(re.search(rf"\b{c}\s*=", where) for c in tablas[tabla]["particion"]):
            malas.append(f"{tabla}: ALLOW FILTERING fuera de una única partición")
    chequear(f"03_consultas.cql: {len(selects)} SELECT restringen la partition key (sin scans)", [], malas)


# =========================================================================== MongoDB
def simular_mongo() -> None:
    print("\n=== MongoDB")
    try:
        import mongomock
        import pymongo
    except ImportError:
        print("  (instalá mongomock para verificar la parte de MongoDB: pip install mongomock)")
        return
    from catalogo import Catalogo
    from consultas_mongo import construir
    from documentos import Documentos
    from esquema_mongo import COLECCIONES, validar_documento
    from telemetria import generar_lecturas

    cat = Catalogo()
    lecturas, _ = generar_lecturas(cat)
    docs = Documentos(cat, lecturas)
    db = mongomock.MongoClient().logistica_docs
    for nombre, definicion in COLECCIONES.items():
        for claves, opts in definicion["indices"]:
            if "2dsphere" in claves.values():
                continue
            db[nombre].create_index(list(claves.items()), **{k: v for k, v in opts.items() if k != "expireAfterSeconds"})
    for nombre, lista in [("eventos_logisticos", docs.eventos), ("alertas", docs.alertas),
                          ("conversaciones", docs.conversaciones), ("mensajes", docs.mensajes),
                          ("reportes_generados", docs.reportes)]:
        db[nombre].insert_many([dict(d) for d in lista])
        chequear(f"{nombre}: {len(lista)} documentos cumplen el $jsonSchema", 0,
                 sum(len(validar_documento(nombre, d)) for d in lista))
    # Índice único parcial: segunda alerta activa con la misma clave -> E11000
    al = dict(docs.alertas[0])
    al["_id"] = "dup"
    try:
        db.alertas.insert_one(al)
        chequear("índice único parcial rechaza alerta activa duplicada", "DuplicateKeyError", "insertó")
    except pymongo.errors.DuplicateKeyError:
        chequear("índice único parcial rechaza alerta activa duplicada", "DuplicateKeyError", "DuplicateKeyError")
    cerrada = next(a for a in docs.alertas if not a["activa"])
    copia = dict(cerrada, _id="dup-cerrada")
    db.alertas.insert_one(copia)
    chequear("índice parcial permite alertas cerradas con la misma clave", 1, db.alertas.count_documents({"_id": "dup-cerrada"}))
    db.alertas.delete_one({"_id": "dup-cerrada"})
    invalida = dict(docs.alertas[0], estado="INVENTADA")
    chequear("validador detecta estado inválido", True, bool(validar_documento("alertas", invalida)))
    # Referencias cruzadas
    ids_eventos = {e["_id"] for e in docs.eventos}
    chequear("toda alerta referencia un evento existente", 0, sum(a["id_evento"] not in ids_eventos for a in docs.alertas))
    ids_conv = {c["_id"] for c in docs.conversaciones}
    chequear("todo mensaje pertenece a una conversación existente", 0, sum(m["id_conversacion"] not in ids_conv for m in docs.mensajes))
    chequear("cantidad_mensajes coincide con los mensajes guardados", 0,
             sum(c["cantidad_mensajes"] != sum(m["id_conversacion"] == c["_id"] for m in docs.mensajes) for c in docs.conversaciones))
    remitente_ajeno = sum(1 for m in docs.mensajes
                          for c in docs.conversaciones if c["_id"] == m["id_conversacion"]
                          and f"{m['remitente']['tipo']}:{m['remitente']['id_entidad']}" not in c["ids_participantes"])
    chequear("todo remitente participa de su conversación", 0, remitente_ajeno)
    uuids = {f["uuid"] for f in cat.filas_csv()}
    refs = [(c, d[c]) for d in docs.eventos + docs.alertas for c in ("id_envio", "id_sensor", "id_contenedor", "id_cliente", "id_regla", "id_tramo")
            if d.get(c)]
    chequear("todas las referencias existen en catalogo_ids.csv", 0, sum(v not in uuids for _, v in refs))

    pasos, _ = construir(cat, docs)
    con_py = [p for p in pasos if p.py]
    print(f"  ({len(con_py)} de {len(pasos)} consultas de 03_consultas.js tienen equivalente verificable en mongomock)")
    for p in con_py:
        chequear(f"{p.req} · {p.descripcion}", p.esperado, p.py(db))


if __name__ == "__main__":
    esperado = json.load(open(os.path.join(AQUI, "esperado.json"), encoding="utf-8"))
    simular_cassandra(esperado)
    simular_mongo()
    print("\n" + ("TODO OK" if not fallas else f"{len(fallas)} FALLAS: " + "; ".join(fallas)))
    sys.exit(1 if fallas else 0)
