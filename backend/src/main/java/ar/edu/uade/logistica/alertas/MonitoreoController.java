package ar.edu.uade.logistica.alertas;

import java.util.UUID;

import ar.edu.uade.logistica.contracts.alertas.MonitoreoEnvioResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoreo")
@ConditionalOnProperty(name = {"app.mongo.enabled", "app.cassandra.enabled"}, havingValue = "true")
public class MonitoreoController {
    private final MonitoreoService service;

    public MonitoreoController(MonitoreoService service) {
        this.service = service;
    }

    @GetMapping("/envios/{id}")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public MonitoreoEnvioResponse envio(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.envio(id, user);
    }
}
