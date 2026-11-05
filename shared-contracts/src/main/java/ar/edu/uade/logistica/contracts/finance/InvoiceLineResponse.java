package ar.edu.uade.logistica.contracts.finance;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineResponse(
        UUID id, String concept, BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal, UUID routeLegId) {
}
