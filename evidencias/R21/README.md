# R21 — Visualización geográfica de posiciones GPS

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Monitoreo IoT → Recorrido y mapa
- **Pasos:** El mismo contenedor, con internet.
- **Datos de origen:** `ultima_posicion_por_contenedor`.
- **Resultado esperado:** Marcador rojo en la última posición; ni una tardía ni una lectura sin GPS la mueven.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-mapa-recorrido.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
