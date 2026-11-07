package ar.edu.uade.logistica.contracts.finance;

import java.math.BigDecimal;

public record MonthlyCollectionResponse(int collected, BigDecimal total, int skippedClosed) {
}
