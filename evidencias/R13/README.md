# R13 — Registro de pagos

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Facturación → Registrar pago
- **Pasos:** Pagar, repetir la clave y superar el pendiente.
- **Datos de origen:** Factura creada por la prueba.
- **Resultado esperado:** El pago figura en factura y cuenta; duplicado y sobrepago se rechazan.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-cobro-parcial.png`
- `captura-02-cobro-total.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
