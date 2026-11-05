# R29 — Predicción de atrasos

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Analítica → Predicción de atrasos
- **Pasos:** Generar la predicción del día dos veces y ver la validación.
- **Datos de origen:** `prediccion_envio`.
- **Resultado esperado:** Una predicción por envío en tránsito, con riesgo operativo y climático; la segunda corrida no agrega nada.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-predicciones.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
