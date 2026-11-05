# R31 — Análisis histórico de desempeño

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Analítica → Rankings y desempeño
- **Pasos:** Elegir Desempeño por período.
- **Datos de origen:** `v_desempeno_mensual`.
- **Resultado esperado:** Un renglón por mes, con resultados distintos.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-resultado.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
