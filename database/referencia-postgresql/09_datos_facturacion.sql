-- Datos para probar facturación a mano: titulares en cada situación posible.
-- Se cargan con scripts/cargar-datos-facturacion.ps1, aparte de la carga inicial: las pruebas
-- automáticas comparan totales del dataset original. Usa las mismas funciones que la aplicación
-- (emitir_factura, registrar_pago, cerrar_cuenta), así respeta todas las reglas contables.
SET search_path = tp_logistica, public;

CREATE FUNCTION pg_temp.cliente(p_n int, p_nombre text, p_cuit text) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE v_id uuid := ('fac00000-0000-4000-8000-0000000000' || lpad(p_n::text, 2, '0'))::uuid; v_cuenta uuid;
BEGIN
 INSERT INTO cliente(id_cliente, razon_social, cuit, pais, estado) VALUES (v_id, p_nombre, p_cuit, 'Argentina', 'ACTIVO');
 INSERT INTO cuenta_corriente(id_cliente, naturaleza) VALUES (v_id, 'COBRAR') RETURNING id_cuenta INTO v_cuenta;
 RETURN v_cuenta;
END $$;

CREATE FUNCTION pg_temp.proveedor(p_n int, p_nombre text, p_cuit text) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE v_id uuid := ('fac00000-0000-4000-8000-0000000001' || lpad(p_n::text, 2, '0'))::uuid; v_cuenta uuid;
BEGIN
 INSERT INTO proveedor(id_proveedor, razon_social, cuit, pais, tipo_servicio, estado)
 VALUES (v_id, p_nombre, p_cuit, 'Argentina', 'Servicios de prueba', 'ACTIVO');
 INSERT INTO cuenta_corriente(id_proveedor, naturaleza) VALUES (v_id, 'PAGAR') RETURNING id_cuenta INTO v_cuenta;
 RETURN v_cuenta;
END $$;

CREATE FUNCTION pg_temp.operador(p_n int, p_nombre text, p_cuit text) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE v_id uuid := ('fac00000-0000-4000-8000-0000000002' || lpad(p_n::text, 2, '0'))::uuid; v_cuenta uuid;
BEGIN
 INSERT INTO operador_logistico(id_operador, razon_social, cuit, pais, estado) VALUES (v_id, p_nombre, p_cuit, 'Argentina', 'ACTIVO');
 INSERT INTO cuenta_corriente(id_operador, naturaleza) VALUES (v_id, 'PAGAR') RETURNING id_cuenta INTO v_cuenta;
 RETURN v_cuenta;
END $$;

-- Crea la factura en borrador; emitir_factura es un paso aparte, como en la aplicación.
CREATE FUNCTION pg_temp.borrador(p_cuenta uuid, p_clave text, p_concepto text, p_monto numeric) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE c cuenta_corriente%ROWTYPE; v_id uuid;
BEGIN
 SELECT * INTO c FROM cuenta_corriente WHERE id_cuenta = p_cuenta;
 INSERT INTO factura(id_cuenta, id_cliente, id_proveedor, id_operador, fecha_vencimiento, naturaleza, clave_idempotencia, estado)
 VALUES (p_cuenta, c.id_cliente, c.id_proveedor, c.id_operador, current_date + 30, c.naturaleza, p_clave, 'BORRADOR')
 RETURNING id_factura INTO v_id;
 INSERT INTO factura_detalle(id_factura, concepto, cantidad, precio_unitario) VALUES (v_id, p_concepto, 1, p_monto);
 RETURN v_id;
END $$;

CREATE FUNCTION pg_temp.emitida(p_cuenta uuid, p_clave text, p_concepto text, p_monto numeric) RETURNS uuid LANGUAGE plpgsql AS $$
BEGIN
 RETURN emitir_factura(pg_temp.borrador(p_cuenta, p_clave, p_concepto, p_monto));
END $$;

