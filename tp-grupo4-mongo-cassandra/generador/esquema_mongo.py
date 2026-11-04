"""Modelo físico de MongoDB: colecciones, validadores $jsonSchema e índices.

Es la única fuente del esquema:
  * `exportar_mongosh()` genera mongodb/01_schema.js (se corre con mongosh).
  * `validar_documento()` aplica el mismo $jsonSchema en Python para verificar que
    todos los datos de prueba generados cumplen el validador antes de exportarlos.
"""

from __future__ import annotations

import json
from datetime import datetime

from enums import (
    ESTADOS_ALERTA,
    ORIGENES_EVENTO,
    SEVERIDADES,
    TIPOS_CONVERSACION,
    TIPOS_EVENTO,
    TIPOS_MENSAJE,
    TIPOS_PARTICIPANTE,
    TIPOS_REGLA,
)

NUM = "number"  # alias de $jsonSchema para double/int/long/decimal
STR_O_NULL = {"bsonType": ["string", "null"]}
NUM_O_NULL = {"bsonType": ["number", "null"]}
FECHA = {"bsonType": "date"}
TEXTO = {"bsonType": "string", "minLength": 1}

PUNTO_GEOJSON = {
    "bsonType": "object",
    "required": ["type", "coordinates"],
    "properties": {
        "type": {"enum": ["Point"]},
        "coordinates": {  # [longitud, latitud] (orden GeoJSON)
            "bsonType": "array", "minItems": 2, "maxItems": 2,
            "items": {"bsonType": NUM},
        },
    },
}

PARTICIPANTE = {
    "bsonType": "object",
    "required": ["tipo", "id_entidad"],
    "properties": {
        "tipo": {"enum": TIPOS_PARTICIPANTE},
        "id_entidad": TEXTO,
        "id_usuario": STR_O_NULL,
        "nombre": {"bsonType": "string"},
    },
}

