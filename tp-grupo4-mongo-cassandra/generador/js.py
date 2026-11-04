"""Conversión de valores Python a literales de mongosh (ISODate, NumberInt, etc.)."""

from __future__ import annotations

import json
from datetime import date, datetime, timezone

# Campos que el validador exige como int32: se emiten con NumberInt para que mongosh y
# Spring Data (Integer) los guarden/lean como int y no como double.
CAMPOS_INT = {"ocurrencias", "cantidad_mensajes"}


def iso(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.000Z")


def js(valor, clave: str | None = None, sangria: int = 0, compacto: bool = False) -> str:
    pad = "" if compacto else "  " * (sangria + 1)
    fin = "" if compacto else "  " * sangria
    sep = ", " if compacto else ",\n"
    nl = "" if compacto else "\n"
    if valor is None:
        return "null"
    if isinstance(valor, bool):
        return "true" if valor else "false"
    if isinstance(valor, datetime):
        return f'ISODate("{iso(valor)}")'
    if isinstance(valor, date):
        return json.dumps(valor.isoformat())
    if isinstance(valor, int):
        return f"NumberInt({valor})" if clave in CAMPOS_INT else str(valor)
    if isinstance(valor, float):
        return repr(valor)
    if isinstance(valor, str):
        return json.dumps(valor, ensure_ascii=False)
    if isinstance(valor, (list, tuple)):
        if not valor:
            return "[]"
        if all(not isinstance(v, (dict, list)) for v in valor):
            return "[" + ", ".join(js(v, compacto=True) for v in valor) + "]"
        return "[" + nl + sep.join(pad + js(v, sangria=sangria + 1, compacto=compacto) for v in valor) + nl + fin + "]"
    if isinstance(valor, dict):
        if not valor:
            return "{}"
        partes = []
        for k, v in valor.items():
            k_js = k if k.isidentifier() else json.dumps(k)
            partes.append(f"{pad}{k_js}: {js(v, clave=k, sangria=sangria + 1, compacto=compacto)}")
        return "{" + nl + sep.join(partes) + nl + fin + "}"
    raise TypeError(f"No sé convertir {type(valor)} a JS")


def jsc(valor) -> str:
    """Versión compacta (una línea)."""
    return js(valor, compacto=True)
