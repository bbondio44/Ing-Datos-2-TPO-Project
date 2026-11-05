package ar.edu.uade.logistica.analitica;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analitica")
public class AnaliticaController {
    private final AnaliticaRepository analitica;
    private final PrediccionService predicciones;

    public AnaliticaController(AnaliticaRepository analitica, PrediccionService predicciones) {
        this.analitica = analitica;
        this.predicciones = predicciones;
    }

    @GetMapping("/ranking-rutas")
    @PreAuthorize("hasAuthority('ENVIOS_LEER_TODOS')")
    public List<Map<String, Object>> rankingRutas() {
        return analitica.rankingRutas();
    }

    @GetMapping("/ranking-clientes")
    @PreAuthorize("hasAuthority('ENVIOS_LEER_TODOS')")
    public List<Map<String, Object>> rankingClientes() {
        return analitica.rankingClientes();
    }

    @GetMapping("/desempeno")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public List<Map<String, Object>> desempeno(
            @RequestParam(required = false) UUID idCliente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return analitica.desempeno(clienteVisible(idCliente, user), desde, hasta);
    }

    @GetMapping("/predicciones")
    @PreAuthorize("hasAnyAuthority('LOGISTICA_LEER', 'REPORTES_LEER')")
    public List<Map<String, Object>> predicciones(@AuthenticationPrincipal AuthenticatedUser user) {
        return predicciones.vigentes(clienteVisible(null, user));
    }

    @PostMapping("/predicciones/generar")
    @PreAuthorize("hasAuthority('LOGISTICA_ESCRIBIR')")
    public Map<String, Integer> generarPredicciones() {
        return Map.of("generadas", predicciones.generarDelDia());
    }

    @GetMapping("/predicciones/validacion")
    @PreAuthorize("hasAuthority('ENVIOS_LEER_TODOS')")
    public List<Map<String, Object>> validacion() {
        return predicciones.validacion();
    }

    /** Un usuario de cliente siempre queda limitado a su cliente, pida lo que pida. */
    private UUID clienteVisible(UUID pedido, AuthenticatedUser user) {
        return user.permissions().contains("ENVIOS_LEER_TODOS") ? pedido : user.clientId();
    }
}