COLECCIONES: dict[str, dict] = {
    # ------------------------------------------------------------ eventos logísticos
    "eventos_logisticos": {
        "descripcion": "Historial de eventos de cada envío (trazabilidad). Payload variable según tipo.",
        "validador": {
            "bsonType": "object",
            "required": ["_id", "id_envio", "tipo", "fecha", "origen", "creado_en"],
            "properties": {
                "_id": TEXTO,  # id_evento (UUID) -> idempotencia natural
                "id_envio": TEXTO,
                "id_contenedor": STR_O_NULL,
                "id_tramo": STR_O_NULL,
                "tipo": {"enum": TIPOS_EVENTO},
                "fecha": FECHA,
                "ubicacion": {
                    "bsonType": ["object", "null"],
                    "properties": {
                        "nombre": {"bsonType": "string"},
                        "pais": {"bsonType": "string"},
                        "geo": PUNTO_GEOJSON,
                    },
                },
                "descripcion": {"bsonType": "string"},
                "payload": {"bsonType": "object"},
                "origen": {"enum": ORIGENES_EVENTO},
                "id_alerta": STR_O_NULL,
                "id_incidente": STR_O_NULL,
                "id_correlacion": {"bsonType": "string"},
                "registrado_por": STR_O_NULL,
                "creado_en": FECHA,
            },
        },
        "indices": [
            ({"id_envio": 1, "fecha": 1}, {"name": "ix_envio_fecha"}),
            ({"id_contenedor": 1, "fecha": 1}, {"name": "ix_contenedor_fecha",
                                                 "partialFilterExpression": {"id_contenedor": {"$type": "string"}}}),
            ({"tipo": 1, "fecha": -1}, {"name": "ix_tipo_fecha"}),
            ({"ubicacion.geo": "2dsphere"}, {"name": "ix_ubicacion_geo"}),
        ],
    },
    # ------------------------------------------------------------ alertas
    "alertas": {
        "descripcion": "Resultado de evaluar reglas (definidas en PostgreSQL) sobre la telemetría.",
        "validador": {
            "bsonType": "object",
            "required": ["_id", "id_regla", "tipo_regla", "id_sensor", "severidad", "estado",
                         "activa", "fecha", "clave_dedup", "ocurrencias", "historial_estados"],
            "properties": {
                "_id": TEXTO,
                "id_regla": TEXTO,
                "tipo_regla": {"enum": TIPOS_REGLA},
                "id_sensor": TEXTO,
                "id_contenedor": STR_O_NULL,
                "id_envio": STR_O_NULL,
                "id_cliente": STR_O_NULL,
                "id_lectura": STR_O_NULL,
                "id_evento": STR_O_NULL,
                "id_incidente": STR_O_NULL,
                "operador": {"enum": [">", ">=", "<", "<="]},
                "umbral": NUM_O_NULL,
                "valor_observado": NUM_O_NULL,
                "severidad": {"enum": SEVERIDADES},
                "estado": {"enum": ESTADOS_ALERTA},
                "activa": {"bsonType": "bool"},
                "mensaje": {"bsonType": "string"},
                "detalle": {"bsonType": ["object", "null"]},  # estructura variable según tipo_regla
                "fecha": FECHA,
                "fecha_lectura": {"bsonType": ["date", "null"]},
                "ultima_ocurrencia": FECHA,
                "ocurrencias": {"bsonType": "int", "minimum": 1},
                "clave_dedup": TEXTO,
                "historial_estados": {
                    "bsonType": "array", "minItems": 1,
                    "items": {
                        "bsonType": "object",
                        "required": ["estado", "fecha"],
                        "properties": {
                            "estado": {"enum": ESTADOS_ALERTA},
                            "fecha": FECHA,
                            "id_usuario": STR_O_NULL,
                            "comentario": {"bsonType": ["string", "null"]},
                        },
                    },
                },
            },
        },
        "indices": [
            # D04: a lo sumo UNA alerta activa por (regla, sensor). Lecturas repetidas
            # suman ocurrencias; al cerrarla, una nueva violación abre otra alerta.
            ({"clave_dedup": 1}, {"name": "ux_dedup_activa", "unique": True,
                                  "partialFilterExpression": {"activa": True}}),
            ({"id_sensor": 1, "fecha": -1}, {"name": "ix_sensor_fecha"}),
            ({"id_envio": 1, "fecha": -1}, {"name": "ix_envio_fecha"}),
            ({"estado": 1, "severidad": 1, "fecha": -1}, {"name": "ix_estado_severidad_fecha"}),
            ({"id_cliente": 1, "estado": 1, "fecha": -1}, {"name": "ix_cliente_estado_fecha"}),
        ],
    },
    # ------------------------------------------------------------ conversaciones
    "conversaciones": {
        "descripcion": "Contexto de mensajería privada o grupal (GrupoDeTrabajo) entre las partes.",
        "validador": {
            "bsonType": "object",
            "required": ["_id", "tipo", "participantes", "ids_participantes", "creada_por",
                         "fecha_creacion", "estado"],
            "properties": {
                "_id": TEXTO,
                "tipo": {"enum": TIPOS_CONVERSACION},
                "asunto": {"bsonType": "string"},
                "nombre_grupo": STR_O_NULL,
                "id_envio": STR_O_NULL,
                "participantes": {"bsonType": "array", "minItems": 2, "items": PARTICIPANTE},
                "ids_participantes": {"bsonType": "array", "minItems": 2, "items": TEXTO},
                "creada_por": TEXTO,
                "fecha_creacion": FECHA,
                "estado": {"enum": ["ABIERTA", "ARCHIVADA"]},
                "cantidad_mensajes": {"bsonType": "int", "minimum": 0},
                "ultimo_mensaje": {
                    "bsonType": ["object", "null"],
                    "properties": {
                        "fecha": FECHA,
                        "remitente": {"bsonType": "string"},
                        "extracto": {"bsonType": "string"},
                    },
                },
            },
        },
        "indices": [
            ({"ids_participantes": 1, "ultimo_mensaje.fecha": -1}, {"name": "ix_participante_actividad"}),
            ({"id_envio": 1}, {"name": "ix_envio",
                               "partialFilterExpression": {"id_envio": {"$type": "string"}}}),
        ],
    },
    # ------------------------------------------------------------ mensajes
    "mensajes": {
        "descripcion": "Mensajes referenciados (no embebidos) para paginar y auditar sin límite de 16 MB.",
        "validador": {
            "bsonType": "object",
            "required": ["_id", "id_conversacion", "remitente", "fecha_envio", "contenido", "tipo", "estado"],
            "properties": {
                "_id": TEXTO,
                "id_conversacion": TEXTO,
                "remitente": PARTICIPANTE,
                "destinatario": {"bsonType": ["object", "null"]},
                "fecha_envio": FECHA,
                "contenido": {"bsonType": "string", "minLength": 1, "maxLength": 4000},
                "tipo": {"enum": TIPOS_MENSAJE},
                "estado": {"enum": ["ENVIADO", "LEIDO"]},
                "leido_por": {"bsonType": "array", "items": {"bsonType": "string"}},
                "adjunto": {"bsonType": ["object", "null"]},
            },
        },
        "indices": [
            ({"id_conversacion": 1, "fecha_envio": -1, "_id": -1}, {"name": "ix_conversacion_fecha"}),
        ],
    },
    # ------------------------------------------------------------ reportes generados
    "reportes_generados": {
        "descripcion": "Resultado (estructura variable) de reportes; la solicitud y el cobro viven en PostgreSQL.",
        "validador": {
            "bsonType": "object",
            "required": ["_id", "id_solicitud", "tipo", "parametros", "estado", "solicitado_por", "generado_en"],
            "properties": {
                "_id": TEXTO,
                "id_solicitud": TEXTO,
                "id_cliente": STR_O_NULL,
                "solicitado_por": TEXTO,
                "tipo": TEXTO,
                "parametros": {"bsonType": "object"},
                "formato": {"enum": ["JSON", "CSV", "PDF"]},
                "estado": {"enum": ["GENERADO", "ERROR"]},
                "resultado": {"bsonType": ["object", "array", "null"]},
                "ubicacion": STR_O_NULL,
                "generado_en": FECHA,
            },
        },
        "indices": [
            ({"id_solicitud": 1}, {"name": "ux_solicitud", "unique": True}),
            ({"id_cliente": 1, "generado_en": -1}, {"name": "ix_cliente_fecha"}),
            ({"tipo": 1, "generado_en": -1}, {"name": "ix_tipo_fecha"}),
        ],
    },
}


