CREATE CONSTRAINT cliente_id IF NOT EXISTS FOR (n:Cliente) REQUIRE n.id_cliente IS UNIQUE;
CREATE CONSTRAINT envio_id IF NOT EXISTS FOR (n:Envio) REQUIRE n.id_envio IS UNIQUE;
CREATE CONSTRAINT contenedor_id IF NOT EXISTS FOR (n:Contenedor) REQUIRE n.id_contenedor IS UNIQUE;
CREATE CONSTRAINT tramo_id IF NOT EXISTS FOR (n:Tramo) REQUIRE n.id_tramo IS UNIQUE;
CREATE CONSTRAINT ubicacion_id IF NOT EXISTS FOR (n:Ubicacion) REQUIRE n.id_ubicacion IS UNIQUE;
CREATE CONSTRAINT vehiculo_id IF NOT EXISTS FOR (n:Vehiculo) REQUIRE n.id_vehiculo IS UNIQUE;
CREATE CONSTRAINT operador_id IF NOT EXISTS FOR (n:Operador) REQUIRE n.id_operador IS UNIQUE;
CREATE CONSTRAINT incidente_id IF NOT EXISTS FOR (n:Incidente) REQUIRE n.id_incidente IS UNIQUE;
CREATE INDEX envio_estado IF NOT EXISTS FOR (n:Envio) ON (n.estado);
CREATE INDEX tramo_medio IF NOT EXISTS FOR (n:Tramo) ON (n.medio_transporte);
CREATE INDEX incidente_tipo IF NOT EXISTS FOR (n:Incidente) ON (n.tipo);

