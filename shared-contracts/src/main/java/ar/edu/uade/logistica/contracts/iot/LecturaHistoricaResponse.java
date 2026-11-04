package ar.edu.uade.logistica.contracts.iot;

import java.time.Instant;
import java.util.UUID;

public record LecturaHistoricaResponse(
        UUID idLectura, UUID idSensor, UUID idContenedor, Instant fechaHora,
        Double temperatura, Double humedad, Double vibracion, Double bateria,
        Double latitud, Double longitud) {
}
