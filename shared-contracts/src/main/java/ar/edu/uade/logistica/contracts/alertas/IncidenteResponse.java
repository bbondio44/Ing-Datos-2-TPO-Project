package ar.edu.uade.logistica.contracts.alertas;

import java.time.Instant;
import java.util.UUID;

public record IncidenteResponse(
        UUID idIncidente, UUID idEnvio, String tipo, String severidad, String descripcion,
        Instant fecha, String estado) {
}
