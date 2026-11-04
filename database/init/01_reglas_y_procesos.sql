-- Reglas transaccionales. Ejecutar luego de 00_esquema.sql.
BEGIN;
SET LOCAL search_path = tp_logistica, public;

-- El mismo contenedor/sensor no puede estar en dos destinos al mismo instante.
-- Advisory lock serializa modificaciones concurrentes por identificador.
-- Intervalos [inicio, fin): dos asignaciones consecutivas no se superponen.
CREATE OR REPLACE FUNCTION tp_logistica.validar_asignacion_envio_insert()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(hashtext(NEW.id_contenedor::text)::bigint);
 IF EXISTS (SELECT 1 FROM envio_contenedor ec WHERE ec.id_contenedor=NEW.id_contenedor
  AND tstzrange(ec.fecha_asociacion,ec.fecha_desasociacion,'[)') &&
      tstzrange(NEW.fecha_asociacion,NEW.fecha_desasociacion,'[)')) THEN
  RAISE EXCEPTION 'Contenedor ya asignado durante ese intervalo'; END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION tp_logistica.validar_asignacion_envio_update()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN
 IF NEW.id_contenedor<>OLD.id_contenedor THEN
  RAISE EXCEPTION 'No cambiar identificador del contenedor: cerrar y crear asignacion'; END IF;
 PERFORM pg_advisory_xact_lock(hashtext(NEW.id_contenedor::text)::bigint);
 IF EXISTS (SELECT 1 FROM envio_contenedor ec WHERE ec.id_contenedor=NEW.id_contenedor
  AND (ec.id_envio,ec.id_contenedor,ec.fecha_asociacion) IS DISTINCT FROM
      (OLD.id_envio,OLD.id_contenedor,OLD.fecha_asociacion)
  AND tstzrange(ec.fecha_asociacion,ec.fecha_desasociacion,'[)') &&
      tstzrange(NEW.fecha_asociacion,NEW.fecha_desasociacion,'[)')) THEN
  RAISE EXCEPTION 'Contenedor ya asignado durante ese intervalo'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER envio_contenedor_insert_guard BEFORE INSERT ON envio_contenedor
 FOR EACH ROW EXECUTE FUNCTION validar_asignacion_envio_insert();
CREATE TRIGGER envio_contenedor_update_guard BEFORE UPDATE ON envio_contenedor
 FOR EACH ROW EXECUTE FUNCTION validar_asignacion_envio_update();

CREATE OR REPLACE FUNCTION tp_logistica.validar_sensor_insert()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(hashtext(NEW.id_sensor::text)::bigint);
 IF EXISTS (SELECT 1 FROM asignacion_sensor_contenedor a WHERE a.id_sensor=NEW.id_sensor
  AND tstzrange(a.fecha_inicio,a.fecha_fin,'[)') &&
      tstzrange(NEW.fecha_inicio,NEW.fecha_fin,'[)')) THEN
  RAISE EXCEPTION 'Sensor ya asignado en el periodo'; END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION tp_logistica.validar_sensor_update()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN
 IF NEW.id_sensor<>OLD.id_sensor OR NEW.id_asignacion<>OLD.id_asignacion THEN
  RAISE EXCEPTION 'No cambiar identificador del sensor o de la asignacion'; END IF;
 PERFORM pg_advisory_xact_lock(hashtext(NEW.id_sensor::text)::bigint);
 IF EXISTS (SELECT 1 FROM asignacion_sensor_contenedor a WHERE a.id_sensor=NEW.id_sensor
  AND a.id_asignacion<>OLD.id_asignacion
  AND tstzrange(a.fecha_inicio,a.fecha_fin,'[)') &&
      tstzrange(NEW.fecha_inicio,NEW.fecha_fin,'[)')) THEN
  RAISE EXCEPTION 'Sensor ya asignado en el periodo'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER sensor_asignacion_insert_guard BEFORE INSERT ON asignacion_sensor_contenedor
 FOR EACH ROW EXECUTE FUNCTION validar_sensor_insert();
