package ar.edu.uade.logistica.iot;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LecturaIotRequestValidationTest {
    private static final Instant AHORA = Instant.now();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAReadingWithoutGps() {
        assertThat(errores(lectura(AHORA, 6.2, 80.0, null, null))).isEmpty();
    }

    @Test
    void rejectsBatteryOutOfRange() {
        assertThat(errores(lectura(AHORA, 6.2, 120.0, null, null))).contains("bateria");
    }

    @Test
    void rejectsLatitudeWithoutLongitude() {
        assertThat(errores(lectura(AHORA, 6.2, null, -34.6, null))).contains("gpsCompleto");
    }

    @Test
    void rejectsAReadingWithoutData() {
        assertThat(errores(lectura(AHORA, null, null, null, null))).contains("conDatos");
    }

    @Test
    void rejectsAReadingFromTheFuture() {
        assertThat(errores(lectura(AHORA.plus(Duration.ofHours(1)), 6.2, null, null, null))).contains("fechaValida");
    }

    @Test
    void acceptsALateReading() {
        assertThat(errores(lectura(AHORA.minus(Duration.ofDays(3)), 6.2, null, null, null))).isEmpty();
    }

    private Set<String> errores(LecturaIotRequest lectura) {
        return validator.validate(lectura).stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private LecturaIotRequest lectura(Instant instante, Double temperatura, Double bateria, Double latitud, Double longitud) {
        return new LecturaIotRequest(UUID.randomUUID(), UUID.randomUUID(), instante,
                temperatura, null, null, bateria, latitud, longitud);
    }
}
