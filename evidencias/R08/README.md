# R08 — ABM sensores de un contenedor

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Envíos → Detalle; Catálogos → Sensores
- **Pasos:** Asociar y desasociar un sensor; consultar por fecha; probar una superposición.
- **Datos de origen:** Sensor creado por la prueba.
- **Resultado esperado:** Sensores correctos por contenedor y fecha; la superposición se rechaza.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-detalle-envio.png`
- `captura-02-sensores.png`
- `captura-03-sensor-asociado.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
