# R10 — Cuenta corriente: facturación y cobros

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Facturación
- **Pasos:** Nueva factura, Emitir, Registrar pago parcial y total.
- **Datos de origen:** Cliente creado desde la pantalla.
- **Resultado esperado:** El saldo cambia con cada cobro y la factura termina PAGADA.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-facturas-cliente.png`
- `captura-02-cobro-parcial.png`
- `captura-03-cobro-total.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
