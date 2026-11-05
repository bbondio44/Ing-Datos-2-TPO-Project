package ar.edu.uade.logistica.contracts.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountResponse(
        UUID id, String ownerType, UUID ownerId, String ownerName, String nature,
        BigDecimal balance, BigDecimal calculatedBalance, boolean reconciled, Instant updatedAt,
        List<MovementResponse> movements) {
}
