package ar.edu.uade.logistica.contracts.rutas;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Un envío nuevo con su ruta: origen y destino son códigos de la red de rutas. */
public record ViajeRequest(
        @NotNull UUID clientId,
        @NotBlank String origen,
        @NotBlank String destino,
        boolean aereo,
        Instant estimatedDelivery,
        @NotBlank @Pattern(regexp = "BAJA|NORMAL|ALTA|URGENTE") String priority,
        @NotNull @DecimalMin(value = "0.001") BigDecimal volumeM3) {
}
