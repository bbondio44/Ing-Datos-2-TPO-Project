package ar.edu.uade.logistica.contracts.auth;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        Instant startedAt,
        Instant expiresAt,
        Instant revokedAt,
        String state) {
}

