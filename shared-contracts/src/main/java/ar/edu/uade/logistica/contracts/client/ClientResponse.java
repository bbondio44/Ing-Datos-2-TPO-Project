package ar.edu.uade.logistica.contracts.client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String businessName,
        String cuit,
        String email,
        String phone,
        String address,
        String country,
        String state,
        Instant createdAt) {
}
