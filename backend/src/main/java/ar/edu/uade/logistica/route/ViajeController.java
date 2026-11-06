package ar.edu.uade.logistica.route;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.logistica.contracts.logistics.ShipmentResponse;
import ar.edu.uade.logistica.contracts.rutas.ViajeRequest;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/rutas/viajes")
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class ViajeController {
    private final ViajeService service;

    public ViajeController(ViajeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ENVIOS_ESCRIBIR')")
    public ShipmentResponse crear(@Valid @RequestBody ViajeRequest pedido, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.crear(pedido, user);
    }
}
