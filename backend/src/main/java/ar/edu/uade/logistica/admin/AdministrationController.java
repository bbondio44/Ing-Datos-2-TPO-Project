package ar.edu.uade.logistica.admin;

import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.admin.PermissionResponse;
import ar.edu.uade.logistica.contracts.admin.RoleRequest;
import ar.edu.uade.logistica.contracts.admin.RoleResponse;
import ar.edu.uade.logistica.contracts.admin.UserRequest;
import ar.edu.uade.logistica.contracts.admin.UserResponse;
import ar.edu.uade.logistica.contracts.admin.UserStateRequest;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/admin")
public class AdministrationController {
    private final AdministrationService service;

    public AdministrationController(AdministrationService service) { this.service = service; }

    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('ROLES_GESTIONAR')")
    public List<PermissionResponse> permissions() { return service.permissions(); }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('USUARIOS_GESTIONAR','ROLES_GESTIONAR')")
    public List<RoleResponse> roles() { return service.roles(); }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLES_GESTIONAR')")
    public RoleResponse createRole(@Valid @RequestBody RoleRequest request) { return service.createRole(request); }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_GESTIONAR')")
    public RoleResponse updateRole(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return service.updateRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLES_GESTIONAR')")
    public void deleteRole(@PathVariable UUID id) { service.deleteRole(id); }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('USUARIOS_GESTIONAR')")
    public PageResponse<UserResponse> users(@RequestParam(defaultValue = "") String buscar,
                                            @RequestParam(defaultValue = "0") int pagina,
                                            @RequestParam(defaultValue = "50") int tamanio) {
        return service.users(buscar, pagina, tamanio);
    }

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_GESTIONAR')")
    public UserResponse user(@PathVariable UUID id) { return service.user(id); }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USUARIOS_GESTIONAR')")
    public UserResponse createUser(@Valid @RequestBody UserRequest request) { return service.createUser(request); }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_GESTIONAR')")
    public UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UserRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.updateUser(id, request, actor);
    }

    @PatchMapping("/usuarios/{id}/estado")
    @PreAuthorize("hasAuthority('USUARIOS_GESTIONAR')")
    public UserResponse changeState(@PathVariable UUID id, @Valid @RequestBody UserStateRequest request,
                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.changeState(id, request.state(), actor);
    }
}
