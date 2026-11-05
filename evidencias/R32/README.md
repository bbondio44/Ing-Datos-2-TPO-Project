# R32 — Detección de anomalías de temperatura

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Alertas
- **Pasos:** Con 10 lecturas o más, mandar una temperatura muy alejada.
- **Datos de origen:** Histórico del sensor en Cassandra.
- **Resultado esperado:** Alerta ANOMALIA_TEMPERATURA con la lectura que la causó.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-alerta-anomalia.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
