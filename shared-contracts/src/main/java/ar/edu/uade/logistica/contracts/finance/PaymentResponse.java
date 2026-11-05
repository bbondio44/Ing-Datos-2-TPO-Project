package ar.edu.uade.logistica.contracts.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id, UUID invoiceId, UUID accountId, Instant paidAt, BigDecimal amount,
        String paymentMethod, String externalReference, String idempotencyKey) {
}
