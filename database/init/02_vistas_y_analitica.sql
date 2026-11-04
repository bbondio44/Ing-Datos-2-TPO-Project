-- Consultas relacionales para los requisitos R06, R17, R24, R27, R28, R31, R35-R39.
BEGIN;
SET LOCAL search_path=tp_logistica,public;

-- Estado de sesiones calculado, no confiando unicamente en columna estado.
CREATE VIEW v_permisos_usuario AS
 SELECT ur.id_usuario,p.codigo AS permiso
 FROM usuario_rol ur JOIN rol_permiso rp ON rp.id_rol=ur.id_rol
 JOIN permiso p ON p.id_permiso=rp.id_permiso
 GROUP BY ur.id_usuario,p.codigo;

CREATE VIEW v_sesiones_activas AS
 SELECT s.id_sesion,s.id_usuario,u.email,s.fecha_inicio,s.fecha_expiracion
 FROM sesion s JOIN usuario u ON u.id_usuario=s.id_usuario
 WHERE s.estado='ACTIVA' AND s.fecha_revocacion IS NULL
  AND s.fecha_fin IS NULL AND s.fecha_expiracion>now() AND u.estado='ACTIVO';

CREATE VIEW v_envio_contenedor_sensor_activo AS
 SELECT e.id_envio,e.id_cliente,c.id_contenedor,c.codigo_internacional,
        s.id_sensor,s.tipo AS tipo_sensor,s.estado AS estado_sensor,
        ec.fecha_asociacion, ascx.fecha_inicio AS sensor_desde
 FROM envio e
 JOIN envio_contenedor ec ON ec.id_envio=e.id_envio
  AND ec.fecha_asociacion<=now()
  AND (ec.fecha_desasociacion IS NULL OR ec.fecha_desasociacion>now())
 JOIN contenedor c ON c.id_contenedor=ec.id_contenedor
 LEFT JOIN asignacion_sensor_contenedor ascx ON ascx.id_contenedor=c.id_contenedor
  AND ascx.fecha_inicio<=now() AND (ascx.fecha_fin IS NULL OR ascx.fecha_fin>now())
 LEFT JOIN sensor_iot s ON s.id_sensor=ascx.id_sensor;

CREATE VIEW v_seguimiento_tramos AS
 SELECT e.id_envio,e.id_cliente,t.id_tramo,t.numero_tramo,
  t.origen,t.destino,t.medio_transporte,t.estado AS estado_tramo,
  t.fecha_salida,t.fecha_llegada_estimada,t.fecha_llegada_real,
  o.razon_social AS operador,v.identificacion AS vehiculo
 FROM envio e JOIN tramo t ON e.id_envio=t.id_envio
 LEFT JOIN operador_logistico o ON o.id_operador=t.id_operador
 LEFT JOIN vehiculo v ON v.id_vehiculo=t.id_vehiculo;

CREATE VIEW v_envios_reporte AS
 SELECT e.id_envio,e.id_cliente,c.razon_social AS cliente,e.origen,e.destino,
  e.pais_origen,e.pais_destino,e.estado,e.prioridad,e.volumen_m3,
  e.fecha_creacion,e.fecha_entrega_estimada,e.fecha_entrega_real,
  (e.estado NOT IN ('ENTREGADO','CANCELADO') AND
   e.fecha_entrega_estimada IS NOT NULL AND e.fecha_entrega_estimada<now()) AS demorado,
  COALESCE(i.incidentes_abiertos,0) AS incidentes_abiertos,
  COALESCE(i.incidentes_totales,0) AS incidentes_totales
 FROM envio e JOIN cliente c ON c.id_cliente=e.id_cliente
 LEFT JOIN LATERAL
   (SELECT count(*) FILTER(WHERE estado<>'CERRADO') AS incidentes_abiertos,
           count(*) AS incidentes_totales
    FROM incidente WHERE id_envio=e.id_envio) i ON true;
-- Filtrar en API: WHERE pais_destino=..., estado=..., id_cliente=..., demorado etc.

CREATE VIEW v_ranking_clientes_volumen AS
 SELECT c.id_cliente,c.razon_social,COUNT(e.id_envio) AS cantidad_envios,
  COALESCE(SUM(e.volumen_m3),0)::numeric(18,3) AS volumen_total_m3
 FROM cliente c LEFT JOIN envio e ON e.id_cliente=c.id_cliente AND e.estado<>'CANCELADO'
 GROUP BY c.id_cliente,c.razon_social;

