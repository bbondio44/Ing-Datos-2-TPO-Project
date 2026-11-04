package ar.edu.uade.logistica.contracts.iot;

import java.time.Instant;
import java.util.UUID;

/** conexion: ONLINE, OFFLINE o SIN_DATOS (el sensor nunca reportó). */
public record SensorEstadoResponse(
        UUID idSensor, String tipo, UUID idContenedor, String conexion,
        Double bateria, Instant ultimaSenal, Long minutosSinSenal) {
}