CREATE TRIGGER sensor_asignacion_update_guard BEFORE UPDATE ON asignacion_sensor_contenedor
 FOR EACH ROW EXECUTE FUNCTION validar_sensor_update();

-- Estados: validar transiciones, no solo pertenencia al catalogo.
CREATE OR REPLACE FUNCTION tp_logistica.validar_estado_envio()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN
 IF NEW.estado=OLD.estado THEN RETURN NEW; END IF;
 IF NOT (
  (OLD.estado='CREADO' AND NEW.estado IN ('EN_PREPARACION','CANCELADO')) OR
  (OLD.estado='EN_PREPARACION' AND NEW.estado IN ('EN_TRANSITO','CANCELADO')) OR
  (OLD.estado='EN_TRANSITO' AND NEW.estado IN ('DEMORADO','ENTREGADO','CANCELADO')) OR
  (OLD.estado='DEMORADO' AND NEW.estado IN ('EN_TRANSITO','ENTREGADO','CANCELADO'))
 ) THEN RAISE EXCEPTION 'Transicion de envio invalida: % -> %',OLD.estado,NEW.estado; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER envio_estado_guard BEFORE UPDATE OF estado ON envio FOR EACH ROW EXECUTE FUNCTION validar_estado_envio();
CREATE OR REPLACE FUNCTION tp_logistica.validar_estado_incidente()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN
 IF OLD.estado=NEW.estado THEN RETURN NEW; END IF;
 IF NOT ((OLD.estado='ABIERTO' AND NEW.estado IN ('EN_GESTION','CERRADO')) OR
         (OLD.estado='EN_GESTION' AND NEW.estado='CERRADO')) THEN
  RAISE EXCEPTION 'Transicion de incidente invalida'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER incidente_estado_guard BEFORE UPDATE OF estado ON incidente FOR EACH ROW EXECUTE FUNCTION validar_estado_incidente();

CREATE OR REPLACE FUNCTION tp_logistica.validar_tramo()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_operador uuid;
BEGIN
 IF NEW.id_vehiculo IS NOT NULL THEN
  SELECT id_operador INTO v_operador FROM vehiculo WHERE id_vehiculo=NEW.id_vehiculo;
  IF NEW.id_operador IS NULL THEN NEW.id_operador:=v_operador;
  ELSIF NEW.id_operador IS DISTINCT FROM v_operador THEN
   RAISE EXCEPTION 'El vehiculo no pertenece al operador del tramo'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER tramo_operador_guard BEFORE INSERT OR UPDATE ON tramo
 FOR EACH ROW EXECUTE FUNCTION validar_tramo();

CREATE OR REPLACE FUNCTION tp_logistica.validar_incidente_tramo()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN
 IF NEW.id_tramo IS NOT NULL AND NOT EXISTS
   (SELECT 1 FROM tramo WHERE id_tramo=NEW.id_tramo AND id_envio=NEW.id_envio) THEN
  RAISE EXCEPTION 'El tramo no pertenece al envio del incidente'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER incidente_guard BEFORE INSERT OR UPDATE ON incidente
 FOR EACH ROW EXECUTE FUNCTION validar_incidente_tramo();

CREATE OR REPLACE FUNCTION tp_logistica.validar_solicitud_reporte()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_cliente uuid; v_factura_cliente uuid;
BEGIN
 SELECT id_cliente INTO v_cliente FROM usuario WHERE id_usuario=NEW.id_usuario;
 IF v_cliente IS NOT NULL AND v_cliente<>NEW.id_cliente THEN
  RAISE EXCEPTION 'Usuario no pertenece al cliente del reporte'; END IF;
 IF NEW.id_factura IS NOT NULL THEN
  SELECT id_cliente INTO v_factura_cliente FROM factura WHERE id_factura=NEW.id_factura;
  IF v_factura_cliente IS DISTINCT FROM NEW.id_cliente THEN
   RAISE EXCEPTION 'Factura no corresponde al cliente del reporte'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER reporte_titular_guard BEFORE INSERT OR UPDATE ON solicitud_reporte
 FOR EACH ROW EXECUTE FUNCTION validar_solicitud_reporte();

