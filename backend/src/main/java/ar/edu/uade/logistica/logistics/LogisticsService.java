package ar.edu.uade.logistica.logistics;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.logistics.ContainerAssignmentRequest;
import ar.edu.uade.logistica.contracts.logistics.ContainerAssignmentResponse;
import ar.edu.uade.logistica.contracts.logistics.ContainerHistoryResponse;
import ar.edu.uade.logistica.contracts.logistics.ContainerRequest;
import ar.edu.uade.logistica.contracts.logistics.ContainerResponse;
import ar.edu.uade.logistica.contracts.logistics.SensorAssignmentRequest;
import ar.edu.uade.logistica.contracts.logistics.SensorAssignmentResponse;
import ar.edu.uade.logistica.contracts.logistics.SensorRequest;
import ar.edu.uade.logistica.contracts.logistics.SensorResponse;
import ar.edu.uade.logistica.contracts.logistics.ShipmentDetailResponse;
import ar.edu.uade.logistica.contracts.logistics.ShipmentRequest;
import ar.edu.uade.logistica.contracts.logistics.ShipmentResponse;
import ar.edu.uade.logistica.contracts.logistics.ShipmentStateRequest;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
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
public class LogisticsService {
    private static final String SHIPMENT_SELECT = """
            SELECT e.id_envio,e.id_cliente,c.razon_social AS cliente,e.origen,e.destino,
                   e.pais_origen,e.pais_destino,e.fecha_creacion,e.fecha_entrega_estimada,
                   e.fecha_entrega_real,e.estado,e.prioridad,e.volumen_m3
              FROM tp_logistica.envio e JOIN tp_logistica.cliente c ON c.id_cliente=e.id_cliente
            """;
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseAuditContext auditContext;

