# R24 — Trayectos y maestros logísticos

Estado: `PENDIENTE` con prerrequisitos parciales verificados.

## Cobertura disponible

- ABM JavaFX y API de ubicaciones con tipos `PUERTO`, `AEROPUERTO`, `DEPOSITO`, `FRONTERA` y `OTRO`.
- ABM JavaFX y API de vehículos asociados a operadores activos.
- Estados de vehículo `ACTIVO`, `MANTENIMIENTO` e `INACTIVO`.
- Búsqueda por identificación, tipo y operador.
- Una ubicación referenciada no podrá eliminarse debido a las claves foráneas físicas de PostgreSQL.
- Eventos outbox verificados: `UBICACION_INSERT`, `UBICACION_UPDATE`, `UBICACION_DELETE`, `VEHICULO_INSERT` y `VEHICULO_UPDATE`.

Resultado ejecutado:

```json
{
  "location": "Terminal Fase 2 Editada",
  "locationType": "DEPOSITO",
  "locationAfterDelete": 404,
  "vehicleUpdatedState": "MANTENIMIENTO",
  "vehicleFinalState": "INACTIVO",
  "inactiveOperatorCreate": 409
}
```

## Pendiente para completar R24

Falta implementar el envío y sus tramos ordenados con origen/destino, medio, fechas, operador, vehículo y proveedores; también falta la consulta de recorrido lógico en Neo4j.
