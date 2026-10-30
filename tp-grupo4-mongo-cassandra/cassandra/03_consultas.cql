-- =====================================================================
-- Cassandra · CONSULTAS Q01–Q12 (flujo del ModeladoCassandraGrupo4.drawio)
-- GENERADO por generador/generar.py con los UUID reales del dataset.
-- Uso: cqlsh -f cassandra/03_consultas.cql    (después de 01_schema y 02_datos_prueba)
-- Cada bloque indica el requisito de promoción que respalda y el RESULTADO ESPERADO.
-- Instante de referencia de la demo ("ahora"): 2026-10-22 23:30 UTC
-- =====================================================================

USE logistica_iot;

-- Códigos legibles -> UUID (contrato común, ver catalogo_ids.csv)
--   SEN-001      = 101231a8-af8f-53ec-bfb0-f4d579ba7455
--   SEN-002      = d9e437b3-519e-5123-badd-eabc293e132b
--   SEN-003      = 12330766-e3ec-52b9-b9c0-b400c16274f6
--   SEN-005      = 94858666-0f1b-5d7a-8f8d-0118791421e9
--   SEN-007      = ee32d9a8-5a2d-5bb2-b703-370676b20446
--   SEN-012      = 5f9d963d-5c94-598d-9f99-83906ed03b7b
--   SEN-025      = 83006645-c8d5-5362-8e16-4f25460e5efe
--   SEN-028      = 51e58f15-26aa-5a22-818e-6a3b41fe2d5b
--   SEN-040      = 1a41b97f-b75c-5365-8111-d65ac5ec9d46
--   MSKU0000001  = 42d867d9-d7a4-5248-9681-33e2c0cdb123
--   MSKU0000002  = 5ce5419b-847b-5ecc-b70d-03b18c5c0003
--   MSKU0000003  = 96f3d043-00e2-5c9e-8a9d-d0810f2f5995
--   MSKU0000025  = 3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41

-- =====================================================================
-- Q01 · Registrar lectura (escritura desnormalizada)                [R18]
-- Una lectura nueva se escribe en las 5 tablas. Se usa LOGGED BATCH porque las
-- filas van a particiones/tablas distintas y deben quedar todas o ninguna.
-- Las tablas 'ultima_*' usan USING TIMESTAMP = instante de la lectura.
-- =====================================================================
SELECT COUNT(*) AS antes FROM lecturas_por_sensor_dia
  WHERE id_sensor = d9e437b3-519e-5123-badd-eabc293e132b AND dia = '2026-10-22';
-- ESPERADO: antes = 24  (25 si este script ya se corrió antes: la fila de prueba queda guardada)

BEGIN BATCH
  INSERT INTO lecturas_por_sensor_dia (id_sensor, dia, fecha_hora, id_lectura, id_contenedor, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (d9e437b3-519e-5123-badd-eabc293e132b, '2026-10-22', '2026-10-22 23:30:00.000+0000', 5f458b90-403a-5c62-bed0-aae030708053, 5ce5419b-847b-5ecc-b70d-03b18c5c0003, 4.72, 79.1, 0.41, -29.23328, -52.30551, 93.1);
  INSERT INTO lecturas_por_contenedor_dia (id_contenedor, dia, fecha_hora, id_sensor, id_lectura, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, '2026-10-22', '2026-10-22 23:30:00.000+0000', d9e437b3-519e-5123-badd-eabc293e132b, 5f458b90-403a-5c62-bed0-aae030708053, 4.72, 79.1, 0.41, -29.23328, -52.30551, 93.1);
  INSERT INTO posiciones_por_contenedor_dia (id_contenedor, dia, fecha_hora, id_sensor, id_lectura, latitud, longitud) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, '2026-10-22', '2026-10-22 23:30:00.000+0000', d9e437b3-519e-5123-badd-eabc293e132b, 5f458b90-403a-5c62-bed0-aae030708053, -29.23328, -52.30551);
  INSERT INTO ultima_lectura_por_sensor (id_sensor, id_contenedor, id_lectura, fecha_hora, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (d9e437b3-519e-5123-badd-eabc293e132b, 5ce5419b-847b-5ecc-b70d-03b18c5c0003, 5f458b90-403a-5c62-bed0-aae030708053, '2026-10-22 23:30:00.000+0000', 4.72, 79.1, 0.41, -29.23328, -52.30551, 93.1) USING TIMESTAMP 1792711800000000;
  INSERT INTO ultima_posicion_por_contenedor (id_contenedor, id_sensor, id_lectura, fecha_hora, latitud, longitud) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, d9e437b3-519e-5123-badd-eabc293e132b, 5f458b90-403a-5c62-bed0-aae030708053, '2026-10-22 23:30:00.000+0000', -29.23328, -52.30551) USING TIMESTAMP 1792711800000000;
