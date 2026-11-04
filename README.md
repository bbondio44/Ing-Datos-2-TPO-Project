# Plataforma de logística internacional e IoT Grupo 4

Aplicación de escritorio JavaFX con backend Spring Boot y persistencia políglota. PostgreSQL es la fuente transaccional; Cassandra conserva telemetría; MongoDB almacena eventos, alertas, reportes y mensajería; Neo4j mantiene una proyección del grafo logístico.

El repositorio está en construcción por fases. El estado verificable y las brechas se registran en [docs/ESTADO_INICIAL.md](docs/ESTADO_INICIAL.md) y [docs/MATRIZ_PROMOCION.md](docs/MATRIZ_PROMOCION.md). No se declara cobertura 40/40 hasta ejecutar cada flujo y guardar su evidencia.

## Requisitos

- JDK 21.
- Docker Desktop para el entorno local de datos.
- Maven Wrapper incluido en el repositorio después del primer bootstrap.

## Arranque local

1. Iniciar PostgreSQL vacío con `docker compose -f infra/docker-compose.yml up -d postgres`.
2. Definir un administrador inicial solo para el primer arranque:

```powershell
$env:APP_BOOTSTRAP_ADMIN_EMAIL='admin@grupo4.local'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD='una-clave-larga-y-unica'
```

3. Instalar los módulos locales una vez con `./mvnw.cmd install -DskipTests`.
4. Ejecutar `./mvnw.cmd -pl backend spring-boot:run`.
5. En otra consola ejecutar `./mvnw.cmd -pl desktop javafx:run`.

El bootstrap no contiene una contraseña predeterminada. Si las variables no están definidas, no crea usuarios. La carga de datos de demostración es opt-in y nunca se ejecuta contra Supabase de forma automática.

## Validación

```powershell
./mvnw.cmd test
./mvnw.cmd package
```

Las referencias originales del equipo están en `database/referencia-postgresql` y `docs/referencias`. No deben ejecutarse scripts destructivos ni el seed de demo sobre una base remota existente.

