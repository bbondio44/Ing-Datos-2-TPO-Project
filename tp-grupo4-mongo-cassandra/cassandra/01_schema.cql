-- =====================================================================
-- Ingeniería de Datos II · Grupo 4 · Persistencia Políglota
-- Cassandra · Telemetría IoT (Persona 2)
-- Modelo físico: ModeladoCassandraGrupo4.drawio (5 tablas, metodología Chebotko)
--
-- Uso:   cqlsh -f cassandra/01_schema.cql
-- Re-ejecutable: todas las sentencias usan IF NOT EXISTS.
-- =====================================================================

-- Keyspace ------------------------------------------------------------
-- Desarrollo / demo: un solo nodo -> SimpleStrategy RF=1.
-- Producción: NetworkTopologyStrategy {'dc1': 3} y escrituras/lecturas
-- con consistencia LOCAL_QUORUM (ver docs/MODELO_CASSANDRA.md).
CREATE KEYSPACE IF NOT EXISTS logistica_iot
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
  AND durable_writes = true;

USE logistica_iot;

-- ---------------------------------------------------------------------
-- T1 · lecturas_por_sensor_dia                  (Q01 escritura, Q02, Q03, Q09, Q10, Q12)
-- Historial por sensor y día.
-- Partición: (id_sensor, dia)  -> acota el tamaño: 1 lectura c/ 5 min = 288 filas/día.
-- Clustering: fecha_hora DESC (la consulta más común es "lo más reciente primero"),
--             id_lectura ASC (desempate: dos lecturas en el mismo instante no se pisan).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lecturas_por_sensor_dia (
    id_sensor      uuid,
    dia            date,
    fecha_hora     timestamp,
    id_lectura     uuid,
    id_contenedor  uuid,
    temperatura    double,
    humedad        double,
    vibracion      double,
    latitud        double,
    longitud       double,
    bateria        double,
    PRIMARY KEY ((id_sensor, dia), fecha_hora, id_lectura)
) WITH CLUSTERING ORDER BY (fecha_hora DESC, id_lectura ASC)
  AND compaction = {'class': 'TimeWindowCompactionStrategy',
                    'compaction_window_unit': 'DAYS',
                    'compaction_window_size': 1}
  AND comment = 'Historial por sensor y dia';

-- ---------------------------------------------------------------------
-- T2 · lecturas_por_contenedor_dia              (Q04)
-- Historial conjunto de todos los sensores de un contenedor.
-- Clustering: fecha_hora DESC, id_sensor ASC, id_lectura ASC.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lecturas_por_contenedor_dia (
    id_contenedor  uuid,
    dia            date,
    fecha_hora     timestamp,
    id_sensor      uuid,
    id_lectura     uuid,
    temperatura    double,
    humedad        double,
    vibracion      double,
    latitud        double,
    longitud       double,
    bateria        double,
    PRIMARY KEY ((id_contenedor, dia), fecha_hora, id_sensor, id_lectura)
) WITH CLUSTERING ORDER BY (fecha_hora DESC, id_sensor ASC, id_lectura ASC)
  AND compaction = {'class': 'TimeWindowCompactionStrategy',
                    'compaction_window_unit': 'DAYS',
                    'compaction_window_size': 1}
  AND comment = 'Historial conjunto de sensores por contenedor y dia';

-- ---------------------------------------------------------------------
-- T3 · posiciones_por_contenedor_dia            (Q07)
-- Histórico de posiciones GPS. Solo se escribe cuando la lectura trae
-- latitud y longitud válidas (no se inventan coordenadas).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS posiciones_por_contenedor_dia (
    id_contenedor  uuid,
    dia            date,
    fecha_hora     timestamp,
    id_sensor      uuid,
    id_lectura     uuid,
    latitud        double,
    longitud       double,
    PRIMARY KEY ((id_contenedor, dia), fecha_hora, id_sensor, id_lectura)
) WITH CLUSTERING ORDER BY (fecha_hora DESC, id_sensor ASC, id_lectura ASC)
  AND compaction = {'class': 'TimeWindowCompactionStrategy',
                    'compaction_window_unit': 'DAYS',
                    'compaction_window_size': 1}
  AND comment = 'Historico de posiciones GPS';

-- ---------------------------------------------------------------------
-- T4 · ultima_lectura_por_sensor                (Q05, Q06, Q11)
-- Estado más reciente del sensor. Una fila por sensor.
-- La aplicación escribe con USING TIMESTAMP = fecha_hora de la lectura
-- (en microsegundos): si llega una lectura atrasada, su timestamp de
-- escritura es menor y Cassandra (last-write-wins) conserva la más nueva.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ultima_lectura_por_sensor (
    id_sensor      uuid,
    id_contenedor  uuid,
    id_lectura     uuid,
    fecha_hora     timestamp,
    temperatura    double,
    humedad        double,
    vibracion      double,
    latitud        double,
    longitud       double,
    bateria        double,
    PRIMARY KEY (id_sensor)
) WITH comment = 'Estado mas reciente del sensor';

-- ---------------------------------------------------------------------
-- T5 · ultima_posicion_por_contenedor           (Q08)
-- Ubicación GPS más reciente. Misma técnica USING TIMESTAMP.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ultima_posicion_por_contenedor (
    id_contenedor  uuid,
    id_sensor      uuid,
    id_lectura     uuid,
    fecha_hora     timestamp,
    latitud        double,
    longitud       double,
    PRIMARY KEY (id_contenedor)
) WITH comment = 'Ubicacion GPS mas reciente';
