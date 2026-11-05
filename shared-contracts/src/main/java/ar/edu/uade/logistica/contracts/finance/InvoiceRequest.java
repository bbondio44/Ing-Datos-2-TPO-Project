package ar.edu.uade.logistica.contracts.finance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceRequest(
        @NotBlank String ownerType,
        @NotNull UUID ownerId,
        UUID shipmentId,
        LocalDate dueDate,
        String idempotencyKey,
        @NotEmpty @Valid List<InvoiceLineRequest> lines) {
}
