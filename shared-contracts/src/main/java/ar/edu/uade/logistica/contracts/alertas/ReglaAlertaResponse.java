package ar.edu.uade.logistica.contracts.alertas;

import java.util.UUID;

public record ReglaAlertaResponse(
        UUID idRegla, UUID idCliente, UUID idSensor, String nombre, String tipoRegla, String operador,
        Double umbral, Integer minutosInactividad, String severidad, boolean activa) {
}
