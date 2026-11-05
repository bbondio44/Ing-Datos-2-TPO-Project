# R16 — Facturación y pagos a proveedores y operadores

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Facturación
- **Pasos:** Factura y pago en una cuenta de proveedor y una de operador.
- **Datos de origen:** Aduana y Servicios SA y Rutas Globales SA.
- **Resultado esperado:** Cada tercero lo refleja en su cuenta a pagar.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-cuenta-proveedor.png`
- `captura-02-cuenta-operador.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
