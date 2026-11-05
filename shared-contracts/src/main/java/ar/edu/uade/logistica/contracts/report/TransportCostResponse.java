package ar.edu.uade.logistica.contracts.report;

import java.math.BigDecimal;

public record TransportCostResponse(
        String transportType, long legsWithActualCost, BigDecimal totalActualCost, BigDecimal averageActualCost) {
}
