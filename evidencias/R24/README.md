# R24 — Trayectos y maestros logísticos

Estado: `IMPLEMENTADO`. Pasó la prueba automatizada y la de pantalla sobre una instalación limpia. Falta que Persona 3 lo marque como terminado.

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

## CRUD de tramos verificado

- `/api/v1/trayectos` y la pantalla Trayectos crean, editan y eliminan tramos; el envío los devuelve en orden.
- Se rechazan con HTTP `409`: vehículo de otro operador, fechas invertidas, tramo finalizado sin llegada real y número de tramo repetido.

## Verificación Neo4j

Se restauró el backup real en la base `82cf1a53` con Neo4j Enterprise 2026.10.1. La consulta de solo lectura devolvió el tramo `trm-01` del envío `env-101`, orden `1`, medio `Terrestre`, vehículo `veh-801` y operador `op-501`. El resultado confirma la proyección Neo4j de los campos R24.

## Capturas

- `captura-01-tramos.png`
- `captura-02-edicion-tramo.png`
