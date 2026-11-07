package ar.edu.uade.logistica.logistics;

import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.logistics.ContainerAssignmentRequest;
import ar.edu.uade.logistica.contracts.logistics.ContainerAssignmentResponse;
import ar.edu.uade.logistica.contracts.logistics.ContainerRequest;
import ar.edu.uade.logistica.contracts.logistics.ContainerResponse;
import ar.edu.uade.logistica.contracts.logistics.SensorAssignmentRequest;
import ar.edu.uade.logistica.contracts.logistics.SensorAssignmentResponse;
import ar.edu.uade.logistica.contracts.logistics.SensorRequest;
import ar.edu.uade.logistica.contracts.logistics.SensorResponse;
import ar.edu.uade.logistica.contracts.logistics.ShipmentDetailResponse;
import ar.edu.uade.logistica.contracts.logistics.ShipmentRequest;
import ar.edu.uade.logistica.contracts.logistics.ShipmentResponse;
import ar.edu.uade.logistica.contracts.logistics.ShipmentStateRequest;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LogisticsController {
    private final LogisticsService service;

    public LogisticsController(LogisticsService service) {
        this.service = service;
    }

    @GetMapping("/envios")
    public PageResponse<ShipmentResponse> shipments(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamanio,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.shipments(buscar, pagina, tamanio, user);
    }

    @GetMapping("/envios/{id}")
    public ShipmentDetailResponse shipment(@PathVariable UUID id,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.shipmentDetail(id, user);
    }

    @PostMapping("/envios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ENVIOS_ESCRIBIR')")
    public ShipmentResponse createShipment(@Valid @RequestBody ShipmentRequest request) {
        return service.createShipment(request);
    }

    @PutMapping("/envios/{id}")
    @PreAuthorize("hasAuthority('ENVIOS_ESCRIBIR')")
    public ShipmentResponse updateShipment(@PathVariable UUID id, @Valid @RequestBody ShipmentRequest request) {
        return service.updateShipment(id, request);
    }

    @PatchMapping("/envios/{id}/estado")
    @PreAuthorize("hasAuthority('ENVIOS_ESCRIBIR')")
    public ShipmentResponse changeShipmentState(@PathVariable UUID id,
                                                 @Valid @RequestBody ShipmentStateRequest request) {
        return service.changeShipmentState(id, request);
    }

    @GetMapping("/contenedores")
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public PageResponse<ContainerResponse> containers(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamanio) {
        return service.containers(buscar, pagina, tamanio);
    }

    @PostMapping("/contenedores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public ContainerResponse createContainer(@Valid @RequestBody ContainerRequest request) {
        return service.createContainer(request);
    }

    @PutMapping("/contenedores/{id}")
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public ContainerResponse updateContainer(@PathVariable UUID id, @Valid @RequestBody ContainerRequest request) {
        return service.updateContainer(id, request);
    }

    @GetMapping("/sensores")
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public PageResponse<SensorResponse> sensors(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamanio) {
        return service.sensors(buscar, pagina, tamanio);
    }

    @GetMapping("/sensores/libres")
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public List<SensorResponse> freeSensors() {
        return service.freeSensors();
    }

    @PostMapping("/sensores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public SensorResponse createSensor(@Valid @RequestBody SensorRequest request) {
        return service.createSensor(request);
    }

    @PutMapping("/sensores/{id}")
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public SensorResponse updateSensor(@PathVariable UUID id, @Valid @RequestBody SensorRequest request) {
        return service.updateSensor(id, request);
    }

    @GetMapping("/envios/{id}/contenedores")
    public List<ContainerAssignmentResponse> containerAssignments(@PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.containerAssignments(id, user);
    }

    @PostMapping("/envios/{id}/contenedores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public ContainerAssignmentResponse assignContainer(@PathVariable UUID id,
            @Valid @RequestBody ContainerAssignmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.assignContainer(id, request, user);
    }

    @DeleteMapping("/envios/{shipmentId}/contenedores/{containerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public void closeContainerAssignment(@PathVariable UUID shipmentId, @PathVariable UUID containerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fin,
            @AuthenticationPrincipal AuthenticatedUser user) {
        service.closeContainerAssignment(shipmentId, containerId, inicio, fin, user);
    }

    @PostMapping("/asignaciones-sensor")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public SensorAssignmentResponse assignSensor(@Valid @RequestBody SensorAssignmentRequest request) {
        return service.assignSensor(request);
    }

    @DeleteMapping("/asignaciones-sensor/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public void closeSensorAssignment(@PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fin) {
        service.closeSensorAssignment(id, fin);
    }

    @GetMapping("/contenedores/{id}/historial")
    public List<ar.edu.uade.logistica.contracts.logistics.ContainerHistoryResponse> containerHistory(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.containerHistory(id, user);
    }

    @GetMapping("/contenedores/{id}/sensores")
    @PreAuthorize("hasAuthority('LOGISTICA_LEER')")
    public List<SensorAssignmentResponse> sensorsAt(@PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fecha) {
        return service.sensorsForContainerAt(id, fecha);
    }
}
