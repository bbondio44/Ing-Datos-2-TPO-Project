package ar.edu.uade.logistica.master;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.master.LocationRequest;
import ar.edu.uade.logistica.contracts.master.LocationResponse;
import ar.edu.uade.logistica.contracts.master.VehicleRequest;
import ar.edu.uade.logistica.contracts.master.VehicleResponse;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class MasterDataService {
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseAuditContext auditContext;

    public MasterDataService(JdbcTemplate jdbcTemplate, DatabaseAuditContext auditContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditContext = auditContext;
    }

    @Transactional(readOnly = true)
    public PageResponse<LocationResponse> locations(String search, int page, int size) {
        PageSpec spec = pageSpec(page, size);
        String term = normalizeSearch(search);
        String condition = term.isBlank() ? "" : " WHERE lower(codigo) LIKE ? OR lower(nombre) LIKE ? OR lower(pais) LIKE ?";
        Object[] filter = term.isBlank() ? new Object[0]
                : new Object[]{"%" + term + "%", "%" + term + "%", "%" + term + "%"};
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tp_logistica.ubicacion" + condition, Long.class, filter);
        Object[] parameters = withPage(filter, spec);
        List<LocationResponse> content = jdbcTemplate.query("""
                SELECT id_ubicacion,codigo,nombre,pais,tipo
                  FROM tp_logistica.ubicacion
                """ + condition + " ORDER BY nombre LIMIT ? OFFSET ?", this::mapLocation, parameters);
        return page(content, spec, total);
    }

    @Transactional(readOnly = true)
    public LocationResponse location(UUID id) {
        List<LocationResponse> result = jdbcTemplate.query("""
                SELECT id_ubicacion,codigo,nombre,pais,tipo
                  FROM tp_logistica.ubicacion WHERE id_ubicacion=?
                """, this::mapLocation, id);
        if (result.isEmpty()) throw new NotFoundException("La ubicación no existe");
        return result.getFirst();
    }

    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        auditContext.applyToCurrentTransaction();
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.ubicacion(id_ubicacion,codigo,nombre,pais,tipo)
                VALUES (?,?,?,?,?)
                """, id, upper(request.code()), clean(request.name()), clean(request.country()), upper(request.type()));
        return location(id);
    }

    @Transactional
    public LocationResponse updateLocation(UUID id, LocationRequest request) {
        auditContext.applyToCurrentTransaction();
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.ubicacion SET codigo=?,nombre=?,pais=?,tipo=? WHERE id_ubicacion=?
                """, upper(request.code()), clean(request.name()), clean(request.country()), upper(request.type()), id);
        if (changed == 0) throw new NotFoundException("La ubicación no existe");
        return location(id);
    }

    @Transactional
    public void deleteLocation(UUID id) {
        auditContext.applyToCurrentTransaction();
        int changed = jdbcTemplate.update("DELETE FROM tp_logistica.ubicacion WHERE id_ubicacion=?", id);
        if (changed == 0) throw new NotFoundException("La ubicación no existe");
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> vehicles(String search, int page, int size) {
        PageSpec spec = pageSpec(page, size);
        String term = normalizeSearch(search);
        String condition = term.isBlank() ? "" : " WHERE lower(v.identificacion) LIKE ? OR lower(v.tipo) LIKE ? OR lower(o.razon_social) LIKE ?";
        Object[] filter = term.isBlank() ? new Object[0]
                : new Object[]{"%" + term + "%", "%" + term + "%", "%" + term + "%"};
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM tp_logistica.vehiculo v "
                + "JOIN tp_logistica.operador_logistico o ON o.id_operador=v.id_operador" + condition,
                Long.class, filter);
        Object[] parameters = withPage(filter, spec);
        List<VehicleResponse> content = jdbcTemplate.query("""
                SELECT v.id_vehiculo,v.id_operador,o.razon_social AS operador,
                       v.tipo,v.identificacion,v.estado
                  FROM tp_logistica.vehiculo v
                  JOIN tp_logistica.operador_logistico o ON o.id_operador=v.id_operador
                """ + condition + " ORDER BY v.identificacion LIMIT ? OFFSET ?", this::mapVehicle, parameters);
        return page(content, spec, total);
    }

    @Transactional(readOnly = true)
    public VehicleResponse vehicle(UUID id) {
        List<VehicleResponse> result = jdbcTemplate.query("""
                SELECT v.id_vehiculo,v.id_operador,o.razon_social AS operador,
                       v.tipo,v.identificacion,v.estado
                  FROM tp_logistica.vehiculo v
                  JOIN tp_logistica.operador_logistico o ON o.id_operador=v.id_operador
                 WHERE v.id_vehiculo=?
                """, this::mapVehicle, id);
        if (result.isEmpty()) throw new NotFoundException("El vehículo no existe");
        return result.getFirst();
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        auditContext.applyToCurrentTransaction();
        requireActiveOperator(request.operatorId());
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.vehiculo
                (id_vehiculo,id_operador,tipo,identificacion,estado) VALUES (?,?,?,?,?)
                """, id, request.operatorId(), upper(request.type()), upper(request.identification()), upper(request.state()));
        return vehicle(id);
    }

    @Transactional
    public VehicleResponse updateVehicle(UUID id, VehicleRequest request) {
        auditContext.applyToCurrentTransaction();
        requireActiveOperator(request.operatorId());
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.vehiculo
                   SET id_operador=?,tipo=?,identificacion=?,estado=? WHERE id_vehiculo=?
                """, request.operatorId(), upper(request.type()), upper(request.identification()), upper(request.state()), id);
        if (changed == 0) throw new NotFoundException("El vehículo no existe");
        return vehicle(id);
    }

    @Transactional
    public void deactivateVehicle(UUID id) {
        auditContext.applyToCurrentTransaction();
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.vehiculo SET estado='INACTIVO'
                 WHERE id_vehiculo=? AND estado <> 'INACTIVO'
                """, id);
        if (changed == 0) throw new NotFoundException("El vehículo activo no existe");
    }

    private void requireActiveOperator(UUID operatorId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM tp_logistica.operador_logistico
                               WHERE id_operador=? AND estado='ACTIVO')
                """, Boolean.class, operatorId);
        if (!Boolean.TRUE.equals(exists)) {
            throw new ConflictException("El operador debe existir y estar activo");
        }
    }

    private LocationResponse mapLocation(ResultSet rs, int row) throws SQLException {
        return new LocationResponse(rs.getObject("id_ubicacion", UUID.class), rs.getString("codigo"),
                rs.getString("nombre"), rs.getString("pais"), rs.getString("tipo"));
    }

    private VehicleResponse mapVehicle(ResultSet rs, int row) throws SQLException {
        return new VehicleResponse(rs.getObject("id_vehiculo", UUID.class),
                rs.getObject("id_operador", UUID.class), rs.getString("operador"), rs.getString("tipo"),
                rs.getString("identificacion"), rs.getString("estado"));
    }

    private static PageSpec pageSpec(int page, int size) {
        return new PageSpec(Math.max(0, page), Math.max(1, Math.min(size, 100)));
    }

    private static Object[] withPage(Object[] filter, PageSpec spec) {
        Object[] parameters = new Object[filter.length + 2];
        System.arraycopy(filter, 0, parameters, 0, filter.length);
        parameters[parameters.length - 2] = spec.size;
        parameters[parameters.length - 1] = spec.page * spec.size;
        return parameters;
    }

    private static <T> PageResponse<T> page(List<T> content, PageSpec spec, Long total) {
        long count = total == null ? 0 : total;
        int pages = count == 0 ? 0 : (int) Math.ceil((double) count / spec.size);
        return new PageResponse<>(content, spec.page, spec.size, count, pages);
    }

    private static String normalizeSearch(String value) {
        return value == null ? "" : value.strip().toLowerCase();
    }

    private static String clean(String value) {
        return value.strip();
    }

    private static String upper(String value) {
        return value.strip().toUpperCase();
    }

    private record PageSpec(int page, int size) {
    }
}