APPLY BATCH;

SELECT COUNT(*) AS despues FROM lecturas_por_sensor_dia
  WHERE id_sensor = d9e437b3-519e-5123-badd-eabc293e132b AND dia = '2026-10-22';
-- ESPERADO: despues = 25

-- R18 · Duplicados: se reenvía EXACTAMENTE la misma lectura (mismo id_lectura e instante).
-- En Cassandra el INSERT es un upsert sobre la misma clave primaria: no se crea otra fila.
BEGIN BATCH
  INSERT INTO lecturas_por_sensor_dia (id_sensor, dia, fecha_hora, id_lectura, id_contenedor, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (d9e437b3-519e-5123-badd-eabc293e132b, '2026-10-22', '2026-10-22 23:30:00.000+0000', 5f458b90-403a-5c62-bed0-aae030708053, 5ce5419b-847b-5ecc-b70d-03b18c5c0003, 4.72, 79.1, 0.41, -29.23328, -52.30551, 93.1);
  INSERT INTO lecturas_por_contenedor_dia (id_contenedor, dia, fecha_hora, id_sensor, id_lectura, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, '2026-10-22', '2026-10-22 23:30:00.000+0000', d9e437b3-519e-5123-badd-eabc293e132b, 5f458b90-403a-5c62-bed0-aae030708053, 4.72, 79.1, 0.41, -29.23328, -52.30551, 93.1);
  INSERT INTO posiciones_por_contenedor_dia (id_contenedor, dia, fecha_hora, id_sensor, id_lectura, latitud, longitud) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, '2026-10-22', '2026-10-22 23:30:00.000+0000', d9e437b3-519e-5123-badd-eabc293e132b, 5f458b90-403a-5c62-bed0-aae030708053, -29.23328, -52.30551);
  INSERT INTO ultima_lectura_por_sensor (id_sensor, id_contenedor, id_lectura, fecha_hora, temperatura, humedad, vibracion, latitud, longitud, bateria) VALUES (d9e437b3-519e-5123-badd-eabc293e132b, 5ce5419b-847b-5ecc-b70d-03b18c5c0003, 5f458b90-403a-5c62-bed0-aae030708053, '2026-10-22 23:30:00.000+0000', 4.72, 79.1, 0.41, -29.23328, -52.30551, 93.1) USING TIMESTAMP 1792711800000000;
  INSERT INTO ultima_posicion_por_contenedor (id_contenedor, id_sensor, id_lectura, fecha_hora, latitud, longitud) VALUES (5ce5419b-847b-5ecc-b70d-03b18c5c0003, d9e437b3-519e-5123-badd-eabc293e132b, 5f458b90-403a-5c62-bed0-aae030708053, '2026-10-22 23:30:00.000+0000', -29.23328, -52.30551) USING TIMESTAMP 1792711800000000;
APPLY BATCH;
SELECT COUNT(*) AS despues_de_reenvio FROM lecturas_por_sensor_dia
  WHERE id_sensor = d9e437b3-519e-5123-badd-eabc293e132b AND dia = '2026-10-22';
-- ESPERADO: despues_de_reenvio = 25  (sin duplicar)

-- =====================================================================
-- Q02 · Historial de un sensor por período que CRUZA LA MEDIANOCHE    [R19]
-- La partición es (id_sensor, dia): se consultan las 2 particiones con IN y
-- la aplicación fusiona/ordena el resultado (sin ALLOW FILTERING).
-- =====================================================================
SELECT dia, fecha_hora, temperatura, humedad, bateria FROM lecturas_por_sensor_dia
  WHERE id_sensor = 12330766-e3ec-52b9-b9c0-b400c16274f6
    AND dia IN ('2026-10-20', '2026-10-21')
    AND fecha_hora >= '2026-10-20 18:00:00.000+0000' AND fecha_hora <= '2026-10-21 06:00:00.000+0000';
-- ESPERADO: 13 filas (18:00 del 20/10 a 06:00 del 21/10, una por hora).
--   Primera: 2026-10-20 18:00 temp 4.35 · Última: 2026-10-21 06:00 temp 4.82

