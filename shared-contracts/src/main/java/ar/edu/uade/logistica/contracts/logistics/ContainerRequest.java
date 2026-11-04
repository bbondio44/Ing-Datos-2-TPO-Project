package ar.edu.uade.logistica.contracts.logistics;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ContainerRequest(
        @NotBlank @Size(max = 60) String internationalCode,
        @NotBlank @Size(max = 60) String type,
        @NotNull @DecimalMin(value = "0.001") BigDecimal capacity,
        @NotBlank @Pattern(regexp = "DISPONIBLE|EN_USO|MANTENIMIENTO|INACTIVO") String state) {
}
