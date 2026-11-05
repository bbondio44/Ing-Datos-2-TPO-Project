package ar.edu.uade.logistica.contracts.admin;

import jakarta.validation.constraints.NotBlank;

public record UserStateRequest(@NotBlank String state) {
}
