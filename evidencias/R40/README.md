# R40 — Mensajería con las partes

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Alertas → Mensajería
- **Pasos:** Entrar con usuarios de distintos clientes y enviar un mensaje.
- **Datos de origen:** Colecciones `conversaciones` y `mensajes`.
- **Resultado esperado:** Cada uno ve solo sus conversaciones; lo ajeno responde 404.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-mensajeria-grupal.png`
- `captura-02-mensajeria-cliente.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
