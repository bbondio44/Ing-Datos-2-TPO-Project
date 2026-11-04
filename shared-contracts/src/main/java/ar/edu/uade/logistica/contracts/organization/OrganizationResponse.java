package ar.edu.uade.logistica.contracts.organization;

import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String businessName,
        String cuit,
        String email,
        String phone,
        String address,
        String country,
        String serviceType,
        String state) {
}
