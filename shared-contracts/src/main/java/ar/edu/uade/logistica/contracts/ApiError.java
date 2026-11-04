package ar.edu.uade.logistica.contracts;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String codigo,
        String mensaje,
        String correlationId,
        Map<String, String> campos) {
}

