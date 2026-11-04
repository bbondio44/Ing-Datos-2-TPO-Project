CREATE KEYSPACE IF NOT EXISTS logistica_iot
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS logistica_iot.lecturas_por_sensor_dia (
    id_sensor uuid,
    dia date,
    fecha_hora timestamp,
    id_lectura uuid,
    id_contenedor uuid,
    temperatura double,
    humedad double,
    vibracion double,
    bateria double,
    PRIMARY KEY ((id_sensor, dia), fecha_hora, id_lectura)
) WITH CLUSTERING ORDER BY (fecha_hora DESC, id_lectura ASC);

CREATE TABLE IF NOT EXISTS logistica_iot.lecturas_por_contenedor_dia (
    id_contenedor uuid,
    dia date,
    fecha_hora timestamp,
    id_sensor uuid,
    id_lectura uuid,
    temperatura double,
    humedad double,
    vibracion double,
    bateria double,
    PRIMARY KEY ((id_contenedor, dia), fecha_hora, id_sensor, id_lectura)
) WITH CLUSTERING ORDER BY (fecha_hora DESC, id_sensor ASC, id_lectura ASC);

CREATE TABLE IF NOT EXISTS logistica_iot.posiciones_por_contenedor_dia (
    id_contenedor uuid,
    dia date,
    fecha_hora timestamp,
    id_sensor uuid,
    id_lectura uuid,
    latitud double,
    longitud double,
    PRIMARY KEY ((id_contenedor, dia), fecha_hora, id_sensor, id_lectura)
) WITH CLUSTERING ORDER BY (fecha_hora DESC, id_sensor ASC, id_lectura ASC);

CREATE TABLE IF NOT EXISTS logistica_iot.ultima_lectura_por_sensor (
    id_sensor uuid PRIMARY KEY,
    id_lectura uuid,
    id_contenedor uuid,
    fecha_hora timestamp,
    temperatura double,
    humedad double,
    vibracion double,
    bateria double
);

CREATE TABLE IF NOT EXISTS logistica_iot.ultima_posicion_por_contenedor (
    id_contenedor uuid PRIMARY KEY,
    id_lectura uuid,
    id_sensor uuid,
    fecha_hora timestamp,
    latitud double,
    longitud double
);

