package ar.edu.uade.logistica.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final List<String> PERMISSIONS = List.of(
            "CLIENTES_LEER_TODOS", "CLIENTES_ESCRIBIR", "SESIONES_GESTIONAR_TODAS",
            "PROVEEDORES_LEER", "PROVEEDORES_ESCRIBIR", "OPERADORES_LEER", "OPERADORES_ESCRIBIR",
            "MAESTROS_LEER", "MAESTROS_ESCRIBIR", "ENVIOS_LEER_TODOS", "ENVIOS_ESCRIBIR",
            "LOGISTICA_LEER", "LOGISTICA_ESCRIBIR");

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public AdminBootstrapRunner(Environment environment, JdbcTemplate jdbcTemplate,
                                PasswordEncoder passwordEncoder, TransactionTemplate transactionTemplate) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = environment.getProperty("APP_BOOTSTRAP_ADMIN_EMAIL");
        String password = environment.getProperty("APP_BOOTSTRAP_ADMIN_PASSWORD");
        if (email == null || email.isBlank()) return;
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD debe tener al menos 12 caracteres");
        }
        transactionTemplate.executeWithoutResult(status -> bootstrap(email.strip().toLowerCase(), password));
    }

    private void bootstrap(String email, String password) {
        UUID roleId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.rol(id_rol, descripcion, nivel_permisos)
                VALUES (?, 'ADMIN', 100) ON CONFLICT (descripcion) DO NOTHING
                """, roleId);
        roleId = jdbcTemplate.queryForObject(
                "SELECT id_rol FROM tp_logistica.rol WHERE descripcion='ADMIN'", UUID.class);

        for (String code : PERMISSIONS) {
            jdbcTemplate.update("""
                    INSERT INTO tp_logistica.permiso(id_permiso, codigo, descripcion)
                    VALUES (?, ?, ?) ON CONFLICT (codigo) DO NOTHING
                    """, UUID.randomUUID(), code, "Permiso " + code);
            UUID permissionId = jdbcTemplate.queryForObject(
                    "SELECT id_permiso FROM tp_logistica.permiso WHERE codigo=?", UUID.class, code);
            jdbcTemplate.update("""
                    INSERT INTO tp_logistica.rol_permiso(id_rol,id_permiso)
                    VALUES (?,?) ON CONFLICT DO NOTHING
                    """, roleId, permissionId);
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tp_logistica.usuario WHERE lower(email)=lower(?)", Integer.class, email);
        if (count != null && count > 0) {
            log.info("El usuario bootstrap ya existe; no se modificó su contraseña");
            return;
        }
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.usuario(id_usuario,nombre,apellido,email,password_hash,estado)
                VALUES (?, 'Administrador', 'Grupo 4', ?, ?, 'ACTIVO')
                """, userId, email, passwordEncoder.encode(password));
        jdbcTemplate.update("INSERT INTO tp_logistica.usuario_rol(id_usuario,id_rol) VALUES (?,?)", userId, roleId);
        log.info("Administrador inicial creado para {}. Retire las variables bootstrap después del primer arranque", email);
    }
}

