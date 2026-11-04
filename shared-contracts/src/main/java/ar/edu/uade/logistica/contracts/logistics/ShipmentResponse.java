package ar.edu.uade.logistica.contracts.logistics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID id, UUID clientId, String clientName,
        String origin, String destination, String originCountry, String destinationCountry,
        Instant createdAt, Instant estimatedDelivery, Instant actualDelivery,
        String state, String priority, BigDecimal volumeM3) {
}
