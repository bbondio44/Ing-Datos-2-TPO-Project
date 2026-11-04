package ar.edu.uade.logistica.contracts.logistics;

import java.time.Instant;
import java.util.UUID;

public record SensorResponse(UUID id, String type, String manufacturer,
                             Instant installationDate, String state) {
}
