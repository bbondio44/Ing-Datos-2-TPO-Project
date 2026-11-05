# R01 — ABM de clientes

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

## Flujo ejecutado

1. Login como administrador mediante token opaco.
2. Alta de `Cliente Ciclo ABM` sin CUIT, para comprobar que el campo opcional se normaliza a `null`.
3. Edición a `Cliente Ciclo ABM Editado` y agregado de teléfono.
4. Baja lógica.
5. Lectura posterior por la API.

Resultado observado:

```json
{
  "clientName": "Cliente Ciclo ABM Editado",
  "clientState": "INACTIVO"
}
```

Se verificaron además:

- CUIT de tres dígitos: HTTP `400`, código `VALIDACION` y error asociado al campo `cuit`.
- Email duplicado: HTTP `409` y código `CONFLICTO_DATOS`.
- Auditoría: operaciones `INSERT` y `UPDATE` con `id_correlacion`.
- Outbox: eventos `CLIENTE_INSERT` y `CLIENTE_UPDATE` en estado `PENDIENTE`.

La pantalla `Clientes` contiene alta, búsqueda, edición, confirmación de baja lógica y mensajes asíncronos. La prueba visual debe repetirse manualmente antes de cambiar el estado a `PROBADO`.

## Capturas

- `captura-01-alta-cliente.png`
- `captura-02-cliente-creado.png`
- `captura-03-clientes.png`
