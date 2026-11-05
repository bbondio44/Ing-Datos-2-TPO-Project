# R20 — Reconstrucción del recorrido de un contenedor

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Monitoreo IoT → Recorrido y mapa
- **Pasos:** Contenedor MSKU0000001 del 20/10 al 22/10/2026.
- **Datos de origen:** `posiciones_por_contenedor_dia`.
- **Resultado esperado:** Línea con los puntos en orden; una lectura tardía queda en su lugar; sin GPS no agrega punto.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-mapa-recorrido.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
