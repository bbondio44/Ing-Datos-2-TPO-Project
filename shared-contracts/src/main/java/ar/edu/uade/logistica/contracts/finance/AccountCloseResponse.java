package ar.edu.uade.logistica.contracts.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountCloseResponse(
        UUID id, UUID accountId, LocalDate period, BigDecimal initialBalance,
        BigDecimal debits, BigDecimal credits, BigDecimal finalBalance, Instant closedAt) {
}
