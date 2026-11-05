package ar.edu.uade.logistica.contracts.alertas;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Regla sobre la telemetría. Sin cliente ni sensor, aplica a todos. */
public record ReglaAlertaRequest(
        UUID idCliente,
        UUID idSensor,
        @NotBlank @Size(max = 140) String nombre,
        @NotBlank @Pattern(regexp = "TEMPERATURA|HUMEDAD|BATERIA|INACTIVIDAD|VIBRACION|ANOMALIA_TEMPERATURA") String tipoRegla,
        @NotBlank @Pattern(regexp = ">|>=|<|<=") String operador,
        Double umbral,
        @Positive Integer minutosInactividad,
        @NotBlank @Pattern(regexp = "BAJA|MEDIA|ALTA|CRITICA") String severidad,
        @NotNull Boolean activa) {

    @AssertTrue(message = "INACTIVIDAD usa minutos; el resto de las reglas usa umbral")
    public boolean isUmbralCoherente() {
        return "INACTIVIDAD".equals(tipoRegla)
                ? minutosInactividad != null && umbral == null
                : umbral != null && minutosInactividad == null;
    }
}
