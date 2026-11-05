package ar.edu.uade.logistica.contracts.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UserRequest(
        UUID clientId,
        UUID supplierId,
        UUID operatorId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        @Size(min = 12, message = "La contraseña debe tener al menos 12 caracteres") String password,
        @NotEmpty Set<UUID> roleIds) {
}
