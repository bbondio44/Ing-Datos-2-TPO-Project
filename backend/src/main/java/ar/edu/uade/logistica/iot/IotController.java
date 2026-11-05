package ar.edu.uade.logistica.iot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import ar.edu.uade.logistica.contracts.iot.LecturaIotResponse;
import ar.edu.uade.logistica.contracts.iot.PosicionResponse;
import ar.edu.uade.logistica.contracts.iot.SensorEstadoResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/iot")
@ConditionalOnProperty(prefix = "app.cassandra", name = "enabled", havingValue = "true")
public class IotController {
    private final IotService service;
    private final IotConsultaService consultas;

    public IotController(IotService service, IotConsultaService consultas) {
        this.service = service;
        this.consultas = consultas;
    }

    @PostMapping("/lecturas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public LecturaIotResponse registrar(@Valid @RequestBody LecturaIotRequest lectura) {
        return service.registrar(lectura);
    }

    @GetMapping("/sensores/{id}/lecturas")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public List<LecturaHistoricaResponse> lecturasPorSensor(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return consultas.lecturasPorSensor(id, desde, hasta, user);
    }

    @GetMapping("/contenedores/{id}/lecturas")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public List<LecturaHistoricaResponse> lecturasPorContenedor(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return consultas.lecturasPorContenedor(id, desde, hasta, user);
    }

    @GetMapping("/contenedores/{id}/recorrido")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public List<PosicionResponse> recorrido(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return consultas.recorrido(id, desde, hasta, user);
    }

    @GetMapping("/contenedores/{id}/ultima-posicion")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public PosicionResponse ultimaPosicion(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return consultas.ultimaPosicion(id, user);
    }

    @GetMapping("/sensores/estado")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public List<SensorEstadoResponse> estadoSensores(@AuthenticationPrincipal AuthenticatedUser user) {
        return consultas.estadoSensores(user);
    }
}
