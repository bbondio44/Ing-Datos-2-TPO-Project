package ar.edu.uade.logistica.integration.neo4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ar.edu.uade.logistica.alertas.AlertaMongoRepository;
import ar.edu.uade.logistica.alertas.EventoDesdeOutbox;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Patrón outbox: cada cambio en PostgreSQL deja una fila en outbox_evento dentro de la misma transacción.
 * Este worker la lee, actualiza Neo4j, registra en MongoDB el evento logístico si cambió un estado y la
 * marca procesada. Si un motor está caído no se pierde nada: el evento queda en ERROR con un próximo
 * intento. Es consistencia eventual: el grafo y el historial se ponen al día después.
 */
@Component
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class OutboxNeo4jWorker {
    private static final Logger log = LoggerFactory.getLogger(OutboxNeo4jWorker.class);
    private static final int TAMANIO_LOTE = 200;
    private static final int ESPERA_MAXIMA_SEGUNDOS = 300;

    private static final Map<String, String> RESTRICCIONES = Map.of(
            "Cliente", "id_cliente", "Envio", "id_envio", "Contenedor", "id_contenedor", "Tramo", "id_tramo",
            "Ubicacion", "id_ubicacion", "Vehiculo", "id_vehiculo", "Operador", "id_operador",
            "Incidente", "id_incidente");
    private static final List<String> INDICES = List.of(
            "CREATE INDEX envio_estado IF NOT EXISTS FOR (n:Envio) ON (n.estado)",
            "CREATE INDEX tramo_medio IF NOT EXISTS FOR (n:Tramo) ON (n.medio_transporte)",
            "CREATE INDEX incidente_tipo IF NOT EXISTS FOR (n:Incidente) ON (n.tipo)");

    // FOR UPDATE SKIP LOCKED: bloqueo exclusivo por fila; un segundo worker salta las filas ya tomadas.
    // El NOT EXISTS mantiene el orden por entidad: si un evento anterior de la misma entidad sigue sin
    // procesar, este espera. Si no, un reintento viejo pisaría en el grafo un cambio más nuevo.
    private static final String RECLAMAR_LOTE = """
            UPDATE tp_logistica.outbox_evento SET estado = 'PROCESANDO'
             WHERE id_evento IN (
                   SELECT o.id_evento FROM tp_logistica.outbox_evento o
                    WHERE o.estado IN ('PENDIENTE', 'ERROR')
                      AND (o.proximo_intento IS NULL OR o.proximo_intento <= now())
                      AND NOT EXISTS (
                          SELECT 1 FROM tp_logistica.outbox_evento previo
                           WHERE previo.agregado_tipo = o.agregado_tipo
                             AND previo.agregado_id = o.agregado_id
                             AND previo.estado <> 'PROCESADO'
                             AND previo.fecha_creacion < o.fecha_creacion)
                    ORDER BY o.fecha_creacion LIMIT ?
                    FOR UPDATE OF o SKIP LOCKED)
            RETURNING id_evento, agregado_tipo, tipo_evento, payload::text AS payload, intentos, fecha_creacion
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Driver driver;
    private final String database;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectProvider<AlertaMongoRepository> eventos;
    private boolean preparado;

    public OutboxNeo4jWorker(JdbcTemplate jdbcTemplate, Driver driver,
                             @Value("${app.neo4j.database:neo4j}") String database,
                             ObjectProvider<AlertaMongoRepository> eventos) {
        this.jdbcTemplate = jdbcTemplate;
        this.driver = driver;
        this.database = database;
        this.eventos = eventos;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:5000}", initialDelayString = "${app.outbox.intervalo-ms:5000}")
    public void procesar() {
        try {
            preparar();
            List<Evento> eventos = reclamarLote();
            for (int i = 0; i < eventos.size(); i++) {
                try {
                    proyectar(eventos.get(i));
                    marcarProcesado(eventos.get(i));
                } catch (RuntimeException | JsonProcessingException exception) {
                    log.warn("No se pudo proyectar el evento {}: {}", eventos.get(i).id(), exception.getMessage());
                    marcarError(eventos.get(i), exception);
                    liberar(eventos.subList(i + 1, eventos.size()));
                    return;
                }
            }
        } catch (RuntimeException exception) {
            log.warn("No se pudo procesar el outbox: {}", exception.getMessage());
        }
    }

    /** Al reiniciar, lo que quedó PROCESANDO de una corrida interrumpida vuelve a la cola. */
    private void preparar() {
        if (preparado) return;
        jdbcTemplate.update("UPDATE tp_logistica.outbox_evento SET estado = 'PENDIENTE' WHERE estado = 'PROCESANDO'");
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            RESTRICCIONES.forEach((etiqueta, propiedad) -> session.run("CREATE CONSTRAINT "
                    + etiqueta.toLowerCase() + "_id IF NOT EXISTS FOR (n:" + etiqueta + ") REQUIRE n." + propiedad
                    + " IS UNIQUE").consume());
            INDICES.forEach(indice -> session.run(indice).consume());
        }
        preparado = true;
    }

    private List<Evento> reclamarLote() {
        List<Evento> eventos = new ArrayList<>(jdbcTemplate.query(RECLAMAR_LOTE, (rs, fila) -> new Evento(
                rs.getObject("id_evento", UUID.class), rs.getString("agregado_tipo"), rs.getString("tipo_evento"),
                rs.getString("payload"), rs.getInt("intentos"), rs.getTimestamp("fecha_creacion").toInstant()),
                TAMANIO_LOTE));
        eventos.sort(Comparator.comparing(Evento::fecha));
        return eventos;
    }

    private void proyectar(Evento evento) throws JsonProcessingException {
        Map<String, Object> payload = mapper.readValue(evento.payload(), new TypeReference<>() { });
        List<ProyeccionGrafo.Sentencia> sentencias =
                ProyeccionGrafo.sentencias(evento.agregado(), evento.tipo(), payload);
        if (!sentencias.isEmpty()) {
            // Una transacción por evento: o se aplica entero o no se aplica.
            try (Session session = driver.session(SessionConfig.forDatabase(database))) {
                session.executeWriteWithoutResult(transaccion -> sentencias.forEach(
                        sentencia -> transaccion.run(sentencia.cypher(), sentencia.parametros()).consume()));
            }
        }
        AlertaMongoRepository mongo = eventos.getIfAvailable();
        if (mongo != null) {
            EventoDesdeOutbox.desde(evento.agregado(), evento.tipo(), payload)
                    .ifPresent(cambio -> mongo.registrarCambio(cambio, evento.fecha(), evento.id().toString()));
        }
    }

    private void marcarProcesado(Evento evento) {
        jdbcTemplate.update("""
                UPDATE tp_logistica.outbox_evento
                   SET estado = 'PROCESADO', fecha_procesamiento = now(), ultimo_error = NULL, proximo_intento = NULL
                 WHERE id_evento = ?
                """, evento.id());
    }

    private void marcarError(Evento evento, Exception error) {
        jdbcTemplate.update("""
                UPDATE tp_logistica.outbox_evento
                   SET estado = 'ERROR', intentos = intentos + 1, ultimo_error = ?,
                       proximo_intento = now() + make_interval(secs => ?)
                 WHERE id_evento = ?
                """, String.valueOf(error.getMessage()), esperaSegundos(evento.intentos()), evento.id());
    }

    private void liberar(List<Evento> pendientes) {
        pendientes.forEach(evento -> jdbcTemplate.update(
                "UPDATE tp_logistica.outbox_evento SET estado = 'PENDIENTE' WHERE id_evento = ?", evento.id()));
    }

    /** Cada fallo duplica la espera, hasta 5 minutos. */
    static int esperaSegundos(int intentos) {
        return (int) Math.min(ESPERA_MAXIMA_SEGUNDOS, 5L << Math.min(intentos, 10));
    }

    private record Evento(UUID id, String agregado, String tipo, String payload, int intentos, Instant fecha) { }
}
