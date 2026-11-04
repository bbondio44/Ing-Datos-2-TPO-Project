package ar.edu.uade.logistica.contracts.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehicleRequest(
        @NotNull UUID operatorId,
        @NotBlank @Pattern(regexp = "CAMION|BARCO|AVION|TREN|OTRO",
                message = "El tipo de vehículo no es válido") String type,
        @NotBlank @Size(max = 80) String identification,
        @NotBlank @Pattern(regexp = "ACTIVO|INACTIVO|MANTENIMIENTO",
                message = "El estado del vehículo no es válido") String state) {
}
