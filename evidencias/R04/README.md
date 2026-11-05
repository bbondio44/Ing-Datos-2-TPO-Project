# R04 — Proveedores

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

## Cobertura verificada

- Pantalla JavaFX accesible desde `Proveedores`, con búsqueda, alta, edición y baja lógica.
- API protegida `/api/v1/proveedores` con permisos separados de lectura y escritura.
- Persistencia real en `tp_logistica.proveedor`.
- CUIT válido `30710123450` aceptado y CUIT duplicado rechazado con HTTP `409`.
- Outbox generado en la misma transacción: `PROVEEDOR_INSERT` y `PROVEEDOR_UPDATE`.

Resultado del ciclo ejecutado:

```json
{
  "businessName": "Proveedor Fase 2 Editado",
  "serviceType": "DEPOSITO Y ADUANA",
  "state": "INACTIVO",
  "duplicateCuit": 409
}
```

## Relaciones verificadas

- El proveedor se elige al crear o editar un tramo y se ve en la columna Proveedores de Trayectos.
- Un tramo conserva dos proveedores al editarlo; un proveedor inactivo se rechaza con HTTP `409`.
- La factura y el pago de un proveedor se reflejan en su cuenta a pagar (ver R16).

Limitación: la pantalla de Trayectos carga un proveedor por tramo y conserva los demás. Varios proveedores nuevos en un tramo se cargan por API.

## Capturas

- `captura-01-proveedores.png`
- `captura-02-tramos.png`
- `captura-03-edicion-tramo.png`
- `captura-04-cuenta-proveedor.png`
