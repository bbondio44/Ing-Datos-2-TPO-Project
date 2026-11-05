package ar.edu.uade.logistica.contracts.finance;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CloseAccountRequest(@NotNull LocalDate period) {
}
