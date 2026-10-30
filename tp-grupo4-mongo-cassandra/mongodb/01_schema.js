// =====================================================================
// Ingeniería de Datos II · Grupo 4 · MongoDB (Persona 2)
// Script de creación GENERADO desde generador/esquema_mongo.py (no editar a mano)
// Uso: mongosh "mongodb://localhost:27017/logistica_docs" mongodb/01_schema.js
// Re-ejecutable: si la colección existe se actualiza el validador (collMod).
// =====================================================================

const existentes = db.getCollectionNames();
function asegurarColeccion(nombre, esquema) {
  const opciones = { validator: { $jsonSchema: esquema }, validationLevel: 'strict', validationAction: 'error' };
  if (existentes.includes(nombre)) {
    db.runCommand(Object.assign({ collMod: nombre }, opciones));
    print('collMod ' + nombre);
  } else {
    db.createCollection(nombre, opciones);
    print('create  ' + nombre);
  }
}

// ---- eventos_logisticos: Historial de eventos de cada envío (trazabilidad). Payload variable según tipo.
asegurarColeccion('eventos_logisticos', {
  "bsonType": "object",
  "required": [
    "_id",
    "id_envio",
    "tipo",
    "fecha",
    "origen",
    "creado_en"
  ],
  "properties": {
    "_id": {
      "bsonType": "string",
      "minLength": 1
    },
    "id_envio": {
      "bsonType": "string",
      "minLength": 1
    },
    "id_contenedor": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_tramo": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "tipo": {
      "enum": [
        "CARGA_INICIADA",
        "SALIDA",
        "EN_TRANSITO",
        "ADUANA",
        "RETRASO",
        "DESVIO",
        "TRASBORDO",
        "DESCARGA",
        "ENTREGADO",
        "INCIDENTE_DETECTADO",
        "ALERTA_GENERADA"
      ]
    },
    "fecha": {
      "bsonType": "date"
    },
    "ubicacion": {
      "bsonType": [
        "object",
        "null"
      ],
      "properties": {
        "nombre": {
          "bsonType": "string"
        },
        "pais": {
          "bsonType": "string"
        },
        "geo": {
          "bsonType": "object",
          "required": [
            "type",
            "coordinates"
          ],
          "properties": {
            "type": {
              "enum": [
                "Point"
              ]
            },
            "coordinates": {
              "bsonType": "array",
              "minItems": 2,
              "maxItems": 2,
              "items": {
                "bsonType": "number"
              }
            }
          }
        }
      }
    },
    "descripcion": {
      "bsonType": "string"
    },
    "payload": {
      "bsonType": "object"
    },
    "origen": {
      "enum": [
        "OPERADOR",
        "SENSOR",
        "SISTEMA"
      ]
    },
    "id_alerta": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_incidente": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_correlacion": {
      "bsonType": "string"
    },
    "registrado_por": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "creado_en": {
      "bsonType": "date"
    }
  }
});
db.getCollection('eventos_logisticos').createIndex({"id_envio": 1, "fecha": 1}, {"name": "ix_envio_fecha"});
db.getCollection('eventos_logisticos').createIndex({"id_contenedor": 1, "fecha": 1}, {"name": "ix_contenedor_fecha", "partialFilterExpression": {"id_contenedor": {"$type": "string"}}});
db.getCollection('eventos_logisticos').createIndex({"tipo": 1, "fecha": -1}, {"name": "ix_tipo_fecha"});
db.getCollection('eventos_logisticos').createIndex({"ubicacion.geo": "2dsphere"}, {"name": "ix_ubicacion_geo"});

