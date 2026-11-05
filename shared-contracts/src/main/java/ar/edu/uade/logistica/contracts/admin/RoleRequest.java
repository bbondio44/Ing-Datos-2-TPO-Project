package ar.edu.uade.logistica.contracts.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RoleRequest(
        @NotBlank String description,
        @Min(0) @Max(100) int level,
        @NotNull Set<String> permissions) {
}
