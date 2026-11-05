# R11 — Solicitud de reportes

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Analítica → Reportes
- **Pasos:** Solicitar un reporte y elegir la fila.
- **Datos de origen:** Solicitudes en PostgreSQL y resultados en `reportes_generados`.
- **Resultado esperado:** Estado GENERADO y resultado leído de MongoDB.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-reporte-predictivo.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