-- Control financiero: las facturas y las cuentas deben pertenecer a la misma parte.
CREATE OR REPLACE FUNCTION tp_logistica.validar_factura_cuenta()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE c cuenta_corriente%ROWTYPE;
BEGIN
 SELECT * INTO c FROM cuenta_corriente WHERE id_cuenta=NEW.id_cuenta;
 IF NOT FOUND OR NEW.naturaleza<>c.naturaleza OR
   NOT (NEW.id_cliente IS NOT DISTINCT FROM c.id_cliente AND
        NEW.id_proveedor IS NOT DISTINCT FROM c.id_proveedor AND
        NEW.id_operador IS NOT DISTINCT FROM c.id_operador) THEN
  RAISE EXCEPTION 'Factura y cuenta corriente poseen titulares/naturaleza diferentes'; END IF;
 IF TG_OP='UPDATE' THEN
  IF OLD.estado<>'BORRADOR' AND (
   NEW.importe_total IS DISTINCT FROM OLD.importe_total OR
   NEW.id_cuenta IS DISTINCT FROM OLD.id_cuenta OR
   NEW.id_cliente IS DISTINCT FROM OLD.id_cliente OR
   NEW.id_proveedor IS DISTINCT FROM OLD.id_proveedor OR
   NEW.id_operador IS DISTINCT FROM OLD.id_operador OR
   NEW.naturaleza IS DISTINCT FROM OLD.naturaleza OR
   NEW.numero_factura IS DISTINCT FROM OLD.numero_factura OR
   NEW.id_envio IS DISTINCT FROM OLD.id_envio) THEN
   RAISE EXCEPTION 'Factura emitida: datos contables inmutables'; END IF;
  IF OLD.estado IS DISTINCT FROM NEW.estado AND NOT (
    (OLD.estado='BORRADOR' AND NEW.estado='EMITIDA') OR
    (OLD.estado='EMITIDA' AND NEW.estado IN ('PARCIAL','PAGADA')) OR
    (OLD.estado='PARCIAL' AND NEW.estado='PAGADA')) THEN
   RAISE EXCEPTION 'Transicion de factura invalida: % -> %',OLD.estado,NEW.estado; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER factura_guard BEFORE INSERT OR UPDATE ON factura
 FOR EACH ROW EXECUTE FUNCTION validar_factura_cuenta();

CREATE OR REPLACE FUNCTION tp_logistica.validar_detalle_borrador()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_id uuid; v_estado text;
BEGIN
 v_id:=CASE WHEN TG_OP='DELETE' THEN OLD.id_factura ELSE NEW.id_factura END;
 SELECT estado INTO v_estado FROM factura WHERE id_factura=v_id FOR UPDATE;
 IF v_estado IS DISTINCT FROM 'BORRADOR' THEN
  RAISE EXCEPTION 'Los detalles solo pueden cambiar en facturas BORRADOR'; END IF;
 IF TG_OP='UPDATE' THEN
  IF NEW.id_factura<>OLD.id_factura THEN
   RAISE EXCEPTION 'No cambiar factura de un detalle'; END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER detalle_guard BEFORE INSERT OR UPDATE OR DELETE ON factura_detalle
 FOR EACH ROW EXECUTE FUNCTION validar_detalle_borrador();

