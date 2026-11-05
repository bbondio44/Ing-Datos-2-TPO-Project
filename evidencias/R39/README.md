# R39 — Costos por tipo de transporte

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Reportes → R39 Costos
- **Pasos:** Abrir y sumar `costo_real` por medio.
- **Datos de origen:** Tabla `tramo`.
- **Resultado esperado:** Los totales por medio coinciden.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-reporte.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
