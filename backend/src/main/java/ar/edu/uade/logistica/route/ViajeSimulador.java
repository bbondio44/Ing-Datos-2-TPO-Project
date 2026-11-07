package ar.edu.uade.logistica.route;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import ar.edu.uade.logistica.contracts.logistics.ShipmentResponse;
import ar.edu.uade.logistica.contracts.logistics.ShipmentStateRequest;
import ar.edu.uade.logistica.contracts.rutas.PuntoRutaResponse;
import ar.edu.uade.logistica.integration.neo4j.RutasService;
import ar.edu.uade.logistica.iot.IotService;
import ar.edu.uade.logistica.logistics.LogisticsService;
import ar.edu.uade.logistica.security.AuthenticatedUser;

/**
 * Simula un viaje: recorre los tramos del envío de a uno y, en cada paso, los sensores del contenedor
 * reportan la posición exacta entre los dos puntos. Las lecturas entran por IotService, como las de un
 * sensor real. Al terminar el último tramo el envío queda ENTREGADO.
 */
@Component
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class ViajeSimulador {
    private static final Logger log = LoggerFactory.getLogger(ViajeSimulador.class);
    private static final String SENSORES = """
            SELECT DISTINCT a.id_sensor FROM tp_logistica.envio_contenedor ec
              JOIN tp_logistica.asignacion_sensor_contenedor a ON a.id_contenedor = ec.id_contenedor
             WHERE ec.id_envio = ? AND ec.fecha_desasociacion IS NULL AND a.fecha_fin IS NULL
            """;
    private static final String TRAMOS = """
            SELECT id_tramo, origen, destino, estado, fecha_salida FROM tp_logistica.tramo
             WHERE id_envio = ? ORDER BY numero_tramo
            """;

    record Tramo(UUID id, String origen, String destino, String estado, Instant salida) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final LogisticsService envios;
    private final RutasService rutas;
    private final IotService lecturas;
    private final Duration porPunto;
    private final Set<UUID> enViaje = ConcurrentHashMap.newKeySet();

    public ViajeSimulador(JdbcTemplate jdbcTemplate, LogisticsService envios, RutasService rutas, IotService lecturas,
                          @Value("${app.viajes.segundos-por-punto:20}") long segundosPorPunto) {
        this.jdbcTemplate = jdbcTemplate;
        this.envios = envios;
        this.rutas = rutas;
        this.lecturas = lecturas;
        this.porPunto = Duration.ofSeconds(segundosPorPunto);
    }

    /** Si el viaje quedó a medias por un reinicio del backend, retoma desde el tramo en curso. */
    public ShipmentResponse iniciar(UUID idEnvio, AuthenticatedUser user) {
        ShipmentResponse envio = envios.shipment(idEnvio, user);
        if (List.of("ENTREGADO", "CANCELADO").contains(envio.state())) {
            throw new ConflictException("El envío ya está " + envio.state());
        }
        if (tramos(idEnvio).isEmpty()) throw new ConflictException("El envío no tiene tramos para recorrer");
        if (sensores(idEnvio).isEmpty()) {
            throw new ConflictException("Asociá un contenedor con un sensor antes de iniciar el viaje");
        }
        if ("CREADO".equals(envio.state())) envios.changeShipmentState(idEnvio, new ShipmentStateRequest("EN_PREPARACION", null));
        if (List.of("CREADO", "EN_PREPARACION").contains(envio.state())) {
            envios.changeShipmentState(idEnvio, new ShipmentStateRequest("EN_TRANSITO", null));
        }
        enViaje.add(idEnvio);
        return envios.shipment(idEnvio, user);
    }

    @Scheduled(fixedDelayString = "${app.viajes.intervalo-ms:4000}")
    public void avanzar() {
        for (UUID idEnvio : enViaje) {
            try {
                avanzar(idEnvio);
            } catch (RuntimeException exception) {
                log.warn("No se pudo avanzar el viaje {}: {}", idEnvio, exception.getMessage());
            }
        }
    }

    private void avanzar(UUID idEnvio) {
        List<Tramo> tramos = tramos(idEnvio);
        Tramo actual = tramos.stream().filter(t -> "EN_CURSO".equals(t.estado())).findFirst().orElse(null);
        if (actual == null) {
            Tramo siguiente = tramos.stream().filter(t -> "PENDIENTE".equals(t.estado())).findFirst().orElse(null);
            if (siguiente == null) terminar(idEnvio);
            else salir(siguiente);
            return;
        }
        Instant ahora = Instant.now();
        double avance = avance(actual.salida(), ahora, porPunto);
        reportarPosicion(idEnvio, actual, avance, ahora);
        if (avance >= 1) llegar(actual, ahora);
    }

    static double avance(Instant salida, Instant ahora, Duration duracion) {
        return Math.min(1, Duration.between(salida, ahora).toMillis() / (double) duracion.toMillis());
    }

    static double entre(double desde, double hasta, double avance) {
        return desde + (hasta - desde) * avance;
    }

    private void reportarPosicion(UUID idEnvio, Tramo tramo, double avance, Instant ahora) {
        Map<String, PuntoRutaResponse> puntos = rutas.puntos().stream()
                .collect(Collectors.toMap(PuntoRutaResponse::nombre, Function.identity(), (a, b) -> a));
        PuntoRutaResponse origen = puntos.get(tramo.origen());
        PuntoRutaResponse destino = puntos.get(tramo.destino());
        if (origen == null || destino == null) throw new ConflictException("El tramo no está en la red de rutas");
        double latitud = entre(origen.latitud(), destino.latitud(), avance);
        double longitud = entre(origen.longitud(), destino.longitud(), avance);
        for (UUID sensor : sensores(idEnvio)) {
            double temperatura = 4 + ThreadLocalRandom.current().nextDouble(0, 2);
            lecturas.registrar(new LecturaIotRequest(UUID.randomUUID(), sensor, ahora, temperatura, 60.0, 0.2, 90.0,
                    latitud, longitud));
        }
    }

    private void salir(Tramo tramo) {
        jdbcTemplate.update("UPDATE tp_logistica.tramo SET estado = 'EN_CURSO', fecha_salida = ? WHERE id_tramo = ?",
                Timestamp.from(Instant.now()), tramo.id());
    }

    /** El costo real del tramo es el calculado al crear el viaje. */
    private void llegar(Tramo tramo, Instant ahora) {
        jdbcTemplate.update("""
                UPDATE tp_logistica.tramo SET estado = 'FINALIZADO', fecha_llegada_real = ?, costo_real = costo_estimado
                 WHERE id_tramo = ?
                """, Timestamp.from(ahora), tramo.id());
    }

    private void terminar(UUID idEnvio) {
        liberarContenedores(idEnvio);
        envios.changeShipmentState(idEnvio, new ShipmentStateRequest("ENTREGADO", Instant.now()));
        enViaje.remove(idEnvio);
    }

    /** Al llegar, el contenedor y sus sensores quedan libres para otro viaje; la asignación queda como historia. */
    private void liberarContenedores(UUID idEnvio) {
        Timestamp ahora = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                UPDATE tp_logistica.asignacion_sensor_contenedor SET fecha_fin = ?
                 WHERE fecha_fin IS NULL AND id_contenedor IN (
                       SELECT id_contenedor FROM tp_logistica.envio_contenedor
                        WHERE id_envio = ? AND fecha_desasociacion IS NULL)
                """, ahora, idEnvio);
        jdbcTemplate.update("""
                UPDATE tp_logistica.contenedor SET estado = 'DISPONIBLE'
                 WHERE id_contenedor IN (SELECT id_contenedor FROM tp_logistica.envio_contenedor
                                          WHERE id_envio = ? AND fecha_desasociacion IS NULL)
                """, idEnvio);
        jdbcTemplate.update("""
                UPDATE tp_logistica.envio_contenedor SET fecha_desasociacion = ?
                 WHERE id_envio = ? AND fecha_desasociacion IS NULL
                """, ahora, idEnvio);
    }

    private List<UUID> sensores(UUID idEnvio) {
        return jdbcTemplate.queryForList(SENSORES, UUID.class, idEnvio);
    }

    private List<Tramo> tramos(UUID idEnvio) {
        return jdbcTemplate.query(TRAMOS, (fila, numero) -> new Tramo(fila.getObject("id_tramo", UUID.class),
                fila.getString("origen"), fila.getString("destino"), fila.getString("estado"),
                fila.getTimestamp("fecha_salida") == null ? null : fila.getTimestamp("fecha_salida").toInstant()), idEnvio);
    }
}
