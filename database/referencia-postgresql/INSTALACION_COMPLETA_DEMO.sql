-- INSTALACION INTEGRAL PARA SUPABASE / POSTGRESQL NUEVO.
-- Incluye datos artificiales y pruebas de aceptacion. Solo desarrollo.
-- Si falla cualquier bloque, revisar error y recrear base limpia antes de reejecutar.


-- ======================================================================
-- ARCHIVO: 00_esquema.sql
-- ======================================================================

-- TP Ingenieria de Datos II - Grupo 4
-- PostgreSQL 15+ / Supabase. Ejecutar primero, en base vacia.
-- Persistencia relacional: no guardar lecturas continuas ni mensajes aqui.
BEGIN;
CREATE SCHEMA IF NOT EXISTS tp_logistica;
SET LOCAL search_path = tp_logistica, public;
CREATE SEQUENCE IF NOT EXISTS tp_logistica.factura_numero_seq START WITH 1;
-- Verificacion basica de estructura y digito verificador del CUIT argentino.
CREATE OR REPLACE FUNCTION tp_logistica.cuit_valido(p_cuit text)
RETURNS boolean LANGUAGE plpgsql IMMUTABLE SET search_path=tp_logistica,public AS $$
DECLARE pesos integer[]:=ARRAY[5,4,3,2,7,6,5,4,3,2];
 v_suma integer:=0; v_digito integer;
BEGIN
 IF p_cuit IS NULL OR p_cuit !~ '^[0-9]{11}$' THEN RETURN false; END IF;
 FOR i IN 1..10 LOOP
  v_suma:=v_suma+substring(p_cuit FROM i FOR 1)::integer*pesos[i];
 END LOOP;
 v_digito:=11-(v_suma%11);
 IF v_digito=11 THEN v_digito:=0; END IF;
 RETURN v_digito<10 AND v_digito=substring(p_cuit FROM 11 FOR 1)::integer;
END $$;

-- 1. Maestros y acceso
CREATE TABLE cliente (
 id_cliente uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 razon_social varchar(160) NOT NULL,
 cuit varchar(11) UNIQUE,
 email varchar(254), telefono varchar(40), direccion varchar(250),
 pais varchar(80) NOT NULL, estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
 fecha_alta timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT cliente_cuit_ck CHECK (cuit IS NULL OR tp_logistica.cuit_valido(cuit)),
 CONSTRAINT cliente_estado_ck CHECK (estado IN ('ACTIVO','INACTIVO'))
);
CREATE UNIQUE INDEX cliente_email_unico ON cliente(lower(email)) WHERE email IS NOT NULL;

CREATE TABLE proveedor (
 id_proveedor uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 razon_social varchar(160) NOT NULL, cuit varchar(11) UNIQUE,
 email varchar(254), telefono varchar(40), direccion varchar(250),
 pais varchar(80) NOT NULL, tipo_servicio varchar(100),
 estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
 CONSTRAINT proveedor_cuit_ck CHECK (cuit IS NULL OR tp_logistica.cuit_valido(cuit)),
 CONSTRAINT proveedor_estado_ck CHECK (estado IN ('ACTIVO','INACTIVO'))
);
CREATE TABLE operador_logistico (
 id_operador uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 razon_social varchar(160) NOT NULL, cuit varchar(11) UNIQUE,
 email varchar(254), telefono varchar(40), direccion varchar(250),
 pais varchar(80) NOT NULL, estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
 CONSTRAINT operador_cuit_ck CHECK (cuit IS NULL OR tp_logistica.cuit_valido(cuit)),
 CONSTRAINT operador_estado_ck CHECK (estado IN ('ACTIVO','INACTIVO'))
);
CREATE TABLE rol (
 id_rol uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 descripcion varchar(80) NOT NULL UNIQUE,
 nivel_permisos smallint NOT NULL CHECK (nivel_permisos BETWEEN 0 AND 100)
);
CREATE TABLE permiso (
 id_permiso uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 codigo varchar(80) NOT NULL UNIQUE,
 descripcion varchar(200) NOT NULL
);
CREATE TABLE rol_permiso (
 id_rol uuid NOT NULL REFERENCES rol(id_rol) ON DELETE CASCADE,
 id_permiso uuid NOT NULL REFERENCES permiso(id_permiso) ON DELETE CASCADE,
 PRIMARY KEY(id_rol,id_permiso)
);
CREATE TABLE usuario (
 id_usuario uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_cliente uuid REFERENCES cliente(id_cliente) ON DELETE RESTRICT,
 id_proveedor uuid REFERENCES proveedor(id_proveedor) ON DELETE RESTRICT,
 id_operador uuid REFERENCES operador_logistico(id_operador) ON DELETE RESTRICT,
 nombre varchar(100) NOT NULL, apellido varchar(100) NOT NULL,
 email varchar(254) NOT NULL, password_hash text NOT NULL,
 estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
 fecha_registro timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT usuario_entidad_ck CHECK (num_nonnulls(id_cliente,id_proveedor,id_operador) <= 1),
 CONSTRAINT usuario_estado_ck CHECK (estado IN ('ACTIVO','BLOQUEADO','INACTIVO'))
);
CREATE UNIQUE INDEX usuario_email_unico ON usuario(lower(email));
CREATE TABLE usuario_rol (
 id_usuario uuid NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
 id_rol uuid NOT NULL REFERENCES rol(id_rol) ON DELETE RESTRICT,
 fecha_asignacion timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY (id_usuario,id_rol)
);
CREATE TABLE sesion (
 id_sesion uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_usuario uuid NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
 token_hash varchar(128) NOT NULL UNIQUE,
 fecha_inicio timestamptz NOT NULL DEFAULT now(),
 fecha_expiracion timestamptz NOT NULL,
 fecha_revocacion timestamptz,
 fecha_fin timestamptz,
 estado varchar(20) NOT NULL DEFAULT 'ACTIVA',
 CONSTRAINT sesion_estado_ck CHECK (estado IN ('ACTIVA','REVOCADA','CERRADA')),
 CONSTRAINT sesion_fechas_ck CHECK (fecha_expiracion > fecha_inicio AND
   (fecha_revocacion IS NULL OR fecha_revocacion >= fecha_inicio) AND
   (fecha_fin IS NULL OR fecha_fin >= fecha_inicio) AND
   (estado<>'REVOCADA' OR fecha_revocacion IS NOT NULL) AND
   (estado<>'CERRADA' OR fecha_fin IS NOT NULL))
);
CREATE INDEX sesion_usuario_estado_idx ON sesion(id_usuario,estado,fecha_expiracion);