-- =====================================================================
-- Q03 · Últimas N lecturas de un sensor (aprovecha fecha_hora DESC)     [R19, R22]
-- =====================================================================
SELECT fecha_hora, temperatura, humedad, vibracion, bateria FROM lecturas_por_sensor_dia
  WHERE id_sensor = 12330766-e3ec-52b9-b9c0-b400c16274f6 AND dia = '2026-10-22'
  LIMIT 5;
-- ESPERADO: 5 filas, de la más nueva a la más vieja: 23:00, 22:00, 21:00, 20:00, 19:00

-- =====================================================================
-- Q04 · Mediciones conjuntas de todos los sensores de un contenedor     [R17, R19]
-- Contenedor REEFER MSKU0000003 (envío E002) durante la excursión de temperatura.
-- =====================================================================
SELECT fecha_hora, id_sensor, temperatura, humedad FROM lecturas_por_contenedor_dia
  WHERE id_contenedor = 96f3d043-00e2-5c9e-8a9d-d0810f2f5995 AND dia = '2026-10-21'
    AND fecha_hora >= '2026-10-21 14:00:00.000+0000' AND fecha_hora <= '2026-10-21 17:00:00.000+0000';
-- ESPERADO: 8 filas (2 sensores x 4 horas). SEN-003 marca 10.8-11.6 °C mientras
--   SEN-028 (mismo contenedor) se mantiene en ~4-5 °C: útil para distinguir falla de sensor.

-- =====================================================================
-- Q05 · Último estado de un sensor + prueba de LECTURA TARDÍA (C03)    [R22, R18]
-- SEN-001 envió al final una lectura de las 05:30 (ver 02_datos_prueba.cql). La fila
-- conserva la de las 23:00 porque su WRITETIME (= instante del evento) es mayor.
-- =====================================================================
SELECT id_sensor, fecha_hora, temperatura, bateria, latitud, longitud, WRITETIME(temperatura) AS wt
  FROM ultima_lectura_por_sensor WHERE id_sensor = 101231a8-af8f-53ec-bfb0-f4d579ba7455;
-- ESPERADO: fecha_hora = 2026-10-22 23:00, temperatura = 3.83, wt = 1792710000000000

-- La lectura tardía SÍ está en el historial:
SELECT fecha_hora, temperatura FROM lecturas_por_sensor_dia
  WHERE id_sensor = 101231a8-af8f-53ec-bfb0-f4d579ba7455 AND dia = '2026-10-22' AND fecha_hora = '2026-10-22 05:30:00.000+0000';
-- ESPERADO: 1 fila (05:30, temperatura 4.61)

-- =====================================================================
-- Q06 · Estado de los sensores de un contenedor                        [R22, R23]
-- La lista de sensores del contenedor sale de PostgreSQL (asignación vigente);
-- MSKU0000025 tiene SEN-025 y SEN-040.
-- =====================================================================
SELECT id_sensor, fecha_hora, bateria, temperatura FROM ultima_lectura_por_sensor
  WHERE id_sensor IN (83006645-c8d5-5362-8e16-4f25460e5efe, 1a41b97f-b75c-5365-8111-d65ac5ec9d46);
-- ESPERADO: 1 fila (SEN-025, 2026-10-22 23:00). SEN-040 no devuelve fila
--   -> la aplicación lo informa como SIN_DATOS (nunca reportó).

-- =====================================================================
-- Q07 · Recorrido GPS histórico de un contenedor                       [R20, R21]
-- Reconstrucción del recorrido: una consulta por día, en orden cronológico
-- (ORDER BY ASC invierte el clustering DESC dentro de la partición).
-- =====================================================================
SELECT fecha_hora, latitud, longitud FROM posiciones_por_contenedor_dia
  WHERE id_contenedor = 42d867d9-d7a4-5248-9681-33e2c0cdb123 AND dia = '2026-10-20'
  ORDER BY fecha_hora ASC;
-- ESPERADO: 24 posiciones
SELECT fecha_hora, latitud, longitud FROM posiciones_por_contenedor_dia
  WHERE id_contenedor = 42d867d9-d7a4-5248-9681-33e2c0cdb123 AND dia = '2026-10-21'
  ORDER BY fecha_hora ASC;
-- ESPERADO: 24 posiciones
SELECT fecha_hora, latitud, longitud FROM posiciones_por_contenedor_dia
  WHERE id_contenedor = 42d867d9-d7a4-5248-9681-33e2c0cdb123 AND dia = '2026-10-22'
  ORDER BY fecha_hora ASC;
