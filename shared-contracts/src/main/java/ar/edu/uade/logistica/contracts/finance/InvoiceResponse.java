package ar.edu.uade.logistica.contracts.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String number,
        UUID accountId,
        String ownerType,
        UUID ownerId,
        String ownerName,
        UUID shipmentId,
        LocalDate issuedAt,
        LocalDate dueAt,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal pending,
        String nature,
        String state,
        String idempotencyKey,
        List<InvoiceLineResponse> lines) {
}