# ======================================================================= exportación mongosh
def exportar_mongosh() -> str:
    lineas = [
        "// =====================================================================",
        "// Ingeniería de Datos II · Grupo 4 · MongoDB (Persona 2)",
        "// Script de creación GENERADO desde generador/esquema_mongo.py (no editar a mano)",
        "// Uso: mongosh \"mongodb://localhost:27017/logistica_docs\" mongodb/01_schema.js",
        "// Re-ejecutable: si la colección existe se actualiza el validador (collMod).",
        "// =====================================================================",
        "",
        "const existentes = db.getCollectionNames();",
        "function asegurarColeccion(nombre, esquema) {",
        "  const opciones = { validator: { $jsonSchema: esquema }, validationLevel: 'strict', validationAction: 'error' };",
        "  if (existentes.includes(nombre)) {",
        "    db.runCommand(Object.assign({ collMod: nombre }, opciones));",
        "    print('collMod ' + nombre);",
        "  } else {",
        "    db.createCollection(nombre, opciones);",
        "    print('create  ' + nombre);",
        "  }",
        "}",
        "",
    ]
    for nombre, definicion in COLECCIONES.items():
        lineas.append(f"// ---- {nombre}: {definicion['descripcion']}")
        esquema = json.dumps(definicion["validador"], indent=2, ensure_ascii=False)
        lineas.append(f"asegurarColeccion('{nombre}', {esquema});")
        for claves, opts in definicion["indices"]:
            lineas.append(
                f"db.getCollection('{nombre}').createIndex({json.dumps(claves)}, {json.dumps(opts)});"
            )
        lineas.append("")
    lineas.append("print('Esquema MongoDB aplicado.');")
    return "\n".join(lineas) + "\n"


# ======================================================================= validador en memoria
_TIPOS = {
    "string": lambda v: isinstance(v, str),
    "object": lambda v: isinstance(v, dict),
    "array": lambda v: isinstance(v, list),
    "date": lambda v: isinstance(v, datetime),
    "bool": lambda v: isinstance(v, bool),
    "null": lambda v: v is None,
    "int": lambda v: isinstance(v, int) and not isinstance(v, bool),
    "long": lambda v: isinstance(v, int) and not isinstance(v, bool),
    "double": lambda v: isinstance(v, float),
    "number": lambda v: isinstance(v, (int, float)) and not isinstance(v, bool),
}


def _errores(valor, esquema: dict, ruta: str) -> list[str]:
    errores: list[str] = []
    tipos = esquema.get("bsonType")
    if tipos is not None:
        tipos = tipos if isinstance(tipos, list) else [tipos]
        if not any(_TIPOS[t](valor) for t in tipos):
            return [f"{ruta}: se esperaba {tipos}, llegó {type(valor).__name__}"]
    if "enum" in esquema and valor not in esquema["enum"]:
        errores.append(f"{ruta}: '{valor}' no está en {esquema['enum']}")
    if isinstance(valor, (int, float)) and not isinstance(valor, bool):
        if "minimum" in esquema and valor < esquema["minimum"]:
            errores.append(f"{ruta}: {valor} < mínimo {esquema['minimum']}")
        if "maximum" in esquema and valor > esquema["maximum"]:
            errores.append(f"{ruta}: {valor} > máximo {esquema['maximum']}")
    if isinstance(valor, str):
        if "minLength" in esquema and len(valor) < esquema["minLength"]:
            errores.append(f"{ruta}: texto más corto que {esquema['minLength']}")
        if "maxLength" in esquema and len(valor) > esquema["maxLength"]:
            errores.append(f"{ruta}: texto más largo que {esquema['maxLength']}")
    if isinstance(valor, dict):
        for requerido in esquema.get("required", []):
            if requerido not in valor:
                errores.append(f"{ruta}.{requerido}: campo obligatorio ausente")
        for clave, sub in esquema.get("properties", {}).items():
            if clave in valor:
                errores += _errores(valor[clave], sub, f"{ruta}.{clave}")
    if isinstance(valor, list):
        if "minItems" in esquema and len(valor) < esquema["minItems"]:
            errores.append(f"{ruta}: menos de {esquema['minItems']} elementos")
        if "maxItems" in esquema and len(valor) > esquema["maxItems"]:
            errores.append(f"{ruta}: más de {esquema['maxItems']} elementos")
        if "items" in esquema:
            for i, item in enumerate(valor):
                errores += _errores(item, esquema["items"], f"{ruta}[{i}]")
    return errores


def validar_documento(coleccion: str, documento: dict) -> list[str]:
    """Devuelve la lista de violaciones del $jsonSchema (vacía si el documento es válido)."""
    return _errores(documento, COLECCIONES[coleccion]["validador"], coleccion)