CREATE VIEW v_ranking_rutas_incidentes AS
 SELECT t.origen,t.destino,t.medio_transporte,
  COUNT(DISTINCT t.id_tramo) AS cantidad_tramos,
  COUNT(i.id_incidente) AS cantidad_incidentes,
  COUNT(i.id_incidente) FILTER(WHERE i.estado<>'CERRADO') AS incidentes_abiertos
 FROM tramo t LEFT JOIN incidente i ON i.id_tramo=t.id_tramo
 WHERE t.estado<>'CANCELADO'
 GROUP BY t.origen,t.destino,t.medio_transporte;

CREATE VIEW v_tiempos_entrega_cliente AS
 SELECT c.id_cliente,c.razon_social,
  COUNT(e.id_envio) AS envios_entregados,
  ROUND(MIN(EXTRACT(EPOCH FROM (e.fecha_entrega_real-e.fecha_creacion))/3600)::numeric,2) AS min_horas,
  ROUND(MAX(EXTRACT(EPOCH FROM (e.fecha_entrega_real-e.fecha_creacion))/3600)::numeric,2) AS max_horas,
  ROUND(AVG(EXTRACT(EPOCH FROM (e.fecha_entrega_real-e.fecha_creacion))/3600)::numeric,2) AS promedio_horas
 FROM cliente c JOIN envio e ON e.id_cliente=c.id_cliente
 WHERE e.estado='ENTREGADO' AND e.fecha_entrega_real IS NOT NULL
 GROUP BY c.id_cliente,c.razon_social;

CREATE VIEW v_tiempo_promedio_entrega AS
 SELECT COUNT(*) AS cantidad_entregados,
  ROUND(AVG(EXTRACT(EPOCH FROM (fecha_entrega_real-fecha_creacion))/3600)::numeric,2)
    AS promedio_horas
 FROM envio WHERE estado='ENTREGADO' AND fecha_entrega_real IS NOT NULL;

CREATE VIEW v_costos_por_transporte AS
 SELECT medio_transporte,
   COUNT(*) FILTER(WHERE costo_real IS NOT NULL) AS tramos_con_costo_real,
   COALESCE(SUM(costo_real),0)::numeric(18,2) AS costo_total_real,
   ROUND(AVG(costo_real),2) AS costo_promedio_real
 FROM tramo WHERE estado<>'CANCELADO'
 GROUP BY medio_transporte;

-- Dimension 'pais' = pais_destino del envio. 'estado' = estado del envio.
-- Denominador = cantidad de envios no cancelados en cada grupo.
CREATE VIEW v_promedio_incidentes_dimension AS
 SELECT e.pais_destino,e.estado AS estado_envio,e.id_cliente,
  COUNT(*)::bigint AS cantidad_envios,
  SUM(coalesce(i.cantidad_incidentes,0))::bigint AS cantidad_incidentes,
  ROUND((SUM(coalesce(i.cantidad_incidentes,0))::numeric/NULLIF(COUNT(*),0)),3)
    AS promedio_incidentes_por_envio
 FROM envio e
 LEFT JOIN LATERAL
  (SELECT COUNT(*) AS cantidad_incidentes FROM incidente WHERE id_envio=e.id_envio) i ON true
 WHERE e.estado<>'CANCELADO'
 GROUP BY e.pais_destino,e.estado,e.id_cliente;

CREATE VIEW v_desempeno_mensual AS
 SELECT date_trunc('month',e.fecha_creacion AT TIME ZONE 'UTC')::date AS periodo,
  e.id_cliente,e.pais_destino,
  COUNT(*) AS envios_creados,
  COUNT(*) FILTER(WHERE e.estado='ENTREGADO') AS entregados,
  COUNT(*) FILTER(WHERE e.estado='ENTREGADO' AND e.fecha_entrega_estimada IS NOT NULL
    AND e.fecha_entrega_real>e.fecha_entrega_estimada) AS entregados_fuera_plazo,
  ROUND((AVG(EXTRACT(EPOCH FROM (e.fecha_entrega_real-e.fecha_creacion))/3600)
    FILTER(WHERE e.estado='ENTREGADO'))::numeric,2) AS horas_promedio_entrega
 FROM envio e WHERE e.estado<>'CANCELADO'
 GROUP BY date_trunc('month',e.fecha_creacion AT TIME ZONE 'UTC')::date,e.id_cliente,e.pais_destino;

