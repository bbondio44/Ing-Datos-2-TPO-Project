package ar.edu.uade.logistica.route;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.logistica.contracts.logistics.ShipmentRequest;
import ar.edu.uade.logistica.contracts.logistics.ShipmentResponse;
import ar.edu.uade.logistica.contracts.route.RouteLegRequest;
import ar.edu.uade.logistica.contracts.rutas.CotizacionResponse;
import ar.edu.uade.logistica.contracts.rutas.PuntoRutaResponse;
import ar.edu.uade.logistica.contracts.rutas.TramoCotizadoResponse;
import ar.edu.uade.logistica.contracts.rutas.ViajeRequest;
import ar.edu.uade.logistica.logistics.LogisticsService;
import ar.edu.uade.logistica.security.AuthenticatedUser;

/** Crea el envío y un tramo por cada conexión de la ruta, en una sola transacción. */
@Service
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class ViajeService {
    private final CotizacionService cotizaciones;
    private final LogisticsService envios;
    private final RouteService tramos;

    public ViajeService(CotizacionService cotizaciones, LogisticsService envios, RouteService tramos) {
        this.cotizaciones = cotizaciones;
        this.envios = envios;
        this.tramos = tramos;
    }

    @Transactional
    public ShipmentResponse crear(ViajeRequest pedido, AuthenticatedUser user) {
        CotizacionResponse cotizacion = cotizaciones.cotizar(pedido.origen(), pedido.destino(), pedido.volumeM3(), pedido.aereo());
        PuntoRutaResponse origen = cotizacion.tramos().getFirst().origen();
        PuntoRutaResponse destino = cotizacion.tramos().getLast().destino();
        ShipmentResponse envio = envios.createShipment(new ShipmentRequest(pedido.clientId(), origen.nombre(), destino.nombre(),
                origen.pais(), destino.pais(), pedido.estimatedDelivery(), pedido.priority(), pedido.volumeM3()));
        int orden = 1;
        for (TramoCotizadoResponse tramo : cotizacion.tramos()) {
            tramos.create(new RouteLegRequest(envio.id(), orden++, tramo.origen().nombre(), tramo.destino().nombre(),
                    null, null, tramo.medio(), null, null, null, null, null, tramo.costo(), null, "PENDIENTE", List.of()), user);
        }
        return envio;
    }
}
