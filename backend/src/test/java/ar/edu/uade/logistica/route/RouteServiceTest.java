package ar.edu.uade.logistica.route;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.contracts.logistics.ShipmentResponse;
import ar.edu.uade.logistica.contracts.route.RouteLegRequest;
import ar.edu.uade.logistica.logistics.LogisticsService;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteServiceTest {
    @Test
    void finishedLegRequiresActualArrival() {
        LogisticsService logistics = mock(LogisticsService.class);
        RouteService service = new RouteService(mock(JdbcTemplate.class),
                mock(DatabaseAuditContext.class), logistics);
        UUID shipmentId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(UUID.randomUUID(), null, null, null,
                "operator@example.test", "Operador", Set.of("OPERADOR"), Set.of("LOGISTICA_ESCRIBIR"));
        when(logistics.shipment(shipmentId, actor)).thenReturn(new ShipmentResponse(
                shipmentId, UUID.randomUUID(), "Cliente", "Buenos Aires", "Santos", "Argentina", "Brasil",
                Instant.now(), Instant.now().plusSeconds(3600), null, "EN_TRANSITO", "NORMAL", BigDecimal.ONE));
        RouteLegRequest request = new RouteLegRequest(shipmentId, 1, "Buenos Aires", "Santos",
                null, null, "MARITIMO", null, null, Instant.now(), Instant.now().plusSeconds(3600),
                null, BigDecimal.TEN, null, "FINALIZADO", List.of());

        assertThrows(ConflictException.class, () -> service.create(request, actor));
    }
}
