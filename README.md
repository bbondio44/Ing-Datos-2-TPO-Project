# Ing-Datos-2-TPO-Project

Ingeniería de Datos II - Segundo Cuatrimestre 2026 - UADE

## Plataforma de logística internacional e IoT - Grupo 4

Aplicación de escritorio JavaFX con backend Spring Boot y persistencia políglota. PostgreSQL es la fuente transaccional; Cassandra conserva telemetría; MongoDB almacena eventos, alertas, reportes y mensajería; Neo4j mantiene una proyección del grafo logístico.

El repositorio está en construcción por fases. El estado verificable y las brechas se registran en [docs/ESTADO_INICIAL.md](docs/ESTADO_INICIAL.md) y [docs/MATRIZ_PROMOCION.md](docs/MATRIZ_PROMOCION.md). No se declara cobertura 40/40 hasta ejecutar cada flujo y guardar su evidencia.

## Requisitos

- JDK 21.
- Docker Desktop para el entorno local de datos.
- Maven Wrapper incluido en el repositorio después del primer bootstrap.

## Arranque local

1. Crear el archivo local de secretos y reemplazar todos los valores de ejemplo:

```powershell
Copy-Item .env.example .env
```

2. Iniciar los cuatro motores desde el único Compose:

```powershell
docker compose --env-file .env -f infra/docker-compose.yml up -d
```

3. Cargar en la consola las variables de PostgreSQL del mismo `.env` y definir un administrador inicial solo para el primer arranque:

```powershell
$env:PG_PASSWORD='el-mismo-secreto-de-POSTGRES_PASSWORD'
$env:APP_BOOTSTRAP_ADMIN_EMAIL='admin@grupo4.local'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD='una-clave-larga-y-unica'
```

4. Instalar los módulos locales una vez con `./mvnw.cmd install -DskipTests`.
5. Ejecutar `./mvnw.cmd -pl backend spring-boot:run`.
6. En otra consola ejecutar `./mvnw.cmd -pl desktop javafx:run`.

El bootstrap no contiene una contraseña predeterminada. Si las variables no están definidas, no crea usuarios. La carga de datos de demostración es opt-in y nunca se ejecuta contra Supabase de forma automática.

Las tareas de Persona 1 y sus casos de aceptación están detallados en [docs/requisitos.md](docs/requisitos.md). La facturación automática programada se habilita con `APP_BILLING_AUTO_ENABLED=true`; también puede ejecutarse manualmente desde la pantalla de Facturación.

## Validación

```powershell
./mvnw.cmd test
./mvnw.cmd package
```

Las referencias originales del equipo están en `database/referencia-postgresql` y `docs/referencias`. No deben ejecutarse scripts destructivos ni el seed de demo sobre una base remota existente.
