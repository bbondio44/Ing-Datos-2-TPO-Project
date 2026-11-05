# Requisitos y pruebas de aceptación de Persona 1

Este archivo concreta el alcance entregable de Persona 1. Un requisito queda `IMPLEMENTADO` cuando existen SQL, API, pantalla y pruebas automatizadas; Persona 3 lo cambia a `PROBADO` después de ejecutar el flujo y guardar evidencia.

## Base común

- Un único `infra/docker-compose.yml` inicia PostgreSQL, MongoDB, Cassandra y Neo4j.
- Las contraseñas son variables obligatorias y no tienen valores reales en el repositorio.
- PostgreSQL revoca privilegios de `PUBLIC`; el backend aplica permisos de aplicación y aislamiento por titular.

## Núcleo

- R01: alta, consulta, edición y baja lógica de clientes; CUIT/email duplicados se rechazan.
- R02: alta y edición de usuarios, asignación de uno o más roles, ABM de roles y permisos efectivos. Bloquear/inactivar revoca sesiones.
- R03: login, expiración, logout y revocación de sesiones.
- R04: ABM de proveedores y asociación a tramos, cuentas, facturas y pagos.
- R05: ABM de operadores y vehículos; asociación coherente a trayectos y cuentas por pagar.
- R06: pantalla de envíos con navegación a contenedores y sensores vigentes e históricos.
- R07: asociación/desasociación temporal de contenedores sin superposición.
- R08: asociación/desasociación temporal de sensores; una superposición por fecha se rechaza.
- R24: tramos ordenados por envío con medio, vehículo, operador, ubicaciones, fechas, costos, estado y proveedores.

## Finanzas

- R10: factura borrador con detalles, emisión mediante función SQL, consulta de saldo y cobros parciales/totales.
- R12: proceso invocable y programable que usa una clave estable por envío/tarifa; dos ejecuciones retornan la misma factura.
- R13: pagos idempotentes; duplicados incompatibles y sobrepagos se rechazan.
- R14: cuenta corriente con movimientos inmutables, saldo almacenado/calculado y conciliación visible.
- R15: cierre mensual por primer día del mes; no permite doble cierre ni cierres retroactivos.
- R16: el mismo circuito funciona para cuentas por pagar de proveedores y operadores.

## Analítica SQL

- R35: envíos con filtros combinables por país destino, estado, cliente, demora e incidentes abiertos.
- R36: mínimo, máximo y promedio por cliente, exclusivamente sobre envíos entregados.
- R37: incidentes por país destino, estado y cliente. Denominador: envíos no cancelados del grupo.
- R38: promedio global o del cliente exclusivamente sobre envíos entregados.
- R39: cantidad, total y promedio de costo real por medio aéreo, marítimo y terrestre.

## Casos negativos obligatorios

- Usuario sin permiso intenta escribir o consultar otro titular.
- Contenedor o sensor se asigna en un intervalo superpuesto.
- Tramo usa un vehículo de otro operador o fechas invertidas.
- Facturación automática se ejecuta dos veces.
- Pago repite clave con otros datos o supera el pendiente.
- Cierre repite período o intenta cerrar antes de un período ya cerrado.
- Reporte de cliente intenta filtrar otro cliente.
