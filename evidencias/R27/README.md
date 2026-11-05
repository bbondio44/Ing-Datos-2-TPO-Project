# R27 — Ranking de rutas con más incidentes

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Analítica → Rankings y desempeño
- **Pasos:** Elegir Rutas con más incidentes.
- **Datos de origen:** `v_ranking_rutas_incidentes`.
- **Resultado esperado:** Orden descendente; los recuentos coinciden con `incidente`. Solo cuenta incidentes con tramo.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-resultado.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
