"""Telemetría IoT simulada (Cassandra) con los casos límite que pide el Plan Maestro."""

from __future__ import annotations

import math
import random
from dataclasses import dataclass
from datetime import datetime, timedelta

from catalogo import FIN_TELEMETRIA, INICIO_TELEMETRIA, PASO, Catalogo, t
from ids import id_derivado

# ------------------------------------------------------------------ casos límite (documentados)
SENSOR_EXCURSION = "SEN-003"          # REEFER, envío E002: temperatura > 8 °C durante 4 horas
EXCURSION = {t("2026-10-21T14:00:00"): 10.8, t("2026-10-21T15:00:00"): 11.4,
             t("2026-10-21T16:00:00"): 11.6, t("2026-10-21T17:00:00"): 10.9}
SENSOR_PICO = "SEN-005"               # REEFER: pico aislado 7.9 °C (no supera 8 °C pero |z| > 3)
PICO = (t("2026-10-22T09:00:00"), 7.9)
SENSOR_INACTIVO = "SEN-007"           # deja de reportar después de esta hora
ULTIMA_DE_INACTIVO = t("2026-10-21T10:00:00")
SENSOR_BATERIA = "SEN-012"            # batería se agota por debajo del 15 %
SENSOR_VIBRACION = "SEN-020"          # dos golpes: el primero se cierra, el segundo reabre alerta
GOLPES = {t("2026-10-20T13:00:00"): 3.4, t("2026-10-22T15:00:00"): 3.1}
SENSOR_SIN_GPS = "SEN-001"            # una lectura sin fix GPS
SIN_GPS = t("2026-10-22T06:00:00")
LECTURA_TARDIA = ("SEN-001", t("2026-10-22T05:30:00"))   # llega al final de la carga
SENSOR_NUNCA = "SEN-040"              # instalado, nunca reportó


@dataclass
class Lectura:
    id_lectura: str
    cod_sensor: str
    id_sensor: str
    cod_contenedor: str
    id_contenedor: str
    fecha_hora: datetime
    temperatura: float | None = None
    humedad: float | None = None
    vibracion: float | None = None
    latitud: float | None = None
    longitud: float | None = None
    bateria: float | None = None

    @property
    def dia(self) -> str:
        return self.fecha_hora.strftime("%Y-%m-%d")

    @property
    def tiene_gps(self) -> bool:
        return self.latitud is not None and self.longitud is not None


def _instantes():
    instante = INICIO_TELEMETRIA
    while instante <= FIN_TELEMETRIA:
        yield instante
        instante += PASO


def generar_lecturas(cat: Catalogo) -> tuple[list[Lectura], Lectura]:
    """Devuelve (lecturas en orden de llegada normal, lectura tardía que llega al final)."""
    lecturas: list[Lectura] = []
    for cod, sensor in cat.sensores.items():
        if cod == SENSOR_NUNCA:
            continue
        rnd = random.Random(f"grupo4-{cod}")
        cont = cat.contenedores[sensor.codigo_contenedor]
        reefer = cont.tipo == "REEFER"
        base_temp = rnd.uniform(4.0, 5.0) if reefer else rnd.uniform(16.0, 20.0)
        bateria = 24.0 if cod == SENSOR_BATERIA else rnd.uniform(88.0, 100.0)
        consumo = 0.22 if cod == SENSOR_BATERIA else 0.04
        for instante in _instantes():
            if cod == SENSOR_INACTIVO and instante > ULTIMA_DE_INACTIVO:
                break
            horas = (instante - INICIO_TELEMETRIA).total_seconds() / 3600
            if reefer:
                temp = base_temp + rnd.uniform(-0.6, 0.6)
            else:
                temp = base_temp + 4.0 * math.sin((instante.hour - 9) / 24 * 2 * math.pi) + rnd.uniform(-0.8, 0.8)
            if cod == SENSOR_EXCURSION and instante in EXCURSION:
                temp = EXCURSION[instante]
            if cod == SENSOR_PICO and instante == PICO[0]:
                temp = PICO[1]
            lec = Lectura(
                id_lectura=id_derivado("lectura", cod, instante.isoformat()),
                cod_sensor=cod, id_sensor=sensor.id,
                cod_contenedor=cont.codigo, id_contenedor=cont.id,
                fecha_hora=instante,
                temperatura=round(temp, 2),
                humedad=round(rnd.uniform(70.0, 85.0) if reefer else rnd.uniform(45.0, 70.0), 1),
                bateria=round(max(bateria - consumo * horas, 1.0), 1),
            )
            if sensor.tipo == "MULTIPARAMETRO":
                lec.vibracion = round(rnd.uniform(0.15, 0.9), 2)
                if cod == SENSOR_VIBRACION and instante in GOLPES:
                    lec.vibracion = GOLPES[instante]
                if not (cod == SENSOR_SIN_GPS and instante == SIN_GPS):
                    lat, lon = cat.posicion(cont.codigo, instante)
                    lec.latitud = round(lat + rnd.uniform(-0.002, 0.002), 5)
                    lec.longitud = round(lon + rnd.uniform(-0.002, 0.002), 5)
            lecturas.append(lec)

    # Lectura tardía: pertenece a las 05:30 del 22/10 pero llega después de la de las 23:00.
    cod, instante = LECTURA_TARDIA
    sensor = cat.sensores[cod]
    cont = cat.contenedores[sensor.codigo_contenedor]
    lat, lon = cat.posicion(cont.codigo, instante)
    tardia = Lectura(id_derivado("lectura", cod, instante.isoformat()), cod, sensor.id, cont.codigo, cont.id,
                     instante, temperatura=4.61, humedad=78.2, vibracion=0.33,
                     latitud=round(lat, 5), longitud=round(lon, 5), bateria=95.9)
    return lecturas, tardia


def ultima_por_sensor(lecturas: list[Lectura]) -> dict[str, Lectura]:
    ultima: dict[str, Lectura] = {}
    for lec in lecturas:
        if lec.cod_sensor not in ultima or lec.fecha_hora > ultima[lec.cod_sensor].fecha_hora:
            ultima[lec.cod_sensor] = lec
    return ultima


def ultima_posicion_por_contenedor(lecturas: list[Lectura]) -> dict[str, Lectura]:
    ultima: dict[str, Lectura] = {}
    for lec in lecturas:
        if not lec.tiene_gps:
            continue
        actual = ultima.get(lec.cod_contenedor)
        if actual is None or lec.fecha_hora > actual.fecha_hora:
            ultima[lec.cod_contenedor] = lec
    return ultima


def zscores(lecturas: list[Lectura], cod_sensor: str) -> list[tuple[Lectura, float]]:
    serie = [l for l in lecturas if l.cod_sensor == cod_sensor and l.temperatura is not None]
    valores = [l.temperatura for l in serie]
    media = sum(valores) / len(valores)
    desvio = math.sqrt(sum((v - media) ** 2 for v in valores) / (len(valores) - 1))
    return [(l, (l.temperatura - media) / desvio) for l in serie]


def anomalias(lecturas: list[Lectura], limite: float = 3.0) -> dict[str, list[tuple[Lectura, float]]]:
    resultado = {}
    for cod in sorted({l.cod_sensor for l in lecturas}):
        fuera = [(l, z) for l, z in zscores(lecturas, cod) if abs(z) > limite]
        if fuera:
            resultado[cod] = fuera
    return resultado


def micros(dt: datetime) -> int:
    return int(dt.timestamp() * 1_000_000)


def sumar_horas(dt: datetime, horas: float) -> datetime:
    return dt + timedelta(hours=horas)
