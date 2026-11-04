package ar.edu.uade.logistica.contracts.master;

import java.util.UUID;

public record VehicleResponse(
        UUID id,
        UUID operatorId,
        String operatorName,
        String type,
        String identification,
        String state) {
}
