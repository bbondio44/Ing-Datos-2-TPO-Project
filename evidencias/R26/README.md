# R26 — Generación de alertas

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Alertas
- **Pasos:** Reconocer y cerrar una alerta; repetir la acción.
- **Datos de origen:** Colección `alertas`.
- **Resultado esperado:** ABIERTA → RECONOCIDA → CERRADA; repetir da error; no se duplican.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-alertas.png`
- `captura-02-alerta-reconocida.png`
- `captura-03-alerta-cerrada.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
