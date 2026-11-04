package ar.edu.uade.logistica.contracts.logistics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SensorRequest(
        @NotBlank @Size(max = 40) String type,
        @Size(max = 100) String manufacturer,
        Instant installationDate,
        @NotBlank @Pattern(regexp = "ACTIVO|INACTIVO|MANTENIMIENTO|FALLA") String state) {
}
