package ar.edu.uade.logistica.alertas;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

import ar.edu.uade.logistica.contracts.alertas.AlertaResponse;
import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaResponse;
import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import ar.edu.uade.logistica.iot.LecturaConsultaCassandraRepository;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Evalúa las reglas de PostgreSQL sobre la telemetría y guarda las alertas en MongoDB. */
@Service
@ConditionalOnProperty(name = {"app.mongo.enabled", "app.cassandra.enabled"}, havingValue = "true")
public class AlertaService {
    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);
    private static final Duration VENTANA_ANOMALIA = Duration.ofDays(2);
    private static final int MUESTRA_MINIMA = 10;
    private static final String VER_TODO = "ENVIOS_LEER_TODOS";

    private static final String ENVIO_EN_INSTANTE = """
            SELECT e.id_envio, e.id_cliente FROM tp_logistica.envio_contenedor ec
              JOIN tp_logistica.envio e ON e.id_envio = ec.id_envio
             WHERE ec.id_contenedor = ? AND ec.fecha_asociacion <= ?
               AND (ec.fecha_desasociacion IS NULL OR ec.fecha_desasociacion > ?)
            """;
    private static final String SENSORES_VIGENTES = """
            SELECT id_sensor, id_contenedor, id_envio, id_cliente
              FROM tp_logistica.v_envio_contenedor_sensor_activo
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ReglaAlertaRepository reglas;
    private final AlertaMongoRepository alertas;
    private final IncidenteRepository incidentes;
    private final LecturaConsultaCassandraRepository lecturas;

    public AlertaService(JdbcTemplate jdbcTemplate, ReglaAlertaRepository reglas, AlertaMongoRepository alertas,
                         IncidenteRepository incidentes, LecturaConsultaCassandraRepository lecturas) {
        this.jdbcTemplate = jdbcTemplate;
        this.reglas = reglas;
        this.alertas = alertas;
        this.incidentes = incidentes;
        this.lecturas = lecturas;
    }

    /** Evalúa una lectura recién guardada contra las reglas que le aplican. */
    public void evaluar(LecturaIotRequest lectura, UUID idContenedor) {
        Timestamp instante = Timestamp.from(lectura.timestamp());
        List<UUID[]> envios = jdbcTemplate.query(ENVIO_EN_INSTANTE, (rs, fila) -> new UUID[] {
                rs.getObject("id_envio", UUID.class), rs.getObject("id_cliente", UUID.class)},
                idContenedor, instante, instante);
        UUID idEnvio = envios.isEmpty() ? null : envios.get(0)[0];
        UUID idCliente = envios.isEmpty() ? null : envios.get(0)[1];
        Instant ahora = Instant.now();

        for (ReglaAlertaResponse regla : reglas.aplicables(lectura.idSensor(), idCliente)) {
            OptionalDouble valor = valorObservado(regla, lectura);
            if (valor.isPresent() && cumple(valor.getAsDouble(), regla.operador(), regla.umbral())) {
                String mensaje = String.format(Locale.ROOT, "%s: valor %.2f %s %.2f",
                        regla.nombre(), valor.getAsDouble(), regla.operador(), regla.umbral());
                registrar(new AlertaNueva(regla, lectura.idSensor(), idContenedor, idEnvio, idCliente,
                        lectura.idLectura(), valor.getAsDouble(), regla.umbral(), mensaje, lectura.timestamp(), ahora));
            }
        }
    }

    /** Tarea periódica: sensores asignados que dejaron de reportar o tienen la batería baja. */
    public void evaluarSensoresVigentes() {
        Instant ahora = Instant.now();
        jdbcTemplate.query(SENSORES_VIGENTES, rs -> {
            UUID idSensor = rs.getObject("id_sensor", UUID.class);
            UUID idContenedor = rs.getObject("id_contenedor", UUID.class);
            UUID idEnvio = rs.getObject("id_envio", UUID.class);
            UUID idCliente = rs.getObject("id_cliente", UUID.class);
            Optional<LecturaHistoricaResponse> ultima = lecturas.ultimaLectura(idSensor);
            if (ultima.isEmpty()) return;
            for (ReglaAlertaResponse regla : reglas.aplicables(idSensor, idCliente)) {
                evaluarEstado(regla, ultima.get(), idSensor, idContenedor, idEnvio, idCliente, ahora);
            }
        });
    }

    private void evaluarEstado(ReglaAlertaResponse regla, LecturaHistoricaResponse ultima, UUID idSensor,
                               UUID idContenedor, UUID idEnvio, UUID idCliente, Instant ahora) {
        if ("INACTIVIDAD".equals(regla.tipoRegla())) {
            long minutos = Duration.between(ultima.fechaHora(), ahora).toMinutes();
            if (minutos > regla.minutosInactividad()) {
                registrar(new AlertaNueva(regla, idSensor, idContenedor, idEnvio, idCliente, null, minutos,
                        regla.minutosInactividad(), "Sensor sin lecturas hace " + minutos + " minutos",
                        ultima.fechaHora(), ahora));
            }
        } else if ("BATERIA".equals(regla.tipoRegla()) && ultima.bateria() != null
                && cumple(ultima.bateria(), regla.operador(), regla.umbral())) {
            String mensaje = String.format(Locale.ROOT, "Batería baja: %.1f %%", ultima.bateria());
            registrar(new AlertaNueva(regla, idSensor, idContenedor, idEnvio, idCliente, ultima.idLectura(),
                    ultima.bateria(), regla.umbral(), mensaje, ultima.fechaHora(), ahora));
        }
    }

    private void registrar(AlertaNueva alerta) {
        if (alertas.sumarOcurrencia(alerta)) return;
        // El incidente va primero: así nunca queda una alerta ALTA/CRITICA sin incidente (control de R25).
        boolean conIncidente = alerta.generaIncidente();
        if (conIncidente) incidentes.crear(alerta);
        if (alertas.insertar(alerta, conIncidente) && alerta.idEnvio() != null) {
            alertas.insertarEvento(alerta, conIncidente);
        }
        log.info("Alerta {} de regla {} para el sensor {}", alerta.idAlerta(), alerta.regla().nombre(), alerta.idSensor());
    }

    private OptionalDouble valorObservado(ReglaAlertaResponse regla, LecturaIotRequest lectura) {
        return switch (regla.tipoRegla()) {
            case "TEMPERATURA" -> opcional(lectura.temperatura());
            case "HUMEDAD" -> opcional(lectura.humedad());
            case "VIBRACION" -> opcional(lectura.vibracion());
            case "BATERIA" -> opcional(lectura.bateria());
            case "ANOMALIA_TEMPERATURA" -> puntajeZ(lectura);
            default -> OptionalDouble.empty();
        };
    }

    /** R32: cuántos desvíos estándar se aleja la temperatura de la media reciente del sensor. */
    private OptionalDouble puntajeZ(LecturaIotRequest lectura) {
        if (lectura.temperatura() == null) return OptionalDouble.empty();
        List<Double> historico = lecturas.lecturasPorSensor(lectura.idSensor(),
                        lectura.timestamp().minus(VENTANA_ANOMALIA), lectura.timestamp()).stream()
                .filter(l -> !lectura.idLectura().equals(l.idLectura()) && l.temperatura() != null)
                .map(LecturaHistoricaResponse::temperatura)
                .toList();
        return puntajeZ(lectura.temperatura(), historico);
    }

    static OptionalDouble puntajeZ(double valor, List<Double> historico) {
        if (historico.size() < MUESTRA_MINIMA) return OptionalDouble.empty();
        double media = historico.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double varianza = historico.stream().mapToDouble(v -> (v - media) * (v - media)).average().orElse(0);
        double desvio = Math.sqrt(varianza);
        return desvio == 0 ? OptionalDouble.empty() : OptionalDouble.of(Math.abs(valor - media) / desvio);
    }

    static boolean cumple(double valor, String operador, double umbral) {
        return switch (operador) {
            case ">" -> valor > umbral;
            case ">=" -> valor >= umbral;
            case "<" -> valor < umbral;
            case "<=" -> valor <= umbral;
            default -> false;
        };
    }

    private OptionalDouble opcional(Double valor) {
        return valor == null ? OptionalDouble.empty() : OptionalDouble.of(valor);
    }

    public List<AlertaResponse> listar(String estado, AuthenticatedUser user) {
        if (user.permissions().contains(VER_TODO)) return alertas.listar(estado, null);
        if (user.clientId() == null) return List.of();
        return alertas.listar(estado, user.clientId().toString());
    }

    public void reconocer(String id, String comentario, AuthenticatedUser user) {
        alertas.reconocer(id, user.userId().toString(), comentario);
    }

    public void cerrar(String id, String comentario, AuthenticatedUser user) {
        alertas.cerrar(id, user.userId().toString(), comentario);
    }
}
