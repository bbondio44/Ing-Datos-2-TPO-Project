package ar.edu.uade.logistica.contracts.logistics;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShipmentRequest(
        @NotNull UUID clientId,
        @NotBlank @Size(max = 180) String origin,
        @NotBlank @Size(max = 180) String destination,
        @NotBlank @Size(max = 80) String originCountry,
        @NotBlank @Size(max = 80) String destinationCountry,
        Instant estimatedDelivery,
        @NotBlank @Pattern(regexp = "BAJA|NORMAL|ALTA|URGENTE") String priority,
        @NotNull @DecimalMin("0.0") BigDecimal volumeM3) {
}
