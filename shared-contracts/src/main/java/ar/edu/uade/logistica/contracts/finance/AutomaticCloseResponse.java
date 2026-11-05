package ar.edu.uade.logistica.contracts.finance;

import java.time.LocalDate;

public record AutomaticCloseResponse(LocalDate period, int closed, int skipped) {
}
