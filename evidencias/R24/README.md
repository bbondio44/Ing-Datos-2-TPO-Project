# R24 — Trayectos y maestros logísticos

Estado: `PENDIENTE`. La consulta de trayecto sobre el backup Neo4j real fue probada; el CRUD completo de envíos y tramos aún falta.

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

Falta implementar el envío y sus tramos ordenados con origen/destino, medio, fechas, operador, vehículo y proveedores en la API y persistencia operativa.

## Verificación Neo4j

Se restauró el backup real en la base `82cf1a53` con Neo4j Enterprise 2026.10.1. La consulta de solo lectura devolvió el tramo `trm-01` del envío `env-101`, orden `1`, medio `Terrestre`, vehículo `veh-801` y operador `op-501`. El resultado confirma la proyección Neo4j de los campos R24, pero no sustituye el CRUD faltante.
