package ar.edu.uade.logistica.report;

import ar.edu.uade.logistica.contracts.report.ClientDeliveryTimeResponse;
import ar.edu.uade.logistica.contracts.report.DeliveryAverageResponse;
import ar.edu.uade.logistica.contracts.report.IncidentAverageResponse;
import ar.edu.uade.logistica.contracts.report.ShipmentReportResponse;
import ar.edu.uade.logistica.contracts.report.TransportCostResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reportes")
@PreAuthorize("hasAuthority('REPORTES_LEER')")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    @GetMapping("/envios")
    public List<ShipmentReportResponse> shipments(
            @RequestParam(required = false) String pais,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) Boolean demorado,
            @RequestParam(required = false) Boolean incidentesAbiertos,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.shipments(pais, estado, clienteId, demorado, incidentesAbiertos, user);
    }

    @GetMapping("/tiempos-entrega-clientes")
    public List<ClientDeliveryTimeResponse> deliveryTimes(@RequestParam(required = false) UUID clienteId,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        return service.deliveryTimes(clienteId, user);
    }

    @GetMapping("/promedio-incidentes")
    public List<IncidentAverageResponse> incidentAverages(
            @RequestParam(required = false) String pais,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID clienteId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.incidentAverages(pais, estado, clienteId, user);
    }

    @GetMapping("/tiempo-promedio-entrega")
    public DeliveryAverageResponse deliveryAverage(@RequestParam(required = false) UUID clienteId,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return service.deliveryAverage(clienteId, user);
    }

    @GetMapping("/costos-transporte")
    public List<TransportCostResponse> transportCosts(@RequestParam(required = false) UUID clienteId,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return service.transportCosts(clienteId, user);
    }
}
