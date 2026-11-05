# R22 — Control de estado de sensores

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Monitoreo IoT → Estado de sensores
- **Pasos:** Abrir la pestaña.
- **Datos de origen:** Última lectura de cada sensor.
- **Resultado esperado:** SEN-040 en SIN_DATOS; más de 120 min sin señal en OFFLINE; con lecturas recientes en ONLINE con su batería.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-estado-sensores.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
