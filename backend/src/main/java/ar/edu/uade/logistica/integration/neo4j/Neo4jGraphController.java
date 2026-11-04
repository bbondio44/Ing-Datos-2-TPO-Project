package ar.edu.uade.logistica.integration.neo4j;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.logistica.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/grafo")
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class Neo4jGraphController {
    private final Neo4jGraphService service;

    public Neo4jGraphController(Neo4jGraphService service) {
        this.service = service;
    }

    @GetMapping("/clientes/{clientId}/red")
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public List<Map<String, Object>> graphForClient(@PathVariable String clientId,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return service.graphForClient(clientId, user);
    }
}