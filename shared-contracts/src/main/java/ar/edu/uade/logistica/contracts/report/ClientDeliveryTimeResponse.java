package ar.edu.uade.logistica.contracts.report;

import java.math.BigDecimal;
import java.util.UUID;

public record ClientDeliveryTimeResponse(
        UUID clientId, String client, long deliveredShipments,
        BigDecimal minimumHours, BigDecimal maximumHours, BigDecimal averageHours) {
}
