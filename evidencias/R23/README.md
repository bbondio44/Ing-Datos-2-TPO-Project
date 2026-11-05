# R23 — Alertas de estado de los sensores

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Alertas
- **Pasos:** Mandar batería menor a 15 o dejar de enviar lecturas.
- **Datos de origen:** Tarea periódica cada 60 s.
- **Resultado esperado:** Alerta de batería o inactividad con condición y fecha.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-alerta-inactividad.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
