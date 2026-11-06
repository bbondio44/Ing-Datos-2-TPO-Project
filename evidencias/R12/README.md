# R12 — Facturación automática

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Facturación → Facturación automática
- **Pasos:** Correr el lote dos veces; una cuenta con el mes cerrado se saltea.
- **Datos de origen:** Tarifa TRANSPORTE vigente.
- **Resultado esperado:** La segunda corrida no crea nada y una cuenta cerrada no frena al resto.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-facturas-cliente.png`
- `captura-02-procesos-de-cierre.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
