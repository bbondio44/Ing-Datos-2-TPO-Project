package ar.edu.uade.logistica.integration.neo4j;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.logistica.contracts.rutas.PuntoRutaResponse;
import ar.edu.uade.logistica.contracts.rutas.RutaResponse;

@RestController
@RequestMapping("/api/v1/rutas")
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class RutasController {
    private final RutasService service;

    public RutasController(RutasService service) {
        this.service = service;
    }

    @GetMapping("/puntos")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'ENVIOS_ESCRIBIR')")
    public List<PuntoRutaResponse> puntos() {
        return service.puntos();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'ENVIOS_ESCRIBIR')")
    public RutaResponse ruta(@RequestParam String origen, @RequestParam String destino,
                             @RequestParam(defaultValue = "false") boolean aereo) {
        return service.ruta(origen, destino, aereo);
    }
}
