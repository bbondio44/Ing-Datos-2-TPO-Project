# Contratos entre PostgreSQL Cassandra MongoDB y Neo4j

## Identidad y tiempo

- Todo identificador de negocio nace como UUID PostgreSQL.
- Cassandra usa `uuid`; MongoDB y Neo4j guardan la representación canónica en texto.
- Los instantes de eventos y telemetría viajan en UTC con formato ISO 8601. Los cierres usan `LocalDate` como primer día del mes y zona de negocio `America/Buenos_Aires`.
- `idLectura`, `eventId` y las claves de idempotencia no se regeneran durante un reintento.

## Cassandra

Keyspace propuesto: `logistica_iot`. Tablas: `lecturas_por_sensor_dia`, `lecturas_por_contenedor_dia`, `posiciones_por_contenedor_dia`, `ultima_lectura_por_sensor`, `ultima_posicion_por_contenedor`.

```json
{
  "idLectura": "uuid",
  "idSensor": "uuid",
  "timestamp": "2026-10-30T15:00:00Z",
  "temperatura": 6.2,
  "humedad": 54.1,
  "vibracion": 0.3,
  "bateria": 82.0,
  "latitud": -34.6037,
  "longitud": -58.3816
}
```

El backend resuelve en PostgreSQL el contenedor y envío vigentes en el timestamp. Las consultas de rango enumeran días y fusionan particiones; no se acepta `ALLOW FILTERING` como estrategia general. Una lectura tardía se conserva en histórico y solo reemplaza la tabla de último valor cuando su timestamp es posterior.

## MongoDB

Base propuesta por el compañero: `logistica_documental`. Colecciones: `eventos_logisticos`, `alertas`, `conversaciones`, `mensajes`, `reportes`, `reclamos`, `predicciones_riesgo`.

Índices mínimos: `(id_envio, fecha_hora)`, `(id_sensor, fecha)`, `(id_conversacion, fecha_hora)` e idempotencia única por evento/lectura/regla. Los mensajes son documentos separados; no se acumulan indefinidamente dentro de una conversación.

Se debe acordar el mapeo del ciclo de alerta. Contrato de aplicación propuesto: `ABIERTA`, `RECONOCIDA`, `CERRADA`; modelo recibido: `ABIERTA`, `EN_REVISION`, `RESUELTA`, `DESCARTADA`. Hasta acordarlo, el requisito R26 queda pendiente.

## Neo4j

Labels: `Cliente`, `Envio`, `Contenedor`, `Tramo`, `Ubicacion`, `Vehiculo`, `Operador`, `Incidente`. Todas las propiedades `id_*` son UUID String. Relaciones: `SOLICITA`, `ASIGNADO_A`, `COMPUESTO_POR`, `ORIGEN`, `DESTINO`, `EJECUTADO_POR`, `PERTENECE_A`, `REGISTRA`.

`ASIGNADO_A` incluye `fecha_asociacion` y `fecha_desasociacion`; `COMPUESTO_POR` incluye `orden`. Los consumidores usan `MERGE` con parámetros y deduplican por `eventId`.

## Evento outbox público

```json
{
  "eventId": "uuid",
  "aggregateType": "envio",
  "aggregateId": "uuid",
  "eventType": "ENVIO_INSERT",
  "occurredAt": "2026-10-30T15:00:00Z",
  "correlationId": "uuid",
  "schemaVersion": 1,
  "payload": {}
}
```

El adaptador mapea el payload real generado por los triggers sin modificarlo. El worker reclama lotes en transacciones breves con `FOR UPDATE SKIP LOCKED`, libera la transacción antes de llamar a destinos y registra estado, intento, error y próximo reintento.

