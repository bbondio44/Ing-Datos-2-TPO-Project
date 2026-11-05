# R06 — ABM envíos, contenedores y sensores

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Envíos → Detalle y trazabilidad
- **Pasos:** Crear un envío y abrir su detalle.
- **Datos de origen:** Envío Buenos Aires → Valencia de Transportes del Sur SA.
- **Resultado esperado:** El envío muestra sus contenedores y sensores, vigentes e históricos.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-envios.png`
- `captura-02-detalle-envio.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
