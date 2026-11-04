package ar.edu.uade.logistica.contracts.logistics;

import java.util.List;

public record ShipmentDetailResponse(
        ShipmentResponse shipment,
        List<ContainerAssignmentResponse> containers,
        List<SensorAssignmentResponse> sensors) {
    public ShipmentDetailResponse {
        containers = List.copyOf(containers);
        sensors = List.copyOf(sensors);
    }
}
