# Estado inicial y brechas del proyecto

Fecha de auditoría: 30 de octubre de 2026. La carpeta de trabajo estaba vacía y no era un repositorio Git. Se localizaron en `Downloads` la consigna oficial, las 40 condiciones de promoción, el plan maestro, la primera entrega, el modelo MongoDB, el diagrama Cassandra, el modelo Neo4j y un ZIP PostgreSQL. Las copias de referencia quedaron bajo `docs/referencias` y `database/referencia-postgresql`.

## Disponibilidad verificada

| Componente | Estado | Evidencia o límite |
|---|---|---|
| Java 21 | Disponible | Temurin 21.0.8 LTS |
| Maven global | No disponible | Se usa Maven Wrapper generado desde contenedor |
| Docker | Disponible | Docker Desktop 29.7.2 iniciado durante la auditoría |
| PostgreSQL local | Preparado | Compose y scripts 00, 01 y 02; sin seed automático |
| PostgreSQL Supabase | No verificado | No hay URL ni credenciales en el entorno; no se ejecutó SQL remoto |
| MongoDB | Contrato disponible | Modelo físico JSON 1.1; adaptador remoto pendiente |
| Cassandra | Contrato disponible | Cinco tablas Chebotko; CQL local agregado y ejecución pendiente |
| Neo4j | Contrato disponible | Modelo original corregido a UUID String; conexión pendiente |

## Hallazgos del modelado

1. El paquete PostgreSQL contiene 31 tablas, 14 vistas y funciones transaccionales para emisión, facturación, pagos, cierre y predicción baseline. Los propios autores advierten que solo tuvo validación estática; debe ejecutarse en PostgreSQL real.
2. El modelo Neo4j original usa varios IDs Integer y envío-contenedor 1:1. El contrato integrado exige UUID como String y asociaciones históricas N:M.
3. MongoDB define alertas `ABIERTA`, `EN_REVISION`, `RESUELTA`, `DESCARTADA`, mientras el prompt maestro propone `ABIERTA`, `RECONOCIDA`, `CERRADA`. Se adopta un contrato público estable con mapeo explícito antes de integrar; no se cambia silenciosamente el modelo del compañero.
4. El plan maestro decía que faltaban tablas financieras y reglas. El ZIP PostgreSQL posterior ya incorpora esas estructuras; el SQL real prevalece.
5. `usuario` solo contiene `id_cliente`. Los logins de proveedor y operador requieren una migración compatible o una tabla de vínculo. Se mantiene pendiente hasta acordar el contrato con el responsable PostgreSQL.
6. No se encontró `05_seguridad_supabase.sql` ni una migración equivalente. El esquema privado no debe exponerse a Data API; la autorización se implementa en backend y se requiere auditar privilegios en el proyecto remoto.

## Prioridad por dependencias

| Prioridad | Bloque | Requisitos dependientes | Estado inicial |
|---:|---|---|---|
| 1 | Ejecución PostgreSQL, auth, clientes, sesiones | R01-R03 y base de casi todos | En implementación |
| 2 | Maestros y logística temporal | R04-R08, R17, R24 | Pendiente |
| 3 | Finanzas y funciones SQL | R10-R16, R33 | Pendiente |
| 4 | Cassandra + ingesta + mapa | R17-R23, R32 | Pendiente |
| 5 | MongoDB + reglas + mensajes | R09, R17, R23, R25-R26, R33-R34, R40 | Pendiente |
| 6 | Outbox + Neo4j | R24, R27, R30 | Pendiente |
| 7 | Analítica, predicción, jobs y exportación | R27-R39 | Pendiente |
| 8 | Evidencia UI y casos negativos | R01-R40 | Pendiente |

## Restricciones operativas actuales

- La base Supabase debe conservarse sin seed ni cambios hasta disponer de conexión y validar su estado.
- El entorno local no carga `03_datos_demo.sql` automáticamente. El seed será un comando separado y explícito.
- Flyway está incluido pero deshabilitado mientras los SQL originales funcionen como instalación de referencia. Las futuras migraciones serán incrementales y no destructivas.
- Ningún requisito se marca `PROBADO` sin ejecutar backend, base y UI con datos controlados.

