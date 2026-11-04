# Ingeniería de Datos II — Grupo 4
## PostgreSQL: base transaccional para plataforma logística + IoT

**Objetivo:** implementar el lado relacional del trabajo práctico y preparar su integración con Cassandra, MongoDB y Neo4j. Está basado en el DER adjunto, la Primera Entrega, los 40 requisitos de promoción y el Plan Maestro (23/10/2026).

**ATENCIÓN:** los scripts se diseñaron para PostgreSQL 15+ / Supabase y tienen controles internos, pero **no se ejecutaron contra una instancia PostgreSQL real en este entorno**. Es obligatorio ejecutarlos y verificar los resultados antes de dar la base por validada. No atribuir al esquema el cumplimiento de las funcionalidades que todavía dependen de backend, sensores, UI y los otros motores.

### Opción rápida: instalar todo de una vez

En una **base nueva y exclusivamente para demostración**, ejecutar `INSTALACION_COMPLETA_DEMO.sql` en el SQL Editor de Supabase. Contiene en orden el esquema, procedimientos, vistas, datos de muestra y consultas de aceptación. **No** ejecutar este archivo sobre una base existente o de producción.

Para sumar también los UUID del catálogo compartido, ejecutar `06_datos_catalogo.sql` después de la instalación completa.

### 1. Archivos y orden de ejecución

1. `00_esquema.sql`: tablas, PK, FK, restricciones, índices, CUIT válido, permisos y outbox.
2. `01_reglas_y_procesos.sql`: triggers, controles de períodos, integridad, transiciones de estados, facturación, pagos, cierre y auditoría.
3. `02_vistas_y_analitica.sql`: vistas para reportes, conciliaciones, sesiones y función de predicción baseline.
4. `03_datos_demo.sql`: dataset artificial de ejemplo, incluye clientes, roles, envíos, tramos, cuentas, facturas, pagos, cierre y reportes.
5. `04_pruebas_aceptacion.sql`: asserts y consultas para la demostración (después de ejecutar datos).
6. `05_pruebas_negativas.sql`: casos de rechazo intencional (comentar/descomentar **de a uno**).
7. `ids_compartidos.csv`: UUID exactos de las entidades de ejemplo para el resto del equipo.
8. `MATRIZ_40_REQUISITOS.md`: responsabilidades y límites por cada requisito.
9. `CONTRATO_INTEGRACION.md`: estructura recomendada para conectar motores.
10. `06_datos_catalogo.sql`: dataset con los UUID compartidos del catálogo Mongo/Cassandra.

**Supabase:** crear un proyecto PostgreSQL > SQL Editor > ejecutar cada archivo completo, en orden. El esquema elegido es **`tp_logistica`**, no `public`. En SQL externos usar prefijo `tp_logistica.*` o configurar `search_path` en la conexión del backend. Para accesos a PostgreSQL desde el servidor, utilizar conexión segura con SSL, credenciales en variables de entorno y un usuario con privilegios mínimos. **Nunca colocar la cadena de conexión o una clave privilegiada en React.**

**PostgreSQL local:** crear una base nueva y ejecutar los mismos SQL en el orden indicado (`psql -v ON_ERROR_STOP=1 -f ...`), o conectarse con DBeaver y ejecutar cada archivo.

Los scripts de esquema, procesos y vistas se envuelven en transacción para no dejar la instalación parcial ante errores. **El seed se ejecuta una sola vez sobre una instalación nueva.**

### Carga del catálogo compartido

En una base nueva, ejecutar `00_esquema.sql`, `01_reglas_y_procesos.sql`, `02_vistas_y_analitica.sql`, `03_datos_demo.sql`, `04_pruebas_aceptacion.sql` y finalmente `06_datos_catalogo.sql`. La prueba de aceptación va antes del catálogo porque verifica los totales del dataset chico. `06_datos_catalogo.sql` agrega los UUID de `../../tp-grupo4-mongo-cassandra/catalogo_ids.csv` sin alterar `03_datos_demo.sql`; no cargarlo dos veces ni sobre una base que ya contenga el catálogo.

El seed usa nombres de país completos, CUIT válidos, emails únicos y fechas fijas. Los estados de los envíos se calculan al `2026-10-22T23:30Z`; los contenedores y sensores quedan asignados en el intervalo `[2026-10-20T00:00Z, 2026-10-23T00:00Z)`. Los usuarios internos no tienen entidad asociada; los usuarios de clientes, proveedores y operadores se vinculan mediante su FK correspondiente. Los hashes de contraseña son deliberadamente aleatorios y no permiten iniciar sesión con las cuentas del catálogo.