-- ESPERADO: 24 posiciones
-- TOTAL ESPERADO: 72 posiciones. El 22/10 hay 24: falta la de las 06:00 (sin fix GPS)
--   y aparece la de las 05:30 (lectura tardía). Primer punto: (-34.58836, -58.37035) · último: (-29.2729, -52.35262).
-- Puntos en orden = polilínea para el mapa (Puerto de Buenos Aires -> Puerto de Santos).

-- =====================================================================
-- Q08 · Última posición GPS (mapa de seguimiento)                      [R21]
-- =====================================================================
SELECT id_contenedor, fecha_hora, latitud, longitud FROM ultima_posicion_por_contenedor
  WHERE id_contenedor IN (42d867d9-d7a4-5248-9681-33e2c0cdb123, 5ce5419b-847b-5ecc-b70d-03b18c5c0003);
-- ESPERADO MSKU0000001: 2026-10-22 23:00 (-29.2729, -52.35262)
-- ESPERADO MSKU0000002: 2026-10-22 23:30 (-29.23328, -52.30551)

-- =====================================================================
-- Q09 · Lecturas que superan un umbral (regla REG-01: temperatura > 8)  [R09, R26, R32]
-- ALLOW FILTERING está acotado a UNA partición (24 filas): costo controlado.
-- La evaluación en línea la hace la aplicación al ingerir; esta consulta audita.
-- =====================================================================
SELECT fecha_hora, temperatura FROM lecturas_por_sensor_dia
  WHERE id_sensor = 12330766-e3ec-52b9-b9c0-b400c16274f6 AND dia = '2026-10-21' AND temperatura > 8.0
  ALLOW FILTERING;
-- ESPERADO: 4 filas: 14:00=10.8, 15:00=11.4, 16:00=11.6, 17:00=10.9
--   (coincide con la alerta REG-01 en MongoDB: ocurrencias = 4)

-- =====================================================================
-- Q10 · Batería: estado actual y lecturas bajo el umbral (REG-03 < 15 %)  [R22, R23]
-- =====================================================================
SELECT id_sensor, fecha_hora, bateria FROM ultima_lectura_por_sensor
  WHERE id_sensor = 5f9d963d-5c94-598d-9f99-83906ed03b7b;
-- ESPERADO: bateria = 8.4 (2026-10-22 23:00)
SELECT fecha_hora, bateria FROM lecturas_por_sensor_dia
  WHERE id_sensor = 5f9d963d-5c94-598d-9f99-83906ed03b7b AND dia = '2026-10-21' AND bateria < 15.0
  ALLOW FILTERING;
-- ESPERADO: 6 filas (desde las 18:00, batería 14.8)
SELECT fecha_hora, bateria FROM lecturas_por_sensor_dia
  WHERE id_sensor = 5f9d963d-5c94-598d-9f99-83906ed03b7b AND dia = '2026-10-22' AND bateria < 15.0
  ALLOW FILTERING;
-- ESPERADO: 24 filas
--   Total 30 = ocurrencias de la alerta REG-03 en MongoDB.

