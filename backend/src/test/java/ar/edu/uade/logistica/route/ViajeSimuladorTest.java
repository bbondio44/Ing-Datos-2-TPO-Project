package ar.edu.uade.logistica.route;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ViajeSimuladorTest {
    private static final Instant SALIDA = Instant.parse("2026-11-06T10:00:00Z");
    private static final Duration POR_PUNTO = Duration.ofSeconds(20);

    @Test
    void elAvanceEsProporcionalAlTiempo() {
        assertThat(ViajeSimulador.avance(SALIDA, SALIDA.plusSeconds(5), POR_PUNTO)).isEqualTo(0.25);
    }

    @Test
    void elAvanceNoPasaDelDestino() {
        assertThat(ViajeSimulador.avance(SALIDA, SALIDA.plusSeconds(90), POR_PUNTO)).isEqualTo(1);
    }

    @Test
    void laPosicionQuedaEntreLosDosPuntos() {
        assertThat(ViajeSimulador.entre(-34.6, -31.6, 0.5)).isCloseTo(-33.1, within(0.0001));
        assertThat(ViajeSimulador.entre(-58.4, -60.7, 0)).isEqualTo(-58.4);
        assertThat(ViajeSimulador.entre(-58.4, -60.7, 1)).isCloseTo(-60.7, within(0.0001));
    }
}
