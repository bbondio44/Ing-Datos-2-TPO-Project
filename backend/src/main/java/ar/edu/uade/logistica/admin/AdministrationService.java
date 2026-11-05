package ar.edu.uade.logistica.admin;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.admin.PermissionResponse;
import ar.edu.uade.logistica.contracts.admin.RoleRequest;
import ar.edu.uade.logistica.contracts.admin.RoleResponse;
import ar.edu.uade.logistica.contracts.admin.UserRequest;
import ar.edu.uade.logistica.contracts.admin.UserResponse;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdministrationService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseAuditContext auditContext;

    public AdministrationService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                                 DatabaseAuditContext auditContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.auditContext = auditContext;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> permissions() {
        return jdbcTemplate.query("""
                SELECT id_permiso,codigo,descripcion FROM tp_logistica.permiso ORDER BY codigo
                """, (rs, row) -> new PermissionResponse(rs.getObject("id_permiso", UUID.class),
                rs.getString("codigo"), rs.getString("descripcion")));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return jdbcTemplate.query("""
                SELECT id_rol,descripcion,nivel_permisos FROM tp_logistica.rol ORDER BY nivel_permisos DESC,descripcion
                """, (rs, row) -> role(rs.getObject("id_rol", UUID.class), rs.getString("descripcion"),
                rs.getInt("nivel_permisos")));
    }

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        auditContext.applyToCurrentTransaction();
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.rol(id_rol,descripcion,nivel_permisos) VALUES (?,?,?)
                """, id, upper(request.description()), request.level());
        replaceRolePermissions(id, request.permissions());
        return role(id);
    }

    @Transactional
    public RoleResponse updateRole(UUID id, RoleRequest request) {
        auditContext.applyToCurrentTransaction();
        int changed = jdbcTemplate.update("""
                UPDATE tp_logistica.rol SET descripcion=?,nivel_permisos=? WHERE id_rol=?
                """, upper(request.description()), request.level(), id);
        if (changed == 0) throw new NotFoundException("El rol no existe");
        replaceRolePermissions(id, request.permissions());
        return role(id);
    }

    @Transactional
    public void deleteRole(UUID id) {
        auditContext.applyToCurrentTransaction();
        String description = jdbcTemplate.query("SELECT descripcion FROM tp_logistica.rol WHERE id_rol=?",
                rs -> rs.next() ? rs.getString(1) : null, id);
        if (description == null) throw new NotFoundException("El rol no existe");
        if ("ADMIN".equals(description)) throw new ConflictException("El rol ADMIN no se puede eliminar");
        int users = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tp_logistica.usuario_rol WHERE id_rol=?", Integer.class, id);
        if (users > 0) throw new ConflictException("El rol está asignado a usuarios");
        jdbcTemplate.update("DELETE FROM tp_logistica.rol WHERE id_rol=?", id);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> users(String search, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        String term = search == null ? "" : search.strip().toLowerCase();
        String where = term.isBlank() ? "" : " WHERE lower(u.email) LIKE ? OR lower(u.nombre||' '||u.apellido) LIKE ?";
        Object[] filter = term.isBlank() ? new Object[0] : new Object[]{"%" + term + "%", "%" + term + "%"};
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tp_logistica.usuario u" + where, Long.class, filter);
        Object[] params = new Object[filter.length + 2];
        System.arraycopy(filter, 0, params, 0, filter.length);
        params[params.length - 2] = safeSize;
        params[params.length - 1] = safePage * safeSize;
        List<UserResponse> content = jdbcTemplate.query("""
                SELECT u.id_usuario,u.id_cliente,u.id_proveedor,u.id_operador,u.nombre,u.apellido,
                       u.email,u.estado,u.fecha_registro
                  FROM tp_logistica.usuario u
                """ + where + " ORDER BY u.apellido,u.nombre LIMIT ? OFFSET ?", this::mapUser, params);
        long count = total == null ? 0 : total;
        int pages = count == 0 ? 0 : (int) Math.ceil((double) count / safeSize);
        return new PageResponse<>(content, safePage, safeSize, count, pages);
    }

    @Transactional(readOnly = true)
    public UserResponse user(UUID id) {
        List<UserResponse> users = jdbcTemplate.query("""
                SELECT id_usuario,id_cliente,id_proveedor,id_operador,nombre,apellido,email,estado,fecha_registro
                  FROM tp_logistica.usuario WHERE id_usuario=?
                """, this::mapUser, id);
        if (users.isEmpty()) throw new NotFoundException("El usuario no existe");
        return users.getFirst();
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        auditContext.applyToCurrentTransaction();
        validateOwner(request);
        if (request.password() == null || request.password().isBlank()) {
            throw new ConflictException("La contraseña es obligatoria al crear un usuario");
        }
        validateRoles(request.roleIds());
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.usuario
                (id_usuario,id_cliente,id_proveedor,id_operador,nombre,apellido,email,password_hash,estado)
                VALUES (?,?,?,?,?,?,?,?, 'ACTIVO')
                """, id, request.clientId(), request.supplierId(), request.operatorId(), clean(request.firstName()),
                clean(request.lastName()), request.email().strip().toLowerCase(),
                passwordEncoder.encode(request.password()));
        replaceUserRoles(id, request.roleIds());
        return user(id);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request, AuthenticatedUser actor) {
        auditContext.applyToCurrentTransaction();
        user(id);
        validateOwner(request);
        validateRoles(request.roleIds());
        if (id.equals(actor.userId()) && request.roleIds().stream().noneMatch(this::isAdminRole)) {
            throw new ConflictException("No puede quitarse a sí mismo el rol ADMIN");
        }
        jdbcTemplate.update("""
                UPDATE tp_logistica.usuario
                   SET id_cliente=?,id_proveedor=?,id_operador=?,nombre=?,apellido=?,email=?
                 WHERE id_usuario=?
                """, request.clientId(), request.supplierId(), request.operatorId(), clean(request.firstName()),
                clean(request.lastName()), request.email().strip().toLowerCase(), id);
        if (request.password() != null && !request.password().isBlank()) {
            jdbcTemplate.update("UPDATE tp_logistica.usuario SET password_hash=? WHERE id_usuario=?",
                    passwordEncoder.encode(request.password()), id);
        }
        replaceUserRoles(id, request.roleIds());
        return user(id);
    }

    @Transactional
    public UserResponse changeState(UUID id, String state, AuthenticatedUser actor) {
        auditContext.applyToCurrentTransaction();
        String normalized = upper(state);
        if (!Set.of("ACTIVO", "BLOQUEADO", "INACTIVO").contains(normalized)) {
            throw new ConflictException("Estado de usuario inválido");
        }
        if (id.equals(actor.userId()) && !"ACTIVO".equals(normalized)) {
            throw new ConflictException("No puede bloquear o desactivar su propio usuario");
        }
        int changed = jdbcTemplate.update("UPDATE tp_logistica.usuario SET estado=? WHERE id_usuario=?",
                normalized, id);
        if (changed == 0) throw new NotFoundException("El usuario no existe");
        if (!"ACTIVO".equals(normalized)) {
            jdbcTemplate.update("""
                    UPDATE tp_logistica.sesion SET estado='REVOCADA',fecha_revocacion=now()
                     WHERE id_usuario=? AND estado='ACTIVA'
                    """, id);
        }
        return user(id);
    }

    private UserResponse mapUser(ResultSet rs, int row) throws SQLException {
        UUID id = rs.getObject("id_usuario", UUID.class);
        return new UserResponse(id, rs.getObject("id_cliente", UUID.class),
                rs.getObject("id_proveedor", UUID.class), rs.getObject("id_operador", UUID.class),
                rs.getString("nombre"), rs.getString("apellido"), rs.getString("email"),
                rs.getString("estado"), rs.getTimestamp("fecha_registro").toInstant(), userRoles(id));
    }

    private Set<RoleResponse> userRoles(UUID userId) {
        return new LinkedHashSet<>(jdbcTemplate.query("""
                SELECT r.id_rol,r.descripcion,r.nivel_permisos
                  FROM tp_logistica.rol r JOIN tp_logistica.usuario_rol ur ON ur.id_rol=r.id_rol
                 WHERE ur.id_usuario=? ORDER BY r.nivel_permisos DESC,r.descripcion
                """, (rs, row) -> role(rs.getObject("id_rol", UUID.class), rs.getString("descripcion"),
                rs.getInt("nivel_permisos")), userId));
    }

    private RoleResponse role(UUID id) {
        List<RoleResponse> roles = jdbcTemplate.query("""
                SELECT id_rol,descripcion,nivel_permisos FROM tp_logistica.rol WHERE id_rol=?
                """, (rs, row) -> role(rs.getObject("id_rol", UUID.class), rs.getString("descripcion"),
                rs.getInt("nivel_permisos")), id);
        if (roles.isEmpty()) throw new NotFoundException("El rol no existe");
        return roles.getFirst();
    }

    private RoleResponse role(UUID id, String description, int level) {
        Set<String> permissions = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT p.codigo FROM tp_logistica.permiso p
                JOIN tp_logistica.rol_permiso rp ON rp.id_permiso=p.id_permiso
                WHERE rp.id_rol=? ORDER BY p.codigo
                """, String.class, id));
        return new RoleResponse(id, description, level, permissions);
    }

    private void replaceRolePermissions(UUID roleId, Set<String> permissions) {
        Set<String> requested = permissions == null ? Set.of() : permissions.stream()
                .map(AdministrationService::upper).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (String code : requested) {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM tp_logistica.permiso WHERE codigo=?)", Boolean.class, code);
            if (!Boolean.TRUE.equals(exists)) throw new ConflictException("El permiso " + code + " no existe");
        }
        jdbcTemplate.update("DELETE FROM tp_logistica.rol_permiso WHERE id_rol=?", roleId);
        for (String code : requested) {
            jdbcTemplate.update("""
                    INSERT INTO tp_logistica.rol_permiso(id_rol,id_permiso)
                    SELECT ?,id_permiso FROM tp_logistica.permiso WHERE codigo=?
                    """, roleId, code);
        }
    }

    private void replaceUserRoles(UUID userId, Set<UUID> roleIds) {
        jdbcTemplate.update("DELETE FROM tp_logistica.usuario_rol WHERE id_usuario=?", userId);
        for (UUID roleId : roleIds) {
            jdbcTemplate.update("INSERT INTO tp_logistica.usuario_rol(id_usuario,id_rol) VALUES (?,?)",
                    userId, roleId);
        }
    }

    private void validateRoles(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) throw new ConflictException("El usuario debe tener al menos un rol");
        for (UUID roleId : roleIds) role(roleId);
    }

    private void validateOwner(UserRequest request) {
        long owners = java.util.stream.Stream.of(request.clientId(), request.supplierId(), request.operatorId())
                .filter(java.util.Objects::nonNull).count();
        if (owners > 1) throw new ConflictException("El usuario solo puede pertenecer a una organización");
    }

    private boolean isAdminRole(UUID roleId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM tp_logistica.rol WHERE id_rol=? AND descripcion='ADMIN')",
                Boolean.class, roleId));
    }

    private static String clean(String value) { return value.strip(); }
    private static String upper(String value) { return value.strip().toUpperCase(); }
}
