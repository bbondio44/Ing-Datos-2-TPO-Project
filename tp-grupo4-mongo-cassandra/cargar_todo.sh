#!/usr/bin/env bash
# Ingeniería de Datos II · Grupo 4 · Carga de MongoDB y Cassandra en Docker (Linux / macOS / Git Bash)
#   ./cargar_todo.sh                         detecta los contenedores por imagen
#   MONGO=mi_mongo CASSANDRA=mi_cass ./cargar_todo.sh
#   MONGO_USER=admin MONGO_PASS=secreto ./cargar_todo.sh
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p evidencias
MONGO_DB="${MONGO_DB:-logistica_docs}"

buscar() { docker ps --format '{{.Names}} {{.Image}}' | awk -v p="$1" '$2 ~ p {print $1; exit}'; }
C="${CASSANDRA:-$(buscar cassandra)}"
M="${MONGO:-$(buscar mongo)}"

if [ -n "$C" ]; then
  echo "==== Cassandra ($C)"
  until docker exec "$C" cqlsh -e "SELECT release_version FROM system.local;" >/dev/null 2>&1; do
    echo "  esperando a Cassandra..."; sleep 5
  done
  docker exec "$C" mkdir -p /tmp/tp_grupo4
  docker cp cassandra/. "$C":/tmp/tp_grupo4/
  docker exec "$C" cqlsh -f /tmp/tp_grupo4/01_schema.cql | tee evidencias/cassandra_01_schema.txt
  echo "  cargando 02_datos_prueba.cql (1-3 minutos)..."
  docker exec "$C" cqlsh --request-timeout=60 -f /tmp/tp_grupo4/02_datos_prueba.cql > evidencias/cassandra_02_datos.txt 2>&1
  docker exec "$C" cqlsh -f /tmp/tp_grupo4/03_consultas.cql > evidencias/cassandra_03_consultas.txt 2>&1
  if grep -Eq 'SyntaxException|InvalidRequest|ConfigurationException' evidencias/cassandra_0*.txt; then
    echo "  ERROR en Cassandra, ver evidencias/cassandra_*.txt"; exit 1
  fi
  docker exec "$C" cqlsh -e "SELECT COUNT(*) FROM logistica_iot.lecturas_por_sensor_dia;" 2>/dev/null | grep -E '^\s*[0-9]+\s*$' \
    | xargs -I{} echo "  filas en lecturas_por_sensor_dia: {} (esperado 2773)" || true
else
  echo "No encontré contenedor de Cassandra (usá CASSANDRA=<nombre>)"
fi

if [ -n "$M" ]; then
  echo "==== MongoDB ($M)"
  SH=mongosh; docker exec "$M" sh -c 'command -v mongosh' >/dev/null || SH=mongo
  AUTH=(); [ -n "${MONGO_USER:-}" ] && AUTH=(-u "$MONGO_USER" -p "$MONGO_PASS" --authenticationDatabase admin)
  docker exec "$M" mkdir -p /tmp/tp_grupo4
  docker cp mongodb/. "$M":/tmp/tp_grupo4/
  for f in 01_schema 02_datos_prueba; do
    docker exec "$M" $SH --quiet ${AUTH[@]+"${AUTH[@]}"} "$MONGO_DB" /tmp/tp_grupo4/$f.js | tee "evidencias/mongodb_$f.txt"
  done
  docker exec "$M" $SH --quiet ${AUTH[@]+"${AUTH[@]}"} "$MONGO_DB" /tmp/tp_grupo4/03_consultas.js > evidencias/mongodb_03_consultas.txt
  grep -E '^(OK|FALLA|RESUMEN)' evidencias/mongodb_03_consultas.txt
else
  echo "No encontré contenedor de MongoDB (usá MONGO=<nombre>)"
fi
