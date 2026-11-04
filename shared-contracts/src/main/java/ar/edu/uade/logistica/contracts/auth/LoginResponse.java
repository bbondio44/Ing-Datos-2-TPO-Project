package ar.edu.uade.logistica.contracts.auth;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        String token,
        Instant expiresAt,
        UUID userId,
        UUID clientId,
        String displayName,
        Set<String> roles,
        Set<String> permissions) {
}

