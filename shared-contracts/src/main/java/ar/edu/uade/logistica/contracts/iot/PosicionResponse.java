package ar.edu.uade.logistica.contracts.iot;

import java.time.Instant;
import java.util.UUID;

public record PosicionResponse(UUID idLectura, UUID idSensor, Instant fechaHora, Double latitud, Double longitud) {
}
