# TP Persistencia Políglota — Ingeniería de Datos II (Grupo 4)

Plataforma de logística internacional con sensores IoT. Son 40 requisitos (R01–R40) sobre cuatro
motores. Un requisito está terminado solo cuando tiene datos de prueba, una prueba ejecutada y
evidencia, no cuando existe el código.

Los 40 requisitos, su prueba de aceptación, el estado de cada uno y la definición de terminado:
@docs/requisitos.md

## Arquitectura

- **Aplicación de escritorio JavaFX** → consume la API del **backend Spring Boot** → cuatro bases.
  El enunciado pide una aplicación, no solo una API: cada requisito tiene que poder usarse desde
  JavaFX. JavaFX nunca se conecta directo a una base.
- **Simulador IoT** (Java): manda lecturas a `POST /api/v1/iot/lecturas`.
- Sincronización: PostgreSQL escribe en la tabla `outbox_evento`; un worker la lee, actualiza
  MongoDB/Neo4j, marca procesado y reintenta si falla.
- Estado al 04/11/2026: solo PostgreSQL está conectado. Spring Boot excluye la autoconfiguración de
  MongoDB, Cassandra y Neo4j, y el endpoint de estado las devuelve como `NO_CONFIGURADA`.
  El endpoint de ingesta IoT todavía no existe.

## Stack

- Backend: Spring Boot (Java). Build tool y versión de Java: ver `pom.xml` o `build.gradle`.
- Cada integrante corre las cuatro bases localmente en Docker (`docker-compose.yml`):
  - PostgreSQL → `localhost:5432`
  - MongoDB → `localhost:27017`, base `logistica_docs`
  - Cassandra → `localhost:9042`, keyspace `logistica_iot`
  - Neo4j → bolt `localhost:7687`, browser `http://localhost:7474`
- Credenciales: siempre desde variables de entorno (`.env`, plantilla en `.env.example`).
  Nunca escribir usuarios ni contraseñas en el código ni en `application.yml`.

## Comandos

<!-- Completar en la primera sesión con los comandos reales del repo. -->
- Levantar bases: `docker compose up -d`
- Estado de las bases: `docker compose ps`
- Build: (completar)
- Tests: (completar)
- Correr la app: (completar)

## Qué guarda cada base (fuente de verdad única por dato)

- PostgreSQL: maestros (clientes, usuarios, envíos, contenedores, sensores), asignaciones con vigencia,
  `regla_alerta`, `incidente`, `solicitud_reporte`, finanzas (cuentas, facturas, pagos, tarifas).
- Cassandra: telemetría IoT (lecturas, posiciones GPS, último estado). Nada transaccional.
- MongoDB: eventos logísticos, alertas, conversaciones y mensajes, resultados de reportes.
- Neo4j: red cliente–envío–tramo–vehículo–operador, rankings y predicciones.

Modelo físico detallado de MongoDB y Cassandra: @docs/modelo-fisico-mongodb-cassandra.md

## Reglas comunes a todas las bases

- Identidad: toda referencia usa el UUID lógico de PostgreSQL. En Cassandra tipo `uuid`, en MongoDB
  `String`. No generar IDs nuevos para entidades que ya existen en PostgreSQL.
- Fechas siempre en UTC (`timestamp` en Cassandra, `Date` en MongoDB).
- Escrituras idempotentes: reintentar un mismo evento no puede duplicar registros.
- MongoDB y Cassandra solo referencian por UUID; nunca son fuente de un estado crítico.

## Quién trabaja qué

- Persona 1 — PostgreSQL, ABM, trayectos, finanzas y reportes SQL: R01–R08, R10, R12–R16, R24,
  R35–R39. Además: `docker-compose.yml` único y seguridad (roles, permisos, propiedad del envío).
- Persona 2 — Cassandra, MongoDB, Neo4j, IoT, alertas, mensajería y reportes: R09, R11, R17–R23,
  R25–R34, R40. Además: conexión de MongoDB/Cassandra y worker del outbox hacia Neo4j.
- Persona 3 — Testing de los 40 requisitos: catálogo de UUID, dataset, pruebas automatizadas,
  casos adversos, evidencias, README y demo. Además: conexión de Neo4j a Spring Boot.
- No modificar código, configuración ni datos del módulo de otra persona sin avisar antes y esperar
  confirmación. Los puntos compartidos (UUID, alertas ↔ incidentes, reportes facturables) se coordinan.

## Reglas de trabajo

- En las bases, usar primero lo visto en clase y seguir su regla de uso: @docs/visto-en-clase.md
- Antes de un cambio que toque varios archivos o el modelo de datos, proponer un plan y esperar OK.
- Un módulo de Spring Data por base (JPA, Spring Data MongoDB, Spring Data Cassandra,
  Spring Data Neo4j), con paquetes separados por base.
- Cassandra: una tabla por patrón de consulta. Nunca `ALLOW FILTERING` sobre toda la tabla.
- Reutilizar las tablas, funciones SQL y contratos que ya existen; no romper endpoints existentes.
- Cada requisito incluye: backend, pantalla JavaFX, datos demo, prueba automatizada y evidencia en
  `evidencias/RXX/`. Al terminar, indicar archivos modificados y cómo probarlo.
- Después de cambiar código, compilar y correr los tests. Si fallan, arreglar antes de seguir.
- No borrar volúmenes de Docker ni datos sin preguntar (`docker compose down -v`, `docker volume rm`).
- Commits chicos con mensaje en español que nombre el requisito (por ejemplo `R17: ingesta de lecturas`).

## Detalles que suelen romper

- Cassandra tarda en arrancar (30-60 s). Si la app falla al conectar, esperar a que
  `docker compose ps` la muestre `healthy`.
- Spring Data Cassandra necesita `spring.cassandra.local-datacenter=datacenter1` (el datacenter por
  defecto de la imagen oficial) y el keyspace `logistica_iot` creado antes de arrancar.
- MongoDB con usuario root: la URI necesita `?authSource=admin`.
- Neo4j exige contraseña de 8 caracteres o más.
