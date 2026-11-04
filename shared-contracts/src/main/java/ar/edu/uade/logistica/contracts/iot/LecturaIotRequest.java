package ar.edu.uade.logistica.contracts.iot;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Lectura que manda un sensor. Las mediciones y el GPS son opcionales. */
public record LecturaIotRequest(
        @NotNull UUID idLectura,
        @NotNull UUID idSensor,
        @NotNull Instant timestamp,
        @DecimalMin("-60") @DecimalMax("100") Double temperatura,
        @DecimalMin("0") @DecimalMax("100") Double humedad,
        @DecimalMin("0") Double vibracion,
        @DecimalMin("0") @DecimalMax("100") Double bateria,
        @DecimalMin("-90") @DecimalMax("90") Double latitud,
        @DecimalMin("-180") @DecimalMax("180") Double longitud) {

    private static final Duration TOLERANCIA_RELOJ = Duration.ofMinutes(5);

    public boolean tieneGps() {
        return latitud != null && longitud != null;
    }

    @AssertTrue(message = "Latitud y longitud deben venir juntas")
    public boolean isGpsCompleto() {
        return (latitud == null) == (longitud == null);
    }

    @AssertTrue(message = "La lectura debe traer al menos una medición o posición")
    public boolean isConDatos() {
        return temperatura != null || humedad != null || vibracion != null || bateria != null || tieneGps();
    }

    @AssertTrue(message = "La fecha de la lectura no puede estar en el futuro")
    public boolean isFechaValida() {
        return timestamp == null || !timestamp.isAfter(Instant.now().plus(TOLERANCIA_RELOJ));
    }
}
