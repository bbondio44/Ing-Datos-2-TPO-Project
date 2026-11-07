# Cumplimiento del enunciado

Comparación del `TP_Persistencia_Poliglota` y de las "Condiciones necesarias para Promocionar" con lo
implementado, al 05/11/2026. Los 40 puntos de promoción están en `docs/requisitos.md` y sus pruebas en
`evidencias/RXX/`; acá está lo que el enunciado pide además de esa lista.

| Sección | Pedido | Dónde está |
|---|---|---|
| Persistencia | Al menos tres paradigmas | Cuatro: relacional, documental, tabular y grafos |
| Trazabilidad | Historial completo de eventos | `eventos_logisticos`; el worker del outbox registra los cambios de estado de envíos y tramos |
| Trazabilidad | Seguimiento temporal del envío | Alertas → Monitoreo de envío |
| Trazabilidad | Reconstrucción del recorrido | Monitoreo IoT → Seguimiento del viaje |
| Comunicación | Mensajería privada y grupal, historial | Alertas → Mensajería; `GrupoDeTrabajo` es la conversación grupal |
| Consultas avanzadas | Ruta completa de un envío | Trayectos y Analítica → Red del cliente |
| Consultas avanzadas | Red de relaciones | Analítica → Red del cliente |
| Consultas avanzadas | Historial de movimientos de un contenedor | Envíos → Catálogos → Historial de movimientos, y recorrido GPS |
| Procesos periódicos | Facturas | `AutomaticBillingJob`, cada día a las 2 |
| Procesos periódicos | Sensores inactivos | `AlertaScheduler`, cada 60 s |
| Procesos periódicos | Cierre mensual | `MonthlyCloseJob`, el primer día del mes |
| Procesos periódicos | Predicción diaria de riesgos | `PrediccionService`, cada día a las 6 UTC |
| Entidad Predicción | Probabilidad de demora, riesgo climático y operativo | `prediccion_envio`; los riesgos están en `variables` |
| Arquitectura | Separación por capas | Controller, servicio y repositorio; `IntegrationStatusController` consulta directo porque es un health check |
| Arquitectura | Manejo de errores | `GlobalExceptionHandler`, con un formato único de error |
| Arquitectura | Registro de logs | SLF4J con id de correlación; los conflictos registran su motivo |
| Calidad | Casos y datos de prueba | `backend/src/test`, `desktop/src/test`, `scripts/pruebas`, dataset en `database/` y `tp-grupo4-mongo-cassandra/` |
| Calidad | Controles de integridad | FK y CHECK en PostgreSQL, validadores en MongoDB, control cruzado Mongo → PostgreSQL en `demo-integrada.ps1` |
| Entregables | Modelos físicos | `docs/modelo-fisico-mongodb-cassandra.md`, `docs/modelo-fisico-neo4j.md`, `database/referencia-postgresql/DER_ACTUALIZADO.mmd` |
| Entregables | Scripts de creación | `database/init`, `database/cassandra`, `database/neo4j`, `tp-grupo4-mongo-cassandra/mongodb` |

## Decisiones a tener presentes en la defensa

- **Riesgo climático:** no hay datos meteorológicos. Es una estimación fija por medio de transporte
  (marítimo 0,50, aéreo 0,35, terrestre 0,20) y así figura en `criterio_riesgos`.
- **Riesgo operativo:** 60 % incidentes abiertos del envío, con tope 3, y 40 % incidentes por envío de la ruta.
- **Chat público:** el objetivo dice "privado / publico" y los requisitos dicen "privada" y "grupal". Se
  implementó privada y grupal; no hay un canal abierto a todos, porque un cliente no puede ver lo ajeno.
- **Reclamos:** la primera entrega nombra reclamos en MongoDB. No están en el enunciado ni en los 40
  puntos, así que no se implementaron.
- **Formato de reportes:** el resultado se guarda y se ve como JSON en la aplicación; no se exporta a PDF.
- **Informe técnico y justificación:** el enunciado los lista como entregables. El contenido técnico está en
  `docs/`, y los seis puntos del "Desafío de Complejidad Adicional" se responden al inicio y en la sección 3 de
  `docs/modelo-fisico-mongodb-cassandra.md`.