-- Validacion de movimientos, bloqueos por periodo cerrado e inmutabilidad.
CREATE OR REPLACE FUNCTION tp_logistica.validar_movimiento()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_factura_cuenta uuid; v_pago_cuenta uuid;
BEGIN
 PERFORM 1 FROM cuenta_corriente WHERE id_cuenta=NEW.id_cuenta FOR UPDATE;
 IF EXISTS (SELECT 1 FROM cierre_cuenta WHERE id_cuenta=NEW.id_cuenta
   AND periodo >= date_trunc('month',NEW.fecha AT TIME ZONE 'UTC')::date) THEN
  RAISE EXCEPTION 'Periodo contable cerrado: registrar ajuste en periodo posterior'; END IF;
 IF NEW.id_factura IS NOT NULL THEN
  SELECT id_cuenta INTO v_factura_cuenta FROM factura WHERE id_factura=NEW.id_factura;
  IF v_factura_cuenta IS DISTINCT FROM NEW.id_cuenta THEN
   RAISE EXCEPTION 'Movimiento no corresponde a cuenta de factura'; END IF;
 END IF;
 IF NEW.id_pago IS NOT NULL THEN
  SELECT id_cuenta INTO v_pago_cuenta FROM pago WHERE id_pago=NEW.id_pago;
  IF v_pago_cuenta IS DISTINCT FROM NEW.id_cuenta THEN
   RAISE EXCEPTION 'Movimiento no corresponde a cuenta de pago'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER movimiento_guard BEFORE INSERT ON movimiento_cuenta
 FOR EACH ROW EXECUTE FUNCTION validar_movimiento();
CREATE OR REPLACE FUNCTION tp_logistica.movimiento_inmutable()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN RAISE EXCEPTION 'Movimientos contables inmutables: usar asiento de ajuste'; END $$;
CREATE TRIGGER movimiento_inmutable_guard BEFORE UPDATE OR DELETE ON movimiento_cuenta
 FOR EACH ROW EXECUTE FUNCTION movimiento_inmutable();
CREATE OR REPLACE FUNCTION tp_logistica.recalcular_saldo()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN
 UPDATE cuenta_corriente SET
  saldo=(SELECT COALESCE(SUM(CASE WHEN tipo_movimiento='DEBITO' THEN importe ELSE -importe END),0)
         FROM movimiento_cuenta WHERE id_cuenta=NEW.id_cuenta),
  fecha_actualizacion=now()
 WHERE id_cuenta=NEW.id_cuenta;
 RETURN NULL;
END $$;
CREATE TRIGGER movimiento_recalcula AFTER INSERT ON movimiento_cuenta
 FOR EACH ROW EXECUTE FUNCTION recalcular_saldo();

CREATE OR REPLACE FUNCTION tp_logistica.emitir_factura(p_id_factura uuid)
RETURNS uuid LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE f factura%ROWTYPE; v_total numeric(16,2); v_cant integer;
BEGIN
 SELECT * INTO f FROM factura WHERE id_factura=p_id_factura FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'Factura inexistente'; END IF;
 IF f.estado<>'BORRADOR' THEN RETURN f.id_factura; END IF;
 SELECT COUNT(*),COALESCE(SUM(subtotal),0) INTO v_cant,v_total
  FROM factura_detalle WHERE id_factura=f.id_factura;
 IF v_cant=0 OR v_total<=0 THEN RAISE EXCEPTION 'Factura sin detalle positivo'; END IF;
 UPDATE factura SET importe_total=v_total, estado='EMITIDA',
  numero_factura=COALESCE(numero_factura,
   'TP-'||to_char(current_date,'YYYY')||'-'||lpad(nextval('tp_logistica.factura_numero_seq')::text,7,'0'))
 WHERE id_factura=f.id_factura;
 INSERT INTO movimiento_cuenta(id_cuenta,id_factura,tipo_movimiento,origen,importe,descripcion)
 VALUES(f.id_cuenta,f.id_factura,'DEBITO','FACTURA',v_total,'Emision de factura')
 ON CONFLICT DO NOTHING;
 RETURN f.id_factura;
END $$;

CREATE OR REPLACE FUNCTION tp_logistica.validar_pago()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE f factura%ROWTYPE; v_pagado numeric(16,2);
BEGIN
 SELECT * INTO f FROM factura WHERE id_factura=NEW.id_factura FOR UPDATE;
 IF NOT FOUND OR f.estado NOT IN ('EMITIDA','PARCIAL') THEN
  RAISE EXCEPTION 'La factura no permite nuevos pagos'; END IF;
 IF f.id_cuenta<>NEW.id_cuenta THEN RAISE EXCEPTION 'Pago vinculado a cuenta diferente'; END IF;
 SELECT COALESCE(SUM(monto),0) INTO v_pagado FROM pago WHERE id_factura=NEW.id_factura;
 IF v_pagado+NEW.monto>f.importe_total THEN
  RAISE EXCEPTION 'Pago excede saldo pendiente de la factura'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER pago_guard BEFORE INSERT ON pago FOR EACH ROW EXECUTE FUNCTION validar_pago();
