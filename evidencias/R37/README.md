# R37 — Promedio de incidentes

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Reportes → R37 Incidentes
- **Pasos:** Abrir y contrastar con SQL.
- **Datos de origen:** Denominador: envíos no cancelados del grupo.
- **Resultado esperado:** Totales y promedio coinciden.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-reporte.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
