# R35 — Reporte de envíos

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Reportes → R35 Envíos
- **Pasos:** Filtrar por cliente, país, estado, demorados e incidentes abiertos; repetir como CLIENTE.
- **Datos de origen:** `v_envios_reporte`.
- **Resultado esperado:** Cada filtro y combinación coincide con SQL; el CLIENTE no ve el combo y solo ve lo suyo.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-filtro-cliente.png`
- `captura-02-vista-cliente.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
