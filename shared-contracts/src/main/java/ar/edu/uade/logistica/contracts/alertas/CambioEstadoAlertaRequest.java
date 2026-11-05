package ar.edu.uade.logistica.contracts.alertas;

import jakarta.validation.constraints.Size;

public record CambioEstadoAlertaRequest(@Size(max = 500) String comentario) {
}
