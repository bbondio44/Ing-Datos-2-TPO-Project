# R14 — Gestión de cuentas corrientes

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Facturación
- **Pasos:** Revisar saldo y movimientos de una cuenta.
- **Datos de origen:** `movimiento_cuenta` y `v_conciliacion_cuentas`.
- **Resultado esperado:** Saldo igual a la suma de movimientos; ninguna cuenta desconciliada.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-cuentas.png`
- `captura-02-cobro-total.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
