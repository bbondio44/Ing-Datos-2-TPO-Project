package ar.edu.uade.logistica.analitica;

import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.contracts.reportes.ReporteRequest;
import ar.edu.uade.logistica.contracts.reportes.ReporteResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reportes")
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "true")
public class ReporteController {
    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public ReporteResponse solicitar(@Valid @RequestBody ReporteRequest pedido,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return service.solicitar(pedido, user);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public List<ReporteResponse> listar(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listar(user);
    }

    @GetMapping("/{id}/resultado")
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public Object resultado(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.resultado(id, user);
    }

    @PostMapping("/{id}/facturar")
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public ReporteResponse facturar(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.facturar(id, user);
    }
}
