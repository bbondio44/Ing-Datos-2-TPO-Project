-- Casos adversos: ejecutar CADA UNO por separado (deben arrojar ERROR).
-- Son verificaciones intencionalmente fallidas; no ejecutarlas dentro de un lote automatico.
SET search_path=tp_logistica,public;

-- N1 Duplicacion de CUIT: deben rechazarse en PostgreSQL.
-- INSERT INTO cliente(razon_social,cuit,pais) VALUES ('Duplicado','30711222339','Argentina');

-- N2 CUIT matematicamente invalido: debe fallar el CHECK cuit_valido.
-- INSERT INTO cliente(razon_social,cuit,pais) VALUES ('CUIT falso','30711222330','Argentina');

-- N3 Asignacion doble, mismo intervalo: trigger bloquea solapamiento.
-- INSERT INTO envio_contenedor(id_envio,id_contenedor,fecha_asociacion)
-- VALUES ((SELECT id_envio FROM envio WHERE origen='Rosario' AND estado='CREADO' LIMIT 1),
-- (SELECT id_contenedor FROM contenedor WHERE codigo_internacional='TPEU0000001'), now());

-- N4 Pago que excede saldo pendiente: debe ser rechazado.
-- SELECT registrar_pago((SELECT id_factura FROM factura WHERE clave_idempotencia='demo-fact-envio-e4'),
-- 999999,'TRANSFERENCIA','demo-sobrepago');

-- N5 Reapertura por SQL directo: cierre es inmutable (rechazo esperado).
-- UPDATE cierre_cuenta SET saldo_final=0;

-- N6 Cambio retroactivo de mes cerrado: rechazo esperado.
-- INSERT INTO movimiento_cuenta(id_cuenta,fecha,tipo_movimiento,origen,importe)
-- VALUES ((SELECT id_cuenta FROM cuenta_corriente cc JOIN cliente c ON c.id_cliente=cc.id_cliente
--          WHERE c.razon_social='Importadora Atlas SA'),
-- (date_trunc('month',now() AT TIME ZONE 'UTC')-interval '1 month'+interval '10 days') AT TIME ZONE 'UTC',
-- 'DEBITO','AJUSTE',100);

-- N7 Emision automatica repetida: NO debe generar una segunda factura (no es error).
-- SELECT facturar_envio((SELECT id_envio FROM envio WHERE estado='ENTREGADO' LIMIT 1),
--  (SELECT id_tarifa FROM tarifa_servicio WHERE tipo_servicio='TRANSPORTE' LIMIT 1),'demo-fact-envio-e1');
-- SELECT count(*) FROM factura WHERE clave_idempotencia='demo-fact-envio-e1'; -- 1
