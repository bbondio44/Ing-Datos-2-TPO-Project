package ar.edu.uade.logistica.contracts.finance;

import java.util.List;
import java.util.UUID;

public record AutomaticInvoiceResponse(int processed, int created, List<UUID> invoiceIds) {
}
