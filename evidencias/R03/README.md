# R03 — Sesiones activas

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

## Flujo ejecutado

1. Crear dos sesiones válidas para el mismo administrador.
2. Listar únicamente sesiones activas y no vencidas.
3. Revocar la sesión más reciente desde la primera sesión.
4. Reutilizar ambos tokens.
5. Cerrar la sesión sobreviviente y reutilizar su token.

Resultado observado:

```json
{
  "revoke": 204,
  "revokedToken": 401,
  "survivingToken": 200,
  "logout": 204,
  "afterLogout": 401
}
```

La aplicación JavaFX expone `Sesiones activas`, permite seleccionar una sesión `ACTIVA` y revocarla. Los tokens se almacenan únicamente como SHA-256 en `tp_logistica.sesion`.

## Capturas

- `captura-01-sesiones.png`
