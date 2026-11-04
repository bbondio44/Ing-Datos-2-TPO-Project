package ar.edu.uade.logistica.security;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        UUID clientId,
        String email,
        String displayName,
        Set<String> roles,
        Set<String> permissions) implements Principal {

    public AuthenticatedUser {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}

