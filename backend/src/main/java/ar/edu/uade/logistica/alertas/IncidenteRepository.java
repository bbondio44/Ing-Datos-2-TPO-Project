package ar.edu.uade.logistica.alertas;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.contracts.alertas.IncidenteResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Los incidentes son estado crítico: su fuente de verdad es PostgreSQL. */
@Repository
public class IncidenteRepository {
    private final JdbcTemplate jdbcTemplate;

    public IncidenteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** id_evento_externo es UNIQUE: reprocesar la misma alerta no crea otro incidente (D04). */
    public void crear(AlertaNueva alerta, UUID idTramo) {
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.incidente
                  (id_incidente, id_envio, id_tramo, id_evento_externo, tipo, severidad, descripcion, fecha)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id_evento_externo) DO NOTHING
                """, UUID.fromString(alerta.idIncidente()), alerta.idEnvio(), idTramo, UUID.fromString(alerta.idEvento()),
                "ALERTA_" + alerta.regla().tipoRegla(), alerta.regla().severidad(), alerta.mensaje(),
                Timestamp.from(alerta.ahora()));
    }

    /** El tramo que se estaba recorriendo en ese instante. Puede no haber, y el incidente queda sin tramo. */
    public UUID tramoEnCurso(UUID idEnvio, Instant instante) {
        Timestamp momento = Timestamp.from(instante);
        List<UUID> tramos = jdbcTemplate.queryForList("""
                SELECT id_tramo FROM tp_logistica.tramo
                 WHERE id_envio = ? AND fecha_salida <= ?
                   AND COALESCE(fecha_llegada_real, fecha_llegada_estimada) >= ?
                 ORDER BY numero_tramo LIMIT 1
                """, UUID.class, idEnvio, momento, momento);
        return tramos.isEmpty() ? null : tramos.get(0);
    }

    public List<IncidenteResponse> porEnvio(UUID idEnvio) {
        return jdbcTemplate.query("""
                SELECT id_incidente, id_envio, tipo, severidad, descripcion, fecha, estado
                  FROM tp_logistica.incidente WHERE id_envio = ? ORDER BY fecha DESC
                """, (rs, fila) -> new IncidenteResponse(
                rs.getObject("id_incidente", UUID.class), rs.getObject("id_envio", UUID.class), rs.getString("tipo"),
                rs.getString("severidad"), rs.getString("descripcion"), rs.getTimestamp("fecha").toInstant(),
                rs.getString("estado")), idEnvio);
    }
}
