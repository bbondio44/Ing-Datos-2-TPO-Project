# Lo visto en clase · Ingeniería de Datos II

Resumen del material de la cátedra (Lic. Claudio Godio): presentación de la materia, "Arquitecturas
de almacenamiento y distribución", clase de Cassandra, "Modelado en Cassandra", Laboratorio 1 de
MongoDB, Laboratorio 3 de Cassandra, el ejercicio de modelado de Cassandra, la clase de bases de
grafos, "Introducción a Cypher" y la guía de ObjectDB con Java.

## Regla de uso

- Para el modelado y las operaciones sobre las bases, usar **primero** lo que está en este archivo.
- Si hace falta algo que no figura acá (un comando, operador, tipo de índice o patrón), **avisar antes
  de usarlo**: decir qué es, por qué hace falta y qué alternativa vista en clase existe.
- Si se termina usando algo fuera de clase, dejar un comentario `// FUERA DE CLASE: <qué es y por qué>`
  en el código o en el script, para poder defenderlo en la evaluación.
- Explicar todo con los términos de la materia (partition key, clustering key, consistencia eventual,
  quórum, etc.), porque así se pregunta en la defensa.
- Esta regla aplica a las bases. El código Java/Spring Boot de la aplicación (capas, controladores,
  JavaFX) no está cubierto por este material, salvo JPA (sección 5).

---

## 1. Conceptos generales

**Temas de la materia:** RDBMS vs NoSQL (ACID o consistencia eventual), bases orientadas a objetos,
documentales, clave-valor, tabulares, de grafos y acceso desde una aplicación.

**ACID:** atomicidad, consistencia, aislamiento, durabilidad. Bloqueos compartidos y exclusivos,
granularidad (fila, clave, página, extent, tabla, base), interbloqueo. Cuellos de botella de los
RDBMS: logs (redo/undo), concurrencia, transacciones distribuidas, buffers.

**NoSQL:** esquema prescindible, desnormalización, escalado horizontal natural, no garantizan ACID.
Conviene cuando ACID y los JOIN no son centrales y hace falta escalar y replicar. Si los datos son
transaccionales, la opción correcta es un RDBMS. Aplicaciones típicas: Web 2.0, **IoT**, perfiles,
gestión de contenidos.

**BASE:** Basically Available, Soft state, Eventually consistent.

**Big Data:** 3 V (volumen, velocidad, variedad) y 7 V (+ viabilidad, veracidad, visualización, valor).

**Distribución:** partición vertical y horizontal, clúster, escalado vertical y horizontal,
replicación (maestro-esclavo, esclavo-maestro-esclavo, peer to peer), particionamiento, híbrido.
Arquitecturas *shared nothing* (sin SPOF) y *shared disks*. Almacenamiento DAS, NAS, SAN.

**Teorema CAP:** solo dos de tres entre consistencia, disponibilidad y tolerancia a particiones.
- CA: RDBMS y bases de grafos (sin distribución).
- CP: MongoDB (según configuración de réplicas, lectura y escritura), HBase, BigTable.
- AP: Cassandra, Dynamo, CouchDB, Riak.

**Parámetros N, R, W:** N = réplicas, R = réplicas que deben responder una lectura, W = réplicas
que deben confirmar una escritura.
- Consistencia por escrituras: W = N, R = 1 (modelo RDBMS).
- Consistencia por lecturas: W = 1, R = N.
- Quórum: W > N/2, R > N/2. **Lectura fuertemente consistente si R + W > N.**
- Consistencia eventual: sin nuevas escrituras, todas las lecturas terminan devolviendo el último valor.

---

## 2. MongoDB (base documental)

### Conceptos
- Documentos JSON almacenados como **BSON**; campos con documentos, arreglos y arreglos de documentos.
- Documentos en **colecciones**, colecciones en bases. Esquema flexible.
- Todo documento tiene `_id` único e indexado (se puede asignar o dejar que genere un ObjectId).
- Documentos embebidos y arreglos reducen la necesidad de joins.
- Límite de 16 MB por documento (para archivos mayores, GridFS).
- Motor de almacenamiento WiredTiger.
- Seguridad: autenticación, autorización, encriptado, TLS/SSL.

