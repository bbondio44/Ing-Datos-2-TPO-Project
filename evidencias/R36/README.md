# R36 — Tiempos máximo y mínimo por cliente

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Reportes → R36 Máx./mín.
- **Pasos:** Abrir y contrastar con SQL.
- **Datos de origen:** Envíos entregados.
- **Resultado esperado:** Mínimo y máximo coinciden; un cliente sin entregas no aparece.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-reporte.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
