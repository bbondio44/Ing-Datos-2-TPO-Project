# R03 — Sesiones activas

Estado: `IMPLEMENTADO`. Se verificó el flujo real de backend y PostgreSQL. Falta la evidencia visual manual de la ventana de sesiones para promoverlo a `PROBADO`.

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
