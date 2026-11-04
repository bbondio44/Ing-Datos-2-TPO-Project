# Contrato de integración — PostgreSQL, Cassandra, MongoDB, Neo4j

## Identificadores compartidos

- PostgreSQL: `uuid` para todas las entidades primarias de negocio.
- Cassandra: `uuid` (`id_sensor`, `id_contenedor`, etc.). El documento CQL del grupo ya utiliza UUID para esas entidades.
- MongoDB: string UUID en campos `id_envio`, `id_sensor`, `id_contenedor`, `id_regla`, `id_solicitud`, etc.
- Neo4j: mismo UUID en forma **String**. Sustituir los identificadores Integer del modelo físico Neo4j original. Las ubicaciones usan `id_ubicacion` o código estable, no solo nombre.
- `ids_compartidos.csv` enumera los UUID del dataset para que el equipo no invente otros identificadores para la misma entidad.

## Fuente de verdad

PostgreSQL administra maestros, envíos, tramos, asignaciones, reglas, incidentes, finanzas y solicitudes. Cassandra almacena telemetría y GPS. MongoDB almacena alertas emitidas, eventos, conversaciones, mensajes y artefactos de reporte. Neo4j es una proyección consultable de relaciones.

## Esquema de evento de sincronización

El trigger inserta un registro `outbox_evento` **en la misma transacción de PostgreSQL** que la escritura de negocio:

```json
{
  "id_evento": "uuid-evento-outbox",
  "agregado_tipo": "envio",
  "agregado_id": "uuid-envio",
  "tipo_evento": "ENVIO_INSERT",
  "payload": { "id_envio": "uuid-envio", "id_cliente": "uuid-cliente" },
  "id_correlacion": "uuid-request",
  "estado": "PENDIENTE",
  "intentos": 0
}
```

El payload real puede incluir más propiedades; los consumidores deben tener contrato y versión estable para cambios futuros. Evitar incluir credenciales y datos personales innecesarios.

**Worker recomendado:** consultar eventos `PENDIENTE` o `ERROR` con `FOR UPDATE SKIP LOCKED` dentro de transacciones breves; reclamar eventos (`PROCESANDO`), publicar cambios y marcar `PROCESADO`; ante error, registrar `ultimo_error`, `intentos` y `proximo_intento`. Los destinos deben deduplicar usando `id_evento`; al reiniciar un worker, reencolar eventos `PROCESANDO` abandonados. No sostener la transacción SQL abierta mientras se hacen llamadas lentas a Neo4j/MongoDB.

Para Neo4j, hacer `MERGE` por `id_cliente`, `id_envio`, `id_tramo`, etc. Las relaciones de envío-contenedor se modelan temporalmente (N:M en el historial) y los incidentes se unen al envío y opcionalmente al tramo. Crear nodos/relaciones en el orden adecuado o resolver ausencias con upserts idempotentes.

## Flujo IoT recomendado

1. API recibe `{id_lectura, id_sensor, fecha_hora, temperatura?, humedad?, bateria?, latitud?, longitud?}`.
2. Validar en PG que el sensor está registrado y cuál era su contenedor **en fecha_hora** (consultar `asignacion_sensor_contenedor`). Si se necesita conocer el envío, consultar `envio_contenedor` para ese mismo instante.
3. Guardar lecturas en Cassandra (`lecturas_por_sensor_dia`, `lecturas_por_contenedor_dia`, y tabla GPS solo con coordenadas válidas).
4. Actualizar último valor/posición sin reemplazar datos nuevos por lecturas tardías.
5. Evaluar `regla_alerta` de PG. Registrar alerta en MongoDB por clave idempotente. Si corresponde, crear `incidente` PG con el mismo `id_evento_externo` del evento MongoDB.
6. Proveer API integrada de monitoreo y mapa.

**Cuidado:** no hay claves foráneas físicas entre motores. Toda referencia externa debe validarse en la API. Las escrituras multi-motor no son una sola transacción ACID; documentar consistencia eventual y reintentos.

## Flujos financieros

1. Crear/sincronizar cuenta corriente del titular correspondiente.
2. Facturar con funciones transaccionales PG y claves idempotentes.
3. Cobrar/pagar mediante `registrar_pago`; el trigger crea movimiento y actualiza estado/saldo.
4. Mensualmente, ejecutar `cerrar_cuenta`. Un mes cerrado no admite retroactivos: ajustar en un mes posterior.
5. Comprobar `v_conciliacion_cuentas` (`saldo_conciliado = true`).

## Privacidad, sesiones y autorización

Backend verifica el usuario, sus permisos y la propiedad del envío/factura/reporte/conversación en **cada** consulta. El RDBMS no implementa por sí mismo RLS de tenant en este paquete. Deshabilitar accesos directos de clientes a tablas mediante la API pública. Para PostgreSQL alojado en Supabase, mantener el esquema fuera de la API de datos pública, usar un usuario de backend con permisos mínimos y no compartir secretos en frontend ni Git.
