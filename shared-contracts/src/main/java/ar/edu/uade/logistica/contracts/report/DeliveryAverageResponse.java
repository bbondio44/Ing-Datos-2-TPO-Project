package ar.edu.uade.logistica.contracts.report;

import java.math.BigDecimal;

public record DeliveryAverageResponse(long deliveredShipments, BigDecimal averageHours) {
}
