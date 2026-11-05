package ar.edu.uade.logistica.contracts.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RouteLegRequest(
        @NotNull UUID shipmentId,
        @Min(1) int order,
        @NotBlank String origin,
        @NotBlank String destination,
        UUID originLocationId,
        UUID destinationLocationId,
        @NotBlank String transportType,
        UUID vehicleId,
        UUID operatorId,
        Instant departureAt,
        Instant estimatedArrivalAt,
        Instant actualArrivalAt,
        @DecimalMin("0.00") BigDecimal estimatedCost,
        @DecimalMin("0.00") BigDecimal actualCost,
        @NotBlank String state,
        @Valid List<RouteSupplierRequest> suppliers) {
}
