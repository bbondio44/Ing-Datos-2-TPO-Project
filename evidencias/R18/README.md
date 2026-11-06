# R18 — Registro continuo de lecturas IoT

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Simulador y Monitoreo IoT → Histórico
- **Pasos:** Mandar lecturas consecutivas de un sensor demo.
- **Datos de origen:** Sensor demo `cb6a673c-14a5-5469-a365-30fe9700c80b`.
- **Resultado esperado:** Responde 201 y las lecturas se consultan; reenviar no duplica.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-lecturas-enviadas.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
