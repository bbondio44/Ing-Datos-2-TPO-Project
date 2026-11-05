package ar.edu.uade.logistica.report;

import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ReportServiceTest {
    @Test
    void clientCannotRequestAnotherClientsReport() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReportService service = new ReportService(jdbc);
        UUID ownClient = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), ownClient, null, null,
                "cliente@example.test", "Cliente", Set.of("CLIENTE"), Set.of("REPORTES_LEER"));

        assertThrows(AccessDeniedException.class, () -> service.shipments(
                null, null, UUID.randomUUID(), null, null, user));
        verifyNoInteractions(jdbc);
    }
}
