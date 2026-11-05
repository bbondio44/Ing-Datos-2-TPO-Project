package ar.edu.uade.logistica.contracts.admin;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID clientId,
        UUID supplierId,
        UUID operatorId,
        String firstName,
        String lastName,
        String email,
        String state,
        Instant registeredAt,
        Set<RoleResponse> roles) {
}
