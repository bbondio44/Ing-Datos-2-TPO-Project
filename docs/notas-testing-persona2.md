# Notas para testing · Persona 2

Estado al 04/11/2026. Ya están probados contra las bases reales en Docker y con tests automatizados del
backend. Falta la evidencia en `evidencias/RXX/` y, en cada uno, repetir el chequeo desde JavaFX.

## Cómo levantarlo

1. `docker compose up -d` y esperar a que Cassandra esté `healthy`.
2. Cargar datos: Postgres con `database/init` + `03_datos_demo.sql` + `04` + `06_datos_catalogo.sql`;
   Mongo y Cassandra con `tp-grupo4-mongo-cassandra/cargar_todo.ps1`.
3. Correr el backend con el perfil `dev,polyglot`.
4. En `.env`, definir `APP_BOOTSTRAP_ADMIN_EMAIL` y `APP_BOOTSTRAP_ADMIN_PASSWORD` (12 caracteres o más)
   para tener un usuario con clave válida.

Sensores con asignación vigente hoy: los 3 sensores demo (`v_envio_contenedor_sensor_activo`). Las
asignaciones del catálogo terminan el 23/10/2026: sus lecturas nuevas de hoy responden 409. Las lecturas
históricas del catálogo son del 20 al 22/10/2026.

## Requisitos

| Req. | Dónde | Pasos | Resultado esperado |
|---|---|---|---|
| R18 | `POST /api/v1/iot/lecturas`, simulador con `SENSOR_ID` y `API_TOKEN` | Correr el simulador con un sensor demo | Responde 201 y las lecturas aparecen en el histórico |
| R19 | Monitoreo IoT → Histórico | Sensor SEN-001, del 20/10 al 21/10/2026 | Lecturas de los dos días, ordenadas por fecha |
| R20 | Monitoreo IoT → Recorrido y mapa | Contenedor MSKU0000001, del 20/10 al 22/10/2026 | Línea en el mapa con los puntos en orden |
| R21 | Monitoreo IoT → Recorrido y mapa | El mismo contenedor | Marcador rojo en la última posición. Necesita internet |
| R22 | Monitoreo IoT → Estado de sensores | Abrir la pestaña | SEN-040 en `SIN_DATOS`; sin señal por más de 120 min en `OFFLINE` |
| R09 | Alertas → Reglas | Crear una regla de temperatura y mandar una lectura que la supere | Alerta nueva en la pestaña Alertas |
| R26 | Alertas | Reconocer y después cerrar | ABIERTA → RECONOCIDA → CERRADA; repetir la acción da error |
| R23 | Alertas | Mandar batería menor a 15, o dejar de enviar lecturas de un sensor vigente | Alerta de batería o inactividad (tarea cada 60 s) |
| R25 | Alertas y Monitoreo de envío | Una regla ALTA o CRITICA superada | La alerta muestra incidente y el incidente está en Postgres |
| R32 | Alertas | Sensor con 10 lecturas o más y una temperatura muy alejada | Alerta `ANOMALIA_TEMPERATURA` con la lectura que la causó |
| R17 | Alertas → Monitoreo de envío | UUID del envío E002: `ec22d634-96cd-5267-b7bd-accdce959d92` | Datos de Postgres, lecturas de Cassandra y eventos de Mongo |
| R40 | Alertas → Mensajería, API en `/api/v1/mensajeria/conversaciones` | Entrar con usuarios de distintos clientes | Cada uno ve solo sus conversaciones; lo ajeno responde 404 |

## Casos adversos ya cubiertos

- Reenviar la misma lectura no duplica filas ni suma ocurrencias.
- Una lectura tardía va al historial y no pisa `ultima_*`.
- Una lectura sin GPS no cambia la última posición.
- Con Cassandra caída, la ingesta responde 503 y el reintento funciona.
- Con Mongo caído, la lectura se guarda igual; las alertas de ese momento no se generan.
- Un rango que cruza medianoche lee las dos particiones diarias.

## Pendiente de definir

- En la ventana JavaFX no hay un botón para crear conversaciones: se crean por la API.
- Si Mongo estuvo caído, no se reevalúan después las lecturas de ese período.
