# R19 — Consulta histórica de lecturas

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Monitoreo IoT → Histórico de lecturas
- **Pasos:** Sensor SEN-001 del 20/10 al 21/10/2026.
- **Datos de origen:** Particiones diarias de Cassandra.
- **Resultado esperado:** Lecturas de los dos días, ordenadas por fecha.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-historico-dos-dias.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