### 2. Diseño y decisiones relevantes

**Entidades del modelo original conservadas:** cliente, proveedor, operador_logistico, usuario, rol, usuario_rol, sesion, envio, contenedor, envio_contenedor, sensor_iot, tramo, tramo_proveedor, vehiculo, incidente, cuenta_corriente, factura, pago y movimiento_cuenta.

**Entidades y estructuras incorporadas para la promoción:** permiso, rol_permiso, ubicacion, asignacion_sensor_contenedor, regla_alerta, factura_detalle, tarifa_servicio, cierre_cuenta, solicitud_reporte, prediccion_envio, outbox_evento y auditoria_operacion.

- Todas las entidades primarias usan UUID. Los mismos identificadores se almacenan como `String` en Neo4j, `UUID` en Cassandra y string UUID en MongoDB.
- `usuario` referencia como máximo una entidad entre cliente, proveedor u operador; los usuarios internos no tienen entidad asociada. Los roles del usuario se asignan por separado en `usuario_rol`.
- No hay lectura continua, posicionamiento GPS histórico ni mensajes duplicados en PostgreSQL: pertenecen a Cassandra y MongoDB.
- Los **sensores pueden moverse entre contenedores**. Por eso se reemplaza el `sensor_iot.id_contenedor` fijo del DER por `asignacion_sensor_contenedor` con fechas. La consulta histórica debe seleccionar la asociación vigente **en el timestamp de cada lectura**.
- `envio_contenedor` también guarda inicio/fin y sus triggers impiden asignaciones solapadas del mismo contenedor.
- `incidente.id_tramo` es opcional, pero si se informa debe corresponder al mismo envío. `id_evento_externo` permite vincular al evento MongoDB.
- `ubicacion` incorpora código estable para que Neo4j no dependa del nombre de puerto como identificador.
- `cuenta_corriente` y `factura` tienen **un solo titular**: cliente (saldo por cobrar) O proveedor/operador (saldo por pagar). El saldo significa deuda pendiente a favor de la empresa para clientes y deuda de la empresa para terceros, siempre `débitos - créditos`.
- **Moneda del demo:** ARS. `facturar_envio` y `facturar_reporte` aceptan solo tarifas fijas y ARS. Para multimoneda habría que extender cuentas, facturas, movimientos y diferencias de cambio.
- `pago` se imputa a **una factura**, permite varios pagos parciales hasta cancelarla. No se incluyen anticipos ni un pago distribuido entre varias facturas (extensión futura).
- Los movimientos y cierres son inmutables. Para corregir contabilidad se utiliza un movimiento de ajuste en un período abierto, sin borrar historia.
- Facturación y cobros usan claves de idempotencia. El backend debe enviar una clave estable por operación.
- Hay auditoría de operaciones de negocio y outbox para sincronización asíncrona. La trazabilidad del actor depende de que el backend configure `app.usuario_id` y `app.correlacion_id` dentro de la transacción.
- Los estados permitidos y algunas transiciones se controlan en PostgreSQL. La autorización por rol/cliente corresponde al **backend**; la existencia de `rol_permiso` no autoriza por sí sola al usuario.

### 3. Operaciones principales

**Generar factura de un envío:**
```sql
SELECT tp_logistica.facturar_envio(
  '<ID_ENVIO>'::uuid,
  '<ID_TARIFA>'::uuid,
  'envio:<ID_ENVIO>:servicio-transporte:v1'
);
```

**Registrar un pago parcial o total:**
```sql
SELECT tp_logistica.registrar_pago(
  '<ID_FACTURA>'::uuid,
  20000.00,
  'TRANSFERENCIA',
  'pago:transferencia:12345'
);
```

**Facturas a proveedores/operadores:** insertar borrador con `id_cuenta` y exactamente uno de `id_proveedor`/`id_operador`, insertar detalle, ejecutar `SELECT tp_logistica.emitir_factura('<ID_FACTURA>'::uuid)`, registrar pago por `registrar_pago`.

**Cierre mensual:**
```sql
SELECT tp_logistica.cerrar_cuenta('<ID_CUENTA>'::uuid, DATE '2026-09-01');
```

**Factura de un reporte:** crear solicitud -> backend genera reporte en MongoDB -> actualizar solicitud a `GENERADO` + `id_resultado_externo` -> `facturar_reporte` -> UI entrega archivo autorizado.

