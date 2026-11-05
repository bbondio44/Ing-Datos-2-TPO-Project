package ar.edu.uade.logistica.contracts.admin;

import java.util.Set;
import java.util.UUID;

public record RoleResponse(UUID id, String description, int level, Set<String> permissions) {
}
