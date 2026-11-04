# Verificación estática (sin servidor PostgreSQL)

Se revisó que el SQL generado tuviera referencias a tablas presentes en el esquema, equilibrio de delimitadores y orden de dependencias. Esta verificación **no equivale** a ejecutar SQL ni a pasar las pruebas de aceptación en un motor PostgreSQL real. Se requiere correr los seis SQL en una instancia nueva y revisar los errores que reporte el motor.

Chequeos funcionales previstos en `04_pruebas_aceptacion.sql`:

- maestros, cuentas y tramos con datos;
- roles, permisos y sesión activa/revocada;
- asociaciones históricas y activas de contenedor/sensor;
- reglas, incidentes y rankings con resultados;
- pagos parciales, reportes facturados y predicción baseline;
- conciliación entre saldo almacenado y movimientos;
- cierre único de cuenta mensual;
- eventos outbox iniciales.

Pruebas negativas esperadas en `05_pruebas_negativas.sql`: duplicado y dígito de CUIT, solapamiento temporal, sobrepago, cierre inmutable y ajuste retroactivo. Hay que ejecutarlas individualmente en el SQL Editor una vez instalada la base.
