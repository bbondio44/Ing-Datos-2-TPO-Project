package ar.edu.uade.logistica.report;

import ar.edu.uade.logistica.contracts.report.ClientDeliveryTimeResponse;
import ar.edu.uade.logistica.contracts.report.DeliveryAverageResponse;
import ar.edu.uade.logistica.contracts.report.IncidentAverageResponse;
import ar.edu.uade.logistica.contracts.report.ShipmentReportResponse;
import ar.edu.uade.logistica.contracts.report.TransportCostResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {
    private final JdbcTemplate jdbcTemplate;

    public ReportService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Transactional(readOnly = true)
    public List<ShipmentReportResponse> shipments(String country, String state, UUID clientId,
                                                   Boolean delayed, Boolean openIncidents,
                                                   AuthenticatedUser user) {
        UUID effectiveClient = clientFilter(clientId, user);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (country != null && !country.isBlank()) {
            where.append(" AND lower(pais_destino)=lower(?)");
            params.add(country.strip());
        }
        if (state != null && !state.isBlank()) {
            where.append(" AND estado=?");
            params.add(state.strip().toUpperCase());
        }
        if (effectiveClient != null) {
            where.append(" AND id_cliente=?");
            params.add(effectiveClient);
        }
        if (delayed != null) {
            where.append(" AND demorado=?");
            params.add(delayed);
        }
        if (openIncidents != null) {
            where.append(openIncidents ? " AND incidentes_abiertos>0" : " AND incidentes_abiertos=0");
        }
        return jdbcTemplate.query("SELECT * FROM tp_logistica.v_envios_reporte" + where
                + " ORDER BY fecha_creacion DESC", this::mapShipment, params.toArray());
    }

    @Transactional(readOnly = true)
    public List<ClientDeliveryTimeResponse> deliveryTimes(UUID clientId, AuthenticatedUser user) {
        UUID effectiveClient = clientFilter(clientId, user);
        String where = effectiveClient == null ? "" : " WHERE v.id_cliente=?";
        Object[] params = effectiveClient == null ? new Object[0] : new Object[]{effectiveClient};
        return jdbcTemplate.query("""
                SELECT v.id_cliente,v.razon_social,v.envios_entregados,v.min_horas,v.max_horas,v.promedio_horas
                  FROM tp_logistica.v_tiempos_entrega_cliente v
                """ + where + " ORDER BY v.razon_social", (rs, row) -> new ClientDeliveryTimeResponse(
                rs.getObject("id_cliente", UUID.class), rs.getString("razon_social"),
                rs.getLong("envios_entregados"), rs.getBigDecimal("min_horas"),
                rs.getBigDecimal("max_horas"), rs.getBigDecimal("promedio_horas")), params);
    }

    @Transactional(readOnly = true)
    public List<IncidentAverageResponse> incidentAverages(String country, String state, UUID clientId,
                                                           AuthenticatedUser user) {
        UUID effectiveClient = clientFilter(clientId, user);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (country != null && !country.isBlank()) {
            where.append(" AND lower(v.pais_destino)=lower(?)");
            params.add(country.strip());
        }
        if (state != null && !state.isBlank()) {
            where.append(" AND v.estado_envio=?");
            params.add(state.strip().toUpperCase());
        }
        if (effectiveClient != null) {
            where.append(" AND v.id_cliente=?");
            params.add(effectiveClient);
        }
        return jdbcTemplate.query("""
                SELECT v.pais_destino,v.estado_envio,v.id_cliente,c.razon_social,
                       v.cantidad_envios,v.cantidad_incidentes,v.promedio_incidentes_por_envio
                  FROM tp_logistica.v_promedio_incidentes_dimension v
                  JOIN tp_logistica.cliente c ON c.id_cliente=v.id_cliente
                """ + where + " ORDER BY v.pais_destino,v.estado_envio,c.razon_social",
                (rs, row) -> new IncidentAverageResponse(rs.getString("pais_destino"),
                        rs.getString("estado_envio"), rs.getObject("id_cliente", UUID.class),
                        rs.getString("razon_social"), rs.getLong("cantidad_envios"),
                        rs.getLong("cantidad_incidentes"), rs.getBigDecimal("promedio_incidentes_por_envio")),
                params.toArray());
    }

    @Transactional(readOnly = true)
    public DeliveryAverageResponse deliveryAverage(UUID clientId, AuthenticatedUser user) {
        UUID effectiveClient = clientFilter(clientId, user);
        if (effectiveClient == null) {
            return jdbcTemplate.queryForObject("SELECT * FROM tp_logistica.v_tiempo_promedio_entrega",
                    (rs, row) -> new DeliveryAverageResponse(rs.getLong("cantidad_entregados"),
                            rs.getBigDecimal("promedio_horas")));
        }
        return jdbcTemplate.queryForObject("""
                SELECT count(*) AS cantidad_entregados,
                       round(avg(extract(epoch FROM (fecha_entrega_real-fecha_creacion))/3600)::numeric,2)
                         AS promedio_horas
                  FROM tp_logistica.envio
                 WHERE estado='ENTREGADO' AND fecha_entrega_real IS NOT NULL AND id_cliente=?
                """, (rs, row) -> new DeliveryAverageResponse(rs.getLong("cantidad_entregados"),
                rs.getBigDecimal("promedio_horas")), effectiveClient);
    }

    @Transactional(readOnly = true)
    public List<TransportCostResponse> transportCosts(UUID clientId, AuthenticatedUser user) {
        UUID effectiveClient = clientFilter(clientId, user);
        if (effectiveClient == null) {
            return jdbcTemplate.query("SELECT * FROM tp_logistica.v_costos_por_transporte ORDER BY medio_transporte",
                    this::mapCost);
        }
        return jdbcTemplate.query("""
                SELECT t.medio_transporte,
                       count(*) FILTER(WHERE t.costo_real IS NOT NULL) AS tramos_con_costo_real,
                       coalesce(sum(t.costo_real),0)::numeric(18,2) AS costo_total_real,
                       round(avg(t.costo_real),2) AS costo_promedio_real
                  FROM tp_logistica.tramo t JOIN tp_logistica.envio e ON e.id_envio=t.id_envio
                 WHERE t.estado<>'CANCELADO' AND e.id_cliente=?
                 GROUP BY t.medio_transporte ORDER BY t.medio_transporte
                """, this::mapCost, effectiveClient);
    }

    private UUID clientFilter(UUID requested, AuthenticatedUser user) {
        if (user.permissions().contains("REPORTES_LEER_TODOS")) return requested;
        if (user.clientId() == null) throw new AccessDeniedException("El usuario no pertenece a un cliente");
        if (requested != null && !requested.equals(user.clientId())) {
            throw new AccessDeniedException("No puede consultar reportes de otro cliente");
        }
        return user.clientId();
    }

    private ShipmentReportResponse mapShipment(ResultSet rs, int row) throws SQLException {
        return new ShipmentReportResponse(rs.getObject("id_envio", UUID.class),
                rs.getObject("id_cliente", UUID.class), rs.getString("cliente"), rs.getString("origen"),
                rs.getString("destino"), rs.getString("pais_origen"), rs.getString("pais_destino"),
                rs.getString("estado"), rs.getString("prioridad"), rs.getBigDecimal("volumen_m3"),
                instant(rs, "fecha_creacion"), instant(rs, "fecha_entrega_estimada"),
                instant(rs, "fecha_entrega_real"), rs.getBoolean("demorado"),
                rs.getLong("incidentes_abiertos"), rs.getLong("incidentes_totales"));
    }

    private TransportCostResponse mapCost(ResultSet rs, int row) throws SQLException {
        return new TransportCostResponse(rs.getString("medio_transporte"),
                rs.getLong("tramos_con_costo_real"), rs.getBigDecimal("costo_total_real"),
                rs.getBigDecimal("costo_promedio_real"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