// ---- alertas: Resultado de evaluar reglas (definidas en PostgreSQL) sobre la telemetría.
asegurarColeccion('alertas', {
  "bsonType": "object",
  "required": [
    "_id",
    "id_regla",
    "tipo_regla",
    "id_sensor",
    "severidad",
    "estado",
    "activa",
    "fecha",
    "clave_dedup",
    "ocurrencias",
    "historial_estados"
  ],
  "properties": {
    "_id": {
      "bsonType": "string",
      "minLength": 1
    },
    "id_regla": {
      "bsonType": "string",
      "minLength": 1
    },
    "tipo_regla": {
      "enum": [
        "TEMPERATURA",
        "HUMEDAD",
        "VIBRACION",
        "BATERIA",
        "INACTIVIDAD",
        "ANOMALIA_TEMPERATURA"
      ]
    },
    "id_sensor": {
      "bsonType": "string",
      "minLength": 1
    },
    "id_contenedor": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_envio": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_cliente": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_lectura": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_evento": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_incidente": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "operador": {
      "enum": [
        ">",
        ">=",
        "<",
        "<="
      ]
    },
    "umbral": {
      "bsonType": [
        "number",
        "null"
      ]
    },
    "valor_observado": {
      "bsonType": [
        "number",
        "null"
      ]
    },
    "severidad": {
      "enum": [
        "BAJA",
        "MEDIA",
        "ALTA",
        "CRITICA"
      ]
    },
    "estado": {
      "enum": [
        "ABIERTA",
        "RECONOCIDA",
        "CERRADA"
      ]
    },
    "activa": {
      "bsonType": "bool"
    },
    "mensaje": {
      "bsonType": "string"
    },
    "detalle": {
      "bsonType": [
        "object",
        "null"
      ]
    },
    "fecha": {
      "bsonType": "date"
    },
    "fecha_lectura": {
      "bsonType": [
        "date",
        "null"
      ]
    },
    "ultima_ocurrencia": {
      "bsonType": "date"
    },
    "ocurrencias": {
      "bsonType": "int",
      "minimum": 1
    },
    "clave_dedup": {
      "bsonType": "string",
      "minLength": 1
    },
    "historial_estados": {
      "bsonType": "array",
      "minItems": 1,
      "items": {
        "bsonType": "object",
        "required": [
          "estado",
          "fecha"
        ],
        "properties": {
          "estado": {
            "enum": [
              "ABIERTA",
              "RECONOCIDA",
              "CERRADA"
            ]
          },
          "fecha": {
            "bsonType": "date"
          },
          "id_usuario": {
            "bsonType": [
              "string",
              "null"
            ]
          },
          "comentario": {
            "bsonType": [
              "string",
              "null"
            ]
          }
        }
      }
    }
  }
});
db.getCollection('alertas').createIndex({"clave_dedup": 1}, {"name": "ux_dedup_activa", "unique": true, "partialFilterExpression": {"activa": true}});
db.getCollection('alertas').createIndex({"id_sensor": 1, "fecha": -1}, {"name": "ix_sensor_fecha"});
db.getCollection('alertas').createIndex({"id_envio": 1, "fecha": -1}, {"name": "ix_envio_fecha"});
db.getCollection('alertas').createIndex({"estado": 1, "severidad": 1, "fecha": -1}, {"name": "ix_estado_severidad_fecha"});
db.getCollection('alertas').createIndex({"id_cliente": 1, "estado": 1, "fecha": -1}, {"name": "ix_cliente_estado_fecha"});

// ---- conversaciones: Contexto de mensajería privada o grupal (GrupoDeTrabajo) entre las partes.
asegurarColeccion('conversaciones', {
  "bsonType": "object",
  "required": [
    "_id",
    "tipo",
    "participantes",
    "ids_participantes",
    "creada_por",
    "fecha_creacion",
    "estado"
  ],
  "properties": {
    "_id": {
      "bsonType": "string",
      "minLength": 1
    },
    "tipo": {
      "enum": [
        "PRIVADA",
        "GRUPAL"
      ]
    },
    "asunto": {
      "bsonType": "string"
    },
    "nombre_grupo": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "id_envio": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "participantes": {
      "bsonType": "array",
      "minItems": 2,
      "items": {
        "bsonType": "object",
        "required": [
          "tipo",
          "id_entidad"
        ],
        "properties": {
          "tipo": {
            "enum": [
              "CLIENTE",
              "PROVEEDOR",
              "OPERADOR",
              "INTERNO"
            ]
          },
          "id_entidad": {
            "bsonType": "string",
            "minLength": 1
          },
          "id_usuario": {
            "bsonType": [
              "string",
              "null"
            ]
          },
          "nombre": {
            "bsonType": "string"
          }
        }
      }
    },
    "ids_participantes": {
      "bsonType": "array",
      "minItems": 2,
      "items": {
        "bsonType": "string",
        "minLength": 1
      }
    },
    "creada_por": {
      "bsonType": "string",
      "minLength": 1
    },
    "fecha_creacion": {
      "bsonType": "date"
    },
    "estado": {
      "enum": [
        "ABIERTA",
        "ARCHIVADA"
      ]
    },
    "cantidad_mensajes": {
      "bsonType": "int",
      "minimum": 0
    },
    "ultimo_mensaje": {
      "bsonType": [
        "object",
        "null"
      ],
      "properties": {
        "fecha": {
          "bsonType": "date"
        },
        "remitente": {
          "bsonType": "string"
        },
        "extracto": {
          "bsonType": "string"
        }
      }
    }
  }
});
db.getCollection('conversaciones').createIndex({"ids_participantes": 1, "ultimo_mensaje.fecha": -1}, {"name": "ix_participante_actividad"});
db.getCollection('conversaciones').createIndex({"id_envio": 1}, {"name": "ix_envio", "partialFilterExpression": {"id_envio": {"$type": "string"}}});

