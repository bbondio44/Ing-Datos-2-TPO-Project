package ar.edu.uade.logistica.admin;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AdministrationServiceTest {
    @Test
    void administratorCannotDisableOwnUser() {
        AdministrationService service = new AdministrationService(mock(JdbcTemplate.class),
                mock(PasswordEncoder.class), mock(DatabaseAuditContext.class));
        UUID userId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(userId, null, null, null,
                "admin@example.test", "Admin", Set.of("ADMIN"), Set.of("USUARIOS_GESTIONAR"));

        assertThrows(ConflictException.class, () -> service.changeState(userId, "INACTIVO", actor));
    }
}
