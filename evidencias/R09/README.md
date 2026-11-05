# R09 — ABM reglas y alertas

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Alertas → Reglas
- **Pasos:** Crear una regla, mandar una lectura que la supere, editar y desactivar.
- **Datos de origen:** Sensores demo y reglas de `regla_alerta`.
- **Resultado esperado:** La regla nueva genera una alerta en MongoDB.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-alerta-temperatura.png`
- `captura-02-regla-creada.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
