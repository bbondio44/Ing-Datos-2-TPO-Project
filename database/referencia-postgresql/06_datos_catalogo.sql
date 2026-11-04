-- Contrato de carga PostgreSQL con catalogo_ids.csv. No modifica 03_datos_demo.sql.
-- Las asignaciones cubren [2026-10-20 00:00, 2026-10-23 00:00) UTC.
BEGIN;
SET LOCAL search_path=tp_logistica,public;
SET LOCAL TIME ZONE 'UTC';

INSERT INTO cliente(id_cliente,razon_social,cuit,email,pais,fecha_alta)
VALUES
 ('aef2750c-1592-5ed4-b7a7-ab5771efc165'::uuid, 'Frigorífico del Sur S.A.', '30500000003', 'cliente.c01@catalogo.test', 'Argentina', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('270823a9-7978-5eda-80f0-9ae673331b0b'::uuid, 'Lácteos Pampeanos S.A.', '30500000011', 'cliente.c02@catalogo.test', 'Argentina', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('e32b1f7c-90fe-5eca-935c-8062fca267ae'::uuid, 'Vinos de Cuyo S.R.L.', '30500000038', 'cliente.c03@catalogo.test', 'Argentina', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('07b5388d-ba2c-57c2-adbd-21e43c3f9c4c'::uuid, 'AgroExport Litoral S.A.', '30500000046', 'cliente.c04@catalogo.test', 'Argentina', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('a56aebe9-c263-5b49-bd7c-7a3654bf7861'::uuid, 'Farmacéutica Andina S.A.', '30500000054', 'cliente.c05@catalogo.test', 'Chile', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('c0b39af2-ed0c-56a5-99ae-7382599403ee'::uuid, 'Textil Oriental S.A.', '30500000062', 'cliente.c06@catalogo.test', 'Uruguay', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('23b37d19-2e70-5f15-b7c1-2aa4f3ea38d9'::uuid, 'Autopartes Mercosur S.A.', '30500000070', 'cliente.c07@catalogo.test', 'Brasil', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('e69fcda7-5ac8-5eb4-b731-e9154a6a607f'::uuid, 'Frutas del Valle S.A.', '30500000089', 'cliente.c08@catalogo.test', 'Argentina', TIMESTAMPTZ '2026-10-20T00:00:00Z');

INSERT INTO proveedor(id_proveedor,razon_social,cuit,email,pais,tipo_servicio)
VALUES
 ('6a74a60b-b5fc-5d40-8879-14cf5d7ccc90'::uuid, 'Seguros Carga Segura', '30500000097', 'proveedor.p01@catalogo.test', 'Argentina', 'Servicio logistico'),
 ('ec12ca06-a78a-5605-ab40-31988a417f61'::uuid, 'FrioTech Mantenimiento Reefer', '30500000100', 'proveedor.p02@catalogo.test', 'Argentina', 'Servicio logistico'),
 ('6c866005-e083-5a0a-b855-6f8e23aca2c3'::uuid, 'Despachante Aduanero Gómez', '30500000119', 'proveedor.p03@catalogo.test', 'Argentina', 'Servicio logistico'),
 ('1aeca0f8-8cd4-5a5e-8f87-fb9bdb20f39e'::uuid, 'Embalajes Industriales SRL', '30500000127', 'proveedor.p04@catalogo.test', 'Argentina', 'Servicio logistico'),
 ('899c5257-09ac-51ef-958e-a36a42ba6dc5'::uuid, 'IoT Sensores Patagonia', '30500000135', 'proveedor.p05@catalogo.test', 'Argentina', 'Servicio logistico'),
 ('c89fd6c8-12dc-5938-82cc-2746d57b28f7'::uuid, 'Depósito Fiscal Dock Sud', '30500000143', 'proveedor.p06@catalogo.test', 'Argentina', 'Servicio logistico'),
 ('cfe36932-be4f-5a57-ba2b-849d2ac26bf8'::uuid, 'Combustibles del Plata', '30500000151', 'proveedor.p07@catalogo.test', 'Argentina', 'Servicio logistico'),
 ('ff36ff95-7ed3-5940-bbc0-181762f657c3'::uuid, 'Consultora Logística Norte', '30500000178', 'proveedor.p08@catalogo.test', 'Argentina', 'Servicio logistico');

INSERT INTO operador_logistico(id_operador,razon_social,cuit,email,pais)
VALUES
 ('c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 'Naviera Atlántica S.A.', '30500000186', 'operador.o01@catalogo.test', 'Argentina'),
 ('5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 'Transportes Ruta 9 S.A.', '30500000194', 'operador.o02@catalogo.test', 'Argentina'),
 ('96ac1bd1-ea98-5326-ab6d-609f870b3e93'::uuid, 'AeroCargo Sudamericana', '30500000208', 'operador.o03@catalogo.test', 'Argentina'),
 ('21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 'Logística Cordillera S.A.', '30500000216', 'operador.o04@catalogo.test', 'Argentina'),
 ('4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 'Fletes del Litoral SRL', '30500000224', 'operador.o05@catalogo.test', 'Argentina');

INSERT INTO rol(descripcion,nivel_permisos) VALUES ('OPERACIONES',60),('PROVEEDOR',40) ON CONFLICT (descripcion) DO NOTHING;

INSERT INTO usuario(id_usuario,id_cliente,id_proveedor,id_operador,nombre,apellido,email,password_hash,fecha_registro)
VALUES
 ('aa621106-4bb0-59d5-aafd-48ca46e584cf'::uuid, NULL, NULL, NULL, 'Admin', 'Plataforma', 'u-admin@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('6810f791-1878-5de4-b0f6-7e63d996d4ac'::uuid, NULL, NULL, NULL, 'Lucía', 'Operaciones', 'u-ops@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('f68c3c80-1d10-5742-a526-db58c06e1790'::uuid, 'aef2750c-1592-5ed4-b7a7-ab5771efc165'::uuid, NULL, NULL, 'Martín', 'Frigorífico del Sur', 'u-c01@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('a841cc7a-599e-59a5-9a94-aca73ccae354'::uuid, '270823a9-7978-5eda-80f0-9ae673331b0b'::uuid, NULL, NULL, 'Sofía', 'Lácteos Pampeanos', 'u-c02@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('c5064550-9836-5ea7-907a-6a42e69a8c58'::uuid, NULL, NULL, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 'Diego', 'Naviera Atlántica', 'u-o01@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('ce4c9cee-103c-5a27-99a4-0d7cffa87635'::uuid, NULL, NULL, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 'Carla', 'Transportes Ruta 9', 'u-o02@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('ed4790a5-f2d1-5b87-aa7a-fdfac52d73a3'::uuid, NULL, 'ec12ca06-a78a-5605-ab40-31988a417f61'::uuid, NULL, 'Julián', 'FrioTech', 'u-p02@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('91b073d7-1124-59c9-912f-a7fc845c3aac'::uuid, NULL, '6c866005-e083-5a0a-b855-6f8e23aca2c3'::uuid, NULL, 'Ana', 'Despachante Gómez', 'u-p03@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('527a32d4-0d2d-565f-a516-8c29040deac3'::uuid, NULL, '899c5257-09ac-51ef-958e-a36a42ba6dc5'::uuid, NULL, 'Bruno', 'IoT Sensores', 'u-p05@catalogo.test', '$2a$12$0cpQ9EXjMZLciznp7BdsxeXv.lhQAWGuXvfNquCYrFRBPK8baD9Bi', TIMESTAMPTZ '2026-10-20T00:00:00Z');

INSERT INTO usuario_rol(id_usuario,id_rol,fecha_asignacion)
SELECT asignacion.id_usuario, r.id_rol, TIMESTAMPTZ '2026-10-20T00:00:00Z'
FROM (VALUES
 ('aa621106-4bb0-59d5-aafd-48ca46e584cf'::uuid, 'ADMIN'),
 ('6810f791-1878-5de4-b0f6-7e63d996d4ac'::uuid, 'OPERACIONES'),
 ('f68c3c80-1d10-5742-a526-db58c06e1790'::uuid, 'CLIENTE'),
 ('a841cc7a-599e-59a5-9a94-aca73ccae354'::uuid, 'CLIENTE'),
 ('c5064550-9836-5ea7-907a-6a42e69a8c58'::uuid, 'OPERADOR'),
 ('ce4c9cee-103c-5a27-99a4-0d7cffa87635'::uuid, 'OPERADOR'),
 ('ed4790a5-f2d1-5b87-aa7a-fdfac52d73a3'::uuid, 'PROVEEDOR'),
 ('91b073d7-1124-59c9-912f-a7fc845c3aac'::uuid, 'PROVEEDOR'),
 ('527a32d4-0d2d-565f-a516-8c29040deac3'::uuid, 'PROVEEDOR')
) AS asignacion(id_usuario,descripcion)
JOIN rol r USING (descripcion);

INSERT INTO ubicacion(codigo,nombre,pais,tipo)
VALUES
 ('BUE', 'Buenos Aires', 'Argentina', 'TERMINAL'),
 ('PBA', 'Puerto de Buenos Aires', 'Argentina', 'PUERTO'),
 ('ROS', 'Rosario', 'Argentina', 'TERMINAL'),
 ('COR', 'Córdoba', 'Argentina', 'TERMINAL'),
 ('MZA', 'Mendoza', 'Argentina', 'TERMINAL'),
 ('EZE', 'Aeropuerto de Ezeiza', 'Argentina', 'AEROPUERTO'),
 ('SSZ', 'Puerto de Santos', 'Brasil', 'PUERTO'),
 ('MVD', 'Montevideo', 'Uruguay', 'PUERTO'),
 ('SCL', 'Santiago', 'Chile', 'TERMINAL'),
 ('VAP', 'Valparaíso', 'Chile', 'PUERTO'),
 ('ASU', 'Asunción', 'Paraguay', 'TERMINAL'),
 ('CLL', 'Callao', 'Perú', 'PUERTO'),
 ('MIA', 'Miami', 'Estados Unidos', 'TERMINAL');

INSERT INTO envio(id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_creacion,fecha_entrega_estimada,fecha_entrega_real,estado,prioridad,volumen_m3)
VALUES
 ('d72d0543-b3be-54c5-9248-ceb758d30add'::uuid, 'aef2750c-1592-5ed4-b7a7-ab5771efc165'::uuid, 'Buenos Aires', 'Puerto de Santos', 'Argentina', 'Brasil', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-25T16:00Z', NULL, 'EN_TRANSITO', 'ALTA', 135.400),
 ('ec22d634-96cd-5267-b7bd-accdce959d92'::uuid, '270823a9-7978-5eda-80f0-9ae673331b0b'::uuid, 'Córdoba', 'Puerto de Santos', 'Argentina', 'Brasil', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-26T12:00Z', NULL, 'EN_TRANSITO', 'ALTA', 135.400),
 ('506de4f8-5386-58b4-ad0d-2f851434aec6'::uuid, 'e32b1f7c-90fe-5eca-935c-8062fca267ae'::uuid, 'Mendoza', 'Valparaíso', 'Argentina', 'Chile', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-20T10:00Z', TIMESTAMPTZ '2026-10-20T10:00Z', 'ENTREGADO', 'NORMAL', 135.400),
 ('6378a987-ec3f-504e-80db-0a38c77ee116'::uuid, '07b5388d-ba2c-57c2-adbd-21e43c3f9c4c'::uuid, 'Rosario', 'Montevideo', 'Argentina', 'Uruguay', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-21T18:00Z', TIMESTAMPTZ '2026-10-21T18:00Z', 'ENTREGADO', 'NORMAL', 135.400),
 ('ecae19bc-e959-5b5d-bb35-be45c2e6c910'::uuid, 'a56aebe9-c263-5b49-bd7c-7a3654bf7861'::uuid, 'Buenos Aires', 'Santiago', 'Argentina', 'Chile', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-21T08:00Z', TIMESTAMPTZ '2026-10-21T08:00Z', 'ENTREGADO', 'ALTA', 66.400),
 ('98fc9f7b-15bc-5e20-94c8-b875df1ed8d6'::uuid, 'c0b39af2-ed0c-56a5-99ae-7382599403ee'::uuid, 'Buenos Aires', 'Montevideo', 'Argentina', 'Uruguay', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-21T22:00Z', TIMESTAMPTZ '2026-10-21T22:00Z', 'ENTREGADO', 'BAJA', 33.200),
 ('5bcda798-982d-5078-a904-acb7769b79dd'::uuid, '23b37d19-2e70-5f15-b7c1-2aa4f3ea38d9'::uuid, 'Córdoba', 'Asunción', 'Argentina', 'Paraguay', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-21T17:00Z', TIMESTAMPTZ '2026-10-21T17:00Z', 'ENTREGADO', 'NORMAL', 33.200),
 ('c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4'::uuid, 'e69fcda7-5ac8-5eb4-b731-e9154a6a607f'::uuid, 'Mendoza', 'Aeropuerto de Ezeiza', 'Argentina', 'Argentina', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-21T15:00Z', TIMESTAMPTZ '2026-10-21T15:00Z', 'ENTREGADO', 'ALTA', 33.200),
 ('23314655-0f49-57cf-aa62-6b86d1aa705b'::uuid, 'aef2750c-1592-5ed4-b7a7-ab5771efc165'::uuid, 'Buenos Aires', 'Miami', 'Argentina', 'Estados Unidos', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-22T04:00Z', TIMESTAMPTZ '2026-10-22T04:00Z', 'ENTREGADO', 'ALTA', 33.200),
 ('986c09d3-a6be-5353-a94d-ab683bd9434c'::uuid, '270823a9-7978-5eda-80f0-9ae673331b0b'::uuid, 'Rosario', 'Puerto de Santos', 'Argentina', 'Brasil', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-26T08:00Z', NULL, 'EN_TRANSITO', 'NORMAL', 33.200),
 ('6ee84809-f672-5b8d-87f1-7f395cdaa8ed'::uuid, 'e32b1f7c-90fe-5eca-935c-8062fca267ae'::uuid, 'Mendoza', 'Valparaíso', 'Argentina', 'Chile', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-21T08:00Z', TIMESTAMPTZ '2026-10-21T08:00Z', 'ENTREGADO', 'BAJA', 33.200),
 ('653dad7d-e412-585e-9a1b-c1c97c370d0c'::uuid, '07b5388d-ba2c-57c2-adbd-21e43c3f9c4c'::uuid, 'Rosario', 'Puerto de Santos', 'Argentina', 'Brasil', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-25T17:00Z', NULL, 'EN_TRANSITO', 'NORMAL', 33.200),
 ('dceca613-bcf0-57cf-9929-3d8f07522fe7'::uuid, 'a56aebe9-c263-5b49-bd7c-7a3654bf7861'::uuid, 'Córdoba', 'Callao', 'Argentina', 'Perú', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-22T16:00Z', TIMESTAMPTZ '2026-10-22T16:00Z', 'ENTREGADO', 'ALTA', 33.200),
 ('f6f08a03-2a92-5140-9846-0e2ef076db3d'::uuid, 'c0b39af2-ed0c-56a5-99ae-7382599403ee'::uuid, 'Buenos Aires', 'Montevideo', 'Argentina', 'Uruguay', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-22T18:00Z', TIMESTAMPTZ '2026-10-22T18:00Z', 'ENTREGADO', 'BAJA', 33.200),
 ('cdea56b0-4ef2-5338-8342-9a3444d56919'::uuid, '23b37d19-2e70-5f15-b7c1-2aa4f3ea38d9'::uuid, 'Rosario', 'Mendoza', 'Argentina', 'Argentina', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-21T11:00Z', TIMESTAMPTZ '2026-10-21T11:00Z', 'ENTREGADO', 'NORMAL', 33.200),
 ('7b77f577-b7f6-5cd0-b620-0fcfc4a97c53'::uuid, 'e69fcda7-5ac8-5eb4-b731-e9154a6a607f'::uuid, 'Mendoza', 'Valparaíso', 'Argentina', 'Chile', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-20T09:00Z', TIMESTAMPTZ '2026-10-20T09:00Z', 'ENTREGADO', 'NORMAL', 33.200),
 ('cbb962f0-a717-5f72-8213-1ea6a59dae3e'::uuid, 'aef2750c-1592-5ed4-b7a7-ab5771efc165'::uuid, 'Buenos Aires', 'Puerto de Santos', 'Argentina', 'Brasil', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-26T08:00Z', NULL, 'EN_TRANSITO', 'ALTA', 33.200),
 ('4770f530-1292-5b45-8bc1-bb7e26559b28'::uuid, '270823a9-7978-5eda-80f0-9ae673331b0b'::uuid, 'Córdoba', 'Asunción', 'Argentina', 'Paraguay', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-22T03:00Z', TIMESTAMPTZ '2026-10-22T03:00Z', 'ENTREGADO', 'BAJA', 33.200),
 ('d5b901e5-603a-585b-8e8b-fa02b491e825'::uuid, 'e32b1f7c-90fe-5eca-935c-8062fca267ae'::uuid, 'Mendoza', 'Puerto de Buenos Aires', 'Argentina', 'Argentina', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-21T12:00Z', TIMESTAMPTZ '2026-10-21T12:00Z', 'ENTREGADO', 'NORMAL', 33.200),
 ('c231e5a7-7434-5d4b-b8db-405b7f42be44'::uuid, '07b5388d-ba2c-57c2-adbd-21e43c3f9c4c'::uuid, 'Rosario', 'Miami', 'Argentina', 'Estados Unidos', TIMESTAMPTZ '2026-10-19T00:00:00Z', TIMESTAMPTZ '2026-10-22T06:00Z', TIMESTAMPTZ '2026-10-22T06:00Z', 'ENTREGADO', 'NORMAL', 33.200);

INSERT INTO tramo(id_tramo,id_envio,id_operador,numero_tramo,origen,destino,id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,estado)
SELECT datos.id_tramo,datos.id_envio,datos.id_operador,datos.numero_tramo,datos.origen,datos.destino,uo.id_ubicacion,ud.id_ubicacion,datos.medio_transporte,datos.fecha_salida,datos.fecha_llegada_estimada,datos.estado
FROM (VALUES
 ('22ff43b2-a1c6-5f83-ac06-0a4011c3d943'::uuid, 'd72d0543-b3be-54c5-9248-ceb758d30add'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 1, 'Buenos Aires', 'Puerto de Buenos Aires', 'BUE', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-19T18:00Z', TIMESTAMPTZ '2026-10-19T20:00Z', 'FINALIZADO'),
 ('e8151a73-77d5-56e2-bf25-38268b7bf295'::uuid, 'd72d0543-b3be-54c5-9248-ceb758d30add'::uuid, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 2, 'Puerto de Buenos Aires', 'Puerto de Santos', 'PBA', 'SSZ', 'MARITIMO', TIMESTAMPTZ '2026-10-20T06:00Z', TIMESTAMPTZ '2026-10-25T16:00Z', 'EN_CURSO'),
 ('aeec7c55-cc6e-5ae6-8044-9632f64fac74'::uuid, 'ec22d634-96cd-5267-b7bd-accdce959d92'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 1, 'Córdoba', 'Puerto de Buenos Aires', 'COR', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-19T22:00Z', TIMESTAMPTZ '2026-10-20T16:00Z', 'FINALIZADO'),
 ('832d03fd-5634-54bc-bbb2-b831c1d3bb52'::uuid, 'ec22d634-96cd-5267-b7bd-accdce959d92'::uuid, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 2, 'Puerto de Buenos Aires', 'Puerto de Santos', 'PBA', 'SSZ', 'MARITIMO', TIMESTAMPTZ '2026-10-21T02:00Z', TIMESTAMPTZ '2026-10-26T12:00Z', 'EN_CURSO'),
 ('521dd8bd-0f98-5e42-b8ff-e4b1723f986c'::uuid, '506de4f8-5386-58b4-ad0d-2f851434aec6'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 1, 'Mendoza', 'Santiago', 'MZA', 'SCL', 'TERRESTRE', TIMESTAMPTZ '2026-10-19T16:00Z', TIMESTAMPTZ '2026-10-19T21:00Z', 'FINALIZADO'),
 ('2a08ba84-7b43-5424-aa6e-76fbb0d7e7ff'::uuid, '506de4f8-5386-58b4-ad0d-2f851434aec6'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 2, 'Santiago', 'Valparaíso', 'SCL', 'VAP', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T07:00Z', TIMESTAMPTZ '2026-10-20T10:00Z', 'FINALIZADO'),
 ('a03ca0b0-0d03-5875-9291-5e1cd1a2ef87'::uuid, '6378a987-ec3f-504e-80db-0a38c77ee116'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 1, 'Rosario', 'Puerto de Buenos Aires', 'ROS', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T08:00Z', TIMESTAMPTZ '2026-10-20T16:00Z', 'FINALIZADO'),
 ('bc56de92-e405-5c2c-b6cf-c189645f32f6'::uuid, '6378a987-ec3f-504e-80db-0a38c77ee116'::uuid, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 2, 'Puerto de Buenos Aires', 'Montevideo', 'PBA', 'MVD', 'MARITIMO', TIMESTAMPTZ '2026-10-21T02:00Z', TIMESTAMPTZ '2026-10-21T18:00Z', 'FINALIZADO'),
 ('881df510-7542-50fd-a2cc-f4bbe1db7dee'::uuid, 'ecae19bc-e959-5b5d-bb35-be45c2e6c910'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 1, 'Buenos Aires', 'Mendoza', 'BUE', 'MZA', 'TERRESTRE', TIMESTAMPTZ '2026-10-19T14:00Z', TIMESTAMPTZ '2026-10-20T17:00Z', 'FINALIZADO'),
 ('46c78512-166d-5584-9358-28d9dd67bb85'::uuid, 'ecae19bc-e959-5b5d-bb35-be45c2e6c910'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 2, 'Mendoza', 'Santiago', 'MZA', 'SCL', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T03:00Z', TIMESTAMPTZ '2026-10-21T08:00Z', 'FINALIZADO'),
 ('0e0e2068-d4ee-5749-9bc2-3688386e625e'::uuid, '98fc9f7b-15bc-5e20-94c8-b875df1ed8d6'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 1, 'Buenos Aires', 'Puerto de Buenos Aires', 'BUE', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T18:00Z', TIMESTAMPTZ '2026-10-20T20:00Z', 'FINALIZADO'),
 ('1d914803-2c41-55ef-b2b0-981105b0db81'::uuid, '98fc9f7b-15bc-5e20-94c8-b875df1ed8d6'::uuid, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 2, 'Puerto de Buenos Aires', 'Montevideo', 'PBA', 'MVD', 'MARITIMO', TIMESTAMPTZ '2026-10-21T06:00Z', TIMESTAMPTZ '2026-10-21T22:00Z', 'FINALIZADO'),
 ('769346ac-d406-51ac-8f14-985679cfaeb5'::uuid, '5bcda798-982d-5078-a904-acb7769b79dd'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 1, 'Córdoba', 'Rosario', 'COR', 'ROS', 'TERRESTRE', TIMESTAMPTZ '2026-10-19T20:00Z', TIMESTAMPTZ '2026-10-20T06:00Z', 'FINALIZADO'),
 ('c09375ab-c708-57d4-b799-197a59043538'::uuid, '5bcda798-982d-5078-a904-acb7769b79dd'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 2, 'Rosario', 'Asunción', 'ROS', 'ASU', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T16:00Z', TIMESTAMPTZ '2026-10-21T17:00Z', 'FINALIZADO'),
 ('a5f8a76a-a5ad-5a0e-a311-f01d71122fbe'::uuid, 'c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 1, 'Mendoza', 'Buenos Aires', 'MZA', 'BUE', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T00:00Z', TIMESTAMPTZ '2026-10-21T03:00Z', 'FINALIZADO'),
 ('e9050c1b-1160-529a-8259-e72989da689a'::uuid, 'c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 2, 'Buenos Aires', 'Aeropuerto de Ezeiza', 'BUE', 'EZE', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T13:00Z', TIMESTAMPTZ '2026-10-21T15:00Z', 'FINALIZADO'),
 ('700ccfa7-2500-5352-a827-713d61fb4a19'::uuid, '23314655-0f49-57cf-aa62-6b86d1aa705b'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 1, 'Buenos Aires', 'Aeropuerto de Ezeiza', 'BUE', 'EZE', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T04:00Z', TIMESTAMPTZ '2026-10-21T06:00Z', 'FINALIZADO'),
 ('6cf16c11-01b1-5b46-aaf1-fa7d56d354f1'::uuid, '23314655-0f49-57cf-aa62-6b86d1aa705b'::uuid, '96ac1bd1-ea98-5326-ab6d-609f870b3e93'::uuid, 2, 'Aeropuerto de Ezeiza', 'Miami', 'EZE', 'MIA', 'AEREO', TIMESTAMPTZ '2026-10-21T11:00Z', TIMESTAMPTZ '2026-10-22T04:00Z', 'FINALIZADO'),
 ('beb0be6f-8128-5b77-b89f-05d8c270eef8'::uuid, '986c09d3-a6be-5353-a94d-ab683bd9434c'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 1, 'Rosario', 'Puerto de Buenos Aires', 'ROS', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T04:00Z', TIMESTAMPTZ '2026-10-20T12:00Z', 'FINALIZADO'),
 ('93e65e0a-7c8f-50a1-a85b-7128e7c5dc4a'::uuid, '986c09d3-a6be-5353-a94d-ab683bd9434c'::uuid, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 2, 'Puerto de Buenos Aires', 'Puerto de Santos', 'PBA', 'SSZ', 'MARITIMO', TIMESTAMPTZ '2026-10-20T22:00Z', TIMESTAMPTZ '2026-10-26T08:00Z', 'EN_CURSO'),
 ('f4a96996-c5a2-5b37-bc2b-308ff9529662'::uuid, '6ee84809-f672-5b8d-87f1-7f395cdaa8ed'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 1, 'Mendoza', 'Santiago', 'MZA', 'SCL', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T14:00Z', TIMESTAMPTZ '2026-10-20T19:00Z', 'FINALIZADO'),
 ('c647901c-2afe-5ef8-9333-4be47788079c'::uuid, '6ee84809-f672-5b8d-87f1-7f395cdaa8ed'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 2, 'Santiago', 'Valparaíso', 'SCL', 'VAP', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T05:00Z', TIMESTAMPTZ '2026-10-21T08:00Z', 'FINALIZADO'),
 ('0ef18837-cea0-5ee7-8133-6c57bb9d2e01'::uuid, '653dad7d-e412-585e-9a1b-c1c97c370d0c'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 1, 'Rosario', 'Puerto de Buenos Aires', 'ROS', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-19T13:00Z', TIMESTAMPTZ '2026-10-19T21:00Z', 'FINALIZADO'),
 ('0fafb0aa-1426-53ca-be25-f376fda66da7'::uuid, '653dad7d-e412-585e-9a1b-c1c97c370d0c'::uuid, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 2, 'Puerto de Buenos Aires', 'Puerto de Santos', 'PBA', 'SSZ', 'MARITIMO', TIMESTAMPTZ '2026-10-20T07:00Z', TIMESTAMPTZ '2026-10-25T17:00Z', 'EN_CURSO'),
 ('6b3a18d6-ae1a-52b4-8e34-46c52902dd01'::uuid, 'dceca613-bcf0-57cf-9929-3d8f07522fe7'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 1, 'Córdoba', 'Aeropuerto de Ezeiza', 'COR', 'EZE', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T08:00Z', TIMESTAMPTZ '2026-10-22T02:00Z', 'FINALIZADO'),
 ('1fb6bb17-903d-5fee-a675-3eb489b268a3'::uuid, 'dceca613-bcf0-57cf-9929-3d8f07522fe7'::uuid, '96ac1bd1-ea98-5326-ab6d-609f870b3e93'::uuid, 2, 'Aeropuerto de Ezeiza', 'Callao', 'EZE', 'CLL', 'AEREO', TIMESTAMPTZ '2026-10-22T07:00Z', TIMESTAMPTZ '2026-10-22T16:00Z', 'FINALIZADO'),
 ('e804aef0-961b-536c-a56e-f53d7501b635'::uuid, 'f6f08a03-2a92-5140-9846-0e2ef076db3d'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 1, 'Buenos Aires', 'Puerto de Buenos Aires', 'BUE', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T14:00Z', TIMESTAMPTZ '2026-10-21T16:00Z', 'FINALIZADO'),
 ('457efe24-5708-582a-b74c-feb8dc5a42f9'::uuid, 'f6f08a03-2a92-5140-9846-0e2ef076db3d'::uuid, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 2, 'Puerto de Buenos Aires', 'Montevideo', 'PBA', 'MVD', 'MARITIMO', TIMESTAMPTZ '2026-10-22T02:00Z', TIMESTAMPTZ '2026-10-22T18:00Z', 'FINALIZADO'),
 ('dcab0450-00f4-5a7d-a890-312a079cf3f6'::uuid, 'cdea56b0-4ef2-5338-8342-9a3444d56919'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 1, 'Rosario', 'Córdoba', 'ROS', 'COR', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T02:00Z', TIMESTAMPTZ '2026-10-20T12:00Z', 'FINALIZADO'),
 ('e93319ba-3433-56af-b122-1925b529c370'::uuid, 'cdea56b0-4ef2-5338-8342-9a3444d56919'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 2, 'Córdoba', 'Mendoza', 'COR', 'MZA', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T22:00Z', TIMESTAMPTZ '2026-10-21T11:00Z', 'FINALIZADO'),
 ('2c2bd0d3-af66-5963-8723-cbc7ce49b770'::uuid, '7b77f577-b7f6-5cd0-b620-0fcfc4a97c53'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 1, 'Mendoza', 'Santiago', 'MZA', 'SCL', 'TERRESTRE', TIMESTAMPTZ '2026-10-19T15:00Z', TIMESTAMPTZ '2026-10-19T20:00Z', 'FINALIZADO'),
 ('413f0621-7bd7-5ebd-99e9-07f373e65358'::uuid, '7b77f577-b7f6-5cd0-b620-0fcfc4a97c53'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 2, 'Santiago', 'Valparaíso', 'SCL', 'VAP', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T06:00Z', TIMESTAMPTZ '2026-10-20T09:00Z', 'FINALIZADO'),
 ('e8bb3dd9-25b1-5ec1-8fbb-88ab8c584d62'::uuid, 'cbb962f0-a717-5f72-8213-1ea6a59dae3e'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 1, 'Buenos Aires', 'Puerto de Buenos Aires', 'BUE', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T10:00Z', TIMESTAMPTZ '2026-10-20T12:00Z', 'FINALIZADO'),
 ('44f48daa-e7d0-5756-bb3e-e83ebb7777fd'::uuid, 'cbb962f0-a717-5f72-8213-1ea6a59dae3e'::uuid, 'c6d39b10-a95f-5346-90b9-0de9d2f1f771'::uuid, 2, 'Puerto de Buenos Aires', 'Puerto de Santos', 'PBA', 'SSZ', 'MARITIMO', TIMESTAMPTZ '2026-10-20T22:00Z', TIMESTAMPTZ '2026-10-26T08:00Z', 'EN_CURSO'),
 ('f6642cb1-dc38-50d4-9f55-8720afd16144'::uuid, '4770f530-1292-5b45-8bc1-bb7e26559b28'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 1, 'Córdoba', 'Rosario', 'COR', 'ROS', 'TERRESTRE', TIMESTAMPTZ '2026-10-20T06:00Z', TIMESTAMPTZ '2026-10-20T16:00Z', 'FINALIZADO'),
 ('34dd81b3-884f-501d-ab93-bf704be12d2b'::uuid, '4770f530-1292-5b45-8bc1-bb7e26559b28'::uuid, '4fe76672-61b9-5752-90a1-a4812a5150cd'::uuid, 2, 'Rosario', 'Asunción', 'ROS', 'ASU', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T02:00Z', TIMESTAMPTZ '2026-10-22T03:00Z', 'FINALIZADO'),
 ('478b706d-5b14-59a7-ac23-1213e30a0763'::uuid, 'd5b901e5-603a-585b-8e8b-fa02b491e825'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 1, 'Mendoza', 'Buenos Aires', 'MZA', 'BUE', 'TERRESTRE', TIMESTAMPTZ '2026-10-19T21:00Z', TIMESTAMPTZ '2026-10-21T00:00Z', 'FINALIZADO'),
 ('ca428262-9867-587b-ada6-4622878da9ce'::uuid, 'd5b901e5-603a-585b-8e8b-fa02b491e825'::uuid, '5cc8943e-4796-5ad4-89ab-ebbd3ce8d9cd'::uuid, 2, 'Buenos Aires', 'Puerto de Buenos Aires', 'BUE', 'PBA', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T10:00Z', TIMESTAMPTZ '2026-10-21T12:00Z', 'FINALIZADO'),
 ('059c6b5a-2b9c-508f-b9a3-83059e0118c1'::uuid, 'c231e5a7-7434-5d4b-b8db-405b7f42be44'::uuid, '21797c7a-0338-5b31-b45f-2c487f68eb84'::uuid, 1, 'Rosario', 'Aeropuerto de Ezeiza', 'ROS', 'EZE', 'TERRESTRE', TIMESTAMPTZ '2026-10-21T00:00Z', TIMESTAMPTZ '2026-10-21T08:00Z', 'FINALIZADO'),
 ('646fdaea-f447-5aa0-bf65-1c0f07741a3d'::uuid, 'c231e5a7-7434-5d4b-b8db-405b7f42be44'::uuid, '96ac1bd1-ea98-5326-ab6d-609f870b3e93'::uuid, 2, 'Aeropuerto de Ezeiza', 'Miami', 'EZE', 'MIA', 'AEREO', TIMESTAMPTZ '2026-10-21T13:00Z', TIMESTAMPTZ '2026-10-22T06:00Z', 'FINALIZADO')
) AS datos(id_tramo,id_envio,id_operador,numero_tramo,origen,destino,codigo_origen,codigo_destino,medio_transporte,fecha_salida,fecha_llegada_estimada,estado)
JOIN ubicacion uo ON uo.codigo=datos.codigo_origen
JOIN ubicacion ud ON ud.codigo=datos.codigo_destino;

INSERT INTO contenedor(id_contenedor,codigo_internacional,tipo,capacidad,estado)
VALUES
 ('42d867d9-d7a4-5248-9681-33e2c0cdb123'::uuid, 'MSKU0000001', 'REEFER', 67.7, 'EN_USO'),
 ('5ce5419b-847b-5ecc-b70d-03b18c5c0003'::uuid, 'MSKU0000002', 'REEFER', 67.7, 'EN_USO'),
 ('96f3d043-00e2-5c9e-8a9d-d0810f2f5995'::uuid, 'MSKU0000003', 'REEFER', 67.7, 'EN_USO'),
 ('3d6fc9f1-b24d-542d-9932-01963831a4a0'::uuid, 'MSKU0000004', 'REEFER', 67.7, 'EN_USO'),
 ('d37ca4b8-0970-5cec-ac9b-490429b7df61'::uuid, 'MSKU0000005', 'REEFER', 67.7, 'EN_USO'),
 ('a7ad0dad-7b9a-52d8-adfc-677e239892f3'::uuid, 'MSKU0000006', 'REEFER', 67.7, 'EN_USO'),
 ('5a92f38e-c32b-5035-8b97-1d253d2e58b1'::uuid, 'MSKU0000007', 'REEFER', 67.7, 'EN_USO'),
 ('a64ebff9-ffe1-5cac-9d63-03a4a6ff5225'::uuid, 'MSKU0000008', 'REEFER', 67.7, 'EN_USO'),
 ('2476f78b-a4fe-5d49-af25-e097bb04de76'::uuid, 'MSKU0000009', 'OPEN_TOP', 33.2, 'EN_USO'),
 ('66df8b40-0b65-53d3-ab37-a76ea2405973'::uuid, 'MSKU0000010', 'DRY', 33.2, 'EN_USO'),
 ('1f1e7b63-f3fc-5911-9659-81d67085a4a0'::uuid, 'MSKU0000011', 'DRY', 33.2, 'EN_USO'),
 ('a4f4c383-f83a-5fe5-9a33-902d50f7f487'::uuid, 'MSKU0000012', 'OPEN_TOP', 33.2, 'EN_USO'),
 ('8673db80-6691-5e62-abb9-90bacf34c8d7'::uuid, 'MSKU0000013', 'DRY', 33.2, 'EN_USO'),
 ('544cfffd-f74c-592b-b6f5-12dbfcc2466c'::uuid, 'MSKU0000014', 'DRY', 33.2, 'EN_USO'),
 ('2cb2fb44-c11d-517c-b802-27f09bd1433c'::uuid, 'MSKU0000015', 'OPEN_TOP', 33.2, 'EN_USO'),
 ('8c224184-d7bc-5016-92f6-e72773f8b2c0'::uuid, 'MSKU0000016', 'DRY', 33.2, 'EN_USO'),
 ('4003fbfb-f791-56dd-9be0-e13e9e7b5e8f'::uuid, 'MSKU0000017', 'DRY', 33.2, 'EN_USO'),
 ('ddda79aa-4ad6-5abb-b1d1-b61f25812c47'::uuid, 'MSKU0000018', 'OPEN_TOP', 33.2, 'EN_USO'),
 ('bf05ae76-f8c9-529d-8cdb-52b4cedb42d9'::uuid, 'MSKU0000019', 'DRY', 33.2, 'EN_USO'),
 ('efca5f90-e9bc-58ef-9e6d-87b0d85e4af0'::uuid, 'MSKU0000020', 'DRY', 33.2, 'EN_USO'),
 ('3cb5d737-2d39-56a5-999e-a6fb4d327db4'::uuid, 'MSKU0000021', 'OPEN_TOP', 33.2, 'EN_USO'),
 ('72234b4d-c7e7-575f-9e9b-dd19389d9909'::uuid, 'MSKU0000022', 'DRY', 33.2, 'EN_USO'),
 ('bba9ae70-be3c-57df-9319-4371224c4a5a'::uuid, 'MSKU0000023', 'DRY', 33.2, 'EN_USO'),
 ('5a4a52d9-7346-50a7-9ee8-830e3e31a2ca'::uuid, 'MSKU0000024', 'OPEN_TOP', 33.2, 'EN_USO'),
 ('3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41'::uuid, 'MSKU0000025', 'DRY', 33.2, 'EN_USO');

INSERT INTO sensor_iot(id_sensor,tipo,fabricante,fecha_instalacion,estado)
VALUES
 ('101231a8-af8f-53ec-bfb0-f4d579ba7455'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('d9e437b3-519e-5123-badd-eabc293e132b'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('12330766-e3ec-52b9-b9c0-b400c16274f6'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('51847209-55f1-54b5-83fc-698738457784'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('94858666-0f1b-5d7a-8f8d-0118791421e9'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('8e4ab8c8-5e28-5d73-a6fb-09e7ef2e5eba'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('ee32d9a8-5a2d-5bb2-b703-370676b20446'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('02c4c136-b65a-5b53-ae1a-2fb639f8e342'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('47c50803-5d2b-56e3-b880-3c1183fcee9a'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('45030ca6-267c-52cc-9dd8-ae5c999ebd8b'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('03ebe16a-2179-5a10-8ef3-9ddee070e4f2'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('5f9d963d-5c94-598d-9f99-83906ed03b7b'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('485bbb29-3858-538d-a4fc-14825417c1bb'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('5c6b62dc-896f-56bb-bedf-2fff168dbd28'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('ce360620-7aac-5c00-8eb2-768e008bb00c'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('0c6628df-23c1-5215-8042-6e28f0917c99'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('b8db9365-eabb-5abd-a0ae-f33b9e88eed2'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('a9953280-dfe2-577d-853a-dee5ed77ca63'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('907febb0-9f7a-5107-aa92-8e746dee266b'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('099eb63e-59f7-5b79-82f9-ecd7cc14a31a'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('a64abe30-e9c6-5def-a494-b7da3a1b5401'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('4e26c50a-502b-5de0-a0d2-42bfca9cc588'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('869f9a01-3e51-5644-b367-c63e14f2e613'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('72d03735-e45d-549f-993c-054050f48ad0'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('83006645-c8d5-5362-8e16-4f25460e5efe'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('458d6508-e6b4-5322-b376-9e3a8333f438'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('96b7f637-cb2b-54c0-98bb-1f09d71ef328'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('51e58f15-26aa-5a22-818e-6a3b41fe2d5b'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('b53c466a-939d-5642-99c6-7a1d32725d4f'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('04b93978-b79f-5ec2-b27e-9227c53497d6'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('591f5045-dbec-5eca-b5fb-7d8bdb2a30a8'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('b89f241c-d8f2-5dc8-847e-c8934067b02d'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('4617a0c9-64bf-513a-ac32-1ac5fd19a7db'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('58f32a1b-f2d6-5968-b9bc-e12f672d35fb'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('31dfb9b1-85fd-523e-9bd9-aac4b96b292d'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('13420416-b2c6-5a5d-812b-9e1c5f0cb14a'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('07b2aff3-26f5-590e-9a9c-5f7665997d0e'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('2e417392-252c-5763-9d55-9ed7b82bbcdd'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('df17a291-d8f9-5a4c-8e52-3aa43d682a36'::uuid, 'TEMPERATURA_HUMEDAD', 'Sensitech', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO'),
 ('1a41b97f-b75c-5365-8111-d65ac5ec9d46'::uuid, 'MULTIPARAMETRO', 'Teltonika', TIMESTAMPTZ '2026-10-20T00:00:00Z', 'ACTIVO');

INSERT INTO envio_contenedor(id_envio,id_contenedor,fecha_asociacion,fecha_desasociacion)
VALUES
 ('d72d0543-b3be-54c5-9248-ceb758d30add'::uuid, '42d867d9-d7a4-5248-9681-33e2c0cdb123'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('d72d0543-b3be-54c5-9248-ceb758d30add'::uuid, '5ce5419b-847b-5ecc-b70d-03b18c5c0003'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('ec22d634-96cd-5267-b7bd-accdce959d92'::uuid, '96f3d043-00e2-5c9e-8a9d-d0810f2f5995'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('ec22d634-96cd-5267-b7bd-accdce959d92'::uuid, '3d6fc9f1-b24d-542d-9932-01963831a4a0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('506de4f8-5386-58b4-ad0d-2f851434aec6'::uuid, 'd37ca4b8-0970-5cec-ac9b-490429b7df61'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('506de4f8-5386-58b4-ad0d-2f851434aec6'::uuid, 'a7ad0dad-7b9a-52d8-adfc-677e239892f3'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('6378a987-ec3f-504e-80db-0a38c77ee116'::uuid, '5a92f38e-c32b-5035-8b97-1d253d2e58b1'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('6378a987-ec3f-504e-80db-0a38c77ee116'::uuid, 'a64ebff9-ffe1-5cac-9d63-03a4a6ff5225'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('ecae19bc-e959-5b5d-bb35-be45c2e6c910'::uuid, '2476f78b-a4fe-5d49-af25-e097bb04de76'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('ecae19bc-e959-5b5d-bb35-be45c2e6c910'::uuid, '66df8b40-0b65-53d3-ab37-a76ea2405973'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('98fc9f7b-15bc-5e20-94c8-b875df1ed8d6'::uuid, '1f1e7b63-f3fc-5911-9659-81d67085a4a0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('5bcda798-982d-5078-a904-acb7769b79dd'::uuid, 'a4f4c383-f83a-5fe5-9a33-902d50f7f487'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('c1e5c9aa-6f01-5eff-8623-055ad1cf9fb4'::uuid, '8673db80-6691-5e62-abb9-90bacf34c8d7'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('23314655-0f49-57cf-aa62-6b86d1aa705b'::uuid, '544cfffd-f74c-592b-b6f5-12dbfcc2466c'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('986c09d3-a6be-5353-a94d-ab683bd9434c'::uuid, '2cb2fb44-c11d-517c-b802-27f09bd1433c'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('6ee84809-f672-5b8d-87f1-7f395cdaa8ed'::uuid, '8c224184-d7bc-5016-92f6-e72773f8b2c0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('653dad7d-e412-585e-9a1b-c1c97c370d0c'::uuid, '4003fbfb-f791-56dd-9be0-e13e9e7b5e8f'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('dceca613-bcf0-57cf-9929-3d8f07522fe7'::uuid, 'ddda79aa-4ad6-5abb-b1d1-b61f25812c47'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('f6f08a03-2a92-5140-9846-0e2ef076db3d'::uuid, 'bf05ae76-f8c9-529d-8cdb-52b4cedb42d9'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('cdea56b0-4ef2-5338-8342-9a3444d56919'::uuid, 'efca5f90-e9bc-58ef-9e6d-87b0d85e4af0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('7b77f577-b7f6-5cd0-b620-0fcfc4a97c53'::uuid, '3cb5d737-2d39-56a5-999e-a6fb4d327db4'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('cbb962f0-a717-5f72-8213-1ea6a59dae3e'::uuid, '72234b4d-c7e7-575f-9e9b-dd19389d9909'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('4770f530-1292-5b45-8bc1-bb7e26559b28'::uuid, 'bba9ae70-be3c-57df-9319-4371224c4a5a'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('d5b901e5-603a-585b-8e8b-fa02b491e825'::uuid, '5a4a52d9-7346-50a7-9ee8-830e3e31a2ca'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('c231e5a7-7434-5d4b-b8db-405b7f42be44'::uuid, '3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z');

INSERT INTO asignacion_sensor_contenedor(id_sensor,id_contenedor,fecha_inicio,fecha_fin)
VALUES
 ('101231a8-af8f-53ec-bfb0-f4d579ba7455'::uuid, '42d867d9-d7a4-5248-9681-33e2c0cdb123'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('d9e437b3-519e-5123-badd-eabc293e132b'::uuid, '5ce5419b-847b-5ecc-b70d-03b18c5c0003'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('12330766-e3ec-52b9-b9c0-b400c16274f6'::uuid, '96f3d043-00e2-5c9e-8a9d-d0810f2f5995'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('51847209-55f1-54b5-83fc-698738457784'::uuid, '3d6fc9f1-b24d-542d-9932-01963831a4a0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('94858666-0f1b-5d7a-8f8d-0118791421e9'::uuid, 'd37ca4b8-0970-5cec-ac9b-490429b7df61'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('8e4ab8c8-5e28-5d73-a6fb-09e7ef2e5eba'::uuid, 'a7ad0dad-7b9a-52d8-adfc-677e239892f3'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('ee32d9a8-5a2d-5bb2-b703-370676b20446'::uuid, '5a92f38e-c32b-5035-8b97-1d253d2e58b1'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('02c4c136-b65a-5b53-ae1a-2fb639f8e342'::uuid, 'a64ebff9-ffe1-5cac-9d63-03a4a6ff5225'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('47c50803-5d2b-56e3-b880-3c1183fcee9a'::uuid, '2476f78b-a4fe-5d49-af25-e097bb04de76'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('45030ca6-267c-52cc-9dd8-ae5c999ebd8b'::uuid, '66df8b40-0b65-53d3-ab37-a76ea2405973'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('03ebe16a-2179-5a10-8ef3-9ddee070e4f2'::uuid, '1f1e7b63-f3fc-5911-9659-81d67085a4a0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('5f9d963d-5c94-598d-9f99-83906ed03b7b'::uuid, 'a4f4c383-f83a-5fe5-9a33-902d50f7f487'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('485bbb29-3858-538d-a4fc-14825417c1bb'::uuid, '8673db80-6691-5e62-abb9-90bacf34c8d7'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('5c6b62dc-896f-56bb-bedf-2fff168dbd28'::uuid, '544cfffd-f74c-592b-b6f5-12dbfcc2466c'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('ce360620-7aac-5c00-8eb2-768e008bb00c'::uuid, '2cb2fb44-c11d-517c-b802-27f09bd1433c'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('0c6628df-23c1-5215-8042-6e28f0917c99'::uuid, '8c224184-d7bc-5016-92f6-e72773f8b2c0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('b8db9365-eabb-5abd-a0ae-f33b9e88eed2'::uuid, '4003fbfb-f791-56dd-9be0-e13e9e7b5e8f'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('a9953280-dfe2-577d-853a-dee5ed77ca63'::uuid, 'ddda79aa-4ad6-5abb-b1d1-b61f25812c47'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('907febb0-9f7a-5107-aa92-8e746dee266b'::uuid, 'bf05ae76-f8c9-529d-8cdb-52b4cedb42d9'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('099eb63e-59f7-5b79-82f9-ecd7cc14a31a'::uuid, 'efca5f90-e9bc-58ef-9e6d-87b0d85e4af0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('a64abe30-e9c6-5def-a494-b7da3a1b5401'::uuid, '3cb5d737-2d39-56a5-999e-a6fb4d327db4'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('4e26c50a-502b-5de0-a0d2-42bfca9cc588'::uuid, '72234b4d-c7e7-575f-9e9b-dd19389d9909'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('869f9a01-3e51-5644-b367-c63e14f2e613'::uuid, 'bba9ae70-be3c-57df-9319-4371224c4a5a'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('72d03735-e45d-549f-993c-054050f48ad0'::uuid, '5a4a52d9-7346-50a7-9ee8-830e3e31a2ca'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('83006645-c8d5-5362-8e16-4f25460e5efe'::uuid, '3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('458d6508-e6b4-5322-b376-9e3a8333f438'::uuid, '42d867d9-d7a4-5248-9681-33e2c0cdb123'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('96b7f637-cb2b-54c0-98bb-1f09d71ef328'::uuid, '5ce5419b-847b-5ecc-b70d-03b18c5c0003'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('51e58f15-26aa-5a22-818e-6a3b41fe2d5b'::uuid, '96f3d043-00e2-5c9e-8a9d-d0810f2f5995'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('b53c466a-939d-5642-99c6-7a1d32725d4f'::uuid, '3d6fc9f1-b24d-542d-9932-01963831a4a0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('04b93978-b79f-5ec2-b27e-9227c53497d6'::uuid, 'd37ca4b8-0970-5cec-ac9b-490429b7df61'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('591f5045-dbec-5eca-b5fb-7d8bdb2a30a8'::uuid, 'a7ad0dad-7b9a-52d8-adfc-677e239892f3'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('b89f241c-d8f2-5dc8-847e-c8934067b02d'::uuid, '5a92f38e-c32b-5035-8b97-1d253d2e58b1'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('4617a0c9-64bf-513a-ac32-1ac5fd19a7db'::uuid, 'a64ebff9-ffe1-5cac-9d63-03a4a6ff5225'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('58f32a1b-f2d6-5968-b9bc-e12f672d35fb'::uuid, '2476f78b-a4fe-5d49-af25-e097bb04de76'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('31dfb9b1-85fd-523e-9bd9-aac4b96b292d'::uuid, '66df8b40-0b65-53d3-ab37-a76ea2405973'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('13420416-b2c6-5a5d-812b-9e1c5f0cb14a'::uuid, '1f1e7b63-f3fc-5911-9659-81d67085a4a0'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('07b2aff3-26f5-590e-9a9c-5f7665997d0e'::uuid, 'a4f4c383-f83a-5fe5-9a33-902d50f7f487'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('2e417392-252c-5763-9d55-9ed7b82bbcdd'::uuid, '8673db80-6691-5e62-abb9-90bacf34c8d7'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('df17a291-d8f9-5a4c-8e52-3aa43d682a36'::uuid, '544cfffd-f74c-592b-b6f5-12dbfcc2466c'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z'),
 ('1a41b97f-b75c-5365-8111-d65ac5ec9d46'::uuid, '3b0c2b4a-965d-51ea-9dd5-ccc7f7a2bb41'::uuid, TIMESTAMPTZ '2026-10-20T00:00:00Z', TIMESTAMPTZ '2026-10-23T00:00:00Z');

INSERT INTO regla_alerta(id_regla,nombre,tipo_regla,operador,umbral,minutos_inactividad,severidad,fecha_creacion)
VALUES
 ('96196805-40db-51f4-a2b2-0e8cc7ee2a63'::uuid, 'Cadena de frío: temperatura mayor a 8 °C (contenedores REEFER)', 'TEMPERATURA', '>', 8.0000, NULL, 'ALTA', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('ba9fb76f-fec9-5f6e-b9b5-df61a2035234'::uuid, 'Cadena de frío: temperatura menor a 2 °C (contenedores REEFER)', 'TEMPERATURA', '<', 2.0000, NULL, 'MEDIA', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('e175309e-cb9c-542e-91b0-946800a453f3'::uuid, 'Batería del sensor por debajo del 15 %', 'BATERIA', '<', 15.0000, NULL, 'MEDIA', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('f686b8bb-fc4e-531e-9635-1966bb5ed92f'::uuid, 'Sensor sin reportar más de 120 minutos', 'INACTIVIDAD', '>', NULL, 120, 'ALTA', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('9c1403ef-fefe-5226-a415-d021f0f00376'::uuid, 'Vibración mayor a 2.5 g (posible golpe)', 'VIBRACION', '>', 2.5000, NULL, 'MEDIA', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('8682a474-dea2-5b6e-af19-2862d4840b65'::uuid, 'Humedad relativa mayor a 90 %', 'HUMEDAD', '>', 90.0000, NULL, 'BAJA', TIMESTAMPTZ '2026-10-20T00:00:00Z'),
 ('810b94b9-8c35-5cce-bd94-ce9c0a9ca625'::uuid, 'Anomalía estadística: |z| > 3 respecto del histórico del sensor', 'ANOMALIA_TEMPERATURA', '>', 3.0000, NULL, 'MEDIA', TIMESTAMPTZ '2026-10-20T00:00:00Z');

DO $$ BEGIN
 IF (SELECT count(*) FROM cliente WHERE email LIKE 'cliente.%@catalogo.test') <> 8 THEN RAISE EXCEPTION 'Catalogo: se esperaban 8 clientes'; END IF;
 IF (SELECT count(*) FROM proveedor WHERE email LIKE 'proveedor.%@catalogo.test') <> 8 THEN RAISE EXCEPTION 'Catalogo: se esperaban 8 proveedores'; END IF;
 IF (SELECT count(*) FROM operador_logistico WHERE email LIKE 'operador.%@catalogo.test') <> 5 THEN RAISE EXCEPTION 'Catalogo: se esperaban 5 operadores'; END IF;
 IF (SELECT count(*) FROM usuario WHERE email LIKE 'u-%@catalogo.test') <> 9 THEN RAISE EXCEPTION 'Catalogo: se esperaban 9 usuarios'; END IF;
 IF (SELECT count(*) FROM usuario_rol ur JOIN usuario u USING (id_usuario) WHERE u.email LIKE 'u-%@catalogo.test') <> 9 THEN RAISE EXCEPTION 'Catalogo: se esperaban 9 asignaciones de rol'; END IF;
 IF (SELECT count(*) FROM ubicacion WHERE codigo IN ('BUE','PBA','ROS','COR','MZA','EZE','SSZ','MVD','SCL','VAP','ASU','CLL','MIA')) <> 13 THEN RAISE EXCEPTION 'Catalogo: se esperaban 13 ubicaciones'; END IF;
 IF (SELECT count(*) FROM envio WHERE fecha_creacion=TIMESTAMPTZ '2026-10-19T00:00:00Z') <> 20 THEN RAISE EXCEPTION 'Catalogo: se esperaban 20 envios'; END IF;
 IF (SELECT count(*) FROM tramo t JOIN envio e USING(id_envio) WHERE e.fecha_creacion=TIMESTAMPTZ '2026-10-19T00:00:00Z') <> 40 THEN RAISE EXCEPTION 'Catalogo: se esperaban 40 tramos'; END IF;
 IF (SELECT count(*) FROM contenedor WHERE codigo_internacional LIKE 'MSKU%') <> 25 THEN RAISE EXCEPTION 'Catalogo: se esperaban 25 contenedores'; END IF;
 IF (SELECT count(*) FROM sensor_iot WHERE fecha_instalacion=TIMESTAMPTZ '2026-10-20T00:00:00Z') <> 40 THEN RAISE EXCEPTION 'Catalogo: se esperaban 40 sensores'; END IF;
 IF (SELECT count(*) FROM regla_alerta WHERE fecha_creacion=TIMESTAMPTZ '2026-10-20T00:00:00Z') <> 7 THEN RAISE EXCEPTION 'Catalogo: se esperaban 7 reglas'; END IF;
 IF (SELECT count(*) FROM envio_contenedor WHERE fecha_asociacion=TIMESTAMPTZ '2026-10-20T00:00:00Z' AND fecha_desasociacion=TIMESTAMPTZ '2026-10-23T00:00:00Z') <> 25 THEN RAISE EXCEPTION 'Catalogo: asignaciones de contenedor incompletas'; END IF;
 IF (SELECT count(*) FROM asignacion_sensor_contenedor WHERE fecha_inicio=TIMESTAMPTZ '2026-10-20T00:00:00Z' AND fecha_fin=TIMESTAMPTZ '2026-10-23T00:00:00Z') <> 40 THEN RAISE EXCEPTION 'Catalogo: asignaciones de sensor incompletas'; END IF;
END $$;
COMMIT;
