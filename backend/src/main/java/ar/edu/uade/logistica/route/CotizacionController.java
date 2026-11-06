package ar.edu.uade.logistica.route;

import java.math.BigDecimal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.logistica.contracts.rutas.CotizacionResponse;

@RestController
@RequestMapping("/api/v1/rutas/cotizacion")
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class CotizacionController {
    private final CotizacionService service;

    public CotizacionController(CotizacionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'ENVIOS_ESCRIBIR')")
    public CotizacionResponse cotizar(@RequestParam String origen, @RequestParam String destino,
                                      @RequestParam BigDecimal volumen,
                                      @RequestParam(defaultValue = "false") boolean aereo) {
        return service.cotizar(origen, destino, volumen, aereo);
    }
}
