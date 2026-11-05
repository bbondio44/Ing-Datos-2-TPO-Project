package ar.edu.uade.logistica.contracts.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TariffResponse(
        UUID id, String concept, String serviceType, String calculation, BigDecimal price,
        String currency, LocalDate validFrom, LocalDate validUntil, boolean active) {
}