CREATE VIEW v_conciliacion_cuentas AS
 SELECT c.id_cuenta,c.naturaleza,c.id_cliente,c.id_proveedor,c.id_operador,
  c.saldo AS saldo_almacenado,
  COALESCE(SUM(CASE WHEN m.tipo_movimiento='DEBITO' THEN m.importe ELSE -m.importe END),0)
   ::numeric(16,2) AS saldo_calculado,
  c.saldo=COALESCE(SUM(CASE WHEN m.tipo_movimiento='DEBITO' THEN m.importe ELSE -m.importe END),0)
   AS saldo_conciliado
 FROM cuenta_corriente c LEFT JOIN movimiento_cuenta m ON m.id_cuenta=c.id_cuenta
 GROUP BY c.id_cuenta,c.naturaleza,c.id_cliente,c.id_proveedor,c.id_operador,c.saldo;

CREATE VIEW v_saldo_factura AS
 SELECT f.id_factura,f.numero_factura,f.id_cuenta,f.naturaleza,f.importe_total,f.estado,
  COALESCE(SUM(p.monto),0)::numeric(16,2) AS total_pagado,
  (f.importe_total-COALESCE(SUM(p.monto),0))::numeric(16,2) AS pendiente
 FROM factura f LEFT JOIN pago p ON p.id_factura=f.id_factura
 GROUP BY f.id_factura,f.numero_factura,f.id_cuenta,f.naturaleza,f.importe_total,f.estado;

-- Baseline reproducible: promedio de duracion de envios ENTREGADOS de la misma ruta
-- o, si no hay, de todos los envios entregados. No sustituye un modelo validado.
CREATE OR REPLACE FUNCTION tp_logistica.generar_prediccion_baseline(p_id_envio uuid)
RETURNS uuid LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE e envio%ROWTYPE; v_horas numeric; v_riesgo numeric; v_muestra integer;
 v_id uuid; v_criterio text;
BEGIN
 SELECT * INTO e FROM envio WHERE id_envio=p_id_envio;
 IF NOT FOUND OR e.estado IN ('ENTREGADO','CANCELADO') THEN
  RAISE EXCEPTION 'Solo se predicen envios vigentes no entregados'; END IF;
 SELECT COUNT(*),AVG(EXTRACT(EPOCH FROM (fecha_entrega_real-fecha_creacion))/3600),
  AVG(CASE WHEN fecha_entrega_estimada IS NOT NULL AND
    fecha_entrega_real>fecha_entrega_estimada THEN 1.0 ELSE 0.0 END)
 INTO v_muestra,v_horas,v_riesgo FROM envio
 WHERE estado='ENTREGADO' AND fecha_entrega_real IS NOT NULL
  AND pais_origen=e.pais_origen AND pais_destino=e.pais_destino;
 v_criterio:='misma_ruta_paises';
 IF v_muestra=0 THEN
  SELECT COUNT(*),AVG(EXTRACT(EPOCH FROM (fecha_entrega_real-fecha_creacion))/3600),
   AVG(CASE WHEN fecha_entrega_estimada IS NOT NULL AND
     fecha_entrega_real>fecha_entrega_estimada THEN 1.0 ELSE 0.0 END)
  INTO v_muestra,v_horas,v_riesgo FROM envio
  WHERE estado='ENTREGADO' AND fecha_entrega_real IS NOT NULL;
  v_criterio:='global';
 END IF;
 IF v_muestra=0 THEN RAISE EXCEPTION 'No hay historico para predecir'; END IF;
 INSERT INTO prediccion_envio(id_envio,modelo,eta_predicha,probabilidad_atraso,variables)
 VALUES(e.id_envio,'baseline_v1',e.fecha_creacion+(v_horas*interval '1 hour'),
  round(v_riesgo,5),jsonb_build_object('muestra',v_muestra,'horas_promedio',round(v_horas,2),
  'criterio',v_criterio,'limitacion','Media historica, sin validacion ML'))
 RETURNING id_prediccion INTO v_id;
 RETURN v_id;
END $$;
COMMIT;
