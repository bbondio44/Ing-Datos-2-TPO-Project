"""Genera TODOS los scripts de la carpeta a partir de un único dataset determinista.

    python generador/generar.py

Salida (se sobrescribe):
    catalogo_ids.csv                  UUID compartidos con PostgreSQL y Neo4j
    incidentes_para_postgres.csv      incidentes referenciados desde MongoDB (fuente: PostgreSQL)
    cassandra/02_datos_prueba.cql     cassandra/03_consultas.cql
    mongodb/01_schema.js              mongodb/02_datos_prueba.js       mongodb/03_consultas.js
    generador/esperado.json           resultados esperados (para comparar con la ejecución real)

Requiere solo Python 3.10+ (sin librerías externas).
"""

from __future__ import annotations

import csv
import json
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(AQUI)
sys.path.insert(0, AQUI)

import consultas_cql  # noqa: E402
import consultas_mongo  # noqa: E402
import cql  # noqa: E402
from catalogo import AHORA_REFERENCIA, Catalogo  # noqa: E402
from documentos import Documentos  # noqa: E402
from esquema_mongo import exportar_mongosh, validar_documento  # noqa: E402
from js import js  # noqa: E402
from telemetria import generar_lecturas  # noqa: E402

COLECCIONES_DATOS = ["eventos_logisticos", "alertas", "conversaciones", "mensajes", "reportes_generados"]


def escribir(ruta: str, contenido: str) -> None:
    ruta = os.path.join(RAIZ, ruta)
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8", newline="\n") as f:
        f.write(contenido)
    print(f"  {os.path.relpath(ruta, RAIZ):38s} {len(contenido.encode()) / 1024:8.1f} KB")


def datos_mongo(colecciones: dict[str, list[dict]]) -> str:
    o = [
        "// =====================================================================",
        "// MongoDB · DATOS DE PRUEBA (GENERADO por generador/generar.py, no editar a mano)",
        "// Uso: mongosh \"mongodb://localhost:27017/logistica_docs\" mongodb/02_datos_prueba.js",
        "// Re-ejecutable: cada documento se carga con replaceOne(upsert) por _id ->",
        "// vuelve el dataset a su estado inicial sin duplicar ni borrar otros datos.",
        "// Las referencias (id_envio, id_sensor, id_cliente, ...) son los UUID de catalogo_ids.csv.",
        "// FUERA DE CLASE: replaceOne y bulkWrite. Solo en esta carga: reponen el dataset sin duplicar.",
        "// Alternativa vista: updateOne con {upsert: true}, documento por documento.",
        f"// \"Ahora\" de referencia de la demo: {AHORA_REFERENCIA:%Y-%m-%d %H:%M} UTC",
        "// =====================================================================",
        "",
        "function cargar(nombre, docs) {",
        "  const ops = docs.map(d => ({ replaceOne: { filter: { _id: d._id }, replacement: d, upsert: true } }));",
        "  const r = db.getCollection(nombre).bulkWrite(ops, { ordered: true });",
        "  print(nombre.padEnd(20) + ' nuevos: ' + String(r.upsertedCount).padStart(4) +",
        "        '  reemplazados: ' + String(r.matchedCount).padStart(4) +",
        "        '  total en colección: ' + db.getCollection(nombre).countDocuments());",
        "}",
        "",
    ]
    for nombre in COLECCIONES_DATOS:
        o.append(f"// ---------------------------------------------------------------- {nombre} ({len(colecciones[nombre])})")
        o.append(f"cargar('{nombre}', {js(colecciones[nombre])});")
        o.append("")
    o.append("print('Datos de prueba de MongoDB cargados.');")
    return "\n".join(o) + "\n"


def main() -> None:
    cat = Catalogo()
    lecturas, tardia = generar_lecturas(cat)
    docs = Documentos(cat, lecturas)
    colecciones = {
        "eventos_logisticos": docs.eventos, "alertas": docs.alertas, "conversaciones": docs.conversaciones,
        "mensajes": docs.mensajes, "reportes_generados": docs.reportes,
    }
    # Todo documento debe cumplir el $jsonSchema antes de exportarse
    errores = [e for n, ds in colecciones.items() for d in ds for e in validar_documento(n, d)]
    if errores:
        raise SystemExit("Documentos inválidos:\n" + "\n".join(errores[:20]))

    print("Generando archivos:")
    filas = cat.filas_csv()
    with open(os.path.join(RAIZ, "catalogo_ids.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]))
        w.writeheader()
        w.writerows(filas)
    print(f"  {'catalogo_ids.csv':38s} {len(filas):8d} filas")
    with open(os.path.join(RAIZ, "incidentes_para_postgres.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(docs.incidentes[0]))
        w.writeheader()
        w.writerows(docs.incidentes)
    print(f"  {'incidentes_para_postgres.csv':38s} {len(docs.incidentes):8d} filas")

    nombres = {c: c for c in cat.sensores}
    escribir("cassandra/02_datos_prueba.cql", cql.datos_prueba(lecturas, tardia, nombres))
    texto_cql, esperado_cql = consultas_cql.generar(cat, lecturas, tardia)
    escribir("cassandra/03_consultas.cql", texto_cql)

    escribir("mongodb/01_schema.js", exportar_mongosh())
    escribir("mongodb/02_datos_prueba.js", datos_mongo(colecciones))
    pasos, _ = consultas_mongo.construir(cat, docs)
    escribir("mongodb/03_consultas.js", consultas_mongo.a_js(pasos))

    esperado = {
        "cassandra": {
            "lecturas_totales": len(lecturas) + 1,
            "filas_lecturas_por_sensor_dia": len(lecturas) + 1,
            "filas_lecturas_por_contenedor_dia": len(lecturas) + 1,
            "filas_posiciones_por_contenedor_dia": sum(1 for l in lecturas if l.tiene_gps) + 1,
            "consultas": esperado_cql,
        },
        "mongodb": {n: len(ds) for n, ds in colecciones.items()},
        "mongodb_consultas": [{"req": p.req, "descripcion": p.descripcion, "esperado": p.esperado} for p in pasos],
    }
    escribir("generador/esperado.json", json.dumps(esperado, indent=2, ensure_ascii=False, default=str))
    print(f"\nLecturas: {len(lecturas) + 1} · Documentos: " +
          ", ".join(f"{n}={len(ds)}" for n, ds in colecciones.items()) + f" · Pasos de verificación Mongo: {len(pasos)}")


if __name__ == "__main__":
    main()