**Predicción baseline de demostración:**
```sql
SELECT tp_logistica.generar_prediccion_baseline('<ID_ENVIO_ACTIVO>'::uuid);
```
La función predice duración promedio histórica por países de origen/destino; si no hay muestra, recurre al promedio global. Es un baseline **simple**, no un modelo de ML entrenado ni validado con conjunto de prueba. Para aprobar predicción hay que evaluar con históricos, mostrar error/limitaciones y reportar la ETA futura.

**Consulta de trazabilidad de una operación:**
```sql
BEGIN;
SELECT set_config('app.usuario_id','<UUID_USUARIO>',true);
SELECT set_config('app.correlacion_id','<UUID_CORRELACION>',true);
-- Realizar INSERT / UPDATE / llamada a funcion de negocio.
COMMIT;
```

### 4. Consultas SQL y definición de métricas

- `v_envios_reporte`: país **de destino**, estado del envío, cliente, demorado (vencido y no entregado/cancelado), incidentes abiertos.
- `v_ranking_clientes_volumen`: suma de `volumen_m3` de envíos **no cancelados**, sin sumar una vez por cada contenedor/tramo.
- `v_ranking_rutas_incidentes`: ruta = origen + destino + medio del **tramo**, cuenta incidentes vinculados al tramo. Incidentes de envío sin tramo **no entran** en este ranking.
- `v_tiempos_entrega_cliente`: mínima, máxima y promedio en horas desde `fecha_creacion` hasta `fecha_entrega_real`, solo `ENTREGADO`.
- `v_tiempo_promedio_entrega`: promedio global de entregados.
- `v_promedio_incidentes_dimension`: agrupa por país **de destino**, estado del **envío** y cliente; divisor = número de envíos no cancelados de ese grupo.
- `v_costos_por_transporte`: suma de `tramo.costo_real` (no estimado) de tramos no cancelados, por medio.
- `v_desempeno_mensual`: mes de **creación** del envío (UTC) y resultados de entregas conocidas para esa cohorte.
- `v_saldo_factura` y `v_conciliacion_cuentas`: verifican imputaciones y saldo almacenado vs. movimientos.
- `v_envio_contenedor_sensor_activo`: relaciones vigentes **en la hora de la consulta**. Histórico se obtiene con filtros por fecha en tablas de asignación.

### 5. Datos demostrativos

El seed incorpora 4 clientes, 2 proveedores, 2 operadores, 3 usuarios y 3 roles, 7 envíos con diferentes estados, 8 tramos, 3 contenedores, 3 sensores, incidentes, reglas, tarifas, pagos parciales, reportes facturables, predicción y un cierre mensual. Contiene una reasignación histórica de sensor y de contenedor.

Ejemplos de resultados **esperados si se ejecuta el seed sin modificaciones**:

- Cuenta `Transportes del Sur SA`: saldo **90.000 ARS** (transporte e1 cobrado, transporte e4 con anticipo 25.000, reporte 15.000 impago).
- Cuenta `Importadora Atlas SA`: saldo **23.000 ARS**, compuesto por débito de 35.000 y crédito de 12.000 del mes anterior.
- Cierre Atlas: total_debitos 35.000, total_creditos 12.000, saldo_final 23.000.
- Promedio general de entrega de los tres envíos entregados: **296 horas**.
- Cliente Transportes del Sur SA: volumen no cancelado **55,2 m³**.

Los datos son sintéticos y los CUIT son ficticios, aunque respetan estructura y dígito verificador. **Los password_hash del seed son marcadores NO válidos para hacer login**: al integrar autenticación reemplazarlos por hashes Argon2id o BCrypt reales (o usar identidad delegada con Supabase Auth). No copiar el dataset a producción.

### 6. Límites / trabajo pendiente del equipo

El SQL no equivale a todos los requisitos funcionando: faltan la API y sus ABM, controles de autorización multiusuario, interfaz, simulador e ingesta Cassandra, reglas efectivamente evaluadas, persistencia de alertas/mensajes MongoDB, proyección Neo4j, renderer de mapas, reportes exportados y predicción histórica evaluada. Revisar `MATRIZ_40_REQUISITOS.md` para saber qué preparar por requisito.

**Controles recomendados antes de la demo:** ejecutar `04_pruebas_aceptacion.sql`, las pruebas negativas individuales, comparar resultados manualmente, ensayar el flujo extremo a extremo, guardar capturas y requests para cada R01-R40, verificar que no se exponen datos de otros clientes, y ensayar reintentos del outbox con MongoDB/Neo4j caídos.