CREATE OR REPLACE FUNCTION tp_logistica.pago_inmutable()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN RAISE EXCEPTION 'Pago confirmado: use operacion de reversa controlada'; END $$;
CREATE TRIGGER pago_inmutable_guard BEFORE UPDATE OR DELETE ON pago
 FOR EACH ROW EXECUTE FUNCTION pago_inmutable();
CREATE OR REPLACE FUNCTION tp_logistica.imputar_pago()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_total numeric(16,2); v_pagado numeric(16,2);
BEGIN
 INSERT INTO movimiento_cuenta(id_cuenta,id_factura,id_pago,tipo_movimiento,origen,importe,descripcion)
 VALUES(NEW.id_cuenta,NEW.id_factura,NEW.id_pago,'CREDITO','PAGO',NEW.monto,'Pago de factura');
 SELECT importe_total INTO v_total FROM factura WHERE id_factura=NEW.id_factura;
 SELECT SUM(monto) INTO v_pagado FROM pago WHERE id_factura=NEW.id_factura;
 UPDATE factura SET estado=CASE WHEN v_pagado>=v_total THEN 'PAGADA' ELSE 'PARCIAL' END
 WHERE id_factura=NEW.id_factura;
 RETURN NULL;
END $$;
CREATE TRIGGER pago_imputacion AFTER INSERT ON pago FOR EACH ROW EXECUTE FUNCTION imputar_pago();
CREATE OR REPLACE FUNCTION tp_logistica.registrar_pago(
 p_id_factura uuid,p_monto numeric,p_medio_pago varchar,p_clave_idempotencia varchar,
 p_referencia_externa varchar DEFAULT NULL)
RETURNS uuid LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_id uuid; v_cuenta uuid;
BEGIN
 IF p_clave_idempotencia IS NULL OR length(btrim(p_clave_idempotencia))=0 THEN
  RAISE EXCEPTION 'Clave idempotente obligatoria'; END IF;
 SELECT id_cuenta INTO v_cuenta FROM factura WHERE id_factura=p_id_factura FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'Factura inexistente'; END IF;
 SELECT id_pago INTO v_id FROM pago WHERE clave_idempotencia=p_clave_idempotencia
   AND id_factura=p_id_factura AND monto=p_monto AND medio_pago=p_medio_pago;
 IF FOUND THEN RETURN v_id; END IF;
 IF EXISTS(SELECT 1 FROM pago WHERE clave_idempotencia=p_clave_idempotencia) THEN
  RAISE EXCEPTION 'Clave de pago reutilizada con datos diferentes'; END IF;
 INSERT INTO pago(id_factura,id_cuenta,monto,medio_pago,clave_idempotencia,referencia_externa)
 VALUES(p_id_factura,v_cuenta,p_monto,p_medio_pago,p_clave_idempotencia,p_referencia_externa)
 ON CONFLICT (clave_idempotencia) DO NOTHING RETURNING id_pago INTO v_id;
 IF v_id IS NULL THEN
  SELECT id_pago INTO v_id FROM pago WHERE clave_idempotencia=p_clave_idempotencia
    AND id_factura=p_id_factura AND monto=p_monto AND medio_pago=p_medio_pago;
  IF v_id IS NULL THEN RAISE EXCEPTION 'Pago duplicado con distintos parametros'; END IF;
 END IF;
 RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION tp_logistica.facturar_envio(
 p_id_envio uuid,p_id_tarifa uuid,p_clave_idempotencia varchar)
