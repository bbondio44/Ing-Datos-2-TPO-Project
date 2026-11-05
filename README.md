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

## Arranque con las cuatro bases

1. Copiar `.env.example` a `.env` y completar las claves. `MONGO_URI` va sin comillas y con la misma clave que `MONGO_PASSWORD`.
2. Levantar las bases con `docker compose up -d` y esperar a que Cassandra figure `healthy` en `docker compose ps`.
3. Cargar los datos:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\cargar-postgres.ps1
powershell -ExecutionPolicy Bypass -File .	p-grupo4-mongo-cassandra\cargar_todo.ps1
```

4. Compilar con `mvn -B -DskipTests install`.
5. Definir en `.env` `APP_BOOTSTRAP_ADMIN_EMAIL` y `APP_BOOTSTRAP_ADMIN_PASSWORD` (12 caracteres o más) para tener un usuario con clave válida.
6. Correr el backend. Neo4j se llena solo con el worker del outbox.

```powershell
$env:SPRING_PROFILES_ACTIVE='dev,polyglot'
mvn -pl backend spring-boot:run
```

7. En otra consola, correr la app con `mvn -pl desktop javafx:run`.

Para el simulador, usar un sensor con asignación vigente (`v_envio_contenedor_sensor_activo`) y el token que devuelve `POST /api/v1/auth/login`. `SIM_TEMPERATURA` fuerza un valor, para disparar una alerta en la demo:

```powershell
$env:SENSOR_ID='cb6a673c-14a5-5469-a365-30fe9700c80b'
$env:API_TOKEN='<token>'
mvn -pl simulator exec:java "-Dexec.mainClass=ar.edu.uade.logistica.simulator.IoTSimulator"
```

Las pruebas de aceptación contra las bases reales están en `scripts/pruebas`, y los pasos manuales por requisito en `docs/notas-testing-persona2.md`.

## Validación

```powershell
./mvnw.cmd test
./mvnw.cmd package
```

Las referencias originales del equipo están en `database/referencia-postgresql` y `docs/referencias`. No deben ejecutarse scripts destructivos ni el seed de demo sobre una base remota existente.
