package ar.edu.uade.logistica.organization;

import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.organization.OrganizationRequest;
import ar.edu.uade.logistica.contracts.organization.OrganizationResponse;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseAuditContext auditContext;

    public OrganizationService(JdbcTemplate jdbcTemplate, DatabaseAuditContext auditContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditContext = auditContext;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> list(OrganizationKind kind, String search, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        String term = search == null ? "" : search.strip().toLowerCase();
        String condition = term.isBlank() ? "" : " WHERE lower(razon_social) LIKE ? OR cuit LIKE ?";
        Object[] filter = term.isBlank() ? new Object[0] : new Object[]{"%" + term + "%", "%" + term + "%"};

        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tp_logistica." + kind.table + condition, Long.class, filter);
        String serviceColumn = kind.serviceType ? "tipo_servicio" : "NULL::varchar AS tipo_servicio";
        String sql = "SELECT " + kind.idColumn + ", razon_social, cuit, email, telefono, direccion, pais, "
                + serviceColumn + ", estado FROM tp_logistica." + kind.table + condition
                + " ORDER BY razon_social LIMIT ? OFFSET ?";
        Object[] parameters = new Object[filter.length + 2];
        System.arraycopy(filter, 0, parameters, 0, filter.length);
        parameters[parameters.length - 2] = safeSize;
        parameters[parameters.length - 1] = safePage * safeSize;
        List<OrganizationResponse> content = jdbcTemplate.query(sql, this::map, parameters);
        long count = total == null ? 0 : total;
        int totalPages = count == 0 ? 0 : (int) Math.ceil((double) count / safeSize);
        return new PageResponse<>(content, safePage, safeSize, count, totalPages);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(OrganizationKind kind, UUID id) {
        String serviceColumn = kind.serviceType ? "tipo_servicio" : "NULL::varchar AS tipo_servicio";
        List<OrganizationResponse> result = jdbcTemplate.query(
                "SELECT " + kind.idColumn + ", razon_social, cuit, email, telefono, direccion, pais, "
                        + serviceColumn + ", estado FROM tp_logistica." + kind.table
                        + " WHERE " + kind.idColumn + " = ?", this::map, id);
        if (result.isEmpty()) throw new NotFoundException(label(kind) + " no existe");
        return result.getFirst();
    }

    @Transactional
    public OrganizationResponse create(OrganizationKind kind, OrganizationRequest request) {
        auditContext.applyToCurrentTransaction();
        UUID id = UUID.randomUUID();
        if (kind.serviceType) {
            jdbcTemplate.update("""
                    INSERT INTO tp_logistica.proveedor
                    (id_proveedor, razon_social, cuit, email, telefono, direccion, pais, tipo_servicio, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVO')
                    """, id, cleanRequired(request.businessName()), nullable(request.cuit()), nullable(request.email()),
                    nullable(request.phone()), nullable(request.address()), cleanRequired(request.country()),
                    nullable(request.serviceType()));
        } else {
            jdbcTemplate.update("""
                    INSERT INTO tp_logistica.operador_logistico
                    (id_operador, razon_social, cuit, email, telefono, direccion, pais, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVO')
                    """, id, cleanRequired(request.businessName()), nullable(request.cuit()), nullable(request.email()),
                    nullable(request.phone()), nullable(request.address()), cleanRequired(request.country()));
        }
        return get(kind, id);
    }

    @Transactional
    public OrganizationResponse update(OrganizationKind kind, UUID id, OrganizationRequest request) {
        auditContext.applyToCurrentTransaction();
        int changed;
        if (kind.serviceType) {
            changed = jdbcTemplate.update("""
                    UPDATE tp_logistica.proveedor
                       SET razon_social=?, cuit=?, email=?, telefono=?, direccion=?, pais=?, tipo_servicio=?
                     WHERE id_proveedor=?
                    """, cleanRequired(request.businessName()), nullable(request.cuit()), nullable(request.email()),
                    nullable(request.phone()), nullable(request.address()), cleanRequired(request.country()),
                    nullable(request.serviceType()), id);
        } else {
            changed = jdbcTemplate.update("""
                    UPDATE tp_logistica.operador_logistico
                       SET razon_social=?, cuit=?, email=?, telefono=?, direccion=?, pais=?
                     WHERE id_operador=?
                    """, cleanRequired(request.businessName()), nullable(request.cuit()), nullable(request.email()),
                    nullable(request.phone()), nullable(request.address()), cleanRequired(request.country()), id);
        }
        if (changed == 0) throw new NotFoundException(label(kind) + " no existe");
        return get(kind, id);
    }

    @Transactional
    public void deactivate(OrganizationKind kind, UUID id) {
        auditContext.applyToCurrentTransaction();
        int changed = jdbcTemplate.update("UPDATE tp_logistica." + kind.table
                + " SET estado='INACTIVO' WHERE " + kind.idColumn + "=? AND estado <> 'INACTIVO'", id);
        if (changed == 0) throw new NotFoundException(label(kind) + " activo no existe");
    }

    private OrganizationResponse map(ResultSet rs, int row) throws SQLException {
        return new OrganizationResponse(rs.getObject(1, UUID.class), rs.getString("razon_social"),
                rs.getString("cuit"), rs.getString("email"), rs.getString("telefono"),
                rs.getString("direccion"), rs.getString("pais"), rs.getString("tipo_servicio"),
                rs.getString("estado"));
    }

    private static String nullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String cleanRequired(String value) {
        return value.strip();
    }

    private static String label(OrganizationKind kind) {
        return kind == OrganizationKind.SUPPLIER ? "El proveedor" : "El operador";
    }
}
