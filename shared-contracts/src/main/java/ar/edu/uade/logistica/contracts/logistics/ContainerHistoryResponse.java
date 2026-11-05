package ar.edu.uade.logistica.contracts.logistics;

import java.time.Instant;
import java.util.UUID;

public record ContainerHistoryResponse(
        UUID shipmentId, String client, String origin, String destination,
        String shipmentState, Instant startAt, Instant endAt, boolean active) {
}
