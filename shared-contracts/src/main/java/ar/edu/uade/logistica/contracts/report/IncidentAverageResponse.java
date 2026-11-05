package ar.edu.uade.logistica.contracts.report;

import java.math.BigDecimal;
import java.util.UUID;

public record IncidentAverageResponse(
        String destinationCountry, String shipmentState, UUID clientId, String client,
        long shipments, long incidents, BigDecimal incidentsPerShipment) {
}
