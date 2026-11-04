# R05 — Operadores logísticos

Estado: `PENDIENTE` con avance parcial ejecutado.

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

## Pendiente para completar R05

Falta implementar y verificar la relación del operador con vehículos, tramos, facturación de terceros y la proyección Neo4j. R05 permanece pendiente hasta cubrir esas dependencias.
