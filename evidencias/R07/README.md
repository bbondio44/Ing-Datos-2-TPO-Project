# R07 — ABM contenedores de un envío

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Envíos → Detalle; Catálogos → Historial de movimientos
- **Pasos:** Asociar, desasociar y volver a asociar un contenedor; probar un intervalo superpuesto.
- **Datos de origen:** Contenedor creado por la prueba.
- **Resultado esperado:** Queda el histórico con fecha de fin; la superposición se rechaza; el historial lista los envíos del contenedor y un cliente ajeno no ve nada.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-detalle-envio.png`
- `captura-02-contenedores.png`
- `captura-03-historial-contenedor.png`
- `captura-04-contenedor-asociado.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
