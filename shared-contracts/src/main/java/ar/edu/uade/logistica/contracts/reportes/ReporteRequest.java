package ar.edu.uade.logistica.contracts.reportes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

/** idCliente es el titular del reporte. Un usuario de cliente no lo manda: se usa el suyo. */
public record ReporteRequest(
        @NotBlank @Pattern(regexp = "RANKING_RUTAS|RANKING_CLIENTES|DESEMPENO|PREDICTIVO") String tipo,
        UUID idCliente,
        LocalDate desde,
        LocalDate hasta) {
}
