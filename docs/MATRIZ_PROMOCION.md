# Matriz de promoción R01 a R40

Estados permitidos: `PENDIENTE`, `IMPLEMENTADO`, `PROBADO`. La existencia de SQL o código sin ejecución no habilita `PROBADO`.

| ID | Requisito | Pantalla | Servicio o fuente | Prueba y evidencia | Estado |
|---|---|---|---|---|---|
| R01 | ABM clientes | Clientes | ClientService, cliente | Alta, edición, baja lógica y CUIT duplicado | IMPLEMENTADO |
| R02 | Usuarios y roles | Administración | Auth y permisos PG | Roles distintos permiten acciones distintas | PENDIENTE |
| R03 | Sesiones activas | Perfil y sesiones | AuthService, sesion | Login, expiración, logout y revocación | IMPLEMENTADO |
| R04 | Proveedores | Proveedores | proveedor | CRUD y uso en tramo/factura | PENDIENTE |
| R05 | Operadores logísticos | Operadores | operador, vehiculo | CRUD, tramo, factura y grafo | PENDIENTE |
| R06 | Envíos contenedores sensores | Envíos | logística PG | Navegación completa con datos | PENDIENTE |
| R07 | Contenedores de envío | Envío detalle | envio_contenedor | Asociación vigente e histórica | PENDIENTE |
| R08 | Sensores de contenedor | Contenedor detalle | asignacion_sensor_contenedor | Consulta por fecha y solapamiento rechazado | PENDIENTE |
| R09 | Reglas y alertas | Alertas | regla PG, Cassandra, Mongo | Umbral real genera alerta | PENDIENTE |
| R10 | Facturación clientes | Finanzas | funciones PG | Parcial y total con saldo conciliado | PENDIENTE |
| R11 | Solicitud reportes | Reportes | solicitud PG y resultado Mongo | Solicitar, ejecutar y descargar | PENDIENTE |
| R12 | Facturación automática | Jobs | facturar_envio/reporte | Dos ejecuciones crean una factura | PENDIENTE |
| R13 | Pagos | Finanzas | registrar_pago | Imputación, duplicado y sobrepago | PENDIENTE |
| R14 | Cuentas corrientes | Cuentas | movimientos y conciliación | Saldo coincide con movimientos | PENDIENTE |
| R15 | Cierre mensual | Cuentas | cerrar_cuenta | Cierre único y retroactivo rechazado | PENDIENTE |
| R16 | Finanzas terceros | Cuentas terceros | PG naturaleza PAGAR | Proveedor y operador conciliados | PENDIENTE |
| R17 | Monitoreo integral | Dashboard IoT | PG Cassandra Mongo | Envío con lectura y eventos | PENDIENTE |
| R18 | Ingesta continua | Simulador | Cassandra | Lecturas consecutivas persistidas | PENDIENTE |
| R19 | Histórico lecturas | IoT histórico | Cassandra por días | Rango que cruza dos días | PENDIENTE |
| R20 | Recorrido contenedor | Mapa | posiciones Cassandra | Trayectoria ordenada | PENDIENTE |
| R21 | GPS | Mapa | última posición e histórico | Punto actual y trayectoria | PENDIENTE |
| R22 | Estado sensores | Sensores | última lectura Cassandra | Activo, inactivo y batería | PENDIENTE |
| R23 | Alertas sensor | Alertas | reglas y Mongo | Batería, inactividad y umbral | PENDIENTE |
| R24 | Trayectos | Tramos | tramo y Neo4j | Proyección Neo4j validada; CRUD/API de tramos pendiente | PENDIENTE |
| R25 | Incidentes | Incidentes | incidente PG | Evento genera incidente relacionado | PENDIENTE |
| R26 | Ciclo alerta | Alertas | MongoDB | Abrir, reconocer, cerrar y deduplicar | PENDIENTE |
| R27 | Ranking rutas | Analítica | vista PG | Conteos manuales coinciden | PENDIENTE |
| R28 | Ranking clientes | Analítica | vista PG | Volumen no cancelado coincide | PENDIENTE |
| R29 | Predicción atrasos | Predicciones | baseline PG | Envío activo con ETA y evaluación | PENDIENTE |
| R30 | Red relaciones | Grafo | Neo4j | Cypher probado con cliente `cli-001` del backup real | PROBADO |
| R31 | Histórico desempeño | Analítica | vista PG | Dos períodos y denominadores | PENDIENTE |
| R32 | Anomalía temperatura | IoT y alertas | Cassandra Mongo PG | Lectura anómala visible | PENDIENTE |
| R33 | Reportes facturables | Reportes | PG Mongo exportación | Filtros, artefacto y factura | PENDIENTE |
| R34 | Reportes predictivos | Reportes | predicciones | ETA, score, factores y límites | PENDIENTE |
| R35 | Reportes envíos | Reportes | v_envios_reporte | Filtros combinables | PENDIENTE |
| R36 | Máximo mínimo por cliente | Analítica | vista PG | Solo entregados y cálculo manual | PENDIENTE |
| R37 | Promedio incidentes | Analítica | vista PG | Dimensiones y denominador | PENDIENTE |
| R38 | Promedio entrega | Analítica | vista PG | Dataset reducido coincide | PENDIENTE |
| R39 | Costos transporte | Analítica | vista PG | Sumas por medio coinciden | PENDIENTE |
| R40 | Mensajería | Mensajes | MongoDB | Privado grupal público y aislamiento | PENDIENTE |

