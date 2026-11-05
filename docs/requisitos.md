# Requisitos de promoción R01–R40

Fuentes: "Condiciones necesarias para Promocionar" (los 40 puntos del profesor), Plan Maestro de
Cumplimiento (qué desarrollar y prueba de aceptación) y auditoría de código del 04/11/2026
(commit 56354b9). El **estado** es una foto de ese día: verificar contra el código actual antes de
asumirlo.

El detalle de implementación de Persona 1 está en `docs/requisitos-persona1.md`.

## Definición de terminado

Un requisito está terminado solo si cumple las cinco condiciones:

1. Tiene datos cargados y el resultado es correcto (un endpoint que responde vacío no cuenta).
2. Funciona desde la aplicación JavaFX, no solo desde SQL o Postman.
3. Respeta permisos, validaciones, errores y duplicados.
4. Tiene una prueba automatizada y evidencia guardada en `evidencias/RXX/` (request o consulta,
   respuesta, datos de origen y captura).
5. Otro integrante puede repetirlo siguiendo el README.

## Criterios de diseño obligatorios

| ID | Tema | Criterio |
|---|---|---|
| D01 | IDs y fechas | UUID compartido entre motores; timestamps en UTC. |
| D02 | Trazabilidad | Cada operación con id de correlación, creador, timestamp y registro de error si falla. |
| D03 | Integridad | FK y CHECK en PostgreSQL; validaciones equivalentes al escribir en los otros motores. |
| D04 | Sin duplicados | Facturación automática, alertas y telemetría con clave idempotente o id de evento. |
| D05 | Historia temporal | Asignaciones envío–contenedor y sensor–contenedor con inicio y fin. |
| D06 | Prueba reproducible | Seed + script de carga + consultas/requests + salida esperada + capturas. |

Cassandra (ingesta y consultas):

| ID | Criterio |
|---|---|
| C01 | Validar y normalizar cada lectura; preservar `id_lectura` y el instante original; escribir historial por sensor, contenedor y GPS cuando aplique. |
| C02 | Rangos de varios días: consultar cada partición diaria y fusionar/ordenar en la aplicación. Nada de `ALLOW FILTERING` como solución general. |
| C03 | Una lectura tardía no puede pisar una más reciente en las tablas `ultima_*`. Probar entradas fuera de orden. |
| C04 | Reprocesar sin duplicar; si no hay GPS válido, no actualizar la última posición. |
| C05 | TTL solo cuando se acuerde la ventana histórica que necesitan reportes y anomalías. |

Caso de aceptación imprescindible de Cassandra: consultar un sensor y un contenedor en un rango que
cruza dos fechas, mostrar las lecturas ordenadas y reconstruir las posiciones GPS en un mapa.

## Casos adversos que hay que probar

- **Duplicados:** crear dos veces la misma factura automática, alerta o lectura → comportamiento idempotente.
- **Fechas y ventanas:** cruzar medianoche y cierre de mes (particiones de Cassandra, estados temporales, saldo anterior).
- **Autorización:** intentar acceder a envíos, reportes o mensajes de otro cliente.
- **Datos incompletos:** sensor sin GPS, tramo sin llegada real, incidente sin tramo, envío no entregado.
- **Falla parcial:** reiniciar el consumidor de eventos (outbox) tras un error y demostrar que sincroniza sin duplicar.
- **Analítica:** contrastar las consultas con cálculos manuales sobre un subconjunto chico.

---

## Persona 1 — PostgreSQL, ABM, trayectos, finanzas y reportes SQL

