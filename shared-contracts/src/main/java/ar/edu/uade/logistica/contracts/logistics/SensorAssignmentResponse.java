package ar.edu.uade.logistica.contracts.logistics;

import java.time.Instant;
import java.util.UUID;

public record SensorAssignmentResponse(
        UUID assignmentId, UUID sensorId, String sensorType,
        UUID containerId, String containerCode, Instant startAt, Instant endAt, boolean active) {
}
