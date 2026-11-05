package ar.edu.uade.logistica.route;

import ar.edu.uade.logistica.contracts.route.RouteLegRequest;
import ar.edu.uade.logistica.contracts.route.RouteLegResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trayectos")
public class RouteController {
    private final RouteService service;

    public RouteController(RouteService service) { this.service = service; }

    @GetMapping
    public List<RouteLegResponse> legs(@RequestParam UUID envioId,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return service.legs(envioId, user);
    }

    @GetMapping("/{id}")
    public RouteLegResponse leg(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.leg(id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public RouteLegResponse create(@Valid @RequestBody RouteLegRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return service.create(request, user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public RouteLegResponse update(@PathVariable UUID id, @Valid @RequestBody RouteLegRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return service.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(id, user);
    }
}
