# Datos de prueba

Dataset sintético y **determinista** (siempre los mismos valores y UUID). “Ahora” de referencia de la
demo: **2026-10-22 23:30 UTC**. Todas las referencias están en `catalogo_ids.csv`.

## Volumen

| Conjunto | Cantidad |
|---|---|
| Clientes / proveedores / operadores | 8 / 8 / 5 |
| Envíos | 20 (2 tramos cada uno: terrestre + terrestre, marítimo o aéreo; 15 entregados, 5 en tránsito) |
| Contenedores | 25 (8 REEFER) |
| Sensores | 40: 25 multiparámetro con GPS, 14 de temperatura/humedad sin GPS, 1 que nunca reportó |
| Lecturas IoT (Cassandra) | 2.772: una por hora por sensor del 20/10 00:00 al 22/10 23:00 + 1 tardía |
| Posiciones GPS | 1.763 |
| Reglas (PostgreSQL `regla_alerta`) | 7: REG-01 temp > 8, REG-02 temp < 2, REG-03 batería < 15, REG-04 inactividad > 120 min, REG-05 vibración > 2.5 g, REG-06 humedad > 90, REG-07 \|z\| > 3 |
| MongoDB | 173 eventos, 8 alertas, 4 conversaciones, 19 mensajes, 3 reportes |
| Incidentes (para PostgreSQL) | 5, en `incidentes_para_postgres.csv` |

## Casos límite sembrados (y qué debería pasar)

| Caso | Dónde | Resultado esperado |
|---|---|---|
| Excursión de frío | SEN-003, contenedor REEFER MSKU0000003 (E002), 21/10 14:00–17:00: 10.8–11.6 °C | 4 lecturas > 8 °C (Q09) → **1** alerta REG-01 con `ocurrencias = 4`, RECONOCIDA, incidente ALTA |
| Mismo contenedor, otro sensor normal | SEN-028 en MSKU0000003 | ~4–5 °C en la misma ventana (Q04): permite distinguir falla de sensor vs. de equipo |
| Pico anómalo bajo el umbral | SEN-005, 22/10 09:00: 7.9 °C | No dispara REG-01 (< 8) pero z = 6.73 → alerta ANOMALIA_TEMPERATURA (Q12, R32) |
| Sensor que deja de reportar | SEN-007, última lectura 21/10 10:00 | Q11 lo marca INACTIVO; alerta REG-04 ALTA + incidente |
| Sensor que nunca reportó | SEN-040 (MSKU0000025) | Sin fila en `ultima_lectura_por_sensor` → SIN_DATOS; alerta REG-04 |
| Batería baja | SEN-012 desde 21/10 18:00 | 30 lecturas < 15 % (Q10) → 1 alerta REG-03 con 30 ocurrencias |
| Golpe, cierre y reapertura | SEN-020, 20/10 13:00 (3.4 g) y 22/10 15:00 (3.1 g) | 2 alertas REG-05: la primera CERRADA, la segunda ABIERTA (el índice único parcial lo permite) |
| Lectura sin GPS | SEN-001, 22/10 06:00 | Se guarda la telemetría, no se crea posición (Q07: el 22/10 hay 23 posiciones horarias + la tardía = 24) |
| Lectura tardía | SEN-001, lectura de las 05:30 enviada después de la de 23:00 | Queda en el historial pero **no** pisa `ultima_lectura_por_sensor` (Q05) |
| Reenvío duplicado | Q01 reenvía la misma lectura | El conteo sigue en 25 (upsert por PK) |
| Rango que cruza medianoche | Q02: 20/10 18:00 → 21/10 06:00 | 13 lecturas de dos particiones diarias |
| Retraso / desvío | E002 (congestión en Santos, +18 h) y E005 (corte RN7) | Eventos con payload distinto, cada uno con su incidente (R25) |
| Acceso indebido a mensajes | C01 intenta abrir la conversación privada C02–O01 | DENEGADO (R40) |

## Resumen de resultados esperados

Cassandra (`03_consultas.cql`, cada consulta trae su `-- ESPERADO`):
Q01 24→25 · Q02 13 · Q03 23:00…19:00 · Q04 8 · Q05 23:00 / 3.83 °C · Q07 72 posiciones ·
Q09 4 · Q10 30 · Q11 39 filas, inactivo SEN-007, sin datos SEN-040 · Q12 z = 6.73.

MongoDB (`03_consultas.js` lo compara solo e imprime OK/FALLA): alertas por estado
ABIERTA 6 / RECONOCIDA 1 / CERRADA 1 · 3 alertas de estado de sensor · 5 incidentes
(2 por evento, 3 por alerta) · 0 alertas graves sin incidente · 5 lecturas anómalas en 2 sensores ·
bandeja de C02 con 2 conversaciones · 2 grupos de trabajo.

Las mediciones son pseudoaleatorias con semilla fija; los valores de posición y temperatura exactos
están en los scripts y en `generador/esperado.json`.
