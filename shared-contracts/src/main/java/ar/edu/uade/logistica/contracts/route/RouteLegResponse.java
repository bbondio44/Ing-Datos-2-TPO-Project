package ar.edu.uade.logistica.contracts.route;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RouteLegResponse(
        UUID id,
        UUID shipmentId,
        int order,
        String origin,
        String destination,
        UUID originLocationId,
        UUID destinationLocationId,
        String transportType,
        UUID vehicleId,
        String vehicle,
        UUID operatorId,
        String operator,
        Instant departureAt,
        Instant estimatedArrivalAt,
        Instant actualArrivalAt,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        String state,
        List<RouteSupplierResponse> suppliers) {
}
