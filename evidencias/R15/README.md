# R15 — Cierre mensual de cuentas corrientes

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Facturación → Cierre mensual y Cerrar todas las cuentas
- **Pasos:** Cerrar el período, repetirlo y probar uno anterior; cerrar todas las cuentas.
- **Datos de origen:** Cuenta del cliente creado desde la pantalla.
- **Resultado esperado:** El cierre informa el saldo que reproduce; repetir y retroceder se rechaza. También corre como tarea periódica.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-cierre-mensual.png`
- `captura-02-procesos-de-cierre.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
