"""Identificadores (decisión D01 del Plan Maestro: UUID lógico compartido entre motores).

- Entidades maestras (clientes, envíos, contenedores, sensores, ...) nacen en PostgreSQL.
  Para el dataset de prueba se generan con uuid5 sobre un namespace fijo, así Persona 1
  (PostgreSQL) y Persona 3 (Neo4j) obtienen exactamente los mismos UUID a partir del
  código legible (ej. "contenedor:MSKU0000001").
- En Cassandra se guardan como tipo uuid; en MongoDB y Neo4j como String.
"""

from __future__ import annotations

import uuid

NAMESPACE_GRUPO4 = uuid.UUID("6f1c2b0e-4d1a-5b7e-9c3f-0a4d2e8b1c74")


def id_estable(tipo: str, codigo: str) -> str:
    """UUID determinístico para el dataset compartido. Ej: id_estable('sensor', 'SEN-001')."""
    return str(uuid.uuid5(NAMESPACE_GRUPO4, f"{tipo}:{codigo}"))


def nuevo_id() -> str:
    return str(uuid.uuid4())


def id_derivado(*partes: str) -> str:
    """UUID determinístico a partir de una clave natural (idempotencia, decisión D04)."""
    return str(uuid.uuid5(NAMESPACE_GRUPO4, "|".join(partes)))


def es_uuid(valor) -> bool:
    try:
        uuid.UUID(str(valor))
        return True
    except (ValueError, TypeError, AttributeError):
        return False
