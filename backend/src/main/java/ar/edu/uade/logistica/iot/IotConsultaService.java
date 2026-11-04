package ar.edu.uade.logistica.iot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ar.edu.uade.logistica.common.BadRequestException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.contracts.iot.PosicionResponse;
import ar.edu.uade.logistica.contracts.iot.SensorEstadoResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.cassandra", name = "enabled", havingValue = "true")
public class IotConsultaService {
    private static final Duration RANGO_MAXIMO = Duration.ofDays(31);
    private static final int MINUTOS_INACTIVIDAD_POR_DEFECTO = 120;
    private static final String VER_TODO = "ENVIOS_LEER_TODOS";

    private static final String SENSOR_DEL_CLIENTE = """
            SELECT count(*) FROM tp_logistica.asignacion_sensor_contenedor a
              JOIN tp_logistica.envio_contenedor ec ON ec.id_contenedor = a.id_contenedor
              JOIN tp_logistica.envio e ON e.id_envio = ec.id_envio
             WHERE a.id_sensor = ? AND e.id_cliente = ?
            """;
    private static final String CONTENEDOR_DEL_CLIENTE = """
            SELECT count(*) FROM tp_logistica.envio_contenedor ec
              JOIN tp_logistica.envio e ON e.id_envio = ec.id_envio
             WHERE ec.id_contenedor = ? AND e.id_cliente = ?
            """;
    private static final String TODOS_LOS_SENSORES = """
            SELECT id_sensor, tipo FROM tp_logistica.sensor_iot ORDER BY tipo, id_sensor
            """;
    private static final String SENSORES_DEL_CLIENTE = """
            SELECT DISTINCT s.id_sensor, s.tipo FROM tp_logistica.sensor_iot s
              JOIN tp_logistica.asignacion_sensor_contenedor a ON a.id_sensor = s.id_sensor
              JOIN tp_logistica.envio_contenedor ec ON ec.id_contenedor = a.id_contenedor
              JOIN tp_logistica.envio e ON e.id_envio = ec.id_envio
             WHERE e.id_cliente = ? ORDER BY s.tipo, s.id_sensor
            """;
    private static final String UMBRAL_INACTIVIDAD = """
            SELECT min(minutos_inactividad) FROM tp_logistica.regla_alerta
             WHERE tipo_regla = 'INACTIVIDAD' AND activa
            """;

    private final JdbcTemplate jdbcTemplate;
    private final LecturaConsultaCassandraRepository lecturas;

    public IotConsultaService(JdbcTemplate jdbcTemplate, LecturaConsultaCassandraRepository lecturas) {
        this.jdbcTemplate = jdbcTemplate;
        this.lecturas = lecturas;
    }

    public List<LecturaHistoricaResponse> lecturasPorSensor(UUID idSensor, Instant desde, Instant hasta,
                                                            AuthenticatedUser user) {
        validarRango(desde, hasta);
        verificarAcceso(SENSOR_DEL_CLIENTE, idSensor, user);
        return lecturas.lecturasPorSensor(idSensor, desde, hasta);
    }

    public List<LecturaHistoricaResponse> lecturasPorContenedor(UUID idContenedor, Instant desde, Instant hasta,
                                                                AuthenticatedUser user) {
        validarRango(desde, hasta);
        verificarAcceso(CONTENEDOR_DEL_CLIENTE, idContenedor, user);
        return lecturas.lecturasPorContenedor(idContenedor, desde, hasta);
    }

    public List<PosicionResponse> recorrido(UUID idContenedor, Instant desde, Instant hasta, AuthenticatedUser user) {
        validarRango(desde, hasta);
        verificarAcceso(CONTENEDOR_DEL_CLIENTE, idContenedor, user);
        return lecturas.recorrido(idContenedor, desde, hasta);
    }

    public PosicionResponse ultimaPosicion(UUID idContenedor, AuthenticatedUser user) {
        verificarAcceso(CONTENEDOR_DEL_CLIENTE, idContenedor, user);
        return lecturas.ultimaPosicion(idContenedor)
                .orElseThrow(() -> new NotFoundException("El contenedor no tiene posiciones registradas"));
    }

    public List<SensorEstadoResponse> estadoSensores(AuthenticatedUser user) {
        List<SensorBasico> sensores = sensoresVisibles(user);
        int minutosLimite = minutosInactividad();
        Instant ahora = Instant.now();
        List<SensorEstadoResponse> estados = new ArrayList<>();
        for (SensorBasico sensor : sensores) {
            estados.add(estado(sensor, lecturas.ultimaLectura(sensor.id()), minutosLimite, ahora));
        }
        return estados;
    }

    /** Conexión según la última señal: sin lecturas, o con una más vieja que el límite, no está online. */
    static SensorEstadoResponse estado(SensorBasico sensor, Optional<LecturaHistoricaResponse> ultima,
                                       int minutosLimite, Instant ahora) {
        if (ultima.isEmpty()) {
            return new SensorEstadoResponse(sensor.id(), sensor.tipo(), null, "SIN_DATOS", null, null, null);
        }
        LecturaHistoricaResponse lectura = ultima.get();
        long minutos = Duration.between(lectura.fechaHora(), ahora).toMinutes();
        return new SensorEstadoResponse(sensor.id(), sensor.tipo(), lectura.idContenedor(),
                minutos > minutosLimite ? "OFFLINE" : "ONLINE", lectura.bateria(), lectura.fechaHora(), minutos);
    }

    private List<SensorBasico> sensoresVisibles(AuthenticatedUser user) {
        if (user.permissions().contains(VER_TODO)) {
            return jdbcTemplate.query(TODOS_LOS_SENSORES, (rs, n) ->
                    new SensorBasico(rs.getObject("id_sensor", UUID.class), rs.getString("tipo")));
        }
        if (user.clientId() == null) return List.of();
        return jdbcTemplate.query(SENSORES_DEL_CLIENTE, (rs, n) ->
                new SensorBasico(rs.getObject("id_sensor", UUID.class), rs.getString("tipo")), user.clientId());
    }

    private int minutosInactividad() {
        Integer minutos = jdbcTemplate.queryForObject(UMBRAL_INACTIVIDAD, Integer.class);
        return minutos == null ? MINUTOS_INACTIVIDAD_POR_DEFECTO : minutos;
    }

    private void validarRango(Instant desde, Instant hasta) {
        if (!desde.isBefore(hasta)) {
            throw new BadRequestException("La fecha desde debe ser anterior a la fecha hasta");
        }
        if (Duration.between(desde, hasta).compareTo(RANGO_MAXIMO) > 0) {
            throw new BadRequestException("El rango no puede superar los 31 días");
        }
    }

    /** Un cliente solo ve lo suyo. Lo ajeno responde 404, para no revelar que existe. */
    private void verificarAcceso(String consulta, UUID id, AuthenticatedUser user) {
        if (user.permissions().contains(VER_TODO)) return;
        Integer cantidad = user.clientId() == null ? Integer.valueOf(0)
                : jdbcTemplate.queryForObject(consulta, Integer.class, id, user.clientId());
        if (cantidad == null || cantidad == 0) {
            throw new NotFoundException("No se encontró el recurso solicitado");
        }
    }

    record SensorBasico(UUID id, String tipo) { }
}
