package ar.edu.uade.logistica.analitica;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** R27, R28, R31: las vistas de PostgreSQL ya hacen el cálculo; acá se ordenan y se filtran. */
@Repository
public class AnaliticaRepository {
    private final JdbcTemplate jdbcTemplate;

    public AnaliticaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> rankingRutas() {
        return jdbcTemplate.queryForList("""
                SELECT origen, destino, medio_transporte, cantidad_tramos, cantidad_incidentes, incidentes_abiertos
                  FROM tp_logistica.v_ranking_rutas_incidentes
                 WHERE cantidad_incidentes > 0
                 ORDER BY cantidad_incidentes DESC, origen, destino
                """);
    }

    public List<Map<String, Object>> rankingClientes() {
        return jdbcTemplate.queryForList("""
                SELECT id_cliente, razon_social, cantidad_envios, volumen_total_m3
                  FROM tp_logistica.v_ranking_clientes_volumen
                 ORDER BY volumen_total_m3 DESC, razon_social
                """);
    }

    /** El promedio de horas se pondera por entregados: promediar promedios daría otro resultado. */
    public List<Map<String, Object>> desempeno(UUID idCliente, LocalDate desde, LocalDate hasta) {
        StringBuilder sql = new StringBuilder("""
                SELECT periodo, SUM(envios_creados) AS envios_creados, SUM(entregados) AS entregados,
                       SUM(entregados_fuera_plazo) AS entregados_fuera_plazo,
                       ROUND(SUM(horas_promedio_entrega * entregados) / NULLIF(SUM(entregados), 0), 2)
                         AS horas_promedio_entrega
                  FROM tp_logistica.v_desempeno_mensual WHERE true
                """);
        List<Object> parametros = new ArrayList<>();
        if (idCliente != null) {
            sql.append(" AND id_cliente = ?");
            parametros.add(idCliente);
        }
        if (desde != null) {
            sql.append(" AND periodo >= ?");
            parametros.add(Date.valueOf(desde.withDayOfMonth(1)));
        }
        if (hasta != null) {
            sql.append(" AND periodo <= ?");
            parametros.add(Date.valueOf(hasta));
        }
        sql.append(" GROUP BY periodo ORDER BY periodo");
        return jdbcTemplate.queryForList(sql.toString(), parametros.toArray());
    }
}