| Nº | Requisito | Qué desarrollar | Prueba de aceptación | Estado 04/11 |
|---|---|---|---|---|
| R01 | ABM clientes | CRUD + validación de CUIT y unicidad | Alta, edición, listado y baja lógica; duplicado rechazado | SI (falta evidencia) |
| R02 | ABM usuarios y roles | CRUD + asignación y comprobación de permisos | Usuario con rol distinto recibe acceso distinto | PARCIAL: falta pantalla/API administrativa |
| R03 | ABM sesiones activas | Expiración, revocación y consulta de sesión vigente | Sesión expirada/revocada no accede; activa sí | SI (falta evidencia) |
| R04 | ABM proveedores | CRUD con estados y unicidad de CUIT | Alta y consulta desde un tramo o factura | PARCIAL: falta vínculo con tramos, facturas y pagos |
| R05 | ABM operadores logísticos | CRUD de operadores, vehículos y relaciones | Operador visible en tramos, facturación y grafo | PARCIAL: falta vínculo con trayectos, facturas y Neo4j |
| R06 | ABM envíos → contenedores → sensores | CRUD y navegación relacional completa | Un envío muestra sus contenedores y sus sensores | PARCIAL: pantalla de envíos deshabilitada |
| R07 | ABM contenedores de un envío | Asociar/desasociar con vigencia histórica | Asociación activa e histórica verificables | PARCIAL: falta pantalla y prueba histórica |
| R08 | ABM sensores de un contenedor | Asociar/desasociar, controlar pertenencia temporal | Sensores correctos por contenedor y por fecha | PARCIAL: falta interfaz y casos límite |
| R24 | Gestión de trayectos de cada envío (seguimiento) | Orden, vehículo, operador, fechas y estados de los tramos | Un envío devuelve sus tramos en secuencia correcta | PARCIAL: tramos y vistas en PostgreSQL, falta flujo en la app |
| R10 | Cuenta corriente: facturación y cobros a clientes | Factura + detalle + pago + movimiento/conciliación | Cobro parcial/total modifica el saldo sin duplicar | NO: hay funciones SQL, falta backend y pantalla |
| R12 | Facturación automática | Tarifa + servicio que crea la factura según un hito | Dos ejecuciones del lote generan una sola factura | NO |
| R13 | Registro de pagos | CRUD de pago e imputación verificable | El pago figura en factura, cuenta y movimientos | NO |
| R14 | Gestión de cuentas corrientes | Débitos, créditos, saldo y trazabilidad | Saldo = suma de movimientos | NO |
| R15 | Cierre mensual de cuentas corrientes | `cierre_cuenta` + consolidación + bloqueo | El cierre reproduce el saldo; no se duplica | NO |
| R16 | Facturación y pagos a proveedores y operadores | Distinguir terceros y egresos | Factura y pago de cada tercero se reflejan en su cuenta | NO |
| R35 | Reporte de envíos (país, estado, cliente, demorados, incidentes abiertos) | Filtros combinables | Cada filtro y combinación da la salida correcta | NO: hay vista SQL |
| R36 | Tiempos máximo y mínimo de envío por cliente | Fechas reales, excluir envíos sin entrega | Mín/máx consistentes con el dataset | NO: hay vista SQL |
| R37 | Promedio de incidentes por país / estado / cliente | Denominador explícito | Promedios reproducibles por dimensión | NO: hay vista SQL |
| R38 | Tiempo promedio de entrega | Duración real solo de envíos finalizados | La media manual coincide con el reporte | NO: hay vista SQL |
| R39 | Costos por tipo de transporte | Agrupar `costo_real` de tramos por medio | Totales aéreo/marítimo/terrestre coinciden | NO: hay vista SQL |

Además: `docker-compose.yml` único con los cuatro motores y seguridad (roles, permisos, propiedad del envío en las escrituras).

## Persona 2 — Cassandra, MongoDB, Neo4j, IoT, alertas, mensajería y reportes

