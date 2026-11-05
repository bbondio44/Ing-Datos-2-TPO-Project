package ar.edu.uade.logistica.route;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.route.RouteLegRequest;
import ar.edu.uade.logistica.contracts.route.RouteLegResponse;
import ar.edu.uade.logistica.contracts.route.RouteSupplierRequest;
import ar.edu.uade.logistica.contracts.route.RouteSupplierResponse;
import ar.edu.uade.logistica.logistics.LogisticsService;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RouteService {
    private static final Set<String> STATES = Set.of("PENDIENTE", "EN_CURSO", "FINALIZADO", "CANCELADO");
    private static final Set<String> TRANSPORT_TYPES = Set.of("MARITIMO", "AEREO", "TERRESTRE");

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseAuditContext auditContext;
    private final LogisticsService logisticsService;

    public RouteService(JdbcTemplate jdbcTemplate, DatabaseAuditContext auditContext,
                        LogisticsService logisticsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditContext = auditContext;
        this.logisticsService = logisticsService;
    }

    @Transactional(readOnly = true)
    public List<RouteLegResponse> legs(UUID shipmentId, AuthenticatedUser user) {
        logisticsService.shipment(shipmentId, user);
        return jdbcTemplate.query("""
                SELECT t.id_tramo,t.id_envio,t.numero_tramo,t.origen,t.destino,
                       t.id_ubicacion_origen,t.id_ubicacion_destino,t.medio_transporte,
                       t.id_vehiculo,v.identificacion AS vehiculo,t.id_operador,o.razon_social AS operador,
                       t.fecha_salida,t.fecha_llegada_estimada,t.fecha_llegada_real,
                       t.costo_estimado,t.costo_real,t.estado
                  FROM tp_logistica.tramo t
                  LEFT JOIN tp_logistica.vehiculo v ON v.id_vehiculo=t.id_vehiculo
                  LEFT JOIN tp_logistica.operador_logistico o ON o.id_operador=t.id_operador
                 WHERE t.id_envio=? ORDER BY t.numero_tramo
                """, this::map, shipmentId);
    }

    @Transactional(readOnly = true)
    public RouteLegResponse leg(UUID id, AuthenticatedUser user) {
        List<RouteLegResponse> result = jdbcTemplate.query("""
                SELECT t.id_tramo,t.id_envio,t.numero_tramo,t.origen,t.destino,
                       t.id_ubicacion_origen,t.id_ubicacion_destino,t.medio_transporte,
                       t.id_vehiculo,v.identificacion AS vehiculo,t.id_operador,o.razon_social AS operador,
                       t.fecha_salida,t.fecha_llegada_estimada,t.fecha_llegada_real,
                       t.costo_estimado,t.costo_real,t.estado
                  FROM tp_logistica.tramo t
                  LEFT JOIN tp_logistica.vehiculo v ON v.id_vehiculo=t.id_vehiculo
                  LEFT JOIN tp_logistica.operador_logistico o ON o.id_operador=t.id_operador
                 WHERE t.id_tramo=?
                """, this::map, id);
        if (result.isEmpty()) throw new NotFoundException("El tramo no existe");
        logisticsService.shipment(result.getFirst().shipmentId(), user);
        return result.getFirst();
    }

    @Transactional
    public RouteLegResponse create(RouteLegRequest request, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        logisticsService.shipment(request.shipmentId(), user);
        validate(request);
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.tramo
                (id_tramo,id_envio,id_vehiculo,id_operador,numero_tramo,origen,destino,
                 id_ubicacion_origen,id_ubicacion_destino,medio_transporte,fecha_salida,
                 fecha_llegada_estimada,fecha_llegada_real,costo_estimado,costo_real,estado)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, request.shipmentId(), request.vehicleId(), request.operatorId(), request.order(),
                clean(request.origin()), clean(request.destination()), request.originLocationId(),
                request.destinationLocationId(), upper(request.transportType()), timestamp(request.departureAt()),
                timestamp(request.estimatedArrivalAt()), timestamp(request.actualArrivalAt()),
                request.estimatedCost(), request.actualCost(), upper(request.state()));
        replaceSuppliers(id, request.suppliers());
        return leg(id, user);
    }

    @Transactional
    public RouteLegResponse update(UUID id, RouteLegRequest request, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        RouteLegResponse existing = leg(id, user);
        if (!existing.shipmentId().equals(request.shipmentId())) {
            throw new ConflictException("No se puede mover un tramo a otro envío");
        }
        validate(request);
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.tramo SET id_vehiculo=?,id_operador=?,numero_tramo=?,origen=?,destino=?,
                       id_ubicacion_origen=?,id_ubicacion_destino=?,medio_transporte=?,fecha_salida=?,
                       fecha_llegada_estimada=?,fecha_llegada_real=?,costo_estimado=?,costo_real=?,estado=?
                 WHERE id_tramo=?
                """, request.vehicleId(), request.operatorId(), request.order(), clean(request.origin()),
                clean(request.destination()), request.originLocationId(), request.destinationLocationId(),
                upper(request.transportType()), timestamp(request.departureAt()),
                timestamp(request.estimatedArrivalAt()), timestamp(request.actualArrivalAt()),
                request.estimatedCost(), request.actualCost(), upper(request.state()), id);
        if (changed == 0) throw new NotFoundException("El tramo no existe");
        replaceSuppliers(id, request.suppliers());
        return leg(id, user);
    }

    @Transactional
    public void delete(UUID id, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        RouteLegResponse leg = leg(id, user);
        if (!Set.of("PENDIENTE", "CANCELADO").contains(leg.state())) {
            throw new ConflictException("Solo se pueden eliminar tramos pendientes o cancelados");
        }
        jdbcTemplate.update("DELETE FROM tp_logistica.tramo_proveedor WHERE id_tramo=?", id);
        jdbcTemplate.update("DELETE FROM tp_logistica.tramo WHERE id_tramo=?", id);
    }

    private void validate(RouteLegRequest request) {
        String state = upper(request.state());
        String transport = upper(request.transportType());
        if (!STATES.contains(state)) throw new ConflictException("Estado de tramo inválido");
        if (!TRANSPORT_TYPES.contains(transport)) throw new ConflictException("Medio de transporte inválido");
        if ("FINALIZADO".equals(state) && request.actualArrivalAt() == null) {
            throw new ConflictException("Un tramo finalizado requiere fecha de llegada real");
        }
        if (request.departureAt() != null && request.estimatedArrivalAt() != null
                && request.estimatedArrivalAt().isBefore(request.departureAt())) {
            throw new ConflictException("La llegada estimada no puede ser anterior a la salida");
        }
        if (request.departureAt() != null && request.actualArrivalAt() != null
                && request.actualArrivalAt().isBefore(request.departureAt())) {
            throw new ConflictException("La llegada real no puede ser anterior a la salida");
        }
        if (request.vehicleId() != null) {
            if (request.operatorId() == null) throw new ConflictException("El vehículo requiere un operador");
            Boolean matches = jdbcTemplate.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM tp_logistica.vehiculo
                                   WHERE id_vehiculo=? AND id_operador=? AND estado='ACTIVO')
                    """, Boolean.class, request.vehicleId(), request.operatorId());
            if (!Boolean.TRUE.equals(matches)) {
                throw new ConflictException("El vehículo no pertenece al operador o no está activo");
            }
        }
    }

    private void replaceSuppliers(UUID legId, List<RouteSupplierRequest> suppliers) {
        jdbcTemplate.update("DELETE FROM tp_logistica.tramo_proveedor WHERE id_tramo=?", legId);
        if (suppliers == null) return;
        for (RouteSupplierRequest supplier : suppliers) {
            Boolean active = jdbcTemplate.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM tp_logistica.proveedor
                                   WHERE id_proveedor=? AND estado='ACTIVO')
                    """, Boolean.class, supplier.supplierId());
            if (!Boolean.TRUE.equals(active)) throw new ConflictException("El proveedor debe estar activo");
            jdbcTemplate.update("""
                    INSERT INTO tp_logistica.tramo_proveedor
                    (id_tramo,id_proveedor,servicio_prestado,costo_servicio) VALUES (?,?,?,?)
                    """, legId, supplier.supplierId(), clean(supplier.service()), supplier.cost());
        }
    }

    private RouteLegResponse map(ResultSet rs, int row) throws SQLException {
        UUID id = rs.getObject("id_tramo", UUID.class);
        return new RouteLegResponse(id, rs.getObject("id_envio", UUID.class), rs.getInt("numero_tramo"),
                rs.getString("origen"), rs.getString("destino"),
                rs.getObject("id_ubicacion_origen", UUID.class), rs.getObject("id_ubicacion_destino", UUID.class),
                rs.getString("medio_transporte"), rs.getObject("id_vehiculo", UUID.class), rs.getString("vehiculo"),
                rs.getObject("id_operador", UUID.class), rs.getString("operador"), instant(rs, "fecha_salida"),
                instant(rs, "fecha_llegada_estimada"), instant(rs, "fecha_llegada_real"),
                rs.getBigDecimal("costo_estimado"), rs.getBigDecimal("costo_real"), rs.getString("estado"),
                suppliers(id));
    }

    private List<RouteSupplierResponse> suppliers(UUID legId) {
        return jdbcTemplate.query("""
                SELECT tp.id_proveedor,p.razon_social,tp.servicio_prestado,tp.costo_servicio
                  FROM tp_logistica.tramo_proveedor tp
                  JOIN tp_logistica.proveedor p ON p.id_proveedor=tp.id_proveedor
                 WHERE tp.id_tramo=? ORDER BY p.razon_social,tp.servicio_prestado
                """, (rs, row) -> new RouteSupplierResponse(rs.getObject("id_proveedor", UUID.class),
                rs.getString("razon_social"), rs.getString("servicio_prestado"),
                rs.getBigDecimal("costo_servicio")), legId);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static String clean(String value) { return value.strip(); }
    private static String upper(String value) { return value.strip().toUpperCase(); }
}
