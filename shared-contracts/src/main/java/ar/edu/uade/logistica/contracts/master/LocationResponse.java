package ar.edu.uade.logistica.contracts.master;

import java.util.UUID;

public record LocationResponse(UUID id, String code, String name, String country, String type) {
}