RETURNS uuid LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE e envio%ROWTYPE; t tarifa_servicio%ROWTYPE; v_cuenta uuid; v_id uuid;
BEGIN
 IF p_clave_idempotencia IS NULL OR length(btrim(p_clave_idempotencia))=0 THEN
  RAISE EXCEPTION 'Clave idempotente obligatoria'; END IF;
 SELECT id_factura INTO v_id FROM factura WHERE clave_idempotencia=p_clave_idempotencia
  AND id_envio=p_id_envio;
 IF FOUND THEN RETURN v_id; END IF;
 IF EXISTS(SELECT 1 FROM factura WHERE clave_idempotencia=p_clave_idempotencia) THEN
  RAISE EXCEPTION 'Clave de factura reutilizada con otro envio'; END IF;
 SELECT * INTO e FROM envio WHERE id_envio=p_id_envio;
 IF NOT FOUND OR e.estado='CANCELADO' THEN RAISE EXCEPTION 'Envio inexistente/cancelado'; END IF;
 SELECT * INTO t FROM tarifa_servicio WHERE id_tarifa=p_id_tarifa AND activa
  AND vigencia_desde<=current_date AND (vigencia_hasta IS NULL OR vigencia_hasta>=current_date)
  AND tipo_servicio IN ('TRANSPORTE','SERVICIO') AND criterio_calculo='FIJO' AND moneda='ARS';
 IF NOT FOUND THEN RAISE EXCEPTION 'Tarifa no vigente o no compatible'; END IF;
 SELECT id_cuenta INTO v_cuenta FROM cuenta_corriente WHERE id_cliente=e.id_cliente;
 IF NOT FOUND THEN RAISE EXCEPTION 'Cuenta corriente del cliente inexistente'; END IF;
 INSERT INTO factura(id_cuenta,id_cliente,id_envio,naturaleza,clave_idempotencia)
 VALUES(v_cuenta,e.id_cliente,e.id_envio,'COBRAR',p_clave_idempotencia)
 ON CONFLICT (clave_idempotencia) DO NOTHING RETURNING id_factura INTO v_id;
 IF v_id IS NULL THEN
  SELECT id_factura INTO v_id FROM factura WHERE clave_idempotencia=p_clave_idempotencia
    AND id_envio=p_id_envio;
  IF v_id IS NULL THEN RAISE EXCEPTION 'Clave de factura incompatible'; END IF;
  RETURN v_id;
 END IF;
 INSERT INTO factura_detalle(id_factura,concepto,cantidad,precio_unitario)
 VALUES(v_id,t.concepto,1,t.precio);
 PERFORM emitir_factura(v_id);
 RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION tp_logistica.facturar_reporte(
 p_id_solicitud uuid,p_id_tarifa uuid,p_clave_idempotencia varchar)
RETURNS uuid LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE s solicitud_reporte%ROWTYPE; t tarifa_servicio%ROWTYPE; v_cuenta uuid; v_id uuid;
BEGIN
 SELECT * INTO s FROM solicitud_reporte WHERE id_solicitud=p_id_solicitud FOR UPDATE;
 IF NOT FOUND OR s.estado NOT IN ('GENERADO','FACTURADO','ENTREGADO') THEN
  RAISE EXCEPTION 'Reporte no generado o solicitud inexistente'; END IF;
 IF s.id_factura IS NOT NULL THEN RETURN s.id_factura; END IF;
 SELECT * INTO t FROM tarifa_servicio WHERE id_tarifa=p_id_tarifa AND activa
  AND tipo_servicio='REPORTE' AND criterio_calculo='FIJO' AND moneda='ARS'
  AND vigencia_desde<=current_date AND (vigencia_hasta IS NULL OR vigencia_hasta>=current_date);
 IF NOT FOUND THEN RAISE EXCEPTION 'Tarifa de reporte no vigente'; END IF;
 SELECT id_cuenta INTO v_cuenta FROM cuenta_corriente WHERE id_cliente=s.id_cliente;
 IF NOT FOUND THEN RAISE EXCEPTION 'Cuenta de cliente inexistente'; END IF;
 INSERT INTO factura(id_cuenta,id_cliente,naturaleza,clave_idempotencia)
 VALUES(v_cuenta,s.id_cliente,'COBRAR',p_clave_idempotencia)
 ON CONFLICT (clave_idempotencia) DO NOTHING RETURNING id_factura INTO v_id;
 IF v_id IS NULL THEN
  SELECT id_factura INTO v_id FROM factura WHERE clave_idempotencia=p_clave_idempotencia;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM factura_detalle WHERE id_factura=v_id) THEN
  INSERT INTO factura_detalle(id_factura,concepto,cantidad,precio_unitario)
  VALUES(v_id,t.concepto,1,t.precio);
 END IF;
 PERFORM emitir_factura(v_id);
 UPDATE solicitud_reporte SET id_factura=v_id,precio=t.precio,estado='FACTURADO'
 WHERE id_solicitud=p_id_solicitud;
 RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION tp_logistica.cerrar_cuenta(p_id_cuenta uuid,p_periodo date)
