package ar.edu.uade.logistica.contracts.logistics;

import java.time.Instant;
import java.util.UUID;

public record ContainerAssignmentResponse(
        UUID shipmentId, UUID containerId, String containerCode, String containerType,
        Instant startAt, Instant endAt, boolean active) {
}
