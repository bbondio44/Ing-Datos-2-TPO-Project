# Matriz de 40 requisitos de promoción — responsabilidad PostgreSQL vs integración

**Leyenda:** `PG` = estructura y/o consulta implementada en este paquete; `API` = necesita lógica/autorización en backend; `Cas` = Cassandra; `Mon` = MongoDB; `Neo` = Neo4j; `UI` = pantalla/mapa/descarga. La existencia de PG no significa funcionalidad ya demostrada.

| Nº | Exigencia del profesor | Soporte en este paquete | Pendiente necesario para marcar como operativo |
|---:|---|---|---|
| 01 | ABM clientes | `cliente`, CUIT y unicidad | API + UI alta, edición, baja lógica, permisos |
| 02 | ABM usuarios/roles | `usuario`, `rol`, `usuario_rol`, `permiso`, `rol_permiso`, `v_permisos_usuario` | API de usuarios, autenticación y autorización real |
| 03 | ABM sesiones activas | `sesion`, `v_sesiones_activas` | Emisión y revocación segura de sesiones por API |
| 04 | ABM proveedores | `proveedor` | API + UI y asociación a tramos/facturas |
| 05 | ABM operadores logísticos | `operador_logistico`, `vehiculo`, `tramo` | API + UI y sincronización Neo |
| 06 | ABM envíos-contenedores-sensores | `envio`, `envio_contenedor`, `asignacion_sensor_contenedor` | API de navegación y UI |
| 07 | ABM contenedores de envío | `envio_contenedor`, guardas temporales | API de asociación/desasociación |
| 08 | ABM sensores de contenedor | `asignacion_sensor_contenedor`, guardas temporales | API de asociación/desasociación |
| 09 | ABM reglas/alertas | `regla_alerta` | API de reglas + Cas (lecturas) + motor de evaluación + Mon (alertas) |
| 10 | Facturación/cobros clientes | `factura`, `detalle`, `pago`, `movimiento`, `cuenta` | API + UI y evidencia de cobro parcial/total |
| 11 | Solicitud de reportes | `solicitud_reporte` | API ejecución + Mon resultado + UI |
| 12 | Facturación automática | `tarifa_servicio`, `facturar_envio`, `facturar_reporte` | Disparadores de negocio/hitos desde API/worker |
| 13 | Registro de pagos | `pago`, `registrar_pago` | API + UI, medios de cobro reales/simulados |
| 14 | Gestión cuentas corrientes | `cuenta_corriente`, `movimiento`, vistas de conciliación | API y UI saldo + auditoría |
| 15 | Cierre mensual | `cierre_cuenta`, `cerrar_cuenta`, protección de períodos | API/UI selección y cierre |
| 16 | Facturación/pagos terceros | titular proveedor/operador, emisión + pago | API/UI egresos y conciliación |
| 17 | Monitorear envíos, sensores y eventos | vista PG de asignaciones/estado | Cas lecturas + Mon eventos + API/UI |
| 18 | Registro continuo IoT | `sensor_iot` catálogo | Cas escritura continua + simulador |
| 19 | Histórico de lecturas por contenedor/sensor | asociaciones temporales PG | Cas consultas por día/rango + API reconstrucción |
| 20 | Recorrido de contenedor | claves de contenedor/envío | Cas GPS + ordenar puntos + UI mapa |
| 21 | Visualización GPS | claves de contenedor | Cas GPS + UI mapa |
| 22 | Estado de sensores | `sensor_iot.estado` + fechas asociadas | Cas última lectura / batería / umbral inactividad |
| 23 | Alertas estado sensor | `regla_alerta` | Cas + motor de reglas + Mon alertas |
| 24 | Gestión de trayectos | `tramo`, `v_seguimiento_tramos` | API/UI orden y estados + Neo |
| 25 | Detección de incidentes | `incidente` asociado a tramo/envío/evento externo | Cas/Mon + motor detector + API |
| 26 | Generación de alertas | `regla_alerta` | Mon alerta y cambio de estado + worker |
| 27 | Ranking rutas por incidentes | `v_ranking_rutas_incidentes` | API/UI con filtros y evidencia |
| 28 | Ranking clientes por volumen | `v_ranking_clientes_volumen` | API/UI y prueba de sumatorias |
| 29 | Predicción de atrasos | `prediccion_envio`, `generar_prediccion_baseline` | Validar baseline históricamente + API/UI ETA/riesgo |
| 30 | Red clientes-operadores-envíos | IDs PG + `outbox_evento` | Neo MERGE de nodos/aristas + consulta grafo |
| 31 | Análisis histórico desempeño | `v_desempeno_mensual` | API/UI comparador de períodos |
| 32 | Anomalías de temperatura | `regla_alerta` + `incidente` | Cas temperatura + motor de anomalías + Mon alerta |
| 33 | Reportes parametrizados facturables | `solicitud_reporte`, `tarifa`, `facturar_reporte` | API SQL parametrizada + Mon resultado + exportación UI |
| 34 | Reportes predictivos | `prediccion_envio` + solicitudes | Ejecutar predicciones + Mon reportes + UI |
| 35 | Envíos por filtros | `v_envios_reporte` | API filtros país/estado/cliente/demora/incidente abierto |
| 36 | Tiempo máximo/mínimo por cliente | `v_tiempos_entrega_cliente` | UI/API y comparación manual |
| 37 | Promedio incidentes país/estado/cliente | `v_promedio_incidentes_dimension` | API/UI, documentar país destino/estado envío |
| 38 | Tiempo promedio entrega | `v_tiempo_promedio_entrega` | API/UI, comprobar 296 h en dataset |
| 39 | Costos por medio transporte | `v_costos_por_transporte` | API/UI, probar aéreo/marítimo/terrestre |
| 40 | Mensajería partes | UUID cliente/proveedor/operador/usuario | Mon conversaciones/mensajes + API permisos + UI |

**Condición del docente:** 40/40 flujos con resultados reales en la demo, no solamente tablas. Recomendación: guardar carpetas `evidencias/R01` ... `R40` con request, respuesta y captura, además de caso negativo cuando corresponda.
