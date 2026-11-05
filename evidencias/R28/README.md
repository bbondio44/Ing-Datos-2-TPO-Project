# R28 — Ranking de clientes por volumen

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Analítica → Rankings y desempeño
- **Pasos:** Elegir Clientes por volumen.
- **Datos de origen:** `v_ranking_clientes_volumen`.
- **Resultado esperado:** Suma de `volumen_m3` sin envíos cancelados, en orden descendente.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-resultado.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