### Índices
- **Tipos:** simple, compuesto (importa el orden), multiclave (arreglos), geoespacial, textual, hash.
- **Propiedades:** únicos, parciales (con expresión de filtro), escasos (sparse), TTL.
- `createIndex`, `dropIndex`, `{unique: true}`, índices sobre campos embebidos con notación de punto.
- `explain("executionStats")` para verificar si una consulta usa índice.

### Replicación y sharding
- Replica set: un primario recibe todas las escrituras y las registra en el **oplog**; los secundarios
  replican. Escrituras confirmadas con `{w: "majority"}`.
- Sharding: shards (cada uno puede ser replica set), `mongos` como enrutador, servidores de
  configuración (CSRS). Shard key: alta cardinalidad, baja frecuencia, evitar cambios monótonos
  (fechas o números sucesivos). Hashed shard keys cuando la clave no cumple esas condiciones.

### Operaciones vistas en el laboratorio
- `use`, `db.getCollectionNames()`, `db.stats()`.
- Inserción: `insertOne`, `insert`.
- Consulta: `find(selector, proyección)`.
  - Igualdad `{campo: valor}`; varios campos = AND.
  - `$lt`, `$lte`, `$gt`, `$gte`, `$ne`, `$exists`, `$or`, `$regex`.
  - Búsqueda dentro de arreglos por valor (`{hobbies: 'Swimming'}`).
  - Campos embebidos con notación de punto (`'team.team_short_name'`).
  - Proyección `{name: 1, _id: 0}`.
  - `sort({campo: 1 | -1})`, `limit`, `skip` (paginado), `countDocuments`.
- Actualización: `updateOne`, `updateMany`.
  - Sin operador, el segundo parámetro **reemplaza el documento entero** (cuidado).
  - `$set`, `$inc`, `$push`.
  - **Upsert:** `{upsert: true}` actualiza si existe o inserta si no.
- Borrado: `deleteMany`.

### No visto en el material (avisar antes de usar)
Pipeline de agregación (`aggregate`, `$match`, `$group`, `$lookup`), validadores `$jsonSchema`,
transacciones multi-documento, change streams, `$setOnInsert`, `$addToSet`, `$pull`, `replaceOne`,
`bulkWrite`.

---

## 3. Cassandra (base tabular)

### Arquitectura
- Clave-fila, escalable, **eventualmente consistente**, distribuida. Versión vista: **Cassandra 5.0**.
- Anillo P2P sin nodo principal; cualquier nodo puede ser **coordinador**. Protocolo **Gossip**
  para detectar nodos caídos.
- Nodo, data center, clúster. Replicación multi-datacenter.
- Escritura: **commit log → MemTable → flush a SSTable** (inmutable). Escrituras baratas, lecturas
  más caras (pueden recorrer varias SSTables).
- **Compactación** periódica; en 5.0, **Unified Compaction Strategy (UCS)**.
- Borrado: **tombstones**, eliminados en la compactación; tiempo de gracia; `repair` de mantenimiento.
- **TTL:** fecha de caducidad de los datos.
- Lectura: direct read, digest request, **read repair** en segundo plano; gana la réplica con el
  timestamp más reciente.
- Cada columna es (name, value, **timestamp**). El timestamp lo puede proveer la aplicación
  (microsegundos desde 1970) y se usa para resolver conflictos (last write wins).
- **Consistency level** configurable en lectura y escritura.
- Seguridad: RBAC (`CREATE ROLE`, `ALTER ROLE`, `GRANT`, `REVOKE`), audit logging, Dynamic Data
  Masking (5.0).

### Estructura de datos
- Columna → fila → familia de columnas (tabla) → **keyspace** (uno por aplicación) → clúster.
- **Primary key = partition key + clustering key.**
  - Partition key: filas con el mismo valor quedan juntas en la misma partición (y nodo).
  - Clustering key: orden físico de las filas dentro de la partición.
- Keyspace: `replication_factor` y estrategia `SimpleStrategy` (un data center) o
  `NetworkTopologyStrategy` (varios).
