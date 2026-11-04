package ar.edu.uade.logistica.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class SessionAuthenticationService {
    private static final String AUTH_SQL = """
            SELECT u.id_usuario, u.id_cliente, u.email, concat_ws(' ', u.nombre, u.apellido) AS nombre,
                   array_remove(array_agg(DISTINCT r.descripcion), NULL) AS roles,
                   array_remove(array_agg(DISTINCT p.codigo), NULL) AS permisos
              FROM tp_logistica.sesion s
              JOIN tp_logistica.usuario u ON u.id_usuario = s.id_usuario
              LEFT JOIN tp_logistica.usuario_rol ur ON ur.id_usuario = u.id_usuario
              LEFT JOIN tp_logistica.rol r ON r.id_rol = ur.id_rol
              LEFT JOIN tp_logistica.rol_permiso rp ON rp.id_rol = r.id_rol
              LEFT JOIN tp_logistica.permiso p ON p.id_permiso = rp.id_permiso
             WHERE s.token_hash = ? AND s.estado = 'ACTIVA'
               AND s.fecha_expiracion > now() AND u.estado = 'ACTIVO'
             GROUP BY u.id_usuario, u.id_cliente, u.email, u.nombre, u.apellido
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TokenHasher tokenHasher;

    public SessionAuthenticationService(JdbcTemplate jdbcTemplate, TokenHasher tokenHasher) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenHasher = tokenHasher;
    }

    public Optional<AuthenticatedUser> authenticate(String rawToken) {
        return jdbcTemplate.query(AUTH_SQL, resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(
                    resultSet.getObject("id_usuario", java.util.UUID.class),
                    resultSet.getObject("id_cliente", java.util.UUID.class),
                    resultSet.getString("email"),
                    resultSet.getString("nombre"),
                    toSet(resultSet.getArray("roles")),
                    toSet(resultSet.getArray("permisos"))));
        }, tokenHasher.sha256(rawToken));
    }

    private static Set<String> toSet(Array array) throws java.sql.SQLException {
        if (array == null) {
            return Collections.emptySet();
        }
        Object value = array.getArray();
        if (value instanceof String[] strings) {
            return new LinkedHashSet<>(Arrays.asList(strings));
        }
        Object[] values = (Object[]) value;
        Set<String> result = new LinkedHashSet<>();
        for (Object item : values) {
            if (item != null) result.add(item.toString());
        }
        return result;
    }
}

