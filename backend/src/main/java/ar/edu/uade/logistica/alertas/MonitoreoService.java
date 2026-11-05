package ar.edu.uade.logistica.alertas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.alertas.MonitoreoEnvioResponse;
import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.contracts.iot.PosicionResponse;
import ar.edu.uade.logistica.iot.LecturaConsultaCassandraRepository;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** R17: no hay JOIN entre motores, así que la aplicación consulta cada uno por el UUID compartido. */
@Service
@ConditionalOnProperty(name = {"app.mongo.enabled", "app.cassandra.enabled"}, havingValue = "true")
public class MonitoreoService {
    private final JdbcTemplate jdbcTemplate;
    private final LecturaConsultaCassandraRepository lecturas;
    private final AlertaMongoRepository alertas;
    private final IncidenteRepository incidentes;

    public MonitoreoService(JdbcTemplate jdbcTemplate, LecturaConsultaCassandraRepository lecturas,
                            AlertaMongoRepository alertas, IncidenteRepository incidentes) {
        this.jdbcTemplate = jdbcTemplate;
        this.lecturas = lecturas;
        this.alertas = alertas;
        this.incidentes = incidentes;
    }

    public MonitoreoEnvioResponse envio(UUID id, AuthenticatedUser user) {
        List<Map<String, Object>> filas = jdbcTemplate.queryForList("""
                SELECT id_cliente, origen, destino, estado FROM tp_logistica.envio WHERE id_envio = ?
                """, id);
        if (filas.isEmpty()) throw new NotFoundException("El envío no existe");
        Map<String, Object> envio = filas.get(0);
        UUID idCliente = (UUID) envio.get("id_cliente");
        if (!user.permissions().contains("ENVIOS_LEER_TODOS") && !idCliente.equals(user.clientId())) {
            throw new NotFoundException("El envío no existe");
        }

        List<UUID> contenedores = jdbcTemplate.queryForList("""
                SELECT DISTINCT id_contenedor FROM tp_logistica.envio_contenedor WHERE id_envio = ?
                """, UUID.class, id);
        List<UUID> sensores = jdbcTemplate.queryForList("""
                SELECT DISTINCT a.id_sensor FROM tp_logistica.asignacion_sensor_contenedor a
                  JOIN tp_logistica.envio_contenedor ec ON ec.id_contenedor = a.id_contenedor
                 WHERE ec.id_envio = ?
                """, UUID.class, id);

        List<LecturaHistoricaResponse> ultimasLecturas = new ArrayList<>();
        sensores.forEach(sensor -> lecturas.ultimaLectura(sensor).ifPresent(ultimasLecturas::add));
        List<PosicionResponse> ultimasPosiciones = new ArrayList<>();
        contenedores.forEach(contenedor -> lecturas.ultimaPosicion(contenedor).ifPresent(ultimasPosiciones::add));

        return new MonitoreoEnvioResponse(id, idCliente, (String) envio.get("origen"), (String) envio.get("destino"),
                (String) envio.get("estado"), contenedores, ultimasLecturas, ultimasPosiciones,
                alertas.eventosPorEnvio(id.toString()), alertas.porEnvio(id.toString()), incidentes.porEnvio(id));
    }
}