-- =====================================================================
-- Q11 · Detección de sensores inactivos (proceso periódico)            [R22, R23]
-- La aplicación trae la última lectura de los sensores vigentes (lista desde
-- PostgreSQL) y compara fecha_hora con ahora - 120 min (regla REG-04).
-- Con IN sobre la partition key se evita recorrer toda la tabla.
-- =====================================================================
SELECT id_sensor, id_contenedor, fecha_hora, bateria FROM ultima_lectura_por_sensor
  WHERE id_sensor IN (
    101231a8-af8f-53ec-bfb0-f4d579ba7455,
    d9e437b3-519e-5123-badd-eabc293e132b,
    12330766-e3ec-52b9-b9c0-b400c16274f6,
    51847209-55f1-54b5-83fc-698738457784,
    94858666-0f1b-5d7a-8f8d-0118791421e9,
    8e4ab8c8-5e28-5d73-a6fb-09e7ef2e5eba,
    ee32d9a8-5a2d-5bb2-b703-370676b20446,
    02c4c136-b65a-5b53-ae1a-2fb639f8e342,
    47c50803-5d2b-56e3-b880-3c1183fcee9a,
    45030ca6-267c-52cc-9dd8-ae5c999ebd8b,
    03ebe16a-2179-5a10-8ef3-9ddee070e4f2,
    5f9d963d-5c94-598d-9f99-83906ed03b7b,
    485bbb29-3858-538d-a4fc-14825417c1bb,
    5c6b62dc-896f-56bb-bedf-2fff168dbd28,
    ce360620-7aac-5c00-8eb2-768e008bb00c,
    0c6628df-23c1-5215-8042-6e28f0917c99,
    b8db9365-eabb-5abd-a0ae-f33b9e88eed2,
    a9953280-dfe2-577d-853a-dee5ed77ca63,
    907febb0-9f7a-5107-aa92-8e746dee266b,
    099eb63e-59f7-5b79-82f9-ecd7cc14a31a,
    a64abe30-e9c6-5def-a494-b7da3a1b5401,
    4e26c50a-502b-5de0-a0d2-42bfca9cc588,
    869f9a01-3e51-5644-b367-c63e14f2e613,
    72d03735-e45d-549f-993c-054050f48ad0,
    83006645-c8d5-5362-8e16-4f25460e5efe,
    458d6508-e6b4-5322-b376-9e3a8333f438,
    96b7f637-cb2b-54c0-98bb-1f09d71ef328,
    51e58f15-26aa-5a22-818e-6a3b41fe2d5b,
    b53c466a-939d-5642-99c6-7a1d32725d4f,
    04b93978-b79f-5ec2-b27e-9227c53497d6,
    591f5045-dbec-5eca-b5fb-7d8bdb2a30a8,
    b89f241c-d8f2-5dc8-847e-c8934067b02d,
    4617a0c9-64bf-513a-ac32-1ac5fd19a7db,
    58f32a1b-f2d6-5968-b9bc-e12f672d35fb,
    31dfb9b1-85fd-523e-9bd9-aac4b96b292d,
    13420416-b2c6-5a5d-812b-9e1c5f0cb14a,
    07b2aff3-26f5-590e-9a9c-5f7665997d0e,
    2e417392-252c-5763-9d55-9ed7b82bbcdd,
    df17a291-d8f9-5a4c-8e52-3aa43d682a36,
    1a41b97f-b75c-5365-8111-d65ac5ec9d46
  );
-- ESPERADO: 39 filas (SEN-040 no aparece -> SIN_DATOS).
--   Con ahora = 2026-10-22 23:30, límite = 2026-10-22 21:30:
--   INACTIVOS: SEN-007 (última lectura 2026-10-21 10:00)
--   SIN_DATOS: SEN-040

-- =====================================================================
-- Q12 · Tendencias / estadísticas por sensor y día (base de R32)        [R32, R22]
-- Agregaciones dentro de UNA partición. El z-score (|z| > 3) lo calcula la
-- aplicación con estos valores; el resultado se guarda como alerta en MongoDB.
-- =====================================================================
SELECT COUNT(*) AS lecturas, MIN(temperatura) AS minima, MAX(temperatura) AS maxima, AVG(temperatura) AS promedio
  FROM lecturas_por_sensor_dia WHERE id_sensor = 94858666-0f1b-5d7a-8f8d-0118791421e9 AND dia = '2026-10-20';
-- ESPERADO 2026-10-20: lecturas=24, minima=3.63, maxima=4.67, promedio≈4.10
SELECT COUNT(*) AS lecturas, MIN(temperatura) AS minima, MAX(temperatura) AS maxima, AVG(temperatura) AS promedio
  FROM lecturas_por_sensor_dia WHERE id_sensor = 94858666-0f1b-5d7a-8f8d-0118791421e9 AND dia = '2026-10-21';
-- ESPERADO 2026-10-21: lecturas=24, minima=3.61, maxima=4.7, promedio≈4.20
SELECT COUNT(*) AS lecturas, MIN(temperatura) AS minima, MAX(temperatura) AS maxima, AVG(temperatura) AS promedio
  FROM lecturas_por_sensor_dia WHERE id_sensor = 94858666-0f1b-5d7a-8f8d-0118791421e9 AND dia = '2026-10-22';
-- ESPERADO 2026-10-22: lecturas=24, minima=3.56, maxima=7.9, promedio≈4.38
--   La máxima del 22/10 (7.9 °C a las 09:00) da z = 6.73 > 3:
--   anomalía aunque NO supere el umbral fijo de 8 °C (por eso se usan ambos métodos).
SELECT fecha_hora, temperatura FROM lecturas_por_sensor_dia
  WHERE id_sensor = 94858666-0f1b-5d7a-8f8d-0118791421e9 AND dia = '2026-10-22' AND fecha_hora = '2026-10-22 09:00:00.000+0000';

