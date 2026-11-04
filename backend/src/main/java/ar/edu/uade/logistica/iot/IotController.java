package ar.edu.uade.logistica.iot;

import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import ar.edu.uade.logistica.contracts.iot.LecturaIotResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iot")
@ConditionalOnProperty(prefix = "app.cassandra", name = "enabled", havingValue = "true")
public class IotController {
    private final IotService service;

    public IotController(IotService service) {
        this.service = service;
    }

    @PostMapping("/lecturas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public LecturaIotResponse registrar(@Valid @RequestBody LecturaIotRequest lectura) {
        return service.registrar(lectura);
    }
}
