package ar.edu.uade.logistica.contracts.logistics;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record SensorAssignmentRequest(
        @NotNull UUID sensorId, @NotNull UUID containerId, Instant startAt, Instant endAt) {
}
