# Notas para testing · Persona 2

Estado al 04/11/2026. Los 20 requisitos de Persona 2 están probados contra las bases reales y la salida de
esas pruebas está en `evidencias/RXX/prueba-automatizada.txt`. Falta repetir cada uno desde JavaFX y
guardar la captura.

## Cómo levantarlo

Seguir "Arranque con las cuatro bases" en el `README.md`. Para repetir todas las pruebas automáticas, con el
backend apagado:

```powershell
mvn -B -DskipTests install
powershell -ExecutionPolicy Bypass -File .\scripts\pruebas\correr-todo.ps1
```

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
| R27 | Analítica → Rankings y desempeño | Elegir "Rutas con más incidentes" | Orden descendente; solo cuenta incidentes con tramo |
| R28 | Analítica → Rankings y desempeño | Elegir "Clientes por volumen" | Suma de `volumen_m3`, sin envíos cancelados |
| R31 | Analítica → Rankings y desempeño | Elegir "Desempeño por período" | Un renglón por mes, con resultados distintos |
| R29 | Analítica → Predicción de atrasos | "Generar predicción del día" dos veces | La segunda no agrega nada; "Ver validación" muestra el error medio |
| R34 | Analítica → Reportes | Tipo `PREDICTIVO`, cliente `0bca0b45-e7f6-59b2-a455-02191ee367f6` | ETA, riesgo y factores de sus envíos en tránsito |
| R11 | Analítica → Reportes | Solicitar y elegir la fila | Estado `GENERADO` y el resultado a la derecha, leído de Mongo |
| R33 | Analítica → Reportes | "Facturar" dos veces | Estado `FACTURADO` y una sola factura. Un cliente sin cuenta corriente da error |
| R30 | Analítica → Red del cliente | UUID del cliente C02: `270823a9-7978-5eda-80f0-9ae673331b0b` | Sus envíos, tramos y operadores, iguales a Postgres |

El botón "Reportes" del menú abre los reportes SQL de Persona 1 (R35 a R39). Los de esta guía están en "Analítica".

## Casos adversos ya cubiertos

- Reenviar la misma lectura no duplica filas ni suma ocurrencias.
- Una lectura tardía va al historial y no pisa `ultima_*`.
- Una lectura sin GPS no cambia la última posición.
- Con Cassandra caída, la ingesta responde 503 y el reintento funciona.
- Con Mongo caído, la lectura se guarda y la API responde 503. El reintento con el mismo id no duplica y genera la alerta.
- Un rango que cruza medianoche lee las dos particiones diarias.
- Un usuario con rol CLIENTE ve lo suyo; lo de otro cliente responde 404 y lo que no le corresponde, 403.
- Reprocesar todo el outbox deja los mismos nodos y relaciones en Neo4j.
- Con Neo4j caído los eventos quedan en `ERROR` y se sincronizan solos al volver.

## Pendiente de definir

- Un incidente solo tiene tramo si el envío estaba recorriendo uno en ese instante. Sin tramo, no suma en R27.
