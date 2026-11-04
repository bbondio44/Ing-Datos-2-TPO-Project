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
