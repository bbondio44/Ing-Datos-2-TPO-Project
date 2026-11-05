package ar.edu.uade.logistica.contracts.route;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RouteSupplierRequest(
        @NotNull UUID supplierId,
        @NotBlank String service,
        @NotNull @DecimalMin("0.00") BigDecimal cost) {
}
