package ar.edu.uade.logistica.alertas;

import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaRequest;
import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reglas")
public class ReglaAlertaController {
    private final ReglaAlertaRepository reglas;

    public ReglaAlertaController(ReglaAlertaRepository reglas) {
        this.reglas = reglas;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public List<ReglaAlertaResponse> listar() {
        return reglas.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public ReglaAlertaResponse crear(@Valid @RequestBody ReglaAlertaRequest regla) {
        return reglas.crear(regla);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public ReglaAlertaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ReglaAlertaRequest regla) {
        return reglas.actualizar(id, regla);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public void desactivar(@PathVariable UUID id) {
        reglas.desactivar(id);
    }
}
