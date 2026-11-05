package ar.edu.uade.logistica.contracts.mensajeria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ParticipanteRequest(
        @NotBlank @Pattern(regexp = "CLIENTE|PROVEEDOR|OPERADOR") String tipo,
        @NotNull UUID idEntidad) {
}
