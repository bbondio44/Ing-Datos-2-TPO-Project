package ar.edu.uade.logistica.contracts.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocationRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 80) String country,
        @NotBlank @Pattern(regexp = "PUERTO|AEROPUERTO|DEPOSITO|FRONTERA|OTRO",
                message = "El tipo de ubicación no es válido") String type) {
}
