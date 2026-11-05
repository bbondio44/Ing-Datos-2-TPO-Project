package ar.edu.uade.logistica.contracts.reportes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReporteResponse(
        UUID idSolicitud, UUID idCliente, String tipo, String estado, BigDecimal precio,
        Instant fechaSolicitud, Instant fechaGeneracion, UUID idFactura) {
}
