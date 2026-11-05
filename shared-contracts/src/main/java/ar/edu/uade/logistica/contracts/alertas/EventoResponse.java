package ar.edu.uade.logistica.contracts.alertas;

import java.time.Instant;

public record EventoResponse(
        String id, String tipo, Instant fecha, String descripcion, String origen,
        String idAlerta, String idIncidente) {
}