-- 2. Logistica y trazabilidad temporal
CREATE TABLE ubicacion (
 id_ubicacion uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 codigo varchar(40) NOT NULL UNIQUE, nombre varchar(150) NOT NULL,
 pais varchar(80) NOT NULL, tipo varchar(40) NOT NULL DEFAULT 'PUERTO'
);
CREATE TABLE vehiculo (
 id_vehiculo uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_operador uuid NOT NULL REFERENCES operador_logistico(id_operador),
 tipo varchar(30) NOT NULL, identificacion varchar(80) NOT NULL UNIQUE,
 estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
 CONSTRAINT vehiculo_estado_ck CHECK (estado IN ('ACTIVO','INACTIVO','MANTENIMIENTO'))
);
CREATE TABLE envio (
 id_envio uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_cliente uuid NOT NULL REFERENCES cliente(id_cliente),
 origen varchar(180) NOT NULL, destino varchar(180) NOT NULL,
 pais_origen varchar(80) NOT NULL, pais_destino varchar(80) NOT NULL,
 fecha_creacion timestamptz NOT NULL DEFAULT now(),
 fecha_entrega_estimada timestamptz,
 fecha_entrega_real timestamptz,
 estado varchar(25) NOT NULL DEFAULT 'CREADO',
 prioridad varchar(15) NOT NULL DEFAULT 'NORMAL',
 volumen_m3 numeric(14,3) NOT NULL DEFAULT 0,
 CONSTRAINT envio_estado_ck CHECK (estado IN ('CREADO','EN_PREPARACION','EN_TRANSITO','DEMORADO','ENTREGADO','CANCELADO')),
 CONSTRAINT envio_prioridad_ck CHECK (prioridad IN ('BAJA','NORMAL','ALTA','URGENTE')),
 CONSTRAINT envio_volumen_ck CHECK (volumen_m3 >= 0),
 CONSTRAINT envio_fechas_ck CHECK ((fecha_entrega_real IS NULL OR fecha_entrega_real >= fecha_creacion)
  AND (estado <> 'ENTREGADO' OR fecha_entrega_real IS NOT NULL))
);
CREATE INDEX envio_cliente_fecha_idx ON envio(id_cliente,fecha_creacion DESC);
CREATE INDEX envio_estado_fecha_idx ON envio(estado,fecha_creacion DESC);
CREATE INDEX envio_paises_idx ON envio(pais_destino,pais_origen);
CREATE TABLE contenedor (
 id_contenedor uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 codigo_internacional varchar(60) NOT NULL UNIQUE,
 tipo varchar(60) NOT NULL, capacidad numeric(14,3) NOT NULL CHECK (capacidad > 0),
 estado varchar(25) NOT NULL DEFAULT 'DISPONIBLE',
 CONSTRAINT contenedor_estado_ck CHECK (estado IN ('DISPONIBLE','EN_USO','MANTENIMIENTO','INACTIVO'))
);
CREATE TABLE envio_contenedor (
 id_envio uuid NOT NULL REFERENCES envio(id_envio),
 id_contenedor uuid NOT NULL REFERENCES contenedor(id_contenedor),
 fecha_asociacion timestamptz NOT NULL DEFAULT now(),
 fecha_desasociacion timestamptz,
 PRIMARY KEY (id_envio,id_contenedor,fecha_asociacion),
 CONSTRAINT envio_contenedor_fechas_ck CHECK (fecha_desasociacion IS NULL OR fecha_desasociacion > fecha_asociacion)
);
CREATE INDEX envio_contenedor_activo_idx ON envio_contenedor(id_contenedor) WHERE fecha_desasociacion IS NULL;
CREATE TABLE sensor_iot (
 id_sensor uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 tipo varchar(40) NOT NULL, fabricante varchar(100),
 fecha_instalacion timestamptz,
 estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
 CONSTRAINT sensor_estado_ck CHECK (estado IN ('ACTIVO','INACTIVO','MANTENIMIENTO','FALLA'))
);
CREATE TABLE asignacion_sensor_contenedor (
 id_asignacion uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_sensor uuid NOT NULL REFERENCES sensor_iot(id_sensor),
 id_contenedor uuid NOT NULL REFERENCES contenedor(id_contenedor),
 fecha_inicio timestamptz NOT NULL DEFAULT now(), fecha_fin timestamptz,
 CONSTRAINT sensor_asignacion_fecha_ck CHECK (fecha_fin IS NULL OR fecha_fin > fecha_inicio)
);
CREATE INDEX asig_sensor_idx ON asignacion_sensor_contenedor(id_sensor,fecha_inicio DESC);
CREATE INDEX asig_contenedor_idx ON asignacion_sensor_contenedor(id_contenedor,fecha_inicio DESC);
CREATE TABLE tramo (
 id_tramo uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_envio uuid NOT NULL REFERENCES envio(id_envio),
 id_vehiculo uuid REFERENCES vehiculo(id_vehiculo),
 id_operador uuid REFERENCES operador_logistico(id_operador),
 numero_tramo integer NOT NULL CHECK (numero_tramo > 0),
 origen varchar(180) NOT NULL, destino varchar(180) NOT NULL,
 id_ubicacion_origen uuid REFERENCES ubicacion(id_ubicacion),
 id_ubicacion_destino uuid REFERENCES ubicacion(id_ubicacion),
 medio_transporte varchar(20) NOT NULL,
 fecha_salida timestamptz,
 fecha_llegada_estimada timestamptz, fecha_llegada_real timestamptz,
 costo_estimado numeric(16,2) CHECK (costo_estimado >= 0),
 costo_real numeric(16,2) CHECK (costo_real >= 0),
 estado varchar(20) NOT NULL DEFAULT 'PENDIENTE',
 UNIQUE (id_envio,numero_tramo),
 CONSTRAINT tramo_medio_ck CHECK (medio_transporte IN ('MARITIMO','AEREO','TERRESTRE')),
 CONSTRAINT tramo_estado_ck CHECK (estado IN ('PENDIENTE','EN_CURSO','FINALIZADO','CANCELADO')),
 CONSTRAINT tramo_fechas_ck CHECK (fecha_llegada_real IS NULL OR fecha_salida IS NULL OR fecha_llegada_real >= fecha_salida)
);
CREATE INDEX tramo_envio_orden_idx ON tramo(id_envio,numero_tramo);
CREATE TABLE tramo_proveedor (
 id_tramo uuid NOT NULL REFERENCES tramo(id_tramo),
 id_proveedor uuid NOT NULL REFERENCES proveedor(id_proveedor),
 servicio_prestado varchar(120) NOT NULL,
 costo_servicio numeric(16,2) NOT NULL CHECK (costo_servicio >= 0),
 PRIMARY KEY (id_tramo,id_proveedor,servicio_prestado)
);
CREATE TABLE incidente (
 id_incidente uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_envio uuid NOT NULL REFERENCES envio(id_envio),
 id_tramo uuid REFERENCES tramo(id_tramo),
 id_evento_externo uuid UNIQUE, -- id de evento registrado en MongoDB
 tipo varchar(80) NOT NULL, severidad varchar(15) NOT NULL,
 descripcion text, fecha timestamptz NOT NULL DEFAULT now(),
 estado varchar(20) NOT NULL DEFAULT 'ABIERTO',
 CONSTRAINT incidente_severidad_ck CHECK (severidad IN ('BAJA','MEDIA','ALTA','CRITICA')),
 CONSTRAINT incidente_estado_ck CHECK (estado IN ('ABIERTO','EN_GESTION','CERRADO'))
);
CREATE INDEX incidente_envio_fecha_idx ON incidente(id_envio,fecha DESC);
CREATE INDEX incidente_tramo_fecha_idx ON incidente(id_tramo,fecha DESC);
CREATE INDEX incidente_estado_severidad_idx ON incidente(estado,severidad);
CREATE TABLE regla_alerta (
 id_regla uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_cliente uuid REFERENCES cliente(id_cliente),
 id_sensor uuid REFERENCES sensor_iot(id_sensor),
 nombre varchar(140) NOT NULL,
 tipo_regla varchar(30) NOT NULL,
 operador varchar(4) NOT NULL DEFAULT '>', umbral numeric(16,4),
 minutos_inactividad integer,
 severidad varchar(15) NOT NULL DEFAULT 'MEDIA',
 activa boolean NOT NULL DEFAULT true,
 fecha_creacion timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT regla_tipo_ck CHECK (tipo_regla IN ('TEMPERATURA','HUMEDAD','BATERIA','INACTIVIDAD','VIBRACION','ANOMALIA_TEMPERATURA')),
 CONSTRAINT regla_operador_ck CHECK (operador IN ('>','>=','<','<=','=')),
 CONSTRAINT regla_severidad_ck CHECK (severidad IN ('BAJA','MEDIA','ALTA','CRITICA')),
 CONSTRAINT regla_umbral_ck CHECK ((tipo_regla='INACTIVIDAD' AND minutos_inactividad > 0 AND umbral IS NULL)
  OR (tipo_regla<>'INACTIVIDAD' AND umbral IS NOT NULL AND minutos_inactividad IS NULL))
);
CREATE INDEX regla_alerta_activas_idx ON regla_alerta(tipo_regla,id_sensor) WHERE activa;