// ---- mensajes: Mensajes referenciados (no embebidos) para paginar y auditar sin límite de 16 MB.
asegurarColeccion('mensajes', {
  "bsonType": "object",
  "required": [
    "_id",
    "id_conversacion",
    "remitente",
    "fecha_envio",
    "contenido",
    "tipo",
    "estado"
  ],
  "properties": {
    "_id": {
      "bsonType": "string",
      "minLength": 1
    },
    "id_conversacion": {
      "bsonType": "string",
      "minLength": 1
    },
    "remitente": {
      "bsonType": "object",
      "required": [
        "tipo",
        "id_entidad"
      ],
      "properties": {
        "tipo": {
          "enum": [
            "CLIENTE",
            "PROVEEDOR",
            "OPERADOR",
            "INTERNO"
          ]
        },
        "id_entidad": {
          "bsonType": "string",
          "minLength": 1
        },
        "id_usuario": {
          "bsonType": [
            "string",
            "null"
          ]
        },
        "nombre": {
          "bsonType": "string"
        }
      }
    },
    "destinatario": {
      "bsonType": [
        "object",
        "null"
      ]
    },
    "fecha_envio": {
      "bsonType": "date"
    },
    "contenido": {
      "bsonType": "string",
      "minLength": 1,
      "maxLength": 4000
    },
    "tipo": {
      "enum": [
        "TEXTO",
        "ARCHIVO",
        "SISTEMA"
      ]
    },
    "estado": {
      "enum": [
        "ENVIADO",
        "LEIDO"
      ]
    },
    "leido_por": {
      "bsonType": "array",
      "items": {
        "bsonType": "string"
      }
    },
    "adjunto": {
      "bsonType": [
        "object",
        "null"
      ]
    }
  }
});
db.getCollection('mensajes').createIndex({"id_conversacion": 1, "fecha_envio": -1, "_id": -1}, {"name": "ix_conversacion_fecha"});

// ---- reportes_generados: Resultado (estructura variable) de reportes; la solicitud y el cobro viven en PostgreSQL.
asegurarColeccion('reportes_generados', {
  "bsonType": "object",
  "required": [
    "_id",
    "id_solicitud",
    "tipo",
    "parametros",
    "estado",
    "solicitado_por",
    "generado_en"
  ],
  "properties": {
    "_id": {
      "bsonType": "string",
      "minLength": 1
    },
    "id_solicitud": {
      "bsonType": "string",
      "minLength": 1
    },
    "id_cliente": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "solicitado_por": {
      "bsonType": "string",
      "minLength": 1
    },
    "tipo": {
      "bsonType": "string",
      "minLength": 1
    },
    "parametros": {
      "bsonType": "object"
    },
    "formato": {
      "enum": [
        "JSON",
        "CSV",
        "PDF"
      ]
    },
    "estado": {
      "enum": [
        "GENERADO",
        "ERROR"
      ]
    },
    "resultado": {
      "bsonType": [
        "object",
        "array",
        "null"
      ]
    },
    "ubicacion": {
      "bsonType": [
        "string",
        "null"
      ]
    },
    "generado_en": {
      "bsonType": "date"
    }
  }
});
db.getCollection('reportes_generados').createIndex({"id_solicitud": 1}, {"name": "ux_solicitud", "unique": true});
db.getCollection('reportes_generados').createIndex({"id_cliente": 1, "generado_en": -1}, {"name": "ix_cliente_fecha"});
db.getCollection('reportes_generados').createIndex({"tipo": 1, "generado_en": -1}, {"name": "ix_tipo_fecha"});

print('Esquema MongoDB aplicado.');
