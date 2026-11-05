# Lo usado fuera de clase en las bases

Inventario para la defensa, contra `docs/visto-en-clase.md`. La aplicación en funcionamiento usa casi
solo lo visto: lo demás está en scripts de carga y de verificación que no corren dentro de la app.

## Dentro de la aplicación

| Base | Qué es | Dónde | Por qué | Alternativa vista |
|---|---|---|---|---|
| Cassandra | `USING TIMESTAMP` | `LecturaCassandraRepository` | Una lectura tardía no pisa la última (C03). El timestamp por columna y last write wins sí se vieron; solo la sintaxis es nueva | Leer antes de escribir y comparar en la aplicación |
| Cassandra | `SELECT` con `LIMIT` sin partition key | `IntegrationStatusController` | Health check que hace una lectura real de una fila | Consultar una partición conocida |
| Neo4j | Parámetros `$nombre` | `ProyeccionGrafo`, `Neo4jGraphService` | El driver separa valores y consulta, y evita armar texto a mano | Concatenar valores en el texto, que es inseguro |

MongoDB dentro de la aplicación no usa nada fuera de clase: `insertOne`, `updateOne`, `find`, `sort`,
`limit`, `$set`, `$inc`, `$push` y filtros por igualdad, `$ne` y `$lt`.

## En el esquema

| Base | Qué es | Dónde | Por qué | Alternativa vista |
|---|---|---|---|---|
| MongoDB | Validador `$jsonSchema` y `collMod` | `mongodb/01_schema.js` | Rechaza documentos inválidos, porque MongoDB no tiene CHECK ni FK (D03) | Validar solo en la aplicación |

Los índices únicos, parciales y geoespaciales de MongoDB, y `CREATE CONSTRAINT` y `CREATE INDEX` de Neo4j,
sí se vieron.

## Solo en scripts de carga y verificación

| Base | Qué es | Dónde | Por qué | Alternativa vista |
|---|---|---|---|---|
| MongoDB | `replaceOne`, `bulkWrite` | `mongodb/02_datos_prueba.js` | Reponen el dataset sin duplicar | `updateOne` con `upsert`, documento por documento |
| MongoDB | `aggregate` (`$match`, `$group`, `$lookup`, `$project`, `$unwind`, `$sort`) | `mongodb/03_consultas.js` | Contar y cruzar colecciones al verificar | Recorrer con `find` y contar en JavaScript |
| MongoDB | `$setOnInsert`, `$addToSet`, `$pull`, `$in`, `$near`, `$geoWithin` | `mongodb/03_consultas.js` | Demostrar idempotencia y búsquedas por zona | `$set`, `$push` y `$or` |
| Cassandra | `BATCH` | `cassandra/02_datos_prueba.cql` | Cargar el dataset más rápido | `INSERT` sueltos |
| Cassandra | `ALLOW FILTERING`, acotado a una partición | `cassandra/03_consultas.cql` (Q09, Q10) | Auditar; la aplicación no lo usa | Una tabla por consulta |
| Cassandra | `IN` en el `WHERE` | `cassandra/03_consultas.cql` | Pedir varias particiones de una vez | Una consulta por partición, como hace la app |

Todos tienen su comentario `FUERA DE CLASE` en el archivo.

## PostgreSQL

`visto-en-clase.md` no resume SQL, porque la materia lo da por conocido. Lo más avanzado que se usa, por
si preguntan: triggers y funciones `plpgsql` (reglas, outbox y auditoría), vistas, `jsonb` (`variables` de la
predicción y payload del outbox), `FILTER`, `LATERAL` y `FOR UPDATE SKIP LOCKED` en el worker.
