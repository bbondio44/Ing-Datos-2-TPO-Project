-- Ejecutar tras 03_datos_demo.sql.
-- Todos los asserts deben finalizar sin excepcion; los SELECT deben devolver filas.
BEGIN;
SET LOCAL search_path=tp_logistica,public;
DO $$
DECLARE v integer; v_saldo numeric; v_id uuid; v_cierre cierre_cuenta%ROWTYPE;
BEGIN
 SELECT COUNT(*) INTO v FROM cliente; IF v < 4 THEN RAISE EXCEPTION 'Falla ABM clientes: sin datos'; END IF;
 SELECT COUNT(*) INTO v FROM v_sesiones_activas; IF v<>1 THEN RAISE EXCEPTION 'Falla sesiones activas'; END IF;
 SELECT COUNT(*) INTO v FROM v_envio_contenedor_sensor_activo WHERE id_sensor IS NOT NULL;
 IF v<3 THEN RAISE EXCEPTION 'Falla asociacion activa envio/contenedor/sensor'; END IF;
 SELECT COUNT(*) INTO v FROM envio_contenedor WHERE fecha_desasociacion IS NOT NULL;
 IF v<2 THEN RAISE EXCEPTION 'Falta historico contenedores'; END IF;
 SELECT COUNT(*) INTO v FROM regla_alerta WHERE activa;
 IF v<3 THEN RAISE EXCEPTION 'Faltan reglas IoT'; END IF;
 SELECT COUNT(*) INTO v FROM incidente WHERE id_tramo IS NOT NULL;
 IF v<3 THEN RAISE EXCEPTION 'Falta relacion incidentes/tramos'; END IF;
 SELECT COUNT(*) INTO v FROM v_seguimiento_tramos;
 IF v<8 THEN RAISE EXCEPTION 'Faltan tramos'; END IF;
 SELECT COUNT(*) INTO v FROM v_ranking_rutas_incidentes WHERE cantidad_incidentes>0;
 IF v<2 THEN RAISE EXCEPTION 'Ranking rutas no demuestra diferentes incidentes'; END IF;
 SELECT COUNT(*) INTO v FROM v_tiempos_entrega_cliente;
 IF v<2 THEN RAISE EXCEPTION 'Tiempos por cliente vacios'; END IF;
 SELECT COUNT(*) INTO v FROM v_costos_por_transporte WHERE costo_total_real>0;
 IF v<2 THEN RAISE EXCEPTION 'Faltan medios de transporte con costos reales'; END IF;
 SELECT COUNT(*) INTO v FROM prediccion_envio;
 IF v<1 THEN RAISE EXCEPTION 'Prediccion sin datos'; END IF;
 SELECT COUNT(*) INTO v FROM solicitud_reporte WHERE id_factura IS NOT NULL;
 IF v<1 THEN RAISE EXCEPTION 'Reporte no facturado'; END IF;
 SELECT COUNT(*) INTO v FROM v_conciliacion_cuentas WHERE NOT saldo_conciliado;
 IF v<>0 THEN RAISE EXCEPTION 'Hay saldos sin conciliar'; END IF;
 SELECT saldo INTO v_saldo FROM cuenta_corriente WHERE id_cliente=
  (SELECT id_cliente FROM cliente WHERE razon_social='Transportes del Sur SA');
 IF v_saldo<>90000 THEN RAISE EXCEPTION 'Saldo Acme: esperado 90000, obtenido %',v_saldo; END IF;
 SELECT saldo INTO v_saldo FROM cuenta_corriente WHERE id_cliente=
  (SELECT id_cliente FROM cliente WHERE razon_social='Importadora Atlas SA');
 IF v_saldo<>23000 THEN RAISE EXCEPTION 'Saldo Atlas: esperado 23000, obtenido %',v_saldo; END IF;
 SELECT * INTO v_cierre FROM cierre_cuenta WHERE id_cuenta=
  (SELECT id_cuenta FROM cuenta_corriente WHERE id_cliente=
   (SELECT id_cliente FROM cliente WHERE razon_social='Importadora Atlas SA'));
 IF NOT FOUND OR v_cierre.total_debitos<>35000 OR v_cierre.total_creditos<>12000
    OR v_cierre.saldo_final<>23000 THEN RAISE EXCEPTION 'Falla cierre mensual'; END IF;
 SELECT COUNT(*) INTO v FROM factura WHERE clave_idempotencia='demo-fact-envio-e1';
 IF v<>1 THEN RAISE EXCEPTION 'Factura duplicada'; END IF;
 SELECT COUNT(*) INTO v FROM pago WHERE clave_idempotencia='demo-pago-parcial-e1';
 IF v<>1 THEN RAISE EXCEPTION 'Pago duplicado'; END IF;
 SELECT COUNT(*) INTO v FROM outbox_evento;
 IF v<20 THEN RAISE EXCEPTION 'Outbox no contiene eventos iniciales'; END IF;
 RAISE NOTICE 'OK: estructura poblada, analitica, facturacion, cierres y sincronizacion verificadas';
END $$;
COMMIT;

-- Resultados visibles para capturas / pruebas de aceptacion.
SELECT * FROM tp_logistica.v_sesiones_activas;
SELECT * FROM tp_logistica.v_permisos_usuario ORDER BY id_usuario,permiso;
SELECT * FROM tp_logistica.v_envio_contenedor_sensor_activo ORDER BY id_envio,id_contenedor;
SELECT * FROM tp_logistica.v_envios_reporte ORDER BY fecha_creacion DESC;
SELECT * FROM tp_logistica.v_seguimiento_tramos ORDER BY id_envio,numero_tramo;
SELECT * FROM tp_logistica.v_ranking_rutas_incidentes ORDER BY cantidad_incidentes DESC;
SELECT * FROM tp_logistica.v_ranking_clientes_volumen ORDER BY volumen_total_m3 DESC;
SELECT * FROM tp_logistica.v_tiempos_entrega_cliente ORDER BY razon_social;
SELECT * FROM tp_logistica.v_tiempo_promedio_entrega;
SELECT * FROM tp_logistica.v_promedio_incidentes_dimension ORDER BY pais_destino,estado_envio;
SELECT * FROM tp_logistica.v_costos_por_transporte ORDER BY medio_transporte;
SELECT * FROM tp_logistica.v_desempeno_mensual ORDER BY periodo;
SELECT * FROM tp_logistica.v_conciliacion_cuentas;
SELECT * FROM tp_logistica.v_saldo_factura ORDER BY numero_factura;
SELECT * FROM tp_logistica.cierre_cuenta;
SELECT * FROM tp_logistica.prediccion_envio;
SELECT * FROM tp_logistica.outbox_evento ORDER BY fecha_creacion DESC LIMIT 20;
