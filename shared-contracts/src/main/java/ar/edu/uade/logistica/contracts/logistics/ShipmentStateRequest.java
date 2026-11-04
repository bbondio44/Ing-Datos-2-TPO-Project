package ar.edu.uade.logistica.contracts.logistics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public record ShipmentStateRequest(
        @NotBlank @Pattern(regexp = "CREADO|EN_PREPARACION|EN_TRANSITO|DEMORADO|ENTREGADO|CANCELADO")
        String state,
        Instant actualDelivery) {
}
