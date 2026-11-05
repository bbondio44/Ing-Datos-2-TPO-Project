# R25 — Detección de incidentes

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Alertas y Monitoreo de envío
- **Pasos:** Superar una regla ALTA o CRITICA.
- **Datos de origen:** Tabla `incidente` y alertas de MongoDB.
- **Resultado esperado:** La alerta guarda el incidente, que existe en PostgreSQL. Todo incidente referenciado desde MongoDB existe en PostgreSQL.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-alerta-temperatura.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
