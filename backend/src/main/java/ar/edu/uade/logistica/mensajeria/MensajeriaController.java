package ar.edu.uade.logistica.mensajeria;

import java.time.Instant;
import java.util.List;

import ar.edu.uade.logistica.contracts.mensajeria.ConversacionRequest;
import ar.edu.uade.logistica.contracts.mensajeria.ConversacionResponse;
import ar.edu.uade.logistica.contracts.mensajeria.MensajeRequest;
import ar.edu.uade.logistica.contracts.mensajeria.MensajeResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/mensajeria/conversaciones")
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "true")
public class MensajeriaController {
    private final MensajeriaService service;

    public MensajeriaController(MensajeriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConversacionResponse> bandeja(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.bandeja(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversacionResponse crear(@Valid @RequestBody ConversacionRequest pedido,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return service.crear(pedido, user);
    }

    @GetMapping("/{id}/mensajes")
    public List<MensajeResponse> mensajes(
            @PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant antesDe,
            @RequestParam(defaultValue = "50") int tamanio,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.mensajes(id, antesDe, tamanio, user);
    }

    @PostMapping("/{id}/mensajes")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponse enviar(@PathVariable String id, @Valid @RequestBody MensajeRequest pedido,
                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return service.enviar(id, pedido, user);
    }
}