-- 3. Finanzas (un titular: cliente, proveedor u operador)
CREATE TABLE cuenta_corriente (
 id_cuenta uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_cliente uuid REFERENCES cliente(id_cliente),
 id_proveedor uuid REFERENCES proveedor(id_proveedor),
 id_operador uuid REFERENCES operador_logistico(id_operador),
 naturaleza varchar(10) NOT NULL,
 saldo numeric(16,2) NOT NULL DEFAULT 0,
 fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT cuenta_titular_ck CHECK (num_nonnulls(id_cliente,id_proveedor,id_operador)=1),
 CONSTRAINT cuenta_naturaleza_ck CHECK ((id_cliente IS NOT NULL AND naturaleza='COBRAR')
  OR (id_cliente IS NULL AND naturaleza='PAGAR'))
);
CREATE UNIQUE INDEX cuenta_por_cliente_idx ON cuenta_corriente(id_cliente) WHERE id_cliente IS NOT NULL;
CREATE UNIQUE INDEX cuenta_por_proveedor_idx ON cuenta_corriente(id_proveedor) WHERE id_proveedor IS NOT NULL;
CREATE UNIQUE INDEX cuenta_por_operador_idx ON cuenta_corriente(id_operador) WHERE id_operador IS NOT NULL;
CREATE TABLE factura (
 id_factura uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 numero_factura varchar(60) UNIQUE,
 id_cuenta uuid NOT NULL REFERENCES cuenta_corriente(id_cuenta),
 id_cliente uuid REFERENCES cliente(id_cliente),
 id_proveedor uuid REFERENCES proveedor(id_proveedor),
 id_operador uuid REFERENCES operador_logistico(id_operador),
 id_envio uuid REFERENCES envio(id_envio),
 fecha_emision date NOT NULL DEFAULT current_date,
 fecha_vencimiento date,
 importe_total numeric(16,2) NOT NULL DEFAULT 0,
 naturaleza varchar(10) NOT NULL,
 estado varchar(20) NOT NULL DEFAULT 'BORRADOR',
 clave_idempotencia varchar(150) UNIQUE,
 CONSTRAINT factura_titular_ck CHECK (num_nonnulls(id_cliente,id_proveedor,id_operador)=1),
 CONSTRAINT factura_naturaleza_ck CHECK ((id_cliente IS NOT NULL AND naturaleza='COBRAR')
   OR (id_cliente IS NULL AND naturaleza='PAGAR')),
 CONSTRAINT factura_estado_ck CHECK (estado IN ('BORRADOR','EMITIDA','PARCIAL','PAGADA')),
 CONSTRAINT factura_importe_ck CHECK (importe_total >= 0),
 CONSTRAINT factura_numero_ck CHECK (estado='BORRADOR' OR numero_factura IS NOT NULL),
 CONSTRAINT factura_vencimiento_ck CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha_emision)
);
CREATE INDEX factura_cuenta_estado_idx ON factura(id_cuenta,estado,fecha_emision DESC);
CREATE INDEX factura_cliente_estado_idx ON factura(id_cliente,estado);
CREATE TABLE factura_detalle (
 id_detalle uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_factura uuid NOT NULL REFERENCES factura(id_factura) ON DELETE RESTRICT,
 concepto varchar(180) NOT NULL,
 cantidad numeric(14,3) NOT NULL CHECK (cantidad > 0),
 precio_unitario numeric(16,2) NOT NULL CHECK (precio_unitario >= 0),
 subtotal numeric(16,2) GENERATED ALWAYS AS (round(cantidad*precio_unitario,2)) STORED,
 id_tramo uuid REFERENCES tramo(id_tramo)
);
CREATE INDEX factura_detalle_factura_idx ON factura_detalle(id_factura);
CREATE TABLE pago (
 id_pago uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_factura uuid NOT NULL REFERENCES factura(id_factura),
 id_cuenta uuid NOT NULL REFERENCES cuenta_corriente(id_cuenta),
 fecha timestamptz NOT NULL DEFAULT now(),
 monto numeric(16,2) NOT NULL CHECK (monto > 0),
 medio_pago varchar(30) NOT NULL,
 referencia_externa varchar(150),
 clave_idempotencia varchar(150) NOT NULL UNIQUE,
 CONSTRAINT pago_medio_ck CHECK (medio_pago IN ('TRANSFERENCIA','TARJETA','EFECTIVO','OTRO'))
);
CREATE UNIQUE INDEX pago_referencia_externa_idx ON pago(referencia_externa) WHERE referencia_externa IS NOT NULL;
CREATE INDEX pago_factura_fecha_idx ON pago(id_factura,fecha DESC);
CREATE TABLE movimiento_cuenta (
 id_movimiento uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_cuenta uuid NOT NULL REFERENCES cuenta_corriente(id_cuenta),
 id_factura uuid REFERENCES factura(id_factura),
 id_pago uuid REFERENCES pago(id_pago),
 fecha timestamptz NOT NULL DEFAULT now(),
 tipo_movimiento varchar(10) NOT NULL,
 origen varchar(10) NOT NULL,
 descripcion text,
 importe numeric(16,2) NOT NULL CHECK (importe > 0),
 referencia varchar(100),
 CONSTRAINT movimiento_tipo_ck CHECK (tipo_movimiento IN ('DEBITO','CREDITO')),
 CONSTRAINT movimiento_origen_ck CHECK ((origen='FACTURA' AND tipo_movimiento='DEBITO' AND id_factura IS NOT NULL AND id_pago IS NULL)
   OR (origen='PAGO' AND tipo_movimiento='CREDITO' AND id_pago IS NOT NULL AND id_factura IS NOT NULL)
   OR (origen='AJUSTE' AND id_factura IS NULL AND id_pago IS NULL))
);
CREATE UNIQUE INDEX movimiento_factura_unico_idx ON movimiento_cuenta(id_factura) WHERE origen='FACTURA';
CREATE UNIQUE INDEX movimiento_pago_unico_idx ON movimiento_cuenta(id_pago) WHERE origen='PAGO';
CREATE INDEX movimiento_cuenta_fecha_idx ON movimiento_cuenta(id_cuenta,fecha DESC);
CREATE TABLE cierre_cuenta (
 id_cierre uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_cuenta uuid NOT NULL REFERENCES cuenta_corriente(id_cuenta),
 periodo date NOT NULL,
 saldo_inicial numeric(16,2) NOT NULL,
 total_debitos numeric(16,2) NOT NULL,
 total_creditos numeric(16,2) NOT NULL,
 saldo_final numeric(16,2) NOT NULL,
 fecha_cierre timestamptz NOT NULL DEFAULT now(),
 estado varchar(15) NOT NULL DEFAULT 'CERRADO',
 UNIQUE(id_cuenta,periodo),
 CONSTRAINT cierre_mes_ck CHECK (periodo = date_trunc('month',periodo::timestamp)::date),
 CONSTRAINT cierre_estado_ck CHECK (estado='CERRADO'),
 CONSTRAINT cierre_saldo_ck CHECK (saldo_final=saldo_inicial+total_debitos-total_creditos)
);
CREATE TABLE tarifa_servicio (
 id_tarifa uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 concepto varchar(180) NOT NULL,
 tipo_servicio varchar(20) NOT NULL,
 criterio_calculo varchar(40) NOT NULL DEFAULT 'FIJO',
 precio numeric(16,2) NOT NULL CHECK (precio >= 0),
 moneda varchar(3) NOT NULL DEFAULT 'ARS',
 vigencia_desde date NOT NULL, vigencia_hasta date,
 activa boolean NOT NULL DEFAULT true,
 CONSTRAINT tarifa_tipo_ck CHECK (tipo_servicio IN ('TRANSPORTE','SERVICIO','REPORTE')),
 CONSTRAINT tarifa_vigencia_ck CHECK (vigencia_hasta IS NULL OR vigencia_hasta >= vigencia_desde)
);
CREATE TABLE solicitud_reporte (
 id_solicitud uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_cliente uuid NOT NULL REFERENCES cliente(id_cliente),
 id_usuario uuid NOT NULL REFERENCES usuario(id_usuario),
 tipo_reporte varchar(80) NOT NULL,
 parametros jsonb NOT NULL DEFAULT '{}'::jsonb,
 estado varchar(20) NOT NULL DEFAULT 'SOLICITADO',
 precio numeric(16,2) NOT NULL DEFAULT 0 CHECK (precio >= 0),
 fecha_solicitud timestamptz NOT NULL DEFAULT now(),
 fecha_generacion timestamptz,
 id_factura uuid REFERENCES factura(id_factura),
 id_resultado_externo uuid,
 CONSTRAINT solicitud_params_ck CHECK (jsonb_typeof(parametros)='object'),
 CONSTRAINT solicitud_estado_ck CHECK (estado IN ('SOLICITADO','PROCESANDO','GENERADO','FACTURADO','ENTREGADO','ERROR'))
);
CREATE INDEX solicitud_cliente_fecha_idx ON solicitud_reporte(id_cliente,fecha_solicitud DESC);
CREATE TABLE prediccion_envio (
 id_prediccion uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 id_envio uuid NOT NULL REFERENCES envio(id_envio),
 id_solicitud uuid REFERENCES solicitud_reporte(id_solicitud),
 modelo varchar(100) NOT NULL,
 eta_predicha timestamptz NOT NULL,
 probabilidad_atraso numeric(6,5) NOT NULL CHECK (probabilidad_atraso BETWEEN 0 AND 1),
 variables jsonb NOT NULL DEFAULT '{}'::jsonb,
 fecha_prediccion timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX prediccion_envio_fecha_idx ON prediccion_envio(id_envio,fecha_prediccion DESC);

-- 4. Integracion, trazabilidad y reintentos
CREATE TABLE outbox_evento (
 id_evento uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 agregado_tipo varchar(60) NOT NULL,
 agregado_id uuid NOT NULL,
 tipo_evento varchar(100) NOT NULL,
 payload jsonb NOT NULL,
 clave_evento varchar(200) UNIQUE,
 id_correlacion uuid,
 estado varchar(20) NOT NULL DEFAULT 'PENDIENTE',
 intentos integer NOT NULL DEFAULT 0 CHECK (intentos >= 0),
 ultimo_error text,
 proximo_intento timestamptz,
 fecha_creacion timestamptz NOT NULL DEFAULT now(),
 fecha_procesamiento timestamptz,
 CONSTRAINT outbox_estado_ck CHECK (estado IN ('PENDIENTE','PROCESANDO','PROCESADO','ERROR'))
);
CREATE INDEX outbox_pendientes_idx ON outbox_evento(estado,proximo_intento,fecha_creacion) WHERE estado IN ('PENDIENTE','ERROR');
CREATE TABLE auditoria_operacion (
 id_auditoria uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 tabla varchar(100) NOT NULL, operacion varchar(10) NOT NULL,
 id_entidad uuid,
 id_usuario uuid, id_correlacion uuid,
 anterior jsonb, nuevo jsonb,
 fecha timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT auditoria_operacion_ck CHECK (operacion IN ('INSERT','UPDATE','DELETE'))
);
CREATE INDEX auditoria_entidad_fecha_idx ON auditoria_operacion(tabla,id_entidad,fecha DESC);
COMMIT;


-- ======================================================================
-- ARCHIVO: 01_reglas_y_procesos.sql
-- ======================================================================

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


-- ======================================================================
-- ARCHIVO: 02_vistas_y_analitica.sql
-- ======================================================================

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


-- ======================================================================
-- ARCHIVO: 03_datos_demo.sql
-- ======================================================================

-- Dataset sintetico reproducible - Grupo 4. SOLO ENTORNO DE PRUEBAS.
-- UUID fijos y compartibles: consultar ids_compartidos.csv.
-- Ejecutar UNA vez sobre esquema nuevo, despues de 00, 01 y 02.
BEGIN;
SET LOCAL search_path=tp_logistica,public;
SET LOCAL TIME ZONE 'UTC';

-- Roles, usuarios y terceros
-- Permisos explicitos para autorizacion en el backend.
INSERT INTO permiso(id_permiso,codigo,descripcion) VALUES
 ('aaaaaaaa-0000-4000-8000-000000000001','CLIENTES_VER','Ver clientes'),
 ('aaaaaaaa-0000-4000-8000-000000000002','ENVIOS_VER','Ver envios'),
 ('aaaaaaaa-0000-4000-8000-000000000003','ENVIOS_EDITAR','Modificar envios'),
 ('aaaaaaaa-0000-4000-8000-000000000004','FACTURAS_VER','Ver facturas'),
 ('aaaaaaaa-0000-4000-8000-000000000005','FACTURAS_EMITIR','Emitir facturas'),
 ('aaaaaaaa-0000-4000-8000-000000000006','REPORTES_SOLICITAR','Solicitar reportes');
INSERT INTO rol(id_rol,descripcion,nivel_permisos) VALUES ('ff310fce-0eaa-5b1f-aeeb-22491fbd09f2','ADMIN',100);
INSERT INTO rol(id_rol,descripcion,nivel_permisos) VALUES ('e8f69015-e01f-5f0d-b6f6-8c6232ab1fc7','OPERADOR',60);
INSERT INTO rol(id_rol,descripcion,nivel_permisos) VALUES ('abf16725-06fd-5fae-b53b-9e129a1d4308','CLIENTE',20);
INSERT INTO rol_permiso(id_rol,id_permiso)
 SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p WHERE r.descripcion='ADMIN';
INSERT INTO rol_permiso(id_rol,id_permiso)
 SELECT r.id_rol,p.id_permiso FROM rol r JOIN permiso p ON p.codigo IN ('ENVIOS_VER','ENVIOS_EDITAR','REPORTES_SOLICITAR') WHERE r.descripcion='OPERADOR';
INSERT INTO rol_permiso(id_rol,id_permiso)
 SELECT r.id_rol,p.id_permiso FROM rol r JOIN permiso p ON p.codigo IN ('ENVIOS_VER','FACTURAS_VER','REPORTES_SOLICITAR') WHERE r.descripcion='CLIENTE';
INSERT INTO cliente(id_cliente,razon_social,cuit,pais,email) VALUES ('0bca0b45-e7f6-59b2-a455-02191ee367f6','Transportes del Sur SA','30711222339','Argentina','acme@ejemplo.test');
INSERT INTO cliente(id_cliente,razon_social,cuit,pais,email) VALUES ('63f80ae3-f1d1-58eb-9e2b-6b166d0eba42','Importadora Atlas SA','30733222111','Argentina','atlas@ejemplo.test');
INSERT INTO cliente(id_cliente,razon_social,cuit,pais,email) VALUES ('9f02aa81-e5ab-5eff-9e4a-05db5564d09b','Exportaciones Rio SA','30744555600','Argentina','rio@ejemplo.test');
INSERT INTO cliente(id_cliente,razon_social,cuit,pais,email) VALUES ('e6b21d5f-5124-582d-b5c6-95e7cd3e598f','Comercio Norte SA','30755666773','Argentina','norte@ejemplo.test');
INSERT INTO proveedor(id_proveedor,razon_social,cuit,pais,tipo_servicio) VALUES ('686c38fe-59fb-5492-917b-cfb5a529657a','Naviera Pacifico SRL','30766555442','Argentina','Servicio logistico');
INSERT INTO proveedor(id_proveedor,razon_social,cuit,pais,tipo_servicio) VALUES ('3fdc51d2-de47-5ef1-99e5-c9e682165415','Aduana y Servicios SA','30700777886','Argentina','Servicio logistico');
INSERT INTO operador_logistico(id_operador,razon_social,cuit,pais) VALUES ('17268077-61c8-5740-b3eb-80c6ec372efa','Logistica Oceano SA','30788555101','Argentina');
INSERT INTO operador_logistico(id_operador,razon_social,cuit,pais) VALUES ('bccbc75a-f42b-5063-a3ab-c2664036d43a','Rutas Globales SA','30799555995','Argentina');
INSERT INTO usuario(id_usuario,id_cliente,nombre,apellido,email,password_hash) VALUES ('82a5a0ec-3fc3-5126-9ba3-366d00525da9',NULL,'Admin','Grupo4','admin@ejemplo.test','SOLO_DEMO_NO_UTILIZAR_COMO_PASSWORD');
INSERT INTO usuario(id_usuario,id_cliente,nombre,apellido,email,password_hash) VALUES ('235e98d6-554d-553c-bb1c-04b725cfd432',NULL,'Operador','Demo','operador@ejemplo.test','SOLO_DEMO_NO_UTILIZAR_COMO_PASSWORD');
INSERT INTO usuario(id_usuario,id_cliente,nombre,apellido,email,password_hash) VALUES ('66a14ddd-2244-5bae-8271-a2d88936ff8a','0bca0b45-e7f6-59b2-a455-02191ee367f6','Usuario','Acme','cliente@ejemplo.test','SOLO_DEMO_NO_UTILIZAR_COMO_PASSWORD');
INSERT INTO usuario_rol(id_usuario,id_rol) VALUES ('82a5a0ec-3fc3-5126-9ba3-366d00525da9','ff310fce-0eaa-5b1f-aeeb-22491fbd09f2');
INSERT INTO usuario_rol(id_usuario,id_rol) VALUES ('235e98d6-554d-553c-bb1c-04b725cfd432','e8f69015-e01f-5f0d-b6f6-8c6232ab1fc7');
INSERT INTO usuario_rol(id_usuario,id_rol) VALUES ('66a14ddd-2244-5bae-8271-a2d88936ff8a','abf16725-06fd-5fae-b53b-9e129a1d4308');
INSERT INTO sesion(id_sesion,id_usuario,token_hash,fecha_inicio,fecha_expiracion) VALUES ('c44a8aeb-e98f-55db-b1cb-055ac87c0b8c','82a5a0ec-3fc3-5126-9ba3-366d00525da9',repeat('a',64),now()-interval '1 hour',now()+interval '12 hours');
INSERT INTO sesion(id_sesion,id_usuario,token_hash,fecha_inicio,fecha_expiracion,fecha_revocacion,estado) VALUES ('1cec15d7-5f2e-58fa-8d5e-c24ba6ca5fa3','235e98d6-554d-553c-bb1c-04b725cfd432',repeat('b',64),now()-interval '3 hours',now()+interval '12 hours',now()-interval '1 hour','REVOCADA');

-- Ubicaciones, vehiculos y envios
INSERT INTO ubicacion(id_ubicacion,codigo,nombre,pais,tipo) VALUES ('cc4d398c-a19c-559b-811f-08096d6bc09a','ARBUE','Buenos Aires','Argentina','PUERTO');
INSERT INTO ubicacion(id_ubicacion,codigo,nombre,pais,tipo) VALUES ('3bc40947-a6af-5685-80a1-f22f79128f1b','BRSAN','Santos','Brasil','PUERTO');
INSERT INTO ubicacion(id_ubicacion,codigo,nombre,pais,tipo) VALUES ('aac065bc-32f2-54fc-82bd-b98b42da487a','ESVLC','Valencia','Espana','PUERTO');
INSERT INTO ubicacion(id_ubicacion,codigo,nombre,pais,tipo) VALUES ('b13105d7-21c1-591d-a1af-357491a78fee','ARROS','Rosario','Argentina','TERMINAL');
INSERT INTO vehiculo(id_vehiculo,id_operador,tipo,identificacion) VALUES ('cc63258d-2b55-590e-9510-9fad0bebfb9d','17268077-61c8-5740-b3eb-80c6ec372efa','BUQUE','IMO-TP-0001');
INSERT INTO vehiculo(id_vehiculo,id_operador,tipo,identificacion) VALUES ('c39ffc17-ec06-52a5-9e79-c4153ab55c31','bccbc75a-f42b-5063-a3ab-c2664036d43a','CAMION','CAM-TP-0042');
INSERT INTO envio(id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_creacion,fecha_entrega_estimada,fecha_entrega_real,estado,volumen_m3) VALUES ('e85c8847-5d47-5989-8ce9-69bee973e05c','0bca0b45-e7f6-59b2-a455-02191ee367f6','Buenos Aires','Valencia','Argentina','Espana',now()-interval '70 days',now()-interval '52 days',now()-interval '54 days','ENTREGADO',21.5);
INSERT INTO envio(id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_creacion,fecha_entrega_estimada,fecha_entrega_real,estado,volumen_m3) VALUES ('9f647b96-098b-5443-87a7-43906b8e6544','0bca0b45-e7f6-59b2-a455-02191ee367f6','Buenos Aires','Santos','Argentina','Brasil',now()-interval '55 days',now()-interval '45 days',now()-interval '42 days','ENTREGADO',15.0);
INSERT INTO envio(id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_creacion,fecha_entrega_estimada,fecha_entrega_real,estado,volumen_m3) VALUES ('7700def4-818c-5c2b-9697-c5a4c4d69882','63f80ae3-f1d1-58eb-9e2b-6b166d0eba42','Santos','Buenos Aires','Brasil','Argentina',now()-interval '37 days',now()-interval '28 days',now()-interval '29 days','ENTREGADO',43.2);
INSERT INTO envio(id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_creacion,fecha_entrega_estimada,fecha_entrega_real,estado,volumen_m3) VALUES ('3d90a50f-7e2a-53e6-b5c6-f7a1651a3969','0bca0b45-e7f6-59b2-a455-02191ee367f6','Buenos Aires','Valencia','Argentina','Espana',now()-interval '12 days',now()-interval '2 days',NULL,'EN_TRANSITO',18.7);
INSERT INTO envio(id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_creacion,fecha_entrega_estimada,fecha_entrega_real,estado,volumen_m3) VALUES ('2f8e6cd6-3688-5d0c-8050-9f63b17bcf4f','63f80ae3-f1d1-58eb-9e2b-6b166d0eba42','Buenos Aires','Santos','Argentina','Brasil',now()-interval '4 days',now()-interval '-7 days',NULL,'EN_TRANSITO',9.1);
INSERT INTO envio(id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_creacion,fecha_entrega_estimada,fecha_entrega_real,estado,volumen_m3) VALUES ('c6372b1e-4954-5bb6-8899-f09e8a74e6e1','9f02aa81-e5ab-5eff-9e4a-05db5564d09b','Rosario','Buenos Aires','Argentina','Argentina',now()-interval '2 days',now()-interval '-1 days',NULL,'CREADO',13.3);
INSERT INTO envio(id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_creacion,fecha_entrega_estimada,fecha_entrega_real,estado,volumen_m3) VALUES ('83d9d1c3-385b-5f1d-9710-1fa643c73065','e6b21d5f-5124-582d-b5c6-95e7cd3e598f','Rosario','Valencia','Argentina','Espana',now()-interval '16 days',now()-interval '-4 days',NULL,'CANCELADO',6.4);
INSERT INTO contenedor(id_contenedor,codigo_internacional,tipo,capacidad,estado) VALUES ('4a15062a-049f-545e-b35b-911aefcc1d60','TPEU0000001','REFRIGERADO',30,'EN_USO');
INSERT INTO contenedor(id_contenedor,codigo_internacional,tipo,capacidad,estado) VALUES ('03746dc1-c77b-589a-b751-b144d4e80a93','TPEU0000002','REFRIGERADO',30,'EN_USO');
INSERT INTO contenedor(id_contenedor,codigo_internacional,tipo,capacidad,estado) VALUES ('56d3759c-8c8b-5e91-bfdb-0e1b3df31a35','TPEU0000003','REFRIGERADO',30,'EN_USO');
INSERT INTO envio_contenedor(id_envio,id_contenedor,fecha_asociacion,fecha_desasociacion) VALUES ('e85c8847-5d47-5989-8ce9-69bee973e05c','4a15062a-049f-545e-b35b-911aefcc1d60',now()-interval '69 days',now()-interval '53 days');
INSERT INTO envio_contenedor(id_envio,id_contenedor,fecha_asociacion,fecha_desasociacion) VALUES ('3d90a50f-7e2a-53e6-b5c6-f7a1651a3969','4a15062a-049f-545e-b35b-911aefcc1d60',now()-interval '11 days',NULL);
INSERT INTO envio_contenedor(id_envio,id_contenedor,fecha_asociacion,fecha_desasociacion) VALUES ('2f8e6cd6-3688-5d0c-8050-9f63b17bcf4f','03746dc1-c77b-589a-b751-b144d4e80a93',now()-interval '3 days',NULL);
INSERT INTO envio_contenedor(id_envio,id_contenedor,fecha_asociacion,fecha_desasociacion) VALUES ('7700def4-818c-5c2b-9697-c5a4c4d69882','56d3759c-8c8b-5e91-bfdb-0e1b3df31a35',now()-interval '36 days',now()-interval '29 days');
INSERT INTO sensor_iot(id_sensor,tipo,estado) VALUES ('cb6a673c-14a5-5469-a365-30fe9700c80b','TEMPERATURA','ACTIVO');
INSERT INTO sensor_iot(id_sensor,tipo,estado) VALUES ('18f369fe-9f0b-5ee1-8c0f-bf271eac908f','GPS','ACTIVO');
INSERT INTO sensor_iot(id_sensor,tipo,estado) VALUES ('a752087b-202f-56ff-9b1c-83ac9d6dc748','MULTISENSOR','ACTIVO');
INSERT INTO asignacion_sensor_contenedor(id_asignacion,id_sensor,id_contenedor,fecha_inicio,fecha_fin) VALUES ('88888888-0000-4000-8000-000000000003','a752087b-202f-56ff-9b1c-83ac9d6dc748','4a15062a-049f-545e-b35b-911aefcc1d60',now()-interval '45 days',now()-interval '25 days');
INSERT INTO asignacion_sensor_contenedor(id_asignacion,id_sensor,id_contenedor,fecha_inicio) VALUES ('7b602107-c03a-50be-b480-51738057e5ef','cb6a673c-14a5-5469-a365-30fe9700c80b','4a15062a-049f-545e-b35b-911aefcc1d60',now()-interval '20 days');
INSERT INTO asignacion_sensor_contenedor(id_asignacion,id_sensor,id_contenedor,fecha_inicio) VALUES ('66d6854a-aa8c-5757-a942-bffd3fdf1424','18f369fe-9f0b-5ee1-8c0f-bf271eac908f','4a15062a-049f-545e-b35b-911aefcc1d60',now()-interval '20 days');
INSERT INTO asignacion_sensor_contenedor(id_asignacion,id_sensor,id_contenedor,fecha_inicio) VALUES ('57d501e7-778c-5192-b25d-84f5ede339a0','a752087b-202f-56ff-9b1c-83ac9d6dc748','03746dc1-c77b-589a-b751-b144d4e80a93',now()-interval '20 days');

-- Tramos y servicios externos
INSERT INTO tramo(id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado) VALUES ('0f0b092f-414e-56fe-a4fa-6dffd2ff353e','e85c8847-5d47-5989-8ce9-69bee973e05c','cc63258d-2b55-590e-9510-9fad0bebfb9d','17268077-61c8-5740-b3eb-80c6ec372efa',1,'Buenos Aires','Valencia','cc4d398c-a19c-559b-811f-08096d6bc09a','aac065bc-32f2-54fc-82bd-b98b42da487a','MARITIMO',now()-interval '70 days',now()-interval '54 days',now()-interval '54 days',88000,88000,'FINALIZADO');
INSERT INTO tramo(id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado) VALUES ('7c8c15a2-ab73-59ac-9721-153ee2e830f0','9f647b96-098b-5443-87a7-43906b8e6544','cc63258d-2b55-590e-9510-9fad0bebfb9d','17268077-61c8-5740-b3eb-80c6ec372efa',1,'Buenos Aires','Santos','cc4d398c-a19c-559b-811f-08096d6bc09a','3bc40947-a6af-5685-80a1-f22f79128f1b','MARITIMO',now()-interval '54 days',now()-interval '42 days',now()-interval '42 days',50000,50000,'FINALIZADO');
INSERT INTO tramo(id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado) VALUES ('97a9ec9d-39a1-5b28-afae-73a3f36e8ba4','7700def4-818c-5c2b-9697-c5a4c4d69882','cc63258d-2b55-590e-9510-9fad0bebfb9d','17268077-61c8-5740-b3eb-80c6ec372efa',1,'Santos','Buenos Aires','3bc40947-a6af-5685-80a1-f22f79128f1b','cc4d398c-a19c-559b-811f-08096d6bc09a','MARITIMO',now()-interval '36 days',now()-interval '29 days',now()-interval '29 days',65000,65000,'FINALIZADO');
INSERT INTO tramo(id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado) VALUES ('fc93c640-4140-5243-bbb7-1230d41b9f81','3d90a50f-7e2a-53e6-b5c6-f7a1651a3969','c39ffc17-ec06-52a5-9e79-c4153ab55c31','bccbc75a-f42b-5063-a3ab-c2664036d43a',1,'Buenos Aires','Santos','cc4d398c-a19c-559b-811f-08096d6bc09a','3bc40947-a6af-5685-80a1-f22f79128f1b','TERRESTRE',now()-interval '11 days',now()-interval '9 days',now()-interval '9 days',32000,32000,'FINALIZADO');
INSERT INTO tramo(id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado) VALUES ('2f995106-f978-5828-bfbd-625d9450c30e','3d90a50f-7e2a-53e6-b5c6-f7a1651a3969','cc63258d-2b55-590e-9510-9fad0bebfb9d','17268077-61c8-5740-b3eb-80c6ec372efa',2,'Santos','Valencia','3bc40947-a6af-5685-80a1-f22f79128f1b','aac065bc-32f2-54fc-82bd-b98b42da487a','MARITIMO',now()-interval '9 days',NULL,NULL,81000,NULL,'EN_CURSO');
INSERT INTO tramo(id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado) VALUES ('5c7659a4-b379-568e-91c8-3caa3cbcb017','2f8e6cd6-3688-5d0c-8050-9f63b17bcf4f','c39ffc17-ec06-52a5-9e79-c4153ab55c31','bccbc75a-f42b-5063-a3ab-c2664036d43a',1,'Buenos Aires','Santos','cc4d398c-a19c-559b-811f-08096d6bc09a','3bc40947-a6af-5685-80a1-f22f79128f1b','TERRESTRE',now()-interval '3 days',NULL,NULL,41000,NULL,'EN_CURSO');
INSERT INTO tramo(id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado) VALUES ('b95a4f56-9c4e-5997-943a-acd49ee8d685','c6372b1e-4954-5bb6-8899-f09e8a74e6e1','c39ffc17-ec06-52a5-9e79-c4153ab55c31','bccbc75a-f42b-5063-a3ab-c2664036d43a',1,'Rosario','Buenos Aires','b13105d7-21c1-591d-a1af-357491a78fee','cc4d398c-a19c-559b-811f-08096d6bc09a','TERRESTRE',NULL,NULL,NULL,12000,NULL,'PENDIENTE');
INSERT INTO tramo(id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado) VALUES ('8cfbe0cd-9402-5db7-aa58-442de1755145','9f647b96-098b-5443-87a7-43906b8e6544',NULL,NULL,2,'Santos','Santos','3bc40947-a6af-5685-80a1-f22f79128f1b','3bc40947-a6af-5685-80a1-f22f79128f1b','AEREO',now()-interval '42 days',now()-interval '41 days',now()-interval '41 days',21000,21000,'FINALIZADO');
INSERT INTO tramo_proveedor(id_tramo,id_proveedor,servicio_prestado,costo_servicio) VALUES ('0f0b092f-414e-56fe-a4fa-6dffd2ff353e','3fdc51d2-de47-5ef1-99e5-c9e682165415','Despacho aduanero',9500);
INSERT INTO incidente(id_incidente,id_envio,id_tramo,id_evento_externo,tipo,severidad,descripcion,estado) VALUES ('9ce41cc5-9c38-562f-876d-ce69647e98ba','9f647b96-098b-5443-87a7-43906b8e6544','7c8c15a2-ab73-59ac-9721-153ee2e830f0','4067fbdb-ad9f-5333-82a3-0f7ba3955b39','DEMORA','ALTA','Incidente sintetico de demostracion','CERRADO');
INSERT INTO incidente(id_incidente,id_envio,id_tramo,id_evento_externo,tipo,severidad,descripcion,estado) VALUES ('28f22d34-f742-5720-9791-947f1f83e2db','3d90a50f-7e2a-53e6-b5c6-f7a1651a3969','2f995106-f978-5828-bfbd-625d9450c30e','36862117-0878-5e50-84a6-66dd8f6836eb','TEMPERATURA','CRITICA','Incidente sintetico de demostracion','ABIERTO');
INSERT INTO incidente(id_incidente,id_envio,id_tramo,id_evento_externo,tipo,severidad,descripcion,estado) VALUES ('32a90e14-3f69-5816-a46a-155622afc1af','3d90a50f-7e2a-53e6-b5c6-f7a1651a3969','2f995106-f978-5828-bfbd-625d9450c30e','55bc1306-6e33-5703-aa5d-82f04945788e','ADUANA','MEDIA','Incidente sintetico de demostracion','EN_GESTION');
INSERT INTO incidente(id_incidente,id_envio,id_tramo,id_evento_externo,tipo,severidad,descripcion,estado) VALUES ('e58d7b51-8051-5821-8bd7-0745becdec87','2f8e6cd6-3688-5d0c-8050-9f63b17bcf4f',NULL,'baa01a31-4f20-5bcd-a6be-23ac53174340','DESVIO','BAJA','Incidente sintetico de demostracion','ABIERTO');
INSERT INTO regla_alerta(id_regla,id_cliente,id_sensor,nombre,tipo_regla,operador,umbral,minutos_inactividad,severidad) VALUES ('aa1eda22-9177-5d64-92b7-57d0da6a7283','0bca0b45-e7f6-59b2-a455-02191ee367f6','cb6a673c-14a5-5469-a365-30fe9700c80b','Demo regla TEMPERATURA','TEMPERATURA','>',8,NULL,'ALTA');
INSERT INTO regla_alerta(id_regla,id_cliente,id_sensor,nombre,tipo_regla,operador,umbral,minutos_inactividad,severidad) VALUES ('2614d374-cafe-5687-b207-83528b6d5d98','0bca0b45-e7f6-59b2-a455-02191ee367f6','cb6a673c-14a5-5469-a365-30fe9700c80b','Demo regla BATERIA','BATERIA','<',20,NULL,'ALTA');
INSERT INTO regla_alerta(id_regla,id_cliente,id_sensor,nombre,tipo_regla,operador,umbral,minutos_inactividad,severidad) VALUES ('41b761ad-e950-5ffe-945a-ea330deaccd8','63f80ae3-f1d1-58eb-9e2b-6b166d0eba42','a752087b-202f-56ff-9b1c-83ac9d6dc748','Demo regla INACTIVIDAD','INACTIVIDAD','>',NULL,30,'ALTA');

-- Cuentas, tarifas, facturas y pagos; se prueban funciones transaccionales
INSERT INTO cuenta_corriente(id_cuenta,id_cliente,naturaleza) VALUES ('5738d7bc-1c31-58e4-a03c-b45f6a9beb17','0bca0b45-e7f6-59b2-a455-02191ee367f6','COBRAR');
INSERT INTO cuenta_corriente(id_cuenta,id_cliente,naturaleza) VALUES ('4c3d682f-da4e-5d6a-9065-f84490d7d453','63f80ae3-f1d1-58eb-9e2b-6b166d0eba42','COBRAR');
INSERT INTO cuenta_corriente(id_cuenta,id_proveedor,naturaleza) VALUES ('93ec1c2e-adcb-5cea-a988-139ad424c2d8','3fdc51d2-de47-5ef1-99e5-c9e682165415','PAGAR');
INSERT INTO cuenta_corriente(id_cuenta,id_operador,naturaleza) VALUES ('636a743b-abf2-55c3-a7fb-dec669ef966d','bccbc75a-f42b-5063-a3ab-c2664036d43a','PAGAR');
INSERT INTO tarifa_servicio(id_tarifa,concepto,tipo_servicio,precio,vigencia_desde) VALUES ('2a641623-abab-50dd-914b-e750f6630c88','Servicio transporte internacional','TRANSPORTE',100000,current_date-interval '1 year');
INSERT INTO tarifa_servicio(id_tarifa,concepto,tipo_servicio,precio,vigencia_desde) VALUES ('3d60ca0a-6a85-5957-84d5-d1993533ce12','Reporte analitico parametrizado','REPORTE',15000,current_date-interval '1 year');
SELECT facturar_envio('e85c8847-5d47-5989-8ce9-69bee973e05c','2a641623-abab-50dd-914b-e750f6630c88','demo-fact-envio-e1');
SELECT facturar_envio('3d90a50f-7e2a-53e6-b5c6-f7a1651a3969','2a641623-abab-50dd-914b-e750f6630c88','demo-fact-envio-e4');
SELECT registrar_pago((SELECT id_factura FROM factura WHERE clave_idempotencia='demo-fact-envio-e1'),40000,'TRANSFERENCIA','demo-pago-parcial-e1');
SELECT registrar_pago((SELECT id_factura FROM factura WHERE clave_idempotencia='demo-fact-envio-e1'),60000,'TRANSFERENCIA','demo-pago-restante-e1');
SELECT registrar_pago((SELECT id_factura FROM factura WHERE clave_idempotencia='demo-fact-envio-e4'),25000,'TRANSFERENCIA','demo-pago-parcial-e4');
INSERT INTO factura(id_factura,id_cuenta,id_proveedor,naturaleza,clave_idempotencia) VALUES ('8608e5c1-17a3-5d14-9973-c491a20393af','93ec1c2e-adcb-5cea-a988-139ad424c2d8','3fdc51d2-de47-5ef1-99e5-c9e682165415','PAGAR','demo-fact-proveedor');
INSERT INTO factura_detalle(id_factura,concepto,cantidad,precio_unitario) VALUES ('8608e5c1-17a3-5d14-9973-c491a20393af','Despacho aduanero tercerizado',1,18500);
SELECT emitir_factura('8608e5c1-17a3-5d14-9973-c491a20393af');
SELECT registrar_pago('8608e5c1-17a3-5d14-9973-c491a20393af',18500,'TRANSFERENCIA','demo-pago-prov');
INSERT INTO factura(id_factura,id_cuenta,id_operador,naturaleza,clave_idempotencia) VALUES ('abd9229e-7092-5261-8e24-653d2ed215f8','636a743b-abf2-55c3-a7fb-dec669ef966d','bccbc75a-f42b-5063-a3ab-c2664036d43a','PAGAR','demo-fact-operador');
INSERT INTO factura_detalle(id_factura,concepto,cantidad,precio_unitario) VALUES ('abd9229e-7092-5261-8e24-653d2ed215f8','Operacion terrestre tercerizada',1,35000);
SELECT emitir_factura('abd9229e-7092-5261-8e24-653d2ed215f8');

-- Solicitudes de reporte, facturacion y predicciones
INSERT INTO solicitud_reporte(id_solicitud,id_cliente,id_usuario,tipo_reporte,parametros,estado,fecha_generacion,id_resultado_externo) VALUES ('bbc8ed98-e17f-503d-8523-6cf5ee6217f8','0bca0b45-e7f6-59b2-a455-02191ee367f6','66a14ddd-2244-5bae-8271-a2d88936ff8a','ENVIOS','{"pais_destino":"Espana","estado":"EN_TRANSITO"}'::jsonb,'GENERADO',now(),'dd16c075-8a07-556b-9215-65690645f669');
SELECT facturar_reporte('bbc8ed98-e17f-503d-8523-6cf5ee6217f8','3d60ca0a-6a85-5957-84d5-d1993533ce12','demo-reporte-r1');
INSERT INTO solicitud_reporte(id_solicitud,id_cliente,id_usuario,tipo_reporte,parametros) VALUES ('f947d4ac-252e-5b2f-a17f-52062d978c30','63f80ae3-f1d1-58eb-9e2b-6b166d0eba42','82a5a0ec-3fc3-5126-9ba3-366d00525da9','COSTOS_TRANSPORTE','{"agrupacion":"medio_transporte"}'::jsonb);
SELECT generar_prediccion_baseline('3d90a50f-7e2a-53e6-b5c6-f7a1651a3969');

-- Movimiento historico y cierre mensual, independiente de movimientos actuales
INSERT INTO movimiento_cuenta(id_cuenta,fecha,tipo_movimiento,origen,importe,descripcion) VALUES ('4c3d682f-da4e-5d6a-9065-f84490d7d453',(date_trunc('month',now() AT TIME ZONE 'UTC')-interval '1 month'+interval '3 days') AT TIME ZONE 'UTC','DEBITO','AJUSTE',35000,'Cargo historico inicial');
INSERT INTO movimiento_cuenta(id_cuenta,fecha,tipo_movimiento,origen,importe,descripcion) VALUES ('4c3d682f-da4e-5d6a-9065-f84490d7d453',(date_trunc('month',now() AT TIME ZONE 'UTC')-interval '1 month'+interval '9 days') AT TIME ZONE 'UTC','CREDITO','AJUSTE',12000,'Cobro historico inicial');
SELECT cerrar_cuenta('4c3d682f-da4e-5d6a-9065-f84490d7d453',date_trunc('month',(now() AT TIME ZONE 'UTC')-interval '1 month')::date);
COMMIT;


-- ======================================================================
-- ARCHIVO: 04_pruebas_aceptacion.sql
-- ======================================================================

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
