package ar.edu.uade.logistica.contracts.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
        @NotBlank @Size(max = 160) String businessName,
        @Pattern(regexp = "^\\d{11}$", message = "El CUIT debe tener 11 dígitos") String cuit,
        @Email @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @Size(max = 250) String address,
        @NotBlank @Size(max = 80) String country,
        @Size(max = 100) String serviceType) {
}
