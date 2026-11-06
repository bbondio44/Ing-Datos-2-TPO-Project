package ar.edu.uade.logistica.organization;

import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.organization.OrganizationRequest;
import ar.edu.uade.logistica.contracts.organization.OrganizationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OrganizationController {
    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping("/proveedores")
    @PreAuthorize("hasAuthority('PROVEEDORES_LEER')")
    public PageResponse<OrganizationResponse> suppliers(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamanio) {
        return service.list(OrganizationKind.SUPPLIER, buscar, pagina, tamanio);
    }

    @GetMapping("/proveedores/{id}")
    @PreAuthorize("hasAuthority('PROVEEDORES_LEER')")
    public OrganizationResponse supplier(@PathVariable UUID id) {
        return service.get(OrganizationKind.SUPPLIER, id);
    }

    @PostMapping("/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROVEEDORES_ESCRIBIR')")
    public OrganizationResponse createSupplier(@Valid @RequestBody OrganizationRequest request) {
        return service.create(OrganizationKind.SUPPLIER, request);
    }

    @PutMapping("/proveedores/{id}")
    @PreAuthorize("hasAuthority('PROVEEDORES_ESCRIBIR')")
    public OrganizationResponse updateSupplier(@PathVariable UUID id,
                                                @Valid @RequestBody OrganizationRequest request) {
        return service.update(OrganizationKind.SUPPLIER, id, request);
    }

    @DeleteMapping("/proveedores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROVEEDORES_ESCRIBIR')")
    public void deactivateSupplier(@PathVariable UUID id) {
        service.deactivate(OrganizationKind.SUPPLIER, id);
    }

    @PatchMapping("/proveedores/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROVEEDORES_ESCRIBIR')")
    public void activateSupplier(@PathVariable UUID id) {
        service.activate(OrganizationKind.SUPPLIER, id);
    }

    @GetMapping("/operadores")
    @PreAuthorize("hasAuthority('OPERADORES_LEER')")
    public PageResponse<OrganizationResponse> operators(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamanio) {
        return service.list(OrganizationKind.OPERATOR, buscar, pagina, tamanio);
    }

    @GetMapping("/operadores/{id}")
    @PreAuthorize("hasAuthority('OPERADORES_LEER')")
    public OrganizationResponse operator(@PathVariable UUID id) {
        return service.get(OrganizationKind.OPERATOR, id);
    }

    @PostMapping("/operadores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('OPERADORES_ESCRIBIR')")
    public OrganizationResponse createOperator(@Valid @RequestBody OrganizationRequest request) {
        return service.create(OrganizationKind.OPERATOR, request);
    }

    @PutMapping("/operadores/{id}")
    @PreAuthorize("hasAuthority('OPERADORES_ESCRIBIR')")
    public OrganizationResponse updateOperator(@PathVariable UUID id,
                                                @Valid @RequestBody OrganizationRequest request) {
        return service.update(OrganizationKind.OPERATOR, id, request);
    }

    @DeleteMapping("/operadores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('OPERADORES_ESCRIBIR')")
    public void deactivateOperator(@PathVariable UUID id) {
        service.deactivate(OrganizationKind.OPERATOR, id);
    }

    @PatchMapping("/operadores/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('OPERADORES_ESCRIBIR')")
    public void activateOperator(@PathVariable UUID id) {
        service.activate(OrganizationKind.OPERATOR, id);
    }
}