RETURNS uuid LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_inicio numeric(16,2); v_debitos numeric(16,2); v_creditos numeric(16,2); v_id uuid;
BEGIN
 IF p_periodo<>date_trunc('month',p_periodo::timestamp)::date THEN
  RAISE EXCEPTION 'Periodo debe ser primer dia del mes'; END IF;
 PERFORM 1 FROM cuenta_corriente WHERE id_cuenta=p_id_cuenta FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'Cuenta inexistente'; END IF;
 IF EXISTS(SELECT 1 FROM cierre_cuenta WHERE id_cuenta=p_id_cuenta AND periodo>=p_periodo) THEN
  RAISE EXCEPTION 'Periodo ya cerrado o existe cierre posterior'; END IF;
 SELECT COALESCE(SUM(CASE WHEN tipo_movimiento='DEBITO' THEN importe ELSE -importe END),0)
 INTO v_inicio FROM movimiento_cuenta WHERE id_cuenta=p_id_cuenta
  AND fecha < (p_periodo::timestamp AT TIME ZONE 'UTC');
 SELECT COALESCE(SUM(importe) FILTER (WHERE tipo_movimiento='DEBITO'),0),
        COALESCE(SUM(importe) FILTER (WHERE tipo_movimiento='CREDITO'),0)
 INTO v_debitos,v_creditos FROM movimiento_cuenta
 WHERE id_cuenta=p_id_cuenta AND fecha >= (p_periodo::timestamp AT TIME ZONE 'UTC')
   AND fecha < ((p_periodo+INTERVAL '1 month') AT TIME ZONE 'UTC');
 INSERT INTO cierre_cuenta(id_cuenta,periodo,saldo_inicial,total_debitos,total_creditos,saldo_final)
 VALUES(p_id_cuenta,p_periodo,v_inicio,v_debitos,v_creditos,v_inicio+v_debitos-v_creditos)
 RETURNING id_cierre INTO v_id;
 RETURN v_id;
END $$;
CREATE OR REPLACE FUNCTION tp_logistica.cierre_inmutable()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
BEGIN RAISE EXCEPTION 'Cierre mensual inmutable: requiere reapertura autorizada'; END $$;
CREATE TRIGGER cierre_guard BEFORE UPDATE OR DELETE ON cierre_cuenta
 FOR EACH ROW EXECUTE FUNCTION cierre_inmutable();

