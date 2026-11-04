package ar.edu.uade.logistica.client;

import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.client.ClientRequest;
import ar.edu.uade.logistica.contracts.client.ClientResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) { this.service = service; }

    @GetMapping
    public PageResponse<ClientResponse> list(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamanio,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(buscar, pagina, tamanio, user);
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.get(id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLIENTES_ESCRIBIR')")
    public ClientResponse create(@Valid @RequestBody ClientRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_ESCRIBIR')")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CLIENTES_ESCRIBIR')")
    public void deactivate(@PathVariable UUID id) { service.deactivate(id); }
}
