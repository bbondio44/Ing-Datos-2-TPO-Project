# Matriz de promoción R01 a R40

Estados permitidos: `PENDIENTE`, `IMPLEMENTADO`, `PROBADO`. La existencia de SQL o código sin ejecución no habilita `PROBADO`.

| ID | Requisito | Pantalla | Servicio o fuente | Prueba y evidencia | Estado |
|---|---|---|---|---|---|
| R01 | ABM clientes | Clientes | ClientService, cliente | Alta, edición, baja lógica y CUIT duplicado | IMPLEMENTADO |
| R02 | Usuarios y roles | Administración | Auth y permisos PG | Roles distintos permiten acciones distintas | IMPLEMENTADO |
| R03 | Sesiones activas | Perfil y sesiones | AuthService, sesion | Login, expiración, logout y revocación | IMPLEMENTADO |
| R04 | Proveedores | Proveedores | proveedor | CRUD y uso en tramo/factura | IMPLEMENTADO |
| R05 | Operadores logísticos | Operadores | operador, vehiculo | CRUD, tramo, factura y grafo | IMPLEMENTADO |
| R06 | Envíos contenedores sensores | Envíos | logística PG | Navegación completa con datos | IMPLEMENTADO |
| R07 | Contenedores de envío | Envío detalle | envio_contenedor | Asociación vigente e histórica | IMPLEMENTADO |
| R08 | Sensores de contenedor | Contenedor detalle | asignacion_sensor_contenedor | Consulta por fecha y solapamiento rechazado | IMPLEMENTADO |
| R09 | Reglas y alertas | Alertas | regla PG, Cassandra, Mongo | Umbral real genera alerta | IMPLEMENTADO |
| R10 | Facturación clientes | Finanzas | funciones PG | Parcial y total con saldo conciliado | IMPLEMENTADO |
| R11 | Solicitud reportes | Reportes | solicitud PG y resultado Mongo | Solicitar, ejecutar y descargar | IMPLEMENTADO |
| R12 | Facturación automática | Jobs | facturar_envio/reporte | Dos ejecuciones crean una factura | IMPLEMENTADO |
| R13 | Pagos | Finanzas | registrar_pago | Imputación, duplicado y sobrepago | IMPLEMENTADO |
| R14 | Cuentas corrientes | Cuentas | movimientos y conciliación | Saldo coincide con movimientos | IMPLEMENTADO |
| R15 | Cierre mensual | Cuentas | cerrar_cuenta | Cierre único y retroactivo rechazado | IMPLEMENTADO |
| R16 | Finanzas terceros | Cuentas terceros | PG naturaleza PAGAR | Proveedor y operador conciliados | IMPLEMENTADO |
| R17 | Monitoreo integral | Dashboard IoT | PG Cassandra Mongo | Envío con lectura y eventos | IMPLEMENTADO |
| R18 | Ingesta continua | Simulador | Cassandra | Lecturas consecutivas persistidas | IMPLEMENTADO |
| R19 | Histórico lecturas | IoT histórico | Cassandra por días | Rango que cruza dos días | IMPLEMENTADO |
| R20 | Recorrido contenedor | Seguimiento del viaje | posiciones Cassandra | Trayectoria ordenada | IMPLEMENTADO |
| R21 | GPS | Seguimiento del viaje | última posición e histórico | Punto actual y trayectoria | IMPLEMENTADO |
| R22 | Estado sensores | Sensores | última lectura Cassandra | Activo, inactivo y batería | IMPLEMENTADO |
| R23 | Alertas sensor | Alertas | reglas y Mongo | Batería, inactividad y umbral | IMPLEMENTADO |
| R24 | Trayectos | Tramos | tramo y Neo4j | Orden, medio, vehículo y operador | IMPLEMENTADO |
| R25 | Incidentes | Incidentes | incidente PG | Evento genera incidente relacionado | IMPLEMENTADO |
| R26 | Ciclo alerta | Alertas | MongoDB | Abrir, reconocer, cerrar y deduplicar | IMPLEMENTADO |
| R27 | Ranking rutas | Analítica | vista PG | Conteos manuales coinciden | IMPLEMENTADO |
| R28 | Ranking clientes | Analítica | vista PG | Volumen no cancelado coincide | IMPLEMENTADO |
| R29 | Predicción atrasos | Predicciones | baseline PG | Envío activo con ETA y evaluación | IMPLEMENTADO |
| R30 | Red relaciones | Grafo | Neo4j | Red de un cliente con UUID reales, cargada por el worker del outbox | IMPLEMENTADO |
| R31 | Histórico desempeño | Analítica | vista PG | Dos períodos y denominadores | IMPLEMENTADO |
| R32 | Anomalía temperatura | IoT y alertas | Cassandra Mongo PG | Lectura anómala visible | IMPLEMENTADO |
| R33 | Reportes facturables | Reportes | PG Mongo exportación | Filtros, artefacto y factura | IMPLEMENTADO |
| R34 | Reportes predictivos | Reportes | predicciones | ETA, score, factores y límites | IMPLEMENTADO |
| R35 | Reportes envíos | Reportes | v_envios_reporte | Filtros combinables | IMPLEMENTADO |
| R36 | Máximo mínimo por cliente | Analítica | vista PG | Solo entregados y cálculo manual | IMPLEMENTADO |
| R37 | Promedio incidentes | Analítica | vista PG | Dimensiones y denominador | IMPLEMENTADO |
| R38 | Promedio entrega | Analítica | vista PG | Dataset reducido coincide | IMPLEMENTADO |
| R39 | Costos transporte | Analítica | vista PG | Sumas por medio coinciden | IMPLEMENTADO |
| R40 | Mensajería | Mensajes | MongoDB | Privado grupal público y aislamiento | IMPLEMENTADO |

