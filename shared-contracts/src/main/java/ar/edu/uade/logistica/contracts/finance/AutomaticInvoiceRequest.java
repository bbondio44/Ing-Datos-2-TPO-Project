package ar.edu.uade.logistica.contracts.finance;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AutomaticInvoiceRequest(@NotNull UUID tariffId) {
}