CREATE FUNCTION pg_temp.ajuste(p_cuenta uuid, p_tipo text, p_monto numeric, p_descripcion text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO movimiento_cuenta(id_cuenta, tipo_movimiento, origen, importe, descripcion)
 VALUES (p_cuenta, p_tipo, 'AJUSTE', p_monto, p_descripcion);
END $$;

DO $$
DECLARE v_cuenta uuid; v_factura uuid;
BEGIN
 IF EXISTS (SELECT 1 FROM cliente WHERE cuit = '33700000007') THEN
  RAISE NOTICE 'Los datos de facturación ya estaban cargados';
  RETURN;
 END IF;

 -- Clientes
 PERFORM pg_temp.cliente(1, 'Prueba Fact 01 Sin movimientos', '33700000007');

 v_cuenta := pg_temp.cliente(2, 'Prueba Fact 02 Saldo sin facturas', '33700000015');
 PERFORM pg_temp.ajuste(v_cuenta, 'DEBITO', 25000, 'Saldo inicial de prueba');

 v_cuenta := pg_temp.cliente(3, 'Prueba Fact 03 Factura en borrador', '33700000023');
 PERFORM pg_temp.borrador(v_cuenta, 'PRUEBA-FACT-03', 'Flete sin emitir', 50000);

 v_cuenta := pg_temp.cliente(4, 'Prueba Fact 04 Factura emitida', '33700000031');
 PERFORM pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-04', 'Flete internacional', 120000);

 v_cuenta := pg_temp.cliente(5, 'Prueba Fact 05 Pago parcial', '33700000058');
 v_factura := pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-05', 'Flete marítimo', 80000);
 PERFORM registrar_pago(v_factura, 30000, 'TRANSFERENCIA', 'PRUEBA-PAGO-05');

 v_cuenta := pg_temp.cliente(6, 'Prueba Fact 06 Factura pagada', '33700000066');
 v_factura := pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-06', 'Flete terrestre', 45000);
 PERFORM registrar_pago(v_factura, 45000, 'TARJETA', 'PRUEBA-PAGO-06');

 v_cuenta := pg_temp.cliente(7, 'Prueba Fact 07 Varias facturas', '33700000074');
 PERFORM pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-07A', 'Servicio A', 10000);
 v_factura := pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-07B', 'Servicio B', 20000);
 PERFORM registrar_pago(v_factura, 5000, 'EFECTIVO', 'PRUEBA-PAGO-07B');
 v_factura := pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-07C', 'Servicio C', 15000);
 PERFORM registrar_pago(v_factura, 15000, 'TRANSFERENCIA', 'PRUEBA-PAGO-07C');
 PERFORM pg_temp.borrador(v_cuenta, 'PRUEBA-FACT-07D', 'Servicio D', 5000);

 v_cuenta := pg_temp.cliente(8, 'Prueba Fact 08 Saldo a favor', '33700000082');
 PERFORM pg_temp.ajuste(v_cuenta, 'CREDITO', 10000, 'Saldo a favor de prueba');

 v_cuenta := pg_temp.cliente(9, 'Prueba Fact 09 Inactivo con saldo', '33700000090');
 PERFORM pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-09', 'Flete antes de la baja', 30000);
 UPDATE cliente SET estado = 'INACTIVO' WHERE id_cliente = 'fac00000-0000-4000-8000-000000000009';

 -- El borrador queda sin emitir: emitirlo tiene que chocar con el mes ya cerrado.
 v_cuenta := pg_temp.cliente(10, 'Prueba Fact 10 Mes cerrado', '33700000104');
 PERFORM pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-10A', 'Flete del mes', 12000);
 PERFORM pg_temp.borrador(v_cuenta, 'PRUEBA-FACT-10B', 'Flete fuera de término', 8000);
 PERFORM cerrar_cuenta(v_cuenta, date_trunc('month', now() AT TIME ZONE 'UTC')::date);

 -- Proveedores: la cuenta es a pagar.
 PERFORM pg_temp.proveedor(1, 'Prueba Fact Proveedor 01 Sin facturas', '33700000112');

 v_cuenta := pg_temp.proveedor(2, 'Prueba Fact Proveedor 02 Factura emitida', '33700000120');
 PERFORM pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-P02', 'Servicio de depósito', 70000);

 v_cuenta := pg_temp.proveedor(3, 'Prueba Fact Proveedor 03 Pago parcial', '33700000139');
 v_factura := pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-P03', 'Servicio de aduana', 40000);
 PERFORM registrar_pago(v_factura, 15000, 'TRANSFERENCIA', 'PRUEBA-PAGO-P03');

 -- Operadores: la cuenta es a pagar.
 v_cuenta := pg_temp.operador(1, 'Prueba Fact Operador 01 Factura emitida', '33700000147');
 PERFORM pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-O01', 'Transporte tercerizado', 90000);

 v_cuenta := pg_temp.operador(2, 'Prueba Fact Operador 02 Factura pagada', '33700000155');
 v_factura := pg_temp.emitida(v_cuenta, 'PRUEBA-FACT-O02', 'Transporte tercerizado', 35000);
 PERFORM registrar_pago(v_factura, 35000, 'TRANSFERENCIA', 'PRUEBA-PAGO-O02');
END $$;
