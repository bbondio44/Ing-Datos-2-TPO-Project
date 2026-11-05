package ar.edu.uade.logistica.contracts.admin;

import java.util.UUID;

public record PermissionResponse(UUID id, String code, String description) {
}
