"""Generación de 02_datos_prueba.cql a partir de la telemetría simulada."""

from __future__ import annotations

from collections import defaultdict
from datetime import datetime, timezone

from telemetria import Lectura, micros, ultima_por_sensor, ultima_posicion_por_contenedor

FILAS_POR_BATCH = 12   # filas de UNA misma partición por batch (liviano, sin warnings de tamaño)
MEDICIONES = ["temperatura", "humedad", "vibracion", "latitud", "longitud", "bateria"]


def ts(dt: datetime) -> str:
    return "'" + dt.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M:%S.000+0000") + "'"


def num(v: float) -> str:
    return repr(float(v))


def _insert(tabla: str, columnas: dict, usando_timestamp: int | None = None) -> str:
    # Las columnas en None NO se escriben: escribir null crearía tombstones innecesarios.
    cols = {k: v for k, v in columnas.items() if v is not None}
    sufijo = f" USING TIMESTAMP {usando_timestamp}" if usando_timestamp is not None else ""
    return f"INSERT INTO {tabla} ({', '.join(cols)}) VALUES ({', '.join(cols.values())}){sufijo};"


def _medidas(l: Lectura, campos=MEDICIONES) -> dict:
    return {c: (num(getattr(l, c)) if getattr(l, c) is not None else None) for c in campos}


def fila_sensor(l: Lectura) -> str:
    return _insert("lecturas_por_sensor_dia", {
        "id_sensor": l.id_sensor, "dia": f"'{l.dia}'", "fecha_hora": ts(l.fecha_hora),
        "id_lectura": l.id_lectura, "id_contenedor": l.id_contenedor, **_medidas(l)})


def fila_contenedor(l: Lectura) -> str:
    return _insert("lecturas_por_contenedor_dia", {
        "id_contenedor": l.id_contenedor, "dia": f"'{l.dia}'", "fecha_hora": ts(l.fecha_hora),
        "id_sensor": l.id_sensor, "id_lectura": l.id_lectura, **_medidas(l)})


def fila_posicion(l: Lectura) -> str:
    return _insert("posiciones_por_contenedor_dia", {
        "id_contenedor": l.id_contenedor, "dia": f"'{l.dia}'", "fecha_hora": ts(l.fecha_hora),
        "id_sensor": l.id_sensor, "id_lectura": l.id_lectura,
        "latitud": num(l.latitud), "longitud": num(l.longitud)})


def fila_ultima_lectura(l: Lectura) -> str:
    return _insert("ultima_lectura_por_sensor", {
        "id_sensor": l.id_sensor, "id_contenedor": l.id_contenedor, "id_lectura": l.id_lectura,
        "fecha_hora": ts(l.fecha_hora), **_medidas(l)}, usando_timestamp=micros(l.fecha_hora))


def fila_ultima_posicion(l: Lectura) -> str:
    return _insert("ultima_posicion_por_contenedor", {
        "id_contenedor": l.id_contenedor, "id_sensor": l.id_sensor, "id_lectura": l.id_lectura,
        "fecha_hora": ts(l.fecha_hora), "latitud": num(l.latitud), "longitud": num(l.longitud)},
        usando_timestamp=micros(l.fecha_hora))


def _por_particion(lecturas, clave, generar) -> list[str]:
    grupos: dict[tuple, list[Lectura]] = defaultdict(list)
    for l in lecturas:
        grupos[clave(l)].append(l)
    salida = []
    for k in sorted(grupos):
        filas = sorted(grupos[k], key=lambda l: (l.fecha_hora, l.id_sensor))
        for i in range(0, len(filas), FILAS_POR_BATCH):
            salida.append("BEGIN UNLOGGED BATCH")
            salida += ["  " + generar(l) for l in filas[i:i + FILAS_POR_BATCH]]
            salida.append("APPLY BATCH;")
    return salida


def datos_prueba(lecturas: list[Lectura], tardia: Lectura, nombres: dict[str, str]) -> str:
    ult_s = ultima_por_sensor(lecturas)
    ult_p = ultima_posicion_por_contenedor(lecturas)
    gps = [l for l in lecturas if l.tiene_gps]
    out = [
        "-- =====================================================================",
        "-- Cassandra · DATOS DE PRUEBA (GENERADO por generador/generar.py, no editar a mano)",
        "-- Uso: cqlsh -f cassandra/02_datos_prueba.cql      (después de 01_schema.cql)",
        "-- Re-ejecutable: los INSERT de Cassandra son upserts sobre la misma PK -> no duplica.",
        "--",
        f"-- Lecturas: {len(lecturas) + 1} (incluye 1 lectura tardía) · sensores con datos: {len(ult_s)} de 40",
        f"-- Posiciones GPS: {len(gps) + 1} · contenedores con última posición: {len(ult_p)}",
        "-- Ventana: 2026-10-20 00:00 a 2026-10-22 23:00 UTC, una lectura por hora por sensor.",
        "-- Casos límite incluidos (ver docs/DATOS_DE_PRUEBA.md):",
        "--   SEN-003 temperatura > 8 °C (4 h) · SEN-005 pico anómalo · SEN-007 deja de reportar",
        "--   SEN-012 batería < 15 % · SEN-020 dos golpes de vibración · SEN-001 lectura sin GPS",
        "--   SEN-001 lectura tardía (al final) · SEN-040 nunca reportó",
        "-- =====================================================================",
        "",
        "USE logistica_iot;",
        "",
        "-- ---------------------------------------------------------------- T1 lecturas_por_sensor_dia",
    ]
    out += _por_particion(lecturas, lambda l: (l.cod_sensor, l.dia), fila_sensor)
    out += ["", "-- ---------------------------------------------------------------- T2 lecturas_por_contenedor_dia"]
    out += _por_particion(lecturas, lambda l: (l.cod_contenedor, l.dia), fila_contenedor)
    out += ["", "-- ---------------------------------------------------------------- T3 posiciones_por_contenedor_dia",
            "-- Solo lecturas con GPS válido (la de SEN-001 del 2026-10-22 06:00 no tiene fix y no se escribe)."]
    out += _por_particion(gps, lambda l: (l.cod_contenedor, l.dia), fila_posicion)
    out += ["", "-- ---------------------------------------------------------------- T4 ultima_lectura_por_sensor",
            "-- USING TIMESTAMP = fecha_hora de la lectura en microsegundos (last-write-wins por tiempo de evento).",
            "-- SEN-040 no tiene fila: nunca reportó (la aplicación lo informa como SIN_DATOS)."]
    for cod in sorted(ult_s):
        out.append(f"-- {cod}")
        out.append(fila_ultima_lectura(ult_s[cod]))
    out += ["", "-- ---------------------------------------------------------------- T5 ultima_posicion_por_contenedor"]
    for cod in sorted(ult_p):
        out.append(f"-- {cod}")
        out.append(fila_ultima_posicion(ult_p[cod]))
    out += [
        "",
        "-- ---------------------------------------------------------------- LECTURA TARDÍA (caso C03)",
        f"-- {nombres[tardia.cod_sensor]} envía AHORA una lectura de las {tardia.fecha_hora:%Y-%m-%d %H:%M} UTC,",
        "-- después de haber enviado la de las 23:00. Se guarda en el historial (es un dato válido),",
        "-- pero al escribir las tablas 'ultima_*' con USING TIMESTAMP menor, Cassandra conserva la de las 23:00.",
        fila_sensor(tardia),
        fila_contenedor(tardia),
        fila_posicion(tardia),
        fila_ultima_lectura(tardia),
        fila_ultima_posicion(tardia),
        "",
    ]
    return "\n".join(out) + "\n"
