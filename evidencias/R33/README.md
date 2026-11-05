# R33 — Reportes parametrizados facturables

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Analítica → Reportes
- **Pasos:** Solicitar y tocar Facturar dos veces.
- **Datos de origen:** Tarifa REPORTE y `solicitud_reporte`.
- **Resultado esperado:** Estado FACTURADO y una sola factura. Si el cliente no tiene cuenta corriente, se crea.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-reporte-facturado.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