- Particionador por defecto: **Murmur3Partitioner** (con vnodes).
- Índices secundarios: existen, pero **no se recomiendan** para consultas que recorren mucho para
  devolver poco. En 5.0: **Storage Attached Indexes (SAI)**.

### CQL visto
- `cqlsh`, `SHOW VERSION`, `DESCRIBE KEYSPACES`, `USE`.
- `CREATE KEYSPACE nombre WITH replication = {'class':'SimpleStrategy','replication_factor':1};`
- `CREATE TABLE IF NOT EXISTS ... WITH ...`, `INSERT`, `UPDATE`, `SELECT`.
- **El WHERE debe incluir todas las columnas de la partition key**; si no, la consulta da error.
- `ORDER BY` solo en el orden de las clustering columns.
- Tipos: `tinyint`, `smallint`, `int`, `bigint`, `varint`, `float`, `double`, `decimal`, `counter`
  (solo incrementa/decrementa, no se escribe con INSERT), `boolean`, `blob`, `inet`, `timestamp`,
  `date`, `time`, `duration`, `uuid`, `timeuuid` (ordenable cronológicamente), `text`/`varchar`, `ascii`.
- Colecciones: `list<T>`, `set<T>`, `map<K,V>`.
- **UDT** (`CREATE TYPE`) y `frozen<...>`; **tuplas** (`tuple<...>`, frozen implícito: no se puede
  actualizar un elemento suelto, se reemplaza la tupla entera).
- Funciones: `uuid()`, `now()`, `toTimestamp(now())`.
- `VECTOR<FLOAT, n>`, índice SAI con `similarity_function`, `ORDER BY ... ANN OF [...]`,
  `similarity_cosine`.

### Metodología de modelado (Chebotko)
1. Modelo conceptual: diagrama entidad-relación de Chen.
2. Definir las **consultas** (Q1, Q2, …) a partir de la interfaz o los casos de uso, y el flujo entre ellas.
3. Modelo lógico: **una tabla por consulta**, nombrada `entidad_by_atributo` (ej. `hotels_by_poi`).
   - Partition key = atributos por los que se busca.
   - Clustering columns = unicidad + orden pedido.
   - Agregar las columnas que la consulta necesita.
4. Diagrama de Chebotko: K = partition key, C = clustering (con flecha de orden).
5. Modelo físico: tipos, claves y consultas definitivas.
- Principios: **sin JOIN** (desnormalizar en otra tabla, nunca joins en el cliente salvo último
  recurso), **sin integridad referencial** ni cascada, **minimizar las particiones** que lee cada
  consulta (ideal: una), pensar el **tamaño de las particiones**, el orden es una decisión de diseño.
- Escribir la misma información en varias tablas es barato, pero no se asegura la consistencia entre
  ellas en las actualizaciones.

### No visto en el material (avisar antes de usar)
`BATCH`, `USING TIMESTAMP` en CQL (el concepto de timestamp por columna sí se vio), lightweight
transactions (`IF NOT EXISTS` en INSERT, `IF` en UPDATE), `ALLOW FILTERING`, vistas materializadas,
`TimeWindowCompactionStrategy` u otras compactaciones distintas de UCS, `NetworkTopologyStrategy`
con nivel `LOCAL_QUORUM` (se vio la estrategia y el quórum por separado).

---

## 4. Neo4j (base de grafos)

### Conceptos
- Grafo de propiedades: **nodos** con propiedades y **relaciones** (aristas) con propiedades,
  dirigidas o no dirigidas. Nodos con una o varias **etiquetas**; relaciones con un **tipo**.
- Cuándo usarla: redes sociales o laborales, relaciones entre entidades de distintos dominios,
  motores de recomendación, **búsquedas recursivas de n niveles**, detección de fraude por patrones,
  datos que naturalmente son grafos. Casos citados: eBay (rutas logísticas y entregas), Walmart, NASA.
- Neo4j: código abierto, en Java, lenguaje **Cypher** (influyó en el estándar ISO GQL).
- Community: una sola base de usuario. Enterprise: varias bases, replicación y clustering.
  Siempre existe además la base `system`.
