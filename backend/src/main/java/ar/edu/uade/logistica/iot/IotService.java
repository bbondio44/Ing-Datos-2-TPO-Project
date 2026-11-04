package ar.edu.uade.logistica.iot;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import ar.edu.uade.logistica.contracts.iot.LecturaIotResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.cassandra", name = "enabled", havingValue = "true")
public class IotService {
    private static final String SENSOR_EXISTE = """
            SELECT count(*) FROM tp_logistica.sensor_iot WHERE id_sensor = ?
            """;
    // La asignación que importa es la vigente en el instante de la lectura, no la de ahora:
    // una lectura tardía pertenece al contenedor donde estaba el sensor cuando se midió.
    private static final String CONTENEDOR_EN_INSTANTE = """
            SELECT id_contenedor FROM tp_logistica.asignacion_sensor_contenedor
             WHERE id_sensor = ? AND fecha_inicio <= ? AND (fecha_fin IS NULL OR fecha_fin > ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final LecturaCassandraRepository lecturas;

    public IotService(JdbcTemplate jdbcTemplate, LecturaCassandraRepository lecturas) {
        this.jdbcTemplate = jdbcTemplate;
        this.lecturas = lecturas;
    }

    public LecturaIotResponse registrar(LecturaIotRequest lectura) {
        UUID idContenedor = contenedorEnInstante(lectura);
        lecturas.guardar(lectura, idContenedor);
        return new LecturaIotResponse(lectura.idLectura(), idContenedor);
    }

    private UUID contenedorEnInstante(LecturaIotRequest lectura) {
        Integer sensores = jdbcTemplate.queryForObject(SENSOR_EXISTE, Integer.class, lectura.idSensor());
        if (sensores == null || sensores == 0) {
            throw new NotFoundException("El sensor no existe");
        }
        Timestamp instante = Timestamp.from(lectura.timestamp());
        List<UUID> contenedores = jdbcTemplate.queryForList(
                CONTENEDOR_EN_INSTANTE, UUID.class, lectura.idSensor(), instante, instante);
        if (contenedores.isEmpty()) {
            throw new ConflictException("El sensor no tenía un contenedor asignado en ese instante");
        }
        return contenedores.get(0);
    }
}
