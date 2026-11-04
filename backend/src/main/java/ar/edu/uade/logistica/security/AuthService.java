package ar.edu.uade.logistica.security;

import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.config.AppProperties;
import ar.edu.uade.logistica.contracts.auth.LoginRequest;
import ar.edu.uade.logistica.contracts.auth.LoginResponse;
import ar.edu.uade.logistica.contracts.auth.SessionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private static final String USER_SQL = """
            SELECT id_usuario, id_cliente, nombre, apellido, email, password_hash
              FROM tp_logistica.usuario
             WHERE lower(email) = lower(?) AND estado = 'ACTIVO'
            """;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;
    private final AppProperties properties;

    public AuthService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                       TokenHasher tokenHasher, AppProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.properties = properties;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserRow user = jdbcTemplate.query(USER_SQL, resultSet -> resultSet.next() ? mapUser(resultSet) : null,
                request.email().strip());
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new BadCredentialsException("Email o contraseña incorrectos");
        }

        String token = generateToken();
        Instant expiresAt = Instant.now().plus(properties.sessionTtl());
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.sesion(id_usuario, token_hash, fecha_expiracion, estado)
                VALUES (?, ?, ?, 'ACTIVA')
                """, user.id(), tokenHasher.sha256(token), java.sql.Timestamp.from(expiresAt));

        Set<String> roles = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT r.descripcion FROM tp_logistica.rol r
                JOIN tp_logistica.usuario_rol ur ON ur.id_rol = r.id_rol
                WHERE ur.id_usuario = ? ORDER BY r.descripcion
                """, String.class, user.id()));
        Set<String> permissions = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT DISTINCT p.codigo FROM tp_logistica.permiso p
                JOIN tp_logistica.rol_permiso rp ON rp.id_permiso = p.id_permiso
                JOIN tp_logistica.usuario_rol ur ON ur.id_rol = rp.id_rol
                WHERE ur.id_usuario = ? ORDER BY p.codigo
                """, String.class, user.id()));

        return new LoginResponse(token, expiresAt, user.id(), user.clientId(),
                (user.firstName() + " " + user.lastName()).strip(), roles, permissions);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> sessions(AuthenticatedUser currentUser) {
        return jdbcTemplate.query("""
                SELECT id_sesion, fecha_inicio, fecha_expiracion, fecha_revocacion, estado
                  FROM tp_logistica.sesion
                 WHERE id_usuario = ? AND estado = 'ACTIVA' AND fecha_expiracion > now()
                 ORDER BY fecha_inicio DESC
                """, (rs, rowNum) -> new SessionResponse(
                rs.getObject("id_sesion", UUID.class),
                rs.getTimestamp("fecha_inicio").toInstant(),
                rs.getTimestamp("fecha_expiracion").toInstant(),
                rs.getTimestamp("fecha_revocacion") == null ? null : rs.getTimestamp("fecha_revocacion").toInstant(),
                rs.getString("estado")), currentUser.userId());
    }

    @Transactional
    public void logout(String rawToken) {
        jdbcTemplate.update("""
                UPDATE tp_logistica.sesion SET estado='CERRADA', fecha_fin=now()
                 WHERE token_hash=? AND estado='ACTIVA'
                """, tokenHasher.sha256(rawToken));
    }

    @Transactional
    public void revoke(UUID sessionId, AuthenticatedUser currentUser) {
        int changed;
        if (currentUser.permissions().contains("SESIONES_GESTIONAR_TODAS")) {
            changed = jdbcTemplate.update("""
                    UPDATE tp_logistica.sesion SET estado='REVOCADA', fecha_revocacion=now()
                     WHERE id_sesion=? AND estado='ACTIVA'
                    """, sessionId);
        } else {
            changed = jdbcTemplate.update("""
                    UPDATE tp_logistica.sesion SET estado='REVOCADA', fecha_revocacion=now()
                     WHERE id_sesion=? AND id_usuario=? AND estado='ACTIVA'
                    """, sessionId, currentUser.userId());
        }
        if (changed == 0) throw new NotFoundException("La sesión activa no existe o no pertenece al usuario");
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UserRow mapUser(ResultSet rs) throws SQLException {
        return new UserRow(
                rs.getObject("id_usuario", UUID.class),
                rs.getObject("id_cliente", UUID.class),
                rs.getString("nombre"), rs.getString("apellido"), rs.getString("email"),
                rs.getString("password_hash"));
    }

    private record UserRow(UUID id, UUID clientId, String firstName, String lastName,
                           String email, String passwordHash) {
    }
}