- En CAP se ubica en el espacio **CA**.
- Clúster: **servidores centrales (core)** y **réplicas de lectura**.
  - Los core replican cada transacción con el protocolo **Raft**; se confirma cuando la mayoría
    (N/2 + 1) la acepta. Más cores = más latencia de escritura.
  - Tolerancia a fallas: **M = 2F + 1** (para tolerar 2 cores caídos hacen falta 5). Con 2 cores,
    si uno cae el otro queda en solo lectura.
  - Réplicas de lectura: escalan las consultas, reciben el log de transacciones de forma asíncrona,
    son descartables.
  - **Consistencia causal:** un cliente siempre lee sus propias escrituras.

### Cypher visto
- Patrones ASCII: `(n:Etiqueta {prop: valor})-[:TIPO {prop: valor}]->(m)`.
- Crear: `CREATE` (nodos y relaciones; varios en un bloque).
- **`MERGE`:** busca el patrón; si existe lo usa (como MATCH), si no lo crea (como CREATE).
  Usar `MERGE` para relaciones que no deben duplicarse si se ejecuta varias veces.
- Leer: `MATCH ... WHERE ... RETURN`, alias con `AS`, `ORDER BY` (`DESC`), `LIMIT`, `DISTINCT`.
- Comparaciones y `AND` en `WHERE`.
- Modificar: `SET` (propiedades y etiquetas, varias a la vez), `REMOVE` (propiedad o etiqueta).
- Borrar: `DELETE` (solo si el nodo no tiene relaciones), `DETACH DELETE` (nodo y sus relaciones).
- Agregación: `count(n)`, `count(r)`, `count(*)`.
- `WITH` para encadenar partes de una consulta, renombrar y agrupar
  (`WITH p, count(f) AS cantidad`).
- Caminos: `MATCH p=(a)-[*1..3]->(b)`. **Buena práctica: limitar siempre los saltos**; `[*]` sin
  límite es de las consultas más costosas.
- Funciones: `type(r)`, `labels(n)`.
- Restricción única: `CREATE CONSTRAINT nombre IF NOT EXISTS FOR (p:Etiqueta) REQUIRE p.prop IS UNIQUE`;
  `SHOW CONSTRAINTS`.
- Índices: simple, compuesto, `TEXT INDEX` (para `STARTS WITH`, `ENDS WITH`, `CONTAINS`), índices
  sobre propiedades de relaciones; `SHOW INDEXES`.

### No visto en el material (avisar antes de usar)
`OPTIONAL MATCH`, `UNWIND`, `MERGE ... ON CREATE SET / ON MATCH SET`, `shortestPath`,
`collect`, procedimientos APOC, Graph Data Science, parámetros `$param` en consultas (los usa el
driver desde la aplicación; explicarlos si aparecen).

---

## 5. ObjectDB (base orientada a objetos)

En el TP no usamos ObjectDB, pero su guía usa **JPA**, el mismo estándar que Spring Data JPA usa
con PostgreSQL. Sirve para explicar la capa relacional.

- Almacena objetos Java en forma nativa con JPA o JDO, reduciendo el problema del mapeo
  objeto-relacional.
- Entidad: clase con `@Entity`, `@Id`, `@GeneratedValue` y constructor vacío.
- Flujo: **Entidad → EntityManagerFactory → EntityManager → transacción → persistir / consultar /
  modificar → commit → cierre de recursos.**
  - `Persistence.createEntityManagerFactory("objectdb:archivo.odb")`, `emf.createEntityManager()`.
  - Toda modificación va dentro de una transacción: `em.getTransaction().begin()` … `commit()`.
  - `em.persist(obj)`, `em.find(Clase.class, id)`, modificar con setters dentro de la transacción,
    `em.remove(obj)`.
  - Consultas **JPQL**: `em.createQuery("SELECT a FROM Alumno a", Alumno.class).getResultList()`.

---

## 6. Temas sin material cargado todavía

Redis (clave-valor) y "acceso desde una aplicación" (drivers y Spring Data para MongoDB, Cassandra
y Neo4j).
