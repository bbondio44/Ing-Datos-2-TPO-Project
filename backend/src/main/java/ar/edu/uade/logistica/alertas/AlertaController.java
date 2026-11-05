package ar.edu.uade.logistica.alertas;

import java.util.List;

import ar.edu.uade.logistica.contracts.alertas.AlertaResponse;
import ar.edu.uade.logistica.contracts.alertas.CambioEstadoAlertaRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alertas")
@ConditionalOnProperty(name = {"app.mongo.enabled", "app.cassandra.enabled"}, havingValue = "true")
public class AlertaController {
    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public List<AlertaResponse> listar(@RequestParam(required = false) String estado,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return service.listar(estado, user);
    }

    @PostMapping("/{id}/reconocer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public void reconocer(@PathVariable String id, @Valid @RequestBody CambioEstadoAlertaRequest cambio,
                          @AuthenticationPrincipal AuthenticatedUser user) {
        service.reconocer(id, cambio.comentario(), user);
    }

    @PostMapping("/{id}/cerrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public void cerrar(@PathVariable String id, @Valid @RequestBody CambioEstadoAlertaRequest cambio,
                       @AuthenticationPrincipal AuthenticatedUser user) {
        service.cerrar(id, cambio.comentario(), user);
    }
}
