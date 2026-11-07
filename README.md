# Plataforma de logística internacional e IoT

Trabajo práctico de Ingeniería de Datos II · UADE · Segundo cuatrimestre 2026 · Grupo 4

Aplicación de escritorio JavaFX con backend Spring Boot y persistencia políglota:

| Base | Qué guarda |
|---|---|
| PostgreSQL | Clientes, usuarios, envíos, contenedores, incidentes y finanzas |
| Cassandra | Telemetría IoT: lecturas y posiciones GPS |
| MongoDB | Eventos, alertas, mensajería y resultados de reportes |
| Neo4j | Red de clientes, envíos, tramos y operadores |

## Levantar la app

### Qué hace falta

- [Docker Desktop](https://www.docker.com/products/docker-desktop/), abierto y corriendo.
- JDK 21. Se comprueba con `java -version`.
- Git e internet, para bajar el proyecto, las imágenes y las dependencias.
- Libres los puertos 5432, 27017, 9042, 7474, 7687 y 8080.

Maven no hace falta: el proyecto trae su propio `mvnw`.

### Pasos

1. Si todavía no tenés el proyecto, bajalo una sola vez:

```powershell
git clone https://github.com/bbondio44/Ing-Datos-2-TPO-Project
cd Ing-Datos-2-TPO-Project
git checkout integracion/app-completa
```

Si ya lo tenés, actualizalo desde su carpeta con `git pull`.

2. Hacé doble clic en `iniciar.bat`.

La primera vez tarda unos 4 minutos, o más si tiene que bajar las imágenes. Crea el archivo `.env` con claves propias, levanta las cuatro bases, carga los datos, compila, arranca el backend y abre la app. Las veces siguientes tarda menos de un minuto.

### Entrar

La primera vez, `iniciar.bat` muestra en la ventana negra el email `admin@grupo4.local` y una contraseña, que queda guardada en `.env` como `APP_BOOTSTRAP_ADMIN_PASSWORD`. No hay registro público: el administrador crea los demás usuarios desde **Usuarios y roles**.

### Cerrar

Al cerrar la app se apaga el backend. Las bases siguen prendidas y se apagan con `docker compose stop`, sin perder datos.

Para volver a los datos iniciales, correr `docker compose down -v` y abrir `iniciar.bat`. **Ese comando borra todos los datos cargados.**

## Si algo falla

| Qué pasa | Qué hacer |
|---|---|
| `Docker no responde` | Abrir Docker Desktop y esperar a que diga que está corriendo |
| `No encontre Java` | Instalar el JDK 21 y abrir de nuevo la ventana |
| `Hay una version vieja corriendo` | Cerrar la app y su ventana negra, y volver a abrir `iniciar.bat` |
| `Las bases no arrancaron` | Otro programa usa esos puertos: apagarlo y reintentar |
| `.env no define APP_BOOTSTRAP_ADMIN_EMAIL` | Completar ese dato y `APP_BOOTSTRAP_ADMIN_PASSWORD` (12 caracteres o más) en `.env` y reintentar |
| El login rechaza la contraseña | Escribirla a mano: un espacio de más la invalida |
| Falta el menú Analítica | Estás en otra rama: `git checkout integracion/app-completa` |
| El backend no arranca | Revisar `logs\backend.log` |
| La app abre sin datos de Neo4j | Esperar un minuto: el grafo se llena solo |

Si cambiás las claves del `.env` con las bases ya creadas, dejan de coincidir. En ese caso hay que volver a los datos iniciales.

## Probar la app

### Un viaje completo

1. En **Envíos**, tocá **Catálogos contenedores/sensores** y creá un contenedor. Para las alertas de cadena de frío, el tipo es `REEFER`.
2. Tocá **Nuevo envío** y elegí cliente, origen, destino y volumen. Abajo aparecen la ruta, ciudad por ciudad, y el precio. Al guardar se crean los tramos.
3. En **Detalle y trazabilidad**, asociá el contenedor, elegí esa fila y asociá un sensor.
4. En **Monitoreo IoT → Seguimiento del viaje**, tocá **Elegir envío…** y después **Iniciar viaje**. Avanza solo, unos 20 segundos por punto, y muestra la ubicación con coordenadas. Al llegar, el envío queda ENTREGADO y el contenedor y el sensor quedan libres.
5. En **Facturación**, tocá **Facturación mensual** para juntar los viajes entregados de cada cliente en una factura, y **Cobro mensual** para cobrarla. Corren solos los días 1 y 10.

Para probar facturación con clientes, proveedores y operadores en distintas situaciones:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\cargar-datos-facturacion.ps1
```

### Lecturas a mano

Los sensores también se pueden simular a mano. Con la app abierta, desde PowerShell en la carpeta del proyecto:

```powershell
.\scripts\mandar-lecturas.ps1 -Email TU_EMAIL -Clave TU_CLAVE -Cantidad 12      # serie normal
.\scripts\mandar-lecturas.ps1 -Email TU_EMAIL -Clave TU_CLAVE -Temperatura 45   # dispara alertas
.\scripts\mandar-lecturas.ps1 -Email TU_EMAIL -Clave TU_CLAVE -Bateria 10       # batería baja
```

Para un flujo continuo está el simulador Java. Necesita un sensor con asignación vigente y el token de `POST /api/v1/auth/login`:

```powershell
$env:SENSOR_ID='cb6a673c-14a5-5469-a365-30fe9700c80b'
$env:API_TOKEN='<token>'
.\mvnw.cmd -pl simulator exec:java "-Dexec.mainClass=ar.edu.uade.logistica.simulator.IoTSimulator"
```

### Pruebas automáticas

Con el backend apagado y las bases cargadas:

```powershell
.\mvnw.cmd test                                                                # tests de código
powershell -ExecutionPolicy Bypass -File .\scripts\pruebas\correr-todo.ps1     # los 40 requisitos
powershell -ExecutionPolicy Bypass -File .\scripts\pruebas\capturas.ps1        # pantallas JavaFX
```

`correr-todo.ps1` tarda unos 12 minutos y apaga y prende bases para probar caídas. `capturas.ps1` maneja la app sola y crea datos de prueba. Los resultados y capturas quedan en `evidencias/RXX/`.

## Qué hay en cada carpeta

| Carpeta | Contenido |
|---|---|
| `backend/` | API Spring Boot |
| `desktop/` | Aplicación JavaFX |
| `shared-contracts/` | Clases compartidas entre backend y app |
| `simulator/` | Simulador de sensores IoT |
| `database/` | Esquemas y datos de PostgreSQL, Cassandra y Neo4j |
| `tp-grupo4-mongo-cassandra/` | Dataset y scripts de carga de MongoDB y Cassandra |
| `scripts/` | Arranque, carga de datos y pruebas |
| `evidencias/` | Resultado y capturas de cada requisito |
| `docs/` | Requisitos, modelos físicos y documentación técnica |

## Documentación

- [Los 40 requisitos](docs/requisitos.md)
- [Cumplimiento del enunciado](docs/cumplimiento-enunciado.md)
- [Modelo físico de MongoDB y Cassandra](docs/modelo-fisico-mongodb-cassandra.md)
- [Modelo físico de Neo4j](docs/modelo-fisico-neo4j.md)
- [Lo usado fuera de clase](docs/fuera-de-clase.md)

## Configuración opcional

`docker-compose.yml` es el que usa `iniciar.bat`. `infra/docker-compose.yml` es una alternativa con versiones fijas de las imágenes: usa los mismos puertos y sus propios volúmenes, así que no se prenden los dos a la vez.

Los procesos periódicos se controlan en `.env`: `APP_BILLING_AUTO_ENABLED` para la facturación automática y `APP_CIERRE_AUTO_ENABLED` para el cierre mensual.
