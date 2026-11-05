# R34 — Reportes predictivos

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Analítica → Reportes
- **Pasos:** Tipo PREDICTIVO, cliente `0bca0b45-e7f6-59b2-a455-02191ee367f6`.
- **Datos de origen:** `prediccion_envio` y `reportes_generados`.
- **Resultado esperado:** ETA, probabilidad de atraso y factores de sus envíos en tránsito.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-reporte-predictivo.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
