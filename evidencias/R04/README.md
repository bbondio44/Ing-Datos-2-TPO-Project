# R04 — Proveedores

Estado: `PENDIENTE` con avance parcial ejecutado.

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

## Pendiente para completar R04

El proveedor debe seleccionarse y visualizarse desde tramos y desde la operación financiera de terceros. Hasta verificar esas relaciones y la interfaz completa, R04 no puede pasar a `IMPLEMENTADO` ni `PROBADO`.
