package ar.edu.uade.logistica.analitica;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * R29: usa la función baseline de PostgreSQL, que predice con la duración promedio de envíos entregados
 * entre los mismos países. Es un promedio histórico, no un modelo entrenado.
 */
@Service
public class PrediccionService {
    private static final Logger log = LoggerFactory.getLogger(PrediccionService.class);

    /** El NOT EXISTS evita repetir la predicción del mismo día si la tarea corre dos veces (D04). */
    private static final String ENVIOS_SIN_PREDICCION_HOY = """
            SELECT e.id_envio FROM tp_logistica.envio e
             WHERE e.estado IN ('EN_TRANSITO', 'DEMORADO')
               AND NOT EXISTS (SELECT 1 FROM tp_logistica.prediccion_envio p
                                WHERE p.id_envio = e.id_envio AND p.fecha_prediccion::date = current_date)
            """;
    private static final String ULTIMAS_PREDICCIONES = """
            SELECT e.id_envio, c.razon_social AS cliente, e.origen, e.destino, e.estado,
                   e.fecha_entrega_estimada, p.eta_predicha, p.probabilidad_atraso,
                   p.variables::text AS factores, p.fecha_prediccion
              FROM tp_logistica.prediccion_envio p
              JOIN tp_logistica.envio e ON e.id_envio = p.id_envio
              JOIN tp_logistica.cliente c ON c.id_cliente = e.id_cliente
             WHERE e.estado IN ('EN_TRANSITO', 'DEMORADO')
               AND p.fecha_prediccion = (SELECT max(u.fecha_prediccion) FROM tp_logistica.prediccion_envio u
                                          WHERE u.id_envio = p.id_envio)
            """;
    /** Validación: predice cada envío entregado con el promedio de los demás y compara con lo real. */
    private static final String VALIDACION = """
            WITH duracion AS (
                SELECT EXTRACT(EPOCH FROM (fecha_entrega_real - fecha_creacion)) / 3600 AS horas
                  FROM tp_logistica.envio
                 WHERE estado = 'ENTREGADO' AND fecha_entrega_real IS NOT NULL),
            totales AS (SELECT count(*) AS n, sum(horas) AS suma FROM duracion)
            SELECT t.n AS envios_evaluados, ROUND(avg(d.horas)::numeric, 2) AS horas_promedio_reales,
                   ROUND(avg(abs(d.horas - (t.suma - d.horas) / (t.n - 1)))::numeric, 2) AS error_absoluto_medio_horas
              FROM duracion d CROSS JOIN totales t
             WHERE t.n > 1
             GROUP BY t.n
            """;

    private final JdbcTemplate jdbcTemplate;

    public PrediccionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${app.predicciones.cron:0 0 6 * * *}", zone = "UTC")
    public void tareaDiaria() {
        log.info("Predicciones generadas: {}", generarDelDia());
    }

    public int generarDelDia() {
        int generadas = 0;
        for (UUID idEnvio : jdbcTemplate.queryForList(ENVIOS_SIN_PREDICCION_HOY, UUID.class)) {
            jdbcTemplate.queryForObject("SELECT tp_logistica.generar_prediccion_baseline(?)", UUID.class, idEnvio);
            generadas++;
        }
        return generadas;
    }

    public List<Map<String, Object>> vigentes(UUID idCliente) {
        List<Object> parametros = new ArrayList<>();
        String sql = ULTIMAS_PREDICCIONES;
        if (idCliente != null) {
            sql += " AND e.id_cliente = ?";
            parametros.add(idCliente);
        }
        return jdbcTemplate.queryForList(sql + " ORDER BY p.probabilidad_atraso DESC, e.id_envio", parametros.toArray());
    }

    public List<Map<String, Object>> validacion() {
        return jdbcTemplate.queryForList(VALIDACION);
    }
}
