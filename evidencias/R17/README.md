# R17 — Monitoreo de envíos, sensores, contenedores y eventos

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Alertas → Monitoreo de envío
- **Pasos:** Consultar el envío E002: `ec22d634-96cd-5267-b7bd-accdce959d92`.
- **Datos de origen:** PostgreSQL, Cassandra y MongoDB.
- **Resultado esperado:** Datos del envío, última lectura de cada sensor, eventos, alertas e incidentes.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-monitoreo-envio.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
