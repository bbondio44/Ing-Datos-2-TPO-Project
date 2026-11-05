package ar.edu.uade.logistica.alertas;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AlertaServiceTest {
    @Test
    void comparesAgainstTheThresholdWithTheRuleOperator() {
        assertThat(AlertaService.cumple(8.1, ">", 8)).isTrue();
        assertThat(AlertaService.cumple(8.0, ">", 8)).isFalse();
        assertThat(AlertaService.cumple(8.0, ">=", 8)).isTrue();
        assertThat(AlertaService.cumple(14.9, "<", 15)).isTrue();
        assertThat(AlertaService.cumple(15.0, "<=", 15)).isTrue();
    }

    @Test
    void calculatesTheZScoreAgainstTheHistory() {
        List<Double> historico = List.of(4.0, 6.0, 4.0, 6.0, 4.0, 6.0, 4.0, 6.0, 4.0, 6.0);

        assertThat(AlertaService.puntajeZ(9.0, historico).getAsDouble()).isCloseTo(4.0, within(0.001));
    }

    @Test
    void doesNotDetectAnomaliesWithoutEnoughHistory() {
        assertThat(AlertaService.puntajeZ(30.0, List.of(4.0, 5.0, 6.0))).isEmpty();
    }

    @Test
    void doesNotDetectAnomaliesWhenTheHistoryHasNoVariation() {
        assertThat(AlertaService.puntajeZ(30.0, List.of(5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0))).isEmpty();
    }

    @Test
    void reprocessingTheSameViolationProducesTheSameIds() {
        ReglaAlertaResponse regla = regla("ALTA");
        UUID sensor = UUID.randomUUID();
        UUID lectura = UUID.randomUUID();
        Instant instante = Instant.parse("2026-10-21T12:00:00Z");

        AlertaNueva primera = alerta(regla, sensor, lectura, instante, UUID.randomUUID());
        AlertaNueva reintento = alerta(regla, sensor, lectura, instante, primera.idEnvio());

        assertThat(reintento.idAlerta()).isEqualTo(primera.idAlerta());
        assertThat(reintento.idEvento()).isEqualTo(primera.idEvento());
        assertThat(reintento.idIncidente()).isEqualTo(primera.idIncidente());
    }

    @Test
    void onlyHighSeverityAlertsWithAShipmentCreateIncidents() {
        UUID sensor = UUID.randomUUID();
        Instant instante = Instant.now();

        assertThat(alerta(regla("ALTA"), sensor, null, instante, UUID.randomUUID()).generaIncidente()).isTrue();
        assertThat(alerta(regla("CRITICA"), sensor, null, instante, UUID.randomUUID()).generaIncidente()).isTrue();
        assertThat(alerta(regla("MEDIA"), sensor, null, instante, UUID.randomUUID()).generaIncidente()).isFalse();
        assertThat(alerta(regla("ALTA"), sensor, null, instante, null).generaIncidente()).isFalse();
    }

    private ReglaAlertaResponse regla(String severidad) {
        return new ReglaAlertaResponse(UUID.randomUUID(), null, null, "Regla", "TEMPERATURA", ">", 8.0, null, severidad, true);
    }

    private AlertaNueva alerta(ReglaAlertaResponse regla, UUID sensor, UUID lectura, Instant instante, UUID envio) {
        return new AlertaNueva(regla, sensor, UUID.randomUUID(), envio, UUID.randomUUID(), lectura,
                9.0, 8.0, "mensaje", instante, Instant.now());
    }
}
