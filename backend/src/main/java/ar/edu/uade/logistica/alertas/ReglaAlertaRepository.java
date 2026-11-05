package ar.edu.uade.logistica.alertas;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaRequest;
import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Las reglas son maestros: viven en PostgreSQL. MongoDB solo guarda las alertas que generan. */
@Repository
public class ReglaAlertaRepository {
    private static final String COLUMNAS = """
            id_regla, id_cliente, id_sensor, nombre, tipo_regla, operador, umbral,
            minutos_inactividad, severidad, activa
            """;

    private final JdbcTemplate jdbcTemplate;

    public ReglaAlertaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReglaAlertaResponse> listar() {
        return jdbcTemplate.query("SELECT " + COLUMNAS + " FROM tp_logistica.regla_alerta ORDER BY tipo_regla, nombre",
                this::mapear);
    }

    /** Reglas activas que aplican a un sensor: las suyas, las de su cliente y las globales. */
    public List<ReglaAlertaResponse> aplicables(UUID idSensor, UUID idCliente) {
        return jdbcTemplate.query("SELECT " + COLUMNAS + """
                 FROM tp_logistica.regla_alerta
                 WHERE activa AND (id_sensor IS NULL OR id_sensor = ?)
                   AND (id_cliente IS NULL OR id_cliente = ?)
                """, this::mapear, idSensor, idCliente);
    }

    public ReglaAlertaResponse crear(ReglaAlertaRequest regla) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.regla_alerta
                  (id_regla, id_cliente, id_sensor, nombre, tipo_regla, operador, umbral,
                   minutos_inactividad, severidad, activa)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, regla.idCliente(), regla.idSensor(), regla.nombre(), regla.tipoRegla(), regla.operador(),
                regla.umbral(), regla.minutosInactividad(), regla.severidad(), regla.activa());
        return buscar(id);
    }

    public ReglaAlertaResponse actualizar(UUID id, ReglaAlertaRequest regla) {
        int filas = jdbcTemplate.update("""
                UPDATE tp_logistica.regla_alerta
                   SET id_cliente = ?, id_sensor = ?, nombre = ?, tipo_regla = ?, operador = ?, umbral = ?,
                       minutos_inactividad = ?, severidad = ?, activa = ?
                 WHERE id_regla = ?
                """, regla.idCliente(), regla.idSensor(), regla.nombre(), regla.tipoRegla(), regla.operador(),
                regla.umbral(), regla.minutosInactividad(), regla.severidad(), regla.activa(), id);
        if (filas == 0) throw new NotFoundException("La regla no existe");
        return buscar(id);
    }

    /** Baja lógica: las alertas históricas siguen referenciando la regla. */
    public void desactivar(UUID id) {
        int filas = jdbcTemplate.update("UPDATE tp_logistica.regla_alerta SET activa = false WHERE id_regla = ?", id);
        if (filas == 0) throw new NotFoundException("La regla no existe");
    }

    private ReglaAlertaResponse buscar(UUID id) {
        return jdbcTemplate.queryForObject("SELECT " + COLUMNAS + " FROM tp_logistica.regla_alerta WHERE id_regla = ?",
                this::mapear, id);
    }

    private ReglaAlertaResponse mapear(ResultSet rs, int fila) throws SQLException {
        return new ReglaAlertaResponse(
                rs.getObject("id_regla", UUID.class), rs.getObject("id_cliente", UUID.class),
                rs.getObject("id_sensor", UUID.class), rs.getString("nombre"), rs.getString("tipo_regla"),
                rs.getString("operador"), rs.getObject("umbral") == null ? null : rs.getDouble("umbral"),
                rs.getObject("minutos_inactividad", Integer.class), rs.getString("severidad"), rs.getBoolean("activa"));
    }
}
