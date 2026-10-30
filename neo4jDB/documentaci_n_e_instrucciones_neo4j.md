# Módulo Neo4j: Analítica, Grafos y Reportes (Persona 3)

Este directorio contiene los scripts e instrucciones necesarios para inicializar la base de datos de grafos Neo4j, cargar respaldos (`.dump`), crear restricciones de unicidad, poblar datos de prueba y ejecutar las 13 consultas analíticas asignadas.

---

## ☁️ Importar Respaldos en Neo4j Aura (Nube)

Si vas a importar un archivo de respaldo (`.dump`) generado previamente en tu propia instancia de **Neo4j AuraDB**:

1. Inicia sesión en la consola de [Neo4j Aura](https://console.neo4j.io/).
2. Ubica la instancia donde deseas cargar el respaldo (o crea una nueva base de datos **Free / Professional**).
3. Haz clic en el nombre de la instancia o en los tres puntos (`...`) de la tarjeta de la base de datos y selecciona **Restore / Import**.
4. Selecciona la opción **Upload a file** y arrastra el archivo `.dump` exportado.
5. Confirma la restauración. *Nota: Esto reemplazará todos los datos existentes en esa instancia de Aura.*
6. Una vez completada la carga, presiona **Open / Query** para abrir Neo4j Browser y comenzar a ejecutar las consultas.

---

## 🛠️ Opción Alternativa: Carga Local o Vía Script Cypher (`init_and_seed.cypher`)

Si prefieres levantar una base limpia en tu entorno local (**Neo4j Desktop**, **Docker**) o en una instancia vacía de **Neo4j Aura**:

### 1. Iniciar la base de datos local mediante Docker (Opcional)
```bash
docker run \
  --name neo4j-poliglota \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password123 \
  neo4j:latest
```

### 2. Ejecutar el Script Maestro
1. Abre **Neo4j Browser** (`http://localhost:7474` o desde la consola de Aura).
2. Copia todo el contenido del archivo `init_and_seed.cypher`.
3. Pégalo en la barra de ejecución de Cypher y presiona **Run** (o `Ctrl + Enter`).

Esto creará:
* Las 7 restricciones de unicidad (`Cliente`, `Envio`, `Tramo`, `Ubicacion`, `Vehiculo`, `Operador`, `Incidente`).
* Toda la red de datos de prueba conectada de punta a punta.

---

## 📊 Ejecución de las 13 Consultas y Reportes (R11, R27 - R39)

A continuación se detallan las consultas Cypher para verificar cada requisito en Neo4j Browser:

### R11 - Red de relaciones integradas (Cliente, Envío, Tramo, Vehículo, Operador)
```cypher
MATCH (c:Cliente)-[:SOLICITA]->(e:Envio)-[:COMPUESTO_POR]->(t:Tramo)-[:EJECUTADO_POR]->(v:Vehiculo)-[:PERTENECE_A]->(o:Operador)
RETURN c, e, t, v, o;
```

### R27 - Ranking de rutas/tramos con mayor cantidad de incidentes
```cypher
MATCH (t:Tramo)<-[:COMPUESTO_POR]-(e:Envio)
OPTIONAL MATCH (t)-[:REGISTRA]->(i:Incidente)
RETURN t.id_tramo AS tramo, t.medio_transporte AS transporte, count(i) AS total_incidentes
ORDER BY total_incidentes DESC;
```

### R28 - Ranking de clientes por volumen transportado
```cypher
MATCH (c:Cliente)-[:SOLICITA]->(e:Envio)
RETURN c.id_cliente AS id_cliente, c.razon_social AS cliente, count(e) AS volumen_envios
ORDER BY volumen_envios DESC;
```

### R29 - Predicción de atrasos en envíos
```cypher
MATCH (e:Envio)-[cp:COMPUESTO_POR]->(t:Tramo)
OPTIONAL MATCH (t)-[:REGISTRA]->(i:Incidente)
WITH e, t, cp, count(i) AS incidentes_tramo
RETURN e.id_envio AS envio, sum(incidentes_tramo) AS total_incidentes,
       CASE WHEN sum(incidentes_tramo) >= 2 THEN 'Alto Riesgo' ELSE 'Normal / Sin Riesgo' END AS prediccion;
```

### R30 - Gestión de trayectos de cada envío (Seguimiento de tramos)
```cypher
MATCH (e:Envio)-[cp:COMPUESTO_POR]->(t:Tramo)
OPTIONAL MATCH (t)-[:ORIGEN]->(ori:Ubicacion)
OPTIONAL MATCH (t)-[:DESTINO]->(des:Ubicacion)
RETURN e.id_envio AS envio, cp.orden AS orden, t.id_tramo AS tramo, ori.nombre_puerto_nodo AS origen, des.nombre_puerto_nodo AS destino
ORDER BY e.id_envio, cp.orden;
```

### R31 - Detección de incidentes en envíos y tramos
```cypher
MATCH (e:Envio)
OPTIONAL MATCH (e)-[:REGISTRA]->(ie:Incidente)
OPTIONAL MATCH (e)-[:COMPUESTO_POR]->(t:Tramo)-[:REGISTRA]->(it:Incidente)
RETURN e.id_envio AS envio, collect(DISTINCT ie.id_incidente) AS incidentes_envio, collect(DISTINCT it.id_incidente) AS incidentes_tramo;
```

### R33 - Reporte de Envíos por país, estado, cliente e incidentes abiertos
```cypher
MATCH (c:Cliente)-[:SOLICITA]->(e:Envio)
OPTIONAL MATCH (e)-[:REGISTRA]->(i:Incidente)
RETURN c.pais AS pais, e.estado AS estado, c.razon_social AS cliente, 
       collect(DISTINCT e.id_envio) AS envios, count(DISTINCT i) AS incidentes_abiertos;
```

### R34 - Tiempos máximos y mínimos de envío por cliente
```cypher
MATCH (c:Cliente)-[:SOLICITA]->(e:Envio)-[:COMPUESTO_POR]->(t:Tramo)
WHERE t.fecha_salida IS NOT NULL AND e.fecha_creacion IS NOT NULL
WITH c, e, duration.between(e.fecha_creacion, t.fecha_salida).days AS dias
RETURN c.razon_social AS cliente, max(dias) AS max_dias, min(dias) AS min_dias;
```

### R35 - Promedio de incidentes por país / estado / cliente
```cypher
MATCH (c:Cliente)-[:SOLICITA]->(e:Envio)
OPTIONAL MATCH (e)-[:REGISTRA]->(i:Incidente)
RETURN c.pais AS pais, e.estado AS estado, c.razon_social AS cliente, 
       count(DISTINCT e) AS total_envios, count(DISTINCT i) AS total_incidentes,
       (toFloat(count(DISTINCT i)) / CASE WHEN count(DISTINCT e) = 0 THEN 1 ELSE count(DISTINCT e) END) AS promedio;
```

### R36 - Tiempos promedio de entrega
```cypher
MATCH (e:Envio)
WHERE e.estado = 'Entregado' AND e.fecha_creacion IS NOT NULL
RETURN avg(duration.between(e.fecha_creacion, datetime()).hours) AS promedio_horas_entrega;
```

### R37 - Distribución de tramos y envíos por tipo de transporte
```cypher
MATCH (t:Tramo)<-[:COMPUESTO_POR]-(e:Envio)
RETURN t.medio_transporte AS medio_transporte, count(e) AS total_envios_asociados;
```

### R38 - Análisis predictivo de atrasos y riesgos por severidad
```cypher
MATCH (e:Envio)-[:COMPUESTO_POR]->(t:Tramo)-[:REGISTRA]->(i:Incidente)
RETURN e.id_envio AS envio, i.tipo AS tipo_incidente, i.severidad AS severidad, 'Requiere atención' AS riesgo;
```

### R39 - Reporte operativo parametrizado (Filtro por País / Origen - Destino)
```cypher
MATCH (c:Cliente {pais: "Argentina"})-[:SOLICITA]->(e:Envio)-[cp:COMPUESTO_POR]->(t:Tramo)
OPTIONAL MATCH (t)-[:ORIGEN]->(ori:Ubicacion)
OPTIONAL MATCH (t)-[:DESTINO]->(des:Ubicacion)
RETURN c.razon_social AS cliente, e.id_envio AS envio, cp.orden AS orden, ori.nombre_puerto_nodo AS origen, des.nombre_puerto_nodo AS destino;
```

---

## 📂 Comandos para Git (Subir al repositorio)

Ejecutar desde la raíz del proyecto local:

```bash
git add database/neo4j/
git commit -m "docs: agregar instruccion de importacion en AuraDB y consultas Neo4j (Persona 3)"
git push origin main
```