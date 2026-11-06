# Modelo físico · Neo4j

Es el modelo que está implementado. Reemplaza a `docs/referencias/Modelo_Fisico_Grafos_Neo4j (1).docx`,
que usa identificadores enteros y `REGISTRO_INCIDENTE`. Los identificadores son el UUID lógico de
PostgreSQL (D01) y el grafo se llena solo: el worker del outbox traduce cada cambio con `ProyeccionGrafo`.

## Nodos

| Etiqueta | Clave única | Propiedades |
|---|---|---|
| `Cliente` | `id_cliente` | `razon_social`, `pais`, `estado` |
| `Operador` | `id_operador` | `razon_social`, `pais`, `estado` |
| `Envio` | `id_envio` | `origen`, `destino`, `estado`, `volumen_m3` |
| `Contenedor` | `id_contenedor` | `codigo_internacional`, `tipo`, `estado` |
| `Tramo` | `id_tramo` | `origen`, `destino`, `medio_transporte`, fechas y estado |
| `Ubicacion` | `id_ubicacion` | `codigo`, `nombre`, `pais`, `tipo` |
| `Vehiculo` | `id_vehiculo` | `tipo`, `identificacion`, `estado` |
| `Incidente` | `id_incidente` | `tipo`, `severidad`, `estado`, `descripcion` |
| `Punto` | `codigo` | `nombre`, `pais`, `tipo`, `latitud`, `longitud` |

## Relaciones

| Origen | Relación | Destino | Propiedades |
|---|---|---|---|
| `Cliente` | `SOLICITA` | `Envio` | |
| `Envio` | `ASIGNADO_A` | `Contenedor` | `fecha_asociacion`, `fecha_desasociacion` |
| `Envio` | `COMPUESTO_POR` | `Tramo` | `orden` |
| `Envio` | `REGISTRA` | `Incidente` | |
| `Tramo` | `REGISTRA` | `Incidente` | |
| `Tramo` | `ORIGEN` | `Ubicacion` | |
| `Tramo` | `DESTINO` | `Ubicacion` | |
| `Tramo` | `EJECUTADO_POR` | `Vehiculo` | |
| `Tramo` | `OPERADO_POR` | `Operador` | |
| `Vehiculo` | `PERTENECE_A` | `Operador` | |
| `Punto` | `CONECTA` | `Punto` | `medio`, `km` |

`ASIGNADO_A` lleva `fecha_asociacion` en el patrón del `MERGE`, porque un contenedor puede volver al mismo
envío en otro período (D05). Proveedores y sensores no forman parte de la red.

## Red de rutas

`Punto` y `CONECTA` son la red por donde viajan los envíos: ciudades, puertos, aeropuertos y puntos de alta mar.
Los puntos de alta mar evitan que una ruta marítima cruce tierra, y no pueden ser origen ni destino.
La red es un dato de referencia que vive solo en Neo4j: está en `backend/src/main/resources/neo4j/red_rutas.cypher`
y el worker la carga al arrancar con `MERGE`, así que cargarla otra vez no duplica nada.

Cada conexión se guarda una vez y se recorre sin dirección. La búsqueda usa lo visto en clase, un camino con saltos
limitados (`MATCH p=(a)-[:CONECTA*1..24]-(b) RETURN p`), y la aplicación elige el de menos km.

## Restricciones e índices

Están en `database/neo4j/01_constraints.cypher` y el worker los crea al arrancar:

- Una restricción de unicidad por etiqueta, sobre su clave.
- Índices por `Envio.estado`, `Tramo.medio_transporte` e `Incidente.tipo`.

Neo4j no hace cumplir las cardinalidades: las garantiza PostgreSQL, que es la fuente de verdad.

## Idempotencia y consistencia

Todas las escrituras usan `MERGE`, así que reprocesar un evento no duplica nodos ni relaciones. Un evento
se aplica en una transacción y, si Neo4j está caído, queda en `ERROR` con un próximo intento. Es
consistencia eventual: el grafo se pone al día después.

## Consultas

- **Red del cliente (R30):** `MATCH (:Cliente {id_cliente: $id})-[:SOLICITA]->(e:Envio)` y, desde ahí, tramos,
  operadores, contenedores e incidentes, con una consulta por tipo de relación (`Neo4jGraphService`).
- **Ruta completa de un envío:** `(e:Envio)-[r:COMPUESTO_POR]->(t:Tramo)` ordenado por `r.orden`.
