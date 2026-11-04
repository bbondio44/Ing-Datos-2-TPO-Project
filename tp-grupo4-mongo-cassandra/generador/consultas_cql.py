"""Genera cassandra/03_consultas.cql: Q01–Q12 del drawio con UUID reales y resultado esperado."""

from __future__ import annotations

from datetime import timedelta

from catalogo import AHORA_REFERENCIA, Catalogo, t
from cql import fila_contenedor, fila_posicion, fila_sensor, fila_ultima_lectura, fila_ultima_posicion, ts
from ids import id_derivado
from telemetria import Lectura, ultima_por_sensor, ultima_posicion_por_contenedor, zscores


def generar(cat: Catalogo, lecturas: list[Lectura], tardia: Lectura) -> tuple[str, dict]:
    S = {c: s.id for c, s in cat.sensores.items()}
    C = {c: x.id for c, x in cat.contenedores.items()}
    todas = lecturas + [tardia]
    esperado: dict = {}

    # Q01: nueva lectura de SEN-002 a las 23:30 (dato de ejemplo de ingesta)
    s2 = cat.sensores["SEN-002"]
    c2 = cat.contenedores[s2.codigo_contenedor]
    f_nueva = t("2026-10-22T23:30:00")
    lat, lon = cat.posicion(c2.codigo, f_nueva)
    nueva = Lectura(id_derivado("lectura", "SEN-002", f_nueva.isoformat()), "SEN-002", s2.id, c2.codigo, c2.id,
                    f_nueva, temperatura=4.72, humedad=79.1, vibracion=0.41, latitud=round(lat, 5),
                    longitud=round(lon, 5), bateria=round(next(l.bateria for l in reversed(lecturas) if l.cod_sensor == "SEN-002") - 0.1, 1))
    todas_q01 = todas + [nueva]

    def cuenta(cod_sensor, dia, lista=todas_q01):
        return sum(1 for l in lista if l.cod_sensor == cod_sensor and l.dia == dia)

    o = []
    w = o.append
    w("-- =====================================================================")
    w("-- Cassandra · CONSULTAS Q01–Q12 (flujo del ModeladoCassandraGrupo4.drawio)")
    w("-- GENERADO por generador/generar.py con los UUID reales del dataset.")
    w("-- Uso: cqlsh -f cassandra/03_consultas.cql    (después de 01_schema y 02_datos_prueba)")
    w("-- Cada bloque indica el requisito de promoción que respalda y el RESULTADO ESPERADO.")
    w(f"-- Instante de referencia de la demo (\"ahora\"): {AHORA_REFERENCIA:%Y-%m-%d %H:%M} UTC")
    w("-- =====================================================================")
    w("")
    w("USE logistica_iot;")
    w("")
    w("-- Códigos legibles -> UUID (contrato común, ver catalogo_ids.csv)")
    for cod in ["SEN-001", "SEN-002", "SEN-003", "SEN-005", "SEN-007", "SEN-012", "SEN-025", "SEN-028", "SEN-040"]:
        w(f"--   {cod}      = {S[cod]}")
    for cod in ["MSKU0000001", "MSKU0000002", "MSKU0000003", "MSKU0000025"]:
        w(f"--   {cod}  = {C[cod]}")
    w("")

    # ------------------------------------------------------------ Q01
    w("-- =====================================================================")
    w("-- Q01 · Registrar lectura (escritura desnormalizada)                [R18]")
    w("-- Una lectura nueva se escribe en las 5 tablas. Se usa LOGGED BATCH porque las")
    w("-- filas van a particiones/tablas distintas y deben quedar todas o ninguna.")
    w("-- Las tablas 'ultima_*' usan USING TIMESTAMP = instante de la lectura.")
    w("-- =====================================================================")
    w("SELECT COUNT(*) AS antes FROM lecturas_por_sensor_dia")
    w(f"  WHERE id_sensor = {S['SEN-002']} AND dia = '2026-10-22';")
    antes = cuenta("SEN-002", "2026-10-22", todas)
    w(f"-- ESPERADO: antes = {antes}  ({antes + 1} si este script ya se corrió antes: la fila de prueba queda guardada)")
    w("")
    w("BEGIN BATCH")
    for f in (fila_sensor, fila_contenedor, fila_posicion, fila_ultima_lectura, fila_ultima_posicion):
        w("  " + f(nueva))
    w("APPLY BATCH;")
    w("")
    w("SELECT COUNT(*) AS despues FROM lecturas_por_sensor_dia")
    w(f"  WHERE id_sensor = {S['SEN-002']} AND dia = '2026-10-22';")
    w(f"-- ESPERADO: despues = {antes + 1}")
    w("")
    w("-- R18 · Duplicados: se reenvía EXACTAMENTE la misma lectura (mismo id_lectura e instante).")
    w("-- En Cassandra el INSERT es un upsert sobre la misma clave primaria: no se crea otra fila.")
    w("BEGIN BATCH")
    for f in (fila_sensor, fila_contenedor, fila_posicion, fila_ultima_lectura, fila_ultima_posicion):
        w("  " + f(nueva))
    w("APPLY BATCH;")
    w("SELECT COUNT(*) AS despues_de_reenvio FROM lecturas_por_sensor_dia")
    w(f"  WHERE id_sensor = {S['SEN-002']} AND dia = '2026-10-22';")
    w(f"-- ESPERADO: despues_de_reenvio = {antes + 1}  (sin duplicar)")
    esperado["Q01"] = {"antes": antes, "despues": antes + 1}
    w("")

    # ------------------------------------------------------------ Q02
    desde, hasta = t("2026-10-20T18:00:00"), t("2026-10-21T06:00:00")
    q02 = sorted([l for l in todas_q01 if l.cod_sensor == "SEN-003" and desde <= l.fecha_hora <= hasta],
                 key=lambda l: l.fecha_hora)
    w("-- =====================================================================")
    w("-- Q02 · Historial de un sensor por período que CRUZA LA MEDIANOCHE    [R19]")
    w("-- La partición es (id_sensor, dia): se consultan las 2 particiones con IN y")
    w("-- la aplicación fusiona/ordena el resultado (sin ALLOW FILTERING).")
    w("-- =====================================================================")
    w("SELECT dia, fecha_hora, temperatura, humedad, bateria FROM lecturas_por_sensor_dia")
    w(f"  WHERE id_sensor = {S['SEN-003']}")
    w("    AND dia IN ('2026-10-20', '2026-10-21')")
    w(f"    AND fecha_hora >= {ts(desde)} AND fecha_hora <= {ts(hasta)};")
    w(f"-- ESPERADO: {len(q02)} filas (18:00 del 20/10 a 06:00 del 21/10, una por hora).")
    w(f"--   Primera: {q02[0].fecha_hora:%Y-%m-%d %H:%M} temp {q02[0].temperatura} · Última: {q02[-1].fecha_hora:%Y-%m-%d %H:%M} temp {q02[-1].temperatura}")
    esperado["Q02"] = len(q02)
    w("")

    # ------------------------------------------------------------ Q03
    q03 = sorted([l for l in todas_q01 if l.cod_sensor == "SEN-003" and l.dia == "2026-10-22"],
                 key=lambda l: l.fecha_hora, reverse=True)[:5]
    w("-- =====================================================================")
    w("-- Q03 · Últimas N lecturas de un sensor (aprovecha fecha_hora DESC)     [R19, R22]")
    w("-- =====================================================================")
    w("SELECT fecha_hora, temperatura, humedad, vibracion, bateria FROM lecturas_por_sensor_dia")
    w(f"  WHERE id_sensor = {S['SEN-003']} AND dia = '2026-10-22'")
    w("  LIMIT 5;")
    w("-- ESPERADO: 5 filas, de la más nueva a la más vieja: " +
      ", ".join(f"{l.fecha_hora:%H:%M}" for l in q03))
    esperado["Q03"] = [l.fecha_hora.strftime("%H:%M") for l in q03]
    w("")

    # ------------------------------------------------------------ Q04
    d4, h4 = t("2026-10-21T14:00:00"), t("2026-10-21T17:00:00")
    q04 = [l for l in todas_q01 if l.cod_contenedor == "MSKU0000003" and d4 <= l.fecha_hora <= h4]
    w("-- =====================================================================")
    w("-- Q04 · Mediciones conjuntas de todos los sensores de un contenedor     [R17, R19]")
    w("-- Contenedor REEFER MSKU0000003 (envío E002) durante la excursión de temperatura.")
    w("-- =====================================================================")
    w("SELECT fecha_hora, id_sensor, temperatura, humedad FROM lecturas_por_contenedor_dia")
    w(f"  WHERE id_contenedor = {C['MSKU0000003']} AND dia = '2026-10-21'")
    w(f"    AND fecha_hora >= {ts(d4)} AND fecha_hora <= {ts(h4)};")
    w(f"-- ESPERADO: {len(q04)} filas (2 sensores x 4 horas). SEN-003 marca 10.8-11.6 °C mientras")
    w("--   SEN-028 (mismo contenedor) se mantiene en ~4-5 °C: útil para distinguir falla de sensor.")
    esperado["Q04"] = len(q04)
    w("")

    # ------------------------------------------------------------ Q05 + C03
    ult = ultima_por_sensor(todas_q01)
    u1 = ult["SEN-001"]
    w("-- =====================================================================")
    w("-- Q05 · Último estado de un sensor + prueba de LECTURA TARDÍA (C03)    [R22, R18]")
    w("-- SEN-001 envió al final una lectura de las 05:30 (ver 02_datos_prueba.cql). La fila")
    w("-- conserva la de las 23:00 porque su WRITETIME (= instante del evento) es mayor.")
    w("-- =====================================================================")
    w("SELECT id_sensor, fecha_hora, temperatura, bateria, latitud, longitud, WRITETIME(temperatura) AS wt")
    w(f"  FROM ultima_lectura_por_sensor WHERE id_sensor = {S['SEN-001']};")
    w(f"-- ESPERADO: fecha_hora = {u1.fecha_hora:%Y-%m-%d %H:%M}, temperatura = {u1.temperatura}, "
      f"wt = {int(u1.fecha_hora.timestamp() * 1_000_000)}")
    w("")
    w("-- La lectura tardía SÍ está en el historial:")
    w("SELECT fecha_hora, temperatura FROM lecturas_por_sensor_dia")
    w(f"  WHERE id_sensor = {S['SEN-001']} AND dia = '2026-10-22' AND fecha_hora = {ts(tardia.fecha_hora)};")
    w(f"-- ESPERADO: 1 fila (05:30, temperatura {tardia.temperatura})")
    esperado["Q05"] = {"fecha_hora": u1.fecha_hora.strftime("%Y-%m-%d %H:%M"), "temperatura": u1.temperatura}
    w("")

    # ------------------------------------------------------------ Q06
    w("-- =====================================================================")
    w("-- Q06 · Estado de los sensores de un contenedor                        [R22, R23]")
    w("-- La lista de sensores del contenedor sale de PostgreSQL (asignación vigente);")
    w("-- MSKU0000025 tiene SEN-025 y SEN-040.")
    w("-- =====================================================================")
    w("SELECT id_sensor, fecha_hora, bateria, temperatura FROM ultima_lectura_por_sensor")
    w(f"  WHERE id_sensor IN ({S['SEN-025']}, {S['SEN-040']});")
    w(f"-- ESPERADO: 1 fila (SEN-025, {ult['SEN-025'].fecha_hora:%Y-%m-%d %H:%M}). SEN-040 no devuelve fila")
    w("--   -> la aplicación lo informa como SIN_DATOS (nunca reportó).")
    w("")

    # ------------------------------------------------------------ Q07
    q07 = sorted([l for l in todas_q01 if l.cod_contenedor == "MSKU0000001" and l.tiene_gps], key=lambda l: l.fecha_hora)
    q07_22 = [l for l in q07 if l.dia == "2026-10-22"]
    w("-- =====================================================================")
    w("-- Q07 · Recorrido GPS histórico de un contenedor                       [R20, R21]")
    w("-- Reconstrucción del recorrido: una consulta por día, en orden cronológico")
    w("-- (ORDER BY ASC invierte el clustering DESC dentro de la partición).")
    w("-- =====================================================================")
    for dia in ("2026-10-20", "2026-10-21", "2026-10-22"):
        n = sum(1 for l in q07 if l.dia == dia)
        w("SELECT fecha_hora, latitud, longitud FROM posiciones_por_contenedor_dia")
        w(f"  WHERE id_contenedor = {C['MSKU0000001']} AND dia = '{dia}'")
        w("  ORDER BY fecha_hora ASC;")
        w(f"-- ESPERADO: {n} posiciones")
    w(f"-- TOTAL ESPERADO: {len(q07)} posiciones. El 22/10 hay {len(q07_22)}: falta la de las 06:00 (sin fix GPS)")
    w("--   y aparece la de las 05:30 (lectura tardía). Primer punto: "
      f"({q07[0].latitud}, {q07[0].longitud}) · último: ({q07[-1].latitud}, {q07[-1].longitud}).")
    w("-- Puntos en orden = polilínea para el mapa (Puerto de Buenos Aires -> Puerto de Santos).")
    esperado["Q07"] = {"total": len(q07), "dia_22": len(q07_22)}
    w("")

    # ------------------------------------------------------------ Q08
    up = ultima_posicion_por_contenedor(todas_q01)
    w("-- =====================================================================")
    w("-- Q08 · Última posición GPS (mapa de seguimiento)                      [R21]")
    w("-- =====================================================================")
    w("SELECT id_contenedor, fecha_hora, latitud, longitud FROM ultima_posicion_por_contenedor")
    w(f"  WHERE id_contenedor IN ({C['MSKU0000001']}, {C['MSKU0000002']});")
    for cod in ("MSKU0000001", "MSKU0000002"):
        p = up[cod]
        w(f"-- ESPERADO {cod}: {p.fecha_hora:%Y-%m-%d %H:%M} ({p.latitud}, {p.longitud})")
    esperado["Q08"] = {c: [up[c].latitud, up[c].longitud] for c in ("MSKU0000001", "MSKU0000002")}
    w("")

    # ------------------------------------------------------------ Q09
    q09 = [l for l in todas_q01 if l.cod_sensor == "SEN-003" and l.dia == "2026-10-21" and l.temperatura > 8]
    w("-- =====================================================================")
    w("-- Q09 · Lecturas que superan un umbral (regla REG-01: temperatura > 8)  [R09, R26, R32]")
    w("-- ALLOW FILTERING está acotado a UNA partición (24 filas): costo controlado.")
    w("-- La evaluación en línea la hace la aplicación al ingerir; esta consulta audita.")
    w("-- =====================================================================")
    w("SELECT fecha_hora, temperatura FROM lecturas_por_sensor_dia")
    w(f"  WHERE id_sensor = {S['SEN-003']} AND dia = '2026-10-21' AND temperatura > 8.0")
    w("  ALLOW FILTERING;")
    w(f"-- ESPERADO: {len(q09)} filas: " + ", ".join(f"{l.fecha_hora:%H:%M}={l.temperatura}" for l in q09))
    w("--   (coincide con la alerta REG-01 en MongoDB: ocurrencias = 4)")
    esperado["Q09"] = len(q09)
    w("")

    # ------------------------------------------------------------ Q10
    q10 = [l for l in todas_q01 if l.cod_sensor == "SEN-012" and l.dia == "2026-10-21" and l.bateria < 15]
    q10_total = [l for l in todas_q01 if l.cod_sensor == "SEN-012" and l.bateria < 15]
    w("-- =====================================================================")
    w("-- Q10 · Batería: estado actual y lecturas bajo el umbral (REG-03 < 15 %)  [R22, R23]")
    w("-- =====================================================================")
    w("SELECT id_sensor, fecha_hora, bateria FROM ultima_lectura_por_sensor")
    w(f"  WHERE id_sensor = {S['SEN-012']};")
    w(f"-- ESPERADO: bateria = {ult['SEN-012'].bateria} ({ult['SEN-012'].fecha_hora:%Y-%m-%d %H:%M})")
    for dia in ("2026-10-21", "2026-10-22"):
        n = sum(1 for l in q10_total if l.dia == dia)
        w("SELECT fecha_hora, bateria FROM lecturas_por_sensor_dia")
        w(f"  WHERE id_sensor = {S['SEN-012']} AND dia = '{dia}' AND bateria < 15.0")
        w("  ALLOW FILTERING;")
        w(f"-- ESPERADO: {n} filas" + (f" (desde las {q10[0].fecha_hora:%H:%M}, batería {q10[0].bateria})" if dia == '2026-10-21' else ""))
    w(f"--   Total {len(q10_total)} = ocurrencias de la alerta REG-03 en MongoDB.")
    esperado["Q10"] = len(q10_total)
    w("")

    # ------------------------------------------------------------ Q11
    limite = AHORA_REFERENCIA - timedelta(minutes=120)
    inactivos = sorted(c for c, l in ult.items() if l.fecha_hora < limite)
    sin_datos = sorted(set(cat.sensores) - set(ult))
    w("-- =====================================================================")
    w("-- Q11 · Detección de sensores inactivos (proceso periódico)            [R22, R23]")
    w("-- La aplicación trae la última lectura de los sensores vigentes (lista desde")
    w("-- PostgreSQL) y compara fecha_hora con ahora - 120 min (regla REG-04).")
    w("-- Con IN sobre la partition key se evita recorrer toda la tabla.")
    w("-- =====================================================================")
    ids_in = ",\n    ".join(f"{S[c]}" for c in sorted(cat.sensores))
    w("SELECT id_sensor, id_contenedor, fecha_hora, bateria FROM ultima_lectura_por_sensor")
    w(f"  WHERE id_sensor IN (\n    {ids_in}\n  );")
    w(f"-- ESPERADO: {len(ult)} filas (SEN-040 no aparece -> SIN_DATOS).")
    w(f"--   Con ahora = {AHORA_REFERENCIA:%Y-%m-%d %H:%M}, límite = {limite:%Y-%m-%d %H:%M}:")
    w(f"--   INACTIVOS: {', '.join(inactivos)} (última lectura {ult['SEN-007'].fecha_hora:%Y-%m-%d %H:%M})")
    w(f"--   SIN_DATOS: {', '.join(sin_datos)}")
    esperado["Q11"] = {"filas": len(ult), "inactivos": inactivos, "sin_datos": sin_datos}
    w("")

    # ------------------------------------------------------------ Q12
    w("-- =====================================================================")
    w("-- Q12 · Tendencias / estadísticas por sensor y día (base de R32)        [R32, R22]")
    w("-- Agregaciones dentro de UNA partición. El z-score (|z| > 3) lo calcula la")
    w("-- aplicación con estos valores; el resultado se guarda como alerta en MongoDB.")
    w("-- =====================================================================")
    stats = {}
    for dia in ("2026-10-20", "2026-10-21", "2026-10-22"):
        vals = [l.temperatura for l in todas_q01 if l.cod_sensor == "SEN-005" and l.dia == dia]
        stats[dia] = (len(vals), min(vals), max(vals), sum(vals) / len(vals))
        w("SELECT COUNT(*) AS lecturas, MIN(temperatura) AS minima, MAX(temperatura) AS maxima, AVG(temperatura) AS promedio")
        w(f"  FROM lecturas_por_sensor_dia WHERE id_sensor = {S['SEN-005']} AND dia = '{dia}';")
        n, mn, mx, av = stats[dia]
        w(f"-- ESPERADO {dia}: lecturas={n}, minima={mn}, maxima={mx}, promedio≈{av:.2f}")
    zs = zscores(todas, "SEN-005")
    peor = max(zs, key=lambda x: abs(x[1]))
    w(f"--   La máxima del 22/10 ({peor[0].temperatura} °C a las {peor[0].fecha_hora:%H:%M}) da z = {peor[1]:.2f} > 3:")
    w("--   anomalía aunque NO supere el umbral fijo de 8 °C (por eso se usan ambos métodos).")
    w("SELECT fecha_hora, temperatura FROM lecturas_por_sensor_dia")
    w(f"  WHERE id_sensor = {S['SEN-005']} AND dia = '2026-10-22' AND fecha_hora = {ts(peor[0].fecha_hora)};")
    esperado["Q12"] = {"z": round(peor[1], 2), "dia_22_max": stats["2026-10-22"][2]}
    w("")
    return "\n".join(o) + "\n", esperado
