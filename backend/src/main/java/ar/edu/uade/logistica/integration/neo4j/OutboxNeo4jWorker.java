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
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Patrón outbox: cada cambio en PostgreSQL deja una fila en outbox_evento dentro de la misma transacción.
 * Este worker la lee, actualiza Neo4j y la marca procesada. Si Neo4j está caído no se pierde nada: el
 * evento queda en ERROR con un próximo intento. Es consistencia eventual: el grafo se pone al día después.
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

    // FOR UPDATE SKIP LOCKED: bloqueo exclusivo por fila; un segundo worker salta las filas ya tomadas.
    private static final String RECLAMAR_LOTE = """
            UPDATE tp_logistica.outbox_evento SET estado = 'PROCESANDO'
             WHERE id_evento IN (
                   SELECT id_evento FROM tp_logistica.outbox_evento
                    WHERE estado IN ('PENDIENTE', 'ERROR')
                      AND (proximo_intento IS NULL OR proximo_intento <= now())
                    ORDER BY fecha_creacion LIMIT ?
                    FOR UPDATE SKIP LOCKED)
            RETURNING id_evento, agregado_tipo, tipo_evento, payload::text AS payload, intentos, fecha_creacion
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Driver driver;
    private final String database;
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean preparado;

    public OutboxNeo4jWorker(JdbcTemplate jdbcTemplate, Driver driver,
                             @Value("${app.neo4j.database:neo4j}") String database) {
        this.jdbcTemplate = jdbcTemplate;
        this.driver = driver;
        this.database = database;
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
        if (sentencias.isEmpty()) return;
        // Una transacción por evento: o se aplica entero o no se aplica.
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.executeWriteWithoutResult(transaccion -> sentencias.forEach(
                    sentencia -> transaccion.run(sentencia.cypher(), sentencia.parametros()).consume()));
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
