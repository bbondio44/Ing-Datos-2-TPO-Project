package ar.edu.uade.logistica.contracts.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        UUID id, UUID invoiceId, UUID paymentId, Instant occurredAt, String type,
        String source, String description, BigDecimal amount, String reference) {
}
