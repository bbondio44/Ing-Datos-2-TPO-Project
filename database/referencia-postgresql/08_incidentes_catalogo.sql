-- Incidentes del dataset de MongoDB y Cassandra (tp-grupo4-mongo-cassandra/incidentes_para_postgres.csv).
-- La fuente de verdad del incidente es PostgreSQL: las alertas y eventos de Mongo los referencian por UUID.
-- Re-ejecutable: ON CONFLICT no duplica.
SET search_path = tp_logistica, public;
INSERT INTO incidente (id_incidente, id_envio, id_tramo, id_evento_externo, tipo, severidad, descripcion, fecha, estado) VALUES
  ('f6c7d245-ed5f-50e1-809b-8c09122bf160', 'ec22d634-96cd-5267-b7bd-accdce959d92', '832d03fd-5634-54bc-bbb2-b831c1d3bb52', 'ee352f17-e059-5c08-8905-df74000374e0', 'DEMORA', 'ALTA', 'Demora de 18 h en tramo marítimo', '2026-10-21T08:00:00+00:00', 'ABIERTO'),
  ('f5b72008-9a09-54bd-8682-60e96cdea169', 'ecae19bc-e959-5b5d-bb35-be45c2e6c910', '881df510-7542-50fd-a2cc-f4bbe1db7dee', '766d02b6-8665-5e1f-a65c-b3bc5ce18365', 'DESVIO', 'MEDIA', 'Desvío por corte en RN7', '2026-10-20T03:00:00+00:00', 'ABIERTO'),
  ('4288a948-e9e1-51e6-b693-7029d2d32414', 'ec22d634-96cd-5267-b7bd-accdce959d92', '832d03fd-5634-54bc-bbb2-b831c1d3bb52', '3fa61b6b-b8f6-5faf-9e32-bc7658cd63b5', 'ALERTA_TEMPERATURA', 'ALTA', 'Temperatura 10.8 °C supera 8 °C en contenedor REEFER', '2026-10-21T14:00:05+00:00', 'ABIERTO'),
  ('29ad900d-f0a7-5bdf-a550-c423dd619494', '6378a987-ec3f-504e-80db-0a38c77ee116', 'bc56de92-e405-5c2c-b6cf-c189645f32f6', 'fd3e05e0-2fd5-5b37-adbe-c74c571ede3c', 'ALERTA_INACTIVIDAD', 'ALTA', 'Sensor sin lecturas hace 150 minutos', '2026-10-21T12:30:00+00:00', 'ABIERTO'),
  ('8d248830-f083-5b21-a26e-bad0492938ce', 'c231e5a7-7434-5d4b-b8db-405b7f42be44', '059c6b5a-2b9c-508f-b9a3-83059e0118c1', '58ede133-d04d-5447-a076-8e1257d78394', 'ALERTA_INACTIVIDAD', 'ALTA', 'Sensor sin lecturas desde su instalación', '2026-10-21T02:30:00+00:00', 'ABIERTO')
ON CONFLICT (id_incidente) DO NOTHING;