    public LogisticsService(JdbcTemplate jdbcTemplate, DatabaseAuditContext auditContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditContext = auditContext;
    }

    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> shipments(String search, int page, int size, AuthenticatedUser user) {
        PageSpec spec = pageSpec(page, size);
        String term = normalizeSearch(search);
        List<Object> filter = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (!user.permissions().contains("ENVIOS_LEER_TODOS")) {
            if (user.clientId() == null) throw new AccessDeniedException("No tiene envíos asociados");
            where.append(" AND e.id_cliente=?");
            filter.add(user.clientId());
        }
        if (!term.isBlank()) {
            where.append(" AND (lower(e.origen) LIKE ? OR lower(e.destino) LIKE ? OR lower(c.razon_social) LIKE ? OR e.id_envio::text LIKE ?)");
            for (int index = 0; index < 4; index++) filter.add("%" + term + "%");
        }
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM tp_logistica.envio e "
                + "JOIN tp_logistica.cliente c ON c.id_cliente=e.id_cliente" + where, Long.class, filter.toArray());
        filter.add(spec.size);
        filter.add(spec.page * spec.size);
        List<ShipmentResponse> content = jdbcTemplate.query(SHIPMENT_SELECT + where
                + " ORDER BY e.fecha_creacion DESC LIMIT ? OFFSET ?", this::mapShipment, filter.toArray());
        return page(content, spec, total);
    }

    @Transactional(readOnly = true)
    public ShipmentDetailResponse shipmentDetail(UUID id, AuthenticatedUser user) {
        ShipmentResponse shipment = shipment(id, user);
        List<ContainerAssignmentResponse> containers = containerAssignments(id, user);
        List<SensorAssignmentResponse> sensors = jdbcTemplate.query("""
                SELECT DISTINCT a.id_asignacion,a.id_sensor,s.tipo AS sensor_tipo,
                       a.id_contenedor,c.codigo_internacional,a.fecha_inicio,a.fecha_fin
                  FROM tp_logistica.envio_contenedor ec
                  JOIN tp_logistica.asignacion_sensor_contenedor a ON a.id_contenedor=ec.id_contenedor
                   AND a.fecha_inicio < COALESCE(ec.fecha_desasociacion,'infinity'::timestamptz)
                   AND COALESCE(a.fecha_fin,'infinity'::timestamptz) > ec.fecha_asociacion
                  JOIN tp_logistica.sensor_iot s ON s.id_sensor=a.id_sensor
                  JOIN tp_logistica.contenedor c ON c.id_contenedor=a.id_contenedor
                 WHERE ec.id_envio=? ORDER BY a.fecha_inicio DESC
                """, this::mapSensorAssignment, id);
        return new ShipmentDetailResponse(shipment, containers, sensors);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse shipment(UUID id, AuthenticatedUser user) {
        List<ShipmentResponse> result = jdbcTemplate.query(SHIPMENT_SELECT + " WHERE e.id_envio=?",
                this::mapShipment, id);
        if (result.isEmpty()) throw new NotFoundException("El envío no existe");
        ShipmentResponse shipment = result.getFirst();
        authorizeShipment(shipment.clientId(), user);
        return shipment;
    }

    @Transactional
    public ShipmentResponse createShipment(ShipmentRequest request) {
        auditContext.applyToCurrentTransaction();
        requireActiveClient(request.clientId());
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.envio
                (id_envio,id_cliente,origen,destino,pais_origen,pais_destino,fecha_entrega_estimada,
                 estado,prioridad,volumen_m3)
                VALUES (?,?,?,?,?,?,?,'CREADO',?,?)
                """, id, request.clientId(), clean(request.origin()), clean(request.destination()),
                clean(request.originCountry()), clean(request.destinationCountry()), timestamp(request.estimatedDelivery()),
                upper(request.priority()), request.volumeM3());
        return shipmentForAdmin(id);
    }

    @Transactional
    public ShipmentResponse updateShipment(UUID id, ShipmentRequest request) {
        auditContext.applyToCurrentTransaction();
        requireActiveClient(request.clientId());
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.envio
                   SET id_cliente=?,origen=?,destino=?,pais_origen=?,pais_destino=?,
                       fecha_entrega_estimada=?,prioridad=?,volumen_m3=?
                 WHERE id_envio=? AND estado IN ('CREADO','EN_PREPARACION')
                """, request.clientId(), clean(request.origin()), clean(request.destination()),
                clean(request.originCountry()), clean(request.destinationCountry()), timestamp(request.estimatedDelivery()),
                upper(request.priority()), request.volumeM3(), id);
        if (changed == 0) throw new ConflictException("El envío no existe o su estado ya no permite editar datos generales");
        return shipmentForAdmin(id);
    }

    @Transactional
    public ShipmentResponse changeShipmentState(UUID id, ShipmentStateRequest request) {
        auditContext.applyToCurrentTransaction();
        String state = upper(request.state());
        if ("ENTREGADO".equals(state) && request.actualDelivery() == null) {
            throw new ConflictException("Un envío entregado requiere fecha de entrega real");
        }
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.envio SET estado=?,fecha_entrega_real=? WHERE id_envio=?
                """, state, "ENTREGADO".equals(state) ? timestamp(request.actualDelivery()) : null, id);
        if (changed == 0) throw new NotFoundException("El envío no existe");
        return shipmentForAdmin(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContainerResponse> containers(String search, int page, int size) {
        PageSpec spec = pageSpec(page, size);
        String term = normalizeSearch(search);
        String condition = term.isBlank() ? "" : " WHERE lower(codigo_internacional) LIKE ? OR lower(tipo) LIKE ?";
        Object[] filter = term.isBlank() ? new Object[0] : new Object[]{"%" + term + "%", "%" + term + "%"};
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM tp_logistica.contenedor" + condition,
                Long.class, filter);
        Object[] params = withPage(filter, spec);
        List<ContainerResponse> content = jdbcTemplate.query("""
                SELECT id_contenedor,codigo_internacional,tipo,capacidad,estado
                  FROM tp_logistica.contenedor
                """ + condition + " ORDER BY codigo_internacional LIMIT ? OFFSET ?", this::mapContainer, params);
        return page(content, spec, total);
    }

    @Transactional
    public ContainerResponse createContainer(ContainerRequest request) {
        auditContext.applyToCurrentTransaction();
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.contenedor(id_contenedor,codigo_internacional,tipo,capacidad,estado)
                VALUES (?,?,?,?,?)
                """, id, upper(request.internationalCode()), clean(request.type()), request.capacity(), upper(request.state()));
        return container(id);
    }

    @Transactional
    public ContainerResponse updateContainer(UUID id, ContainerRequest request) {
        auditContext.applyToCurrentTransaction();
        if ("INACTIVO".equals(upper(request.state())) && hasOpenContainerAssignment(id)) {
            throw new ConflictException("No se puede inactivar un contenedor asignado a un envío");
        }
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.contenedor SET codigo_internacional=?,tipo=?,capacidad=?,estado=?
                 WHERE id_contenedor=?
                """, upper(request.internationalCode()), clean(request.type()), request.capacity(), upper(request.state()), id);
        if (changed == 0) throw new NotFoundException("El contenedor no existe");
        return container(id);
    }

    @Transactional(readOnly = true)
    public ContainerResponse container(UUID id) {
        List<ContainerResponse> result = jdbcTemplate.query("""
                SELECT id_contenedor,codigo_internacional,tipo,capacidad,estado
                  FROM tp_logistica.contenedor WHERE id_contenedor=?
                """, this::mapContainer, id);
        if (result.isEmpty()) throw new NotFoundException("El contenedor no existe");
        return result.getFirst();
    }

    @Transactional(readOnly = true)
    public PageResponse<SensorResponse> sensors(String search, int page, int size) {
        PageSpec spec = pageSpec(page, size);
        String term = normalizeSearch(search);
        String condition = term.isBlank() ? "" : " WHERE lower(tipo) LIKE ? OR lower(COALESCE(fabricante,'')) LIKE ? OR id_sensor::text LIKE ?";
        Object[] filter = term.isBlank() ? new Object[0]
                : new Object[]{"%" + term + "%", "%" + term + "%", "%" + term + "%"};
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM tp_logistica.sensor_iot" + condition,
                Long.class, filter);
        Object[] params = withPage(filter, spec);
        List<SensorResponse> content = jdbcTemplate.query("""
                SELECT id_sensor,tipo,fabricante,fecha_instalacion,estado FROM tp_logistica.sensor_iot
                """ + condition + " ORDER BY fecha_instalacion DESC NULLS LAST LIMIT ? OFFSET ?",
                this::mapSensor, params);
        return page(content, spec, total);
    }

    /** Sensores activos sin una asignación vigente: los únicos que se pueden asociar a un contenedor. */
    @Transactional(readOnly = true)
    public List<SensorResponse> freeSensors() {
        return jdbcTemplate.query("""
                SELECT s.id_sensor,s.tipo,s.fabricante,s.fecha_instalacion,s.estado FROM tp_logistica.sensor_iot s
                 WHERE s.estado='ACTIVO' AND NOT EXISTS (
                       SELECT 1 FROM tp_logistica.asignacion_sensor_contenedor a
                        WHERE a.id_sensor=s.id_sensor AND (a.fecha_fin IS NULL OR a.fecha_fin > now()))
                 ORDER BY s.fecha_instalacion DESC NULLS LAST
                """, this::mapSensor);
    }

    @Transactional
    public SensorResponse createSensor(SensorRequest request) {
        auditContext.applyToCurrentTransaction();
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.sensor_iot(id_sensor,tipo,fabricante,fecha_instalacion,estado)
                VALUES (?,?,?,?,?)
                """, id, upper(request.type()), nullable(request.manufacturer()), timestamp(request.installationDate()),
                upper(request.state()));
        return sensor(id);
    }

    @Transactional
    public SensorResponse updateSensor(UUID id, SensorRequest request) {
        auditContext.applyToCurrentTransaction();
        if ("INACTIVO".equals(upper(request.state())) && hasOpenSensorAssignment(id)) {
            throw new ConflictException("No se puede inactivar un sensor con asignación vigente");
        }
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.sensor_iot SET tipo=?,fabricante=?,fecha_instalacion=?,estado=?
                 WHERE id_sensor=?
                """, upper(request.type()), nullable(request.manufacturer()), timestamp(request.installationDate()),
                upper(request.state()), id);
        if (changed == 0) throw new NotFoundException("El sensor no existe");
        return sensor(id);
    }

    @Transactional(readOnly = true)
    public SensorResponse sensor(UUID id) {
        List<SensorResponse> result = jdbcTemplate.query("""
                SELECT id_sensor,tipo,fabricante,fecha_instalacion,estado
                  FROM tp_logistica.sensor_iot WHERE id_sensor=?
                """, this::mapSensor, id);
        if (result.isEmpty()) throw new NotFoundException("El sensor no existe");
        return result.getFirst();
    }

    @Transactional
    public ContainerAssignmentResponse assignContainer(UUID shipmentId, ContainerAssignmentRequest request,
                                                       AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        ShipmentResponse shipment = shipment(shipmentId, user);
        if (List.of("ENTREGADO", "CANCELADO").contains(shipment.state())) {
            throw new ConflictException("El estado del envío no permite asignar contenedores");
        }
        ContainerResponse container = container(request.containerId());
        if (List.of("INACTIVO", "MANTENIMIENTO").contains(container.state())) {
            throw new ConflictException("El contenedor no está disponible para asignación");
        }
        Instant start = request.startAt() == null ? Instant.now() : request.startAt();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.envio_contenedor
                (id_envio,id_contenedor,fecha_asociacion,fecha_desasociacion) VALUES (?,?,?,?)
                """, shipmentId, request.containerId(), timestamp(start), timestamp(request.endAt()));
        if (request.endAt() == null) {
            jdbcTemplate.update("UPDATE tp_logistica.contenedor SET estado='EN_USO' WHERE id_contenedor=?",
                    request.containerId());
        }
        return containerAssignment(shipmentId, request.containerId(), start);
    }

    @Transactional
    public void closeContainerAssignment(UUID shipmentId, UUID containerId, Instant startAt, Instant endAt,
                                         AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        shipment(shipmentId, user);
        Instant end = endAt == null ? Instant.now() : endAt;
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.envio_contenedor SET fecha_desasociacion=?
                 WHERE id_envio=? AND id_contenedor=? AND fecha_asociacion=? AND fecha_desasociacion IS NULL
                """, timestamp(end), shipmentId, containerId, timestamp(startAt));
        if (changed == 0) throw new NotFoundException("La asignación activa no existe");
        if (!hasOpenContainerAssignment(containerId)) {
            jdbcTemplate.update("UPDATE tp_logistica.contenedor SET estado='DISPONIBLE' WHERE id_contenedor=?",
                    containerId);
        }
    }

    /** Consulta avanzada del enunciado: por qué envíos pasó un contenedor. Un cliente ve solo los suyos. */
    @Transactional(readOnly = true)
    public List<ContainerHistoryResponse> containerHistory(UUID containerId, AuthenticatedUser user) {
        boolean all = user.permissions().contains("ENVIOS_LEER_TODOS");
        if (!all && user.clientId() == null) throw new AccessDeniedException("No tiene envíos asociados");
        String sql = """
                SELECT ec.id_envio,c.razon_social,e.origen,e.destino,e.estado,
                       ec.fecha_asociacion,ec.fecha_desasociacion
                  FROM tp_logistica.envio_contenedor ec
                  JOIN tp_logistica.envio e ON e.id_envio=ec.id_envio
                  JOIN tp_logistica.cliente c ON c.id_cliente=e.id_cliente
                 WHERE ec.id_contenedor=?
                """ + (all ? "" : " AND e.id_cliente=?") + " ORDER BY ec.fecha_asociacion DESC";
        Object[] params = all ? new Object[]{containerId} : new Object[]{containerId, user.clientId()};
        return jdbcTemplate.query(sql, (rs, row) -> {
            Instant end = instant(rs, "fecha_desasociacion");
            return new ContainerHistoryResponse(rs.getObject("id_envio", UUID.class), rs.getString("razon_social"),
                    rs.getString("origen"), rs.getString("destino"), rs.getString("estado"),
                    instant(rs, "fecha_asociacion"), end, end == null);
        }, params);
    }

    @Transactional(readOnly = true)
    public List<ContainerAssignmentResponse> containerAssignments(UUID shipmentId, AuthenticatedUser user) {
        shipment(shipmentId, user);
        return jdbcTemplate.query("""
                SELECT ec.id_envio,ec.id_contenedor,c.codigo_internacional,c.tipo,
                       ec.fecha_asociacion,ec.fecha_desasociacion
                  FROM tp_logistica.envio_contenedor ec
                  JOIN tp_logistica.contenedor c ON c.id_contenedor=ec.id_contenedor
                 WHERE ec.id_envio=? ORDER BY ec.fecha_asociacion DESC
                """, this::mapContainerAssignment, shipmentId);
    }

    @Transactional
    public SensorAssignmentResponse assignSensor(SensorAssignmentRequest request) {
        auditContext.applyToCurrentTransaction();
        SensorResponse sensor = sensor(request.sensorId());
        if (!"ACTIVO".equals(sensor.state())) throw new ConflictException("El sensor debe estar activo");
        container(request.containerId());
        UUID assignmentId = UUID.randomUUID();
        Instant start = request.startAt() == null ? Instant.now() : request.startAt();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.asignacion_sensor_contenedor
                (id_asignacion,id_sensor,id_contenedor,fecha_inicio,fecha_fin) VALUES (?,?,?,?,?)
                """, assignmentId, request.sensorId(), request.containerId(), timestamp(start), timestamp(request.endAt()));
        return sensorAssignment(assignmentId);
    }

    @Transactional
    public void closeSensorAssignment(UUID assignmentId, Instant endAt) {
        auditContext.applyToCurrentTransaction();
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.asignacion_sensor_contenedor SET fecha_fin=?
                 WHERE id_asignacion=? AND fecha_fin IS NULL
                """, timestamp(endAt == null ? Instant.now() : endAt), assignmentId);
        if (changed == 0) throw new NotFoundException("La asignación activa del sensor no existe");
    }

    @Transactional(readOnly = true)
    public List<SensorAssignmentResponse> sensorsForContainerAt(UUID containerId, Instant at) {
        container(containerId);
        Instant instant = at == null ? Instant.now() : at;
        return jdbcTemplate.query("""
                SELECT a.id_asignacion,a.id_sensor,s.tipo AS sensor_tipo,a.id_contenedor,
                       c.codigo_internacional,a.fecha_inicio,a.fecha_fin
                  FROM tp_logistica.asignacion_sensor_contenedor a
                  JOIN tp_logistica.sensor_iot s ON s.id_sensor=a.id_sensor
                  JOIN tp_logistica.contenedor c ON c.id_contenedor=a.id_contenedor
                 WHERE a.id_contenedor=? AND a.fecha_inicio<=?
                   AND (a.fecha_fin IS NULL OR a.fecha_fin>?)
                 ORDER BY a.fecha_inicio DESC
                """, this::mapSensorAssignment, containerId, timestamp(instant), timestamp(instant));
    }

    private ShipmentResponse shipmentForAdmin(UUID id) {
        List<ShipmentResponse> result = jdbcTemplate.query(SHIPMENT_SELECT + " WHERE e.id_envio=?",
                this::mapShipment, id);
        if (result.isEmpty()) throw new NotFoundException("El envío no existe");
        return result.getFirst();
    }

    private ContainerAssignmentResponse containerAssignment(UUID shipmentId, UUID containerId, Instant startAt) {
        List<ContainerAssignmentResponse> result = jdbcTemplate.query("""
                SELECT ec.id_envio,ec.id_contenedor,c.codigo_internacional,c.tipo,
                       ec.fecha_asociacion,ec.fecha_desasociacion
                  FROM tp_logistica.envio_contenedor ec JOIN tp_logistica.contenedor c USING(id_contenedor)
                 WHERE ec.id_envio=? AND ec.id_contenedor=? AND ec.fecha_asociacion=?
                """, this::mapContainerAssignment, shipmentId, containerId, timestamp(startAt));
        return result.getFirst();
    }

    private SensorAssignmentResponse sensorAssignment(UUID id) {
        List<SensorAssignmentResponse> result = jdbcTemplate.query("""
                SELECT a.id_asignacion,a.id_sensor,s.tipo AS sensor_tipo,a.id_contenedor,
                       c.codigo_internacional,a.fecha_inicio,a.fecha_fin
                  FROM tp_logistica.asignacion_sensor_contenedor a
                  JOIN tp_logistica.sensor_iot s USING(id_sensor)
                  JOIN tp_logistica.contenedor c USING(id_contenedor)
                 WHERE a.id_asignacion=?
                """, this::mapSensorAssignment, id);
        return result.getFirst();
    }

    private void requireActiveClient(UUID id) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM tp_logistica.cliente WHERE id_cliente=? AND estado='ACTIVO')
                """, Boolean.class, id);
        if (!Boolean.TRUE.equals(exists)) throw new ConflictException("El cliente debe existir y estar activo");
    }

    private void authorizeShipment(UUID clientId, AuthenticatedUser user) {
        if (!user.permissions().contains("ENVIOS_LEER_TODOS") && !clientId.equals(user.clientId())) {
            throw new NotFoundException("El envío no existe");
        }
    }

    private boolean hasOpenContainerAssignment(UUID id) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM tp_logistica.envio_contenedor
                               WHERE id_contenedor=? AND fecha_desasociacion IS NULL)
                """, Boolean.class, id));
    }

    private boolean hasOpenSensorAssignment(UUID id) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM tp_logistica.asignacion_sensor_contenedor
                               WHERE id_sensor=? AND fecha_fin IS NULL)
                """, Boolean.class, id));
    }

    private ShipmentResponse mapShipment(ResultSet rs, int row) throws SQLException {
        return new ShipmentResponse(rs.getObject("id_envio", UUID.class), rs.getObject("id_cliente", UUID.class),
                rs.getString("cliente"), rs.getString("origen"), rs.getString("destino"),
                rs.getString("pais_origen"), rs.getString("pais_destino"), instant(rs, "fecha_creacion"),
                instant(rs, "fecha_entrega_estimada"), instant(rs, "fecha_entrega_real"),
                rs.getString("estado"), rs.getString("prioridad"), rs.getBigDecimal("volumen_m3"));
    }

    private ContainerResponse mapContainer(ResultSet rs, int row) throws SQLException {
        return new ContainerResponse(rs.getObject("id_contenedor", UUID.class),
                rs.getString("codigo_internacional"), rs.getString("tipo"),
                rs.getBigDecimal("capacidad"), rs.getString("estado"));
    }

    private SensorResponse mapSensor(ResultSet rs, int row) throws SQLException {
        return new SensorResponse(rs.getObject("id_sensor", UUID.class), rs.getString("tipo"),
                rs.getString("fabricante"), instant(rs, "fecha_instalacion"), rs.getString("estado"));
    }

    private ContainerAssignmentResponse mapContainerAssignment(ResultSet rs, int row) throws SQLException {
        Instant end = instant(rs, "fecha_desasociacion");
        return new ContainerAssignmentResponse(rs.getObject("id_envio", UUID.class),
                rs.getObject("id_contenedor", UUID.class), rs.getString("codigo_internacional"),
                rs.getString("tipo"), instant(rs, "fecha_asociacion"), end, end == null);
    }

    private SensorAssignmentResponse mapSensorAssignment(ResultSet rs, int row) throws SQLException {
        Instant end = instant(rs, "fecha_fin");
        return new SensorAssignmentResponse(rs.getObject("id_asignacion", UUID.class),
                rs.getObject("id_sensor", UUID.class), rs.getString("sensor_tipo"),
                rs.getObject("id_contenedor", UUID.class), rs.getString("codigo_internacional"),
                instant(rs, "fecha_inicio"), end, end == null);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static PageSpec pageSpec(int page, int size) {
        return new PageSpec(Math.max(0, page), Math.max(1, Math.min(size, 100)));
    }

    private static Object[] withPage(Object[] filter, PageSpec spec) {
        Object[] result = new Object[filter.length + 2];
        System.arraycopy(filter, 0, result, 0, filter.length);
        result[result.length - 2] = spec.size;
        result[result.length - 1] = spec.page * spec.size;
        return result;
    }

    private static <T> PageResponse<T> page(List<T> content, PageSpec spec, Long total) {
        long count = total == null ? 0 : total;
        int pages = count == 0 ? 0 : (int) Math.ceil((double) count / spec.size);
        return new PageResponse<>(content, spec.page, spec.size, count, pages);
    }

    private static String normalizeSearch(String value) { return value == null ? "" : value.strip().toLowerCase(); }
    private static String clean(String value) { return value.strip(); }
    private static String nullable(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private static String upper(String value) { return value.strip().toUpperCase(); }
    private record PageSpec(int page, int size) { }
}
