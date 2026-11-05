package ar.edu.uade.logistica.contracts.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShipmentReportResponse(
        UUID shipmentId, UUID clientId, String client, String origin, String destination,
        String originCountry, String destinationCountry, String state, String priority,
        BigDecimal volumeM3, Instant createdAt, Instant estimatedDelivery, Instant actualDelivery,
        boolean delayed, long openIncidents, long totalIncidents) {
}
