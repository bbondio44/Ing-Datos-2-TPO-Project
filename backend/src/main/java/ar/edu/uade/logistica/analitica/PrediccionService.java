package ar.edu.uade.logistica.analitica;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
                   (p.variables->>'riesgo_operativo')::numeric AS riesgo_operativo,
                   (p.variables->>'riesgo_climatico')::numeric AS riesgo_climatico,
                   p.variables::text AS factores, p.fecha_prediccion
              FROM tp_logistica.prediccion_envio p
              JOIN tp_logistica.envio e ON e.id_envio = p.id_envio
              JOIN tp_logistica.cliente c ON c.id_cliente = e.id_cliente
             WHERE e.estado IN ('EN_TRANSITO', 'DEMORADO')
               AND p.fecha_prediccion = (SELECT max(u.fecha_prediccion) FROM tp_logistica.prediccion_envio u
                                          WHERE u.id_envio = p.id_envio)
            """;
    private static final String SIN_RIESGOS = """
            SELECT p.id_prediccion, e.id_envio, e.pais_origen, e.pais_destino
              FROM tp_logistica.prediccion_envio p
              JOIN tp_logistica.envio e ON e.id_envio = p.id_envio
             WHERE e.estado IN ('EN_TRANSITO', 'DEMORADO') AND p.variables->>'riesgo_operativo' IS NULL
            """;
    /** Incidentes por envío en la misma ruta de países, sin contar el envío evaluado. */
    private static final String TASA_RUTA = """
            SELECT COALESCE(COUNT(i.id_incidente)::numeric / NULLIF(COUNT(DISTINCT e.id_envio), 0), 0)
              FROM tp_logistica.envio e
              LEFT JOIN tp_logistica.incidente i ON i.id_envio = e.id_envio
             WHERE e.pais_origen = ? AND e.pais_destino = ? AND e.id_envio <> ? AND e.estado <> 'CANCELADO'
            """;
    /** El tramo que importa es el que está en curso; si no hay, el próximo; si no, el último. */
    private static final String MEDIO_ACTUAL = """
            SELECT medio_transporte FROM tp_logistica.tramo WHERE id_envio = ?
             ORDER BY CASE estado WHEN 'EN_CURSO' THEN 0 WHEN 'PENDIENTE' THEN 1 ELSE 2 END, numero_tramo
             LIMIT 1
            """;
    private static final String CRITERIO_RIESGOS = "Operativo: incidentes abiertos e historial de la ruta. "
            + "Climatico: estimacion por medio de transporte, sin datos meteorologicos.";

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
        completarRiesgos();
        return generadas;
    }

    /** Suma los riesgos a los factores de la predicción: jsonb admite campos nuevos sin cambiar la tabla. */
    private void completarRiesgos() {
        for (Map<String, Object> fila : jdbcTemplate.queryForList(SIN_RIESGOS)) {
            UUID idEnvio = (UUID) fila.get("id_envio");
            Long abiertos = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM tp_logistica.incidente WHERE id_envio = ? AND estado <> 'CERRADO'
                    """, Long.class, idEnvio);
            BigDecimal tasa = jdbcTemplate.queryForObject(TASA_RUTA, BigDecimal.class,
                    fila.get("pais_origen"), fila.get("pais_destino"), idEnvio);
            List<String> medios = jdbcTemplate.queryForList(MEDIO_ACTUAL, String.class, idEnvio);
            String medio = medios.isEmpty() ? null : medios.get(0);
            String factores = String.format(Locale.ROOT,
                    "{\"riesgo_operativo\":%.2f,\"riesgo_climatico\":%.2f,\"incidentes_abiertos\":%d,"
                            + "\"medio_evaluado\":\"%s\",\"criterio_riesgos\":\"%s\"}",
                    riesgoOperativo(abiertos == null ? 0 : abiertos, tasa == null ? 0 : tasa.doubleValue()),
                    riesgoClimatico(medio), abiertos == null ? 0 : abiertos, medio == null ? "SIN_TRAMO" : medio,
                    CRITERIO_RIESGOS);
            jdbcTemplate.update("""
                    UPDATE tp_logistica.prediccion_envio SET variables = variables || ?::jsonb WHERE id_prediccion = ?
                    """, factores, fila.get("id_prediccion"));
        }
    }

    /** 60 % incidentes abiertos del envío (tope 3) y 40 % incidentes por envío de la ruta (tope 1). */
    static double riesgoOperativo(long abiertos, double tasaRuta) {
        double valor = 0.6 * Math.min(abiertos, 3) / 3.0 + 0.4 * Math.min(tasaRuta, 1.0);
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** No hay datos de clima: es una estimación fija por exposición del medio de transporte. */
    static double riesgoClimatico(String medio) {
        if (medio == null) return 0.25;
        return switch (medio) {
            case "MARITIMO" -> 0.50;
            case "AEREO" -> 0.35;
            case "TERRESTRE" -> 0.20;
            default -> 0.25;
        };
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
