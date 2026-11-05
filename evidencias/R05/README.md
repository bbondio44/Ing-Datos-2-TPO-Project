# R05 — Operadores logísticos

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

## Cobertura verificada

- Pantalla JavaFX accesible desde `Operadores`, con búsqueda, alta, edición y baja lógica.
- API protegida `/api/v1/operadores` con permisos separados de lectura y escritura.
- Persistencia real en `tp_logistica.operador_logistico`.
- CUIT válido `30710123469` almacenado.
- Baja lógica confirmada con estado `INACTIVO`.
- Outbox generado: `OPERADOR_LOGISTICO_INSERT` y `OPERADOR_LOGISTICO_UPDATE`.
- Pantalla JavaFX de vehículos con búsqueda, alta, edición, mantenimiento y baja lógica.
- Asociación real `vehiculo.id_operador`; la tabla muestra la razón social del operador.
- Un operador inactivo no puede recibir vehículos nuevos: HTTP `409`.
- Outbox de vehículo verificado para `INSERT` y dos `UPDATE`, siempre con correlación.

Resultado del ciclo ejecutado:

```json
{
  "businessName": "Operador Fase 2 Editado",
  "state": "INACTIVO",
  "vehicleStateBeforeDeactivation": "MANTENIMIENTO",
  "vehicleFinalState": "INACTIVO",
  "createVehicleForInactiveOperator": 409
}
```

## Relaciones verificadas

- El operador se ve en sus tramos; un vehículo de otro operador se rechaza con HTTP `409`.
- La factura y el pago de un operador se reflejan en su cuenta a pagar (ver R16).
- Neo4j tiene los mismos operadores que PostgreSQL y una relación `OPERADO_POR` por cada tramo con operador.

## Capturas

- `captura-01-operadores.png`
- `captura-02-vehiculos.png`
- `captura-03-tramos.png`
- `captura-04-cuenta-operador.png`
