package ar.edu.uade.logistica.master;

import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.master.LocationRequest;
import ar.edu.uade.logistica.contracts.master.LocationResponse;
import ar.edu.uade.logistica.contracts.master.VehicleRequest;
import ar.edu.uade.logistica.contracts.master.VehicleResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MasterDataController {
    private final MasterDataService service;

    public MasterDataController(MasterDataService service) {
        this.service = service;
    }

    @GetMapping("/ubicaciones")
    @PreAuthorize("hasAuthority('MAESTROS_LEER')")
    public PageResponse<LocationResponse> locations(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamanio) {
        return service.locations(buscar, pagina, tamanio);
    }

    @GetMapping("/ubicaciones/{id}")
    @PreAuthorize("hasAuthority('MAESTROS_LEER')")
    public LocationResponse location(@PathVariable UUID id) {
        return service.location(id);
    }

    @PostMapping("/ubicaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MAESTROS_ESCRIBIR')")
    public LocationResponse createLocation(@Valid @RequestBody LocationRequest request) {
        return service.createLocation(request);
    }

    @PutMapping("/ubicaciones/{id}")
    @PreAuthorize("hasAuthority('MAESTROS_ESCRIBIR')")
    public LocationResponse updateLocation(@PathVariable UUID id, @Valid @RequestBody LocationRequest request) {
        return service.updateLocation(id, request);
    }

    @DeleteMapping("/ubicaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('MAESTROS_ESCRIBIR')")
    public void deleteLocation(@PathVariable UUID id) {
        service.deleteLocation(id);
    }

    @GetMapping("/vehiculos")
    @PreAuthorize("hasAuthority('MAESTROS_LEER')")
    public PageResponse<VehicleResponse> vehicles(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamanio) {
        return service.vehicles(buscar, pagina, tamanio);
    }

    @GetMapping("/vehiculos/{id}")
    @PreAuthorize("hasAuthority('MAESTROS_LEER')")
    public VehicleResponse vehicle(@PathVariable UUID id) {
        return service.vehicle(id);
    }

    @PostMapping("/vehiculos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MAESTROS_ESCRIBIR')")
    public VehicleResponse createVehicle(@Valid @RequestBody VehicleRequest request) {
        return service.createVehicle(request);
    }

    @PutMapping("/vehiculos/{id}")
    @PreAuthorize("hasAuthority('MAESTROS_ESCRIBIR')")
    public VehicleResponse updateVehicle(@PathVariable UUID id, @Valid @RequestBody VehicleRequest request) {
        return service.updateVehicle(id, request);
    }

    @DeleteMapping("/vehiculos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('MAESTROS_ESCRIBIR')")
    public void deactivateVehicle(@PathVariable UUID id) {
        service.deactivateVehicle(id);
    }
}
