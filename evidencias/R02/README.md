# R02 — ABM usuarios y roles

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

- **Pantalla:** Usuarios y roles
- **Pasos:** Crear usuario con rol, entrar con roles distintos, crear y borrar un rol, bloquear un usuario.
- **Datos de origen:** Roles del dataset y usuarios creados por la prueba.
- **Resultado esperado:** Cada rol accede a cosas distintas; un usuario bloqueado pierde la sesión.

## Evidencia

- `prueba-automatizada.txt`: requests, respuestas y contraste con la base.
- `captura-01-usuarios.png`
- `captura-02-roles.png`
- `captura-03-vista-cliente.png`

Las capturas las genera `scripts/pruebas/capturas.ps1` manejando la aplicación real; su detalle está en `evidencias/pantallas-javafx.txt`.
