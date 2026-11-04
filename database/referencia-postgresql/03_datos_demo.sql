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