| Nº | Requisito | Qué desarrollar | Prueba de aceptación | Estado 04/11 |
|---|---|---|---|---|
| R18 | Registro continuo de lecturas IoT | Ingesta y escritura en las tablas desnormalizadas | Serie de lecturas consecutivas consultable | NO: el simulador llama a `POST /api/v1/iot/lecturas`, que no existe |
| R19 | Consulta histórica de lecturas (contenedor y sensor) | Consultas por rango uniendo días | Histórico de ambos en un intervalo de dos días | NO: falta servicio por rango y vista |
| R20 | Reconstrucción del recorrido de un contenedor | Ordenar posiciones GPS y armar la secuencia | Mapa dibuja el itinerario con puntos en el tiempo | NO |
| R21 | Visualización geográfica de posiciones GPS | Última posición y múltiples posiciones válidas | Mapa muestra ubicación y trayectoria | NO: no hay pantalla de mapa (propuesta: WebView con mapa embebido) |
| R22 | Control de estado de sensores | Estado por última lectura, batería e inactividad | Sensor sin lecturas queda marcado inactivo | NO: falta calcular online/offline, batería y última señal |
| R09 | ABM reglas y alertas | `regla_alerta` en PostgreSQL + alerta ejecutada y guardada en MongoDB | Una regla nueva superada por una lectura genera una alerta | NO: falta ABM de reglas, evaluación de lecturas, persistencia en Mongo y pantalla |
| R23 | Alertas de estado de los sensores | Umbrales de batería baja y sin conexión, tarea periódica | Alerta visible con condición y timestamp | NO |
| R26 | Generación de alertas | Evaluar, persistir, consultar y cerrar alertas | Una regla activa produce una alerta que cambia de estado | NO: falta crear, reconocer, cerrar y deduplicar desde la app |
| R25 | Detección de incidentes | Reglas/eventos que crean un incidente (tramo opcional) | Un evento de prueba crea un incidente consultable | NO: falta el detector que convierte lecturas anómalas en incidentes |
| R32 | Detección de anomalías de temperatura | Umbral o salida anómala sobre lecturas históricas | Lectura fuera de rango produce detección (alerta/incidente) inspeccionable | NO |
| R17 | Monitoreo de envíos, sensores, contenedores y eventos | Vista integrada PostgreSQL + Cassandra + MongoDB | Se ve un envío con sus lecturas y eventos asociados | NO: no hay consulta conjunta ni pantalla |
| R40 | Mensajería con clientes, proveedores y operadores | Conversaciones privadas y grupales con permisos | Cliente, proveedor y operador envían y consultan; un ajeno no ve nada | NO: falta API de chat, permisos e interfaz |
| R30 | Red de relaciones clientes–operadores–envíos | Grafo sincronizado con IDs compartidos | Cypher obtiene la red de un cliente concreto | NO |
| R29 | Predicción de atrasos | ETA/riesgo sobre históricos + validación | Pronóstico para un envío aún no entregado | NO: hay función baseline SQL |
| R34 | Reportes predictivos (atrasos y riesgos) | Guardar predicción, riesgo y explicación | Reporte incluye ETA/score de envíos actuales | NO |
| R11 | Solicitud de reportes | Solicitud parametrizada con estado y resultado | El usuario solicita y descarga/visualiza el reporte | NO |
| R33 | Reportes operativos y analíticos parametrizados (facturables) | Parámetros, resultado, tarifa y cargo | Reporte con filtros genera precio y factura vinculada | NO |
| R27 | Ranking de rutas con más incidentes | Agrupar incidentes por ruta/tramo definido | Ranking ordenado con recuentos comprobables | NO: hay vista SQL |
| R28 | Ranking de clientes por volumen transportado | `SUM(volumen_m3)` con criterio de envíos válido | Orden descendente coincide con el dataset | NO: hay vista SQL |
| R31 | Análisis histórico de desempeño | KPI por período y entidad | Comparar dos períodos con resultados distintos | NO: hay vista SQL mensual |

Además: conectar MongoDB y Cassandra a Spring Boot con health check real, y el worker que lee `outbox_evento` y actualiza Neo4j sin duplicar.

## Persona 3 — Testing de los 40 requisitos

- Catálogo único de UUID y dataset que carga las cuatro bases desde cero.
- Conexión de Neo4j a Spring Boot con health check real.
- Una prueba de aceptación automatizada por requisito + chequeo desde JavaFX.
- Casos adversos (sección de arriba) y analítica contra cálculos manuales.
- Evidencias en `evidencias/R01`–`R40`, README de arranque y guion de la demo.
- Solo Persona 3 marca un requisito como terminado.

---

## Dependencias entre módulos

- **R09, R25, R26, R32 (alertas e incidentes):** las reglas viven en PostgreSQL (Persona 1); la
  evaluación y las alertas en MongoDB (Persona 2); las alertas ALTA/CRITICA crean un incidente en PostgreSQL.
- **R17 (monitoreo):** combina datos de PostgreSQL, Cassandra y MongoDB.
- **R18–R22:** necesitan las asignaciones sensor–contenedor vigentes de PostgreSQL (R08).
- **R30 y sincronización:** cambios en PostgreSQL → tabla `outbox_evento` → worker que actualiza
  MongoDB/Neo4j con reintentos e idempotencia.
- **R11, R33 (reportes facturables):** solicitud y factura en PostgreSQL; resultado en MongoDB
  (`reportes_generados`).

## Demo integrada

Recorrido continuo para la defensa: crear envío → asignar contenedor y sensor → el simulador manda
lecturas IoT/GPS → se ven en histórico y mapa → una temperatura fuera de rango dispara alerta e
incidente → el evento queda trazado → reporte → factura → cierre del mes.