-- Trazabilidad de cambios; nunca auditar password_hash ni tokens.
CREATE OR REPLACE FUNCTION tp_logistica.auditar_cambio()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_id uuid; v_antes jsonb; v_despues jsonb; v_actor uuid; v_corr uuid;
BEGIN
 v_antes:=CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN to_jsonb(OLD) ELSE NULL END;
 v_despues:=CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN to_jsonb(NEW) ELSE NULL END;
 v_id:=COALESCE(NULLIF(v_despues->>TG_ARGV[0], '')::uuid, NULLIF(v_antes->>TG_ARGV[0], '')::uuid);
 v_actor:=NULLIF(current_setting('app.usuario_id',true),'')::uuid;
 v_corr:=NULLIF(current_setting('app.correlacion_id',true),'')::uuid;
 INSERT INTO auditoria_operacion(tabla,operacion,id_entidad,id_usuario,id_correlacion,anterior,nuevo)
 VALUES(TG_TABLE_NAME,TG_OP,v_id,v_actor,v_corr,v_antes,v_despues);
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER aud_cliente AFTER INSERT OR UPDATE OR DELETE ON cliente FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_cliente');
CREATE TRIGGER aud_envio AFTER INSERT OR UPDATE OR DELETE ON envio FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_envio');
CREATE TRIGGER aud_tramo AFTER INSERT OR UPDATE OR DELETE ON tramo FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_tramo');
CREATE TRIGGER aud_incidente AFTER INSERT OR UPDATE OR DELETE ON incidente FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_incidente');
CREATE TRIGGER aud_factura AFTER INSERT OR UPDATE OR DELETE ON factura FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_factura');
CREATE TRIGGER aud_pago AFTER INSERT OR UPDATE OR DELETE ON pago FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_pago');
CREATE TRIGGER aud_mov AFTER INSERT ON movimiento_cuenta FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_movimiento');
CREATE TRIGGER aud_cierre AFTER INSERT ON cierre_cuenta FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_cierre');
CREATE TRIGGER aud_regla AFTER INSERT OR UPDATE OR DELETE ON regla_alerta FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_regla');
CREATE TRIGGER aud_reporte AFTER INSERT OR UPDATE OR DELETE ON solicitud_reporte FOR EACH ROW EXECUTE FUNCTION auditar_cambio('id_solicitud');

-- Outbox: escribir en la misma transaccion. Un worker externo envia a Neo4j/Mongo.
CREATE OR REPLACE FUNCTION tp_logistica.publicar_outbox()
RETURNS trigger LANGUAGE plpgsql SET search_path=tp_logistica,public AS $$
DECLARE v_payload jsonb; v_id uuid;
BEGIN
 v_payload:=CASE WHEN TG_OP='DELETE' THEN to_jsonb(OLD) ELSE to_jsonb(NEW) END;
 v_id:=(v_payload->>TG_ARGV[0])::uuid;
 INSERT INTO outbox_evento(agregado_tipo,agregado_id,tipo_evento,payload,id_correlacion)
 VALUES(TG_TABLE_NAME,v_id,upper(TG_TABLE_NAME)||'_'||TG_OP,v_payload,
   NULLIF(current_setting('app.correlacion_id',true),'')::uuid);
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER out_cliente AFTER INSERT OR UPDATE OR DELETE ON cliente FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_cliente');
CREATE TRIGGER out_operador AFTER INSERT OR UPDATE OR DELETE ON operador_logistico FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_operador');
CREATE TRIGGER out_proveedor AFTER INSERT OR UPDATE OR DELETE ON proveedor FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_proveedor');
CREATE TRIGGER out_vehiculo AFTER INSERT OR UPDATE OR DELETE ON vehiculo FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_vehiculo');
CREATE TRIGGER out_ubicacion AFTER INSERT OR UPDATE OR DELETE ON ubicacion FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_ubicacion');
CREATE TRIGGER out_envio AFTER INSERT OR UPDATE OR DELETE ON envio FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_envio');
CREATE TRIGGER out_tramo AFTER INSERT OR UPDATE OR DELETE ON tramo FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_tramo');
CREATE TRIGGER out_incidente AFTER INSERT OR UPDATE OR DELETE ON incidente FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_incidente');
CREATE TRIGGER out_contenedor AFTER INSERT OR UPDATE OR DELETE ON contenedor FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_contenedor');
CREATE TRIGGER out_sensor AFTER INSERT OR UPDATE OR DELETE ON sensor_iot FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_sensor');
CREATE TRIGGER out_asig_sensor AFTER INSERT OR UPDATE OR DELETE ON asignacion_sensor_contenedor FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_asignacion');
-- Para envio_contenedor falta PK individual; se usa id_envio como agregado.
CREATE TRIGGER out_asig_envio AFTER INSERT OR UPDATE OR DELETE ON envio_contenedor FOR EACH ROW EXECUTE FUNCTION publicar_outbox('id_envio');
COMMIT;
