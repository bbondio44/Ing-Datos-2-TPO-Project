package ar.edu.uade.logistica.analitica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrediccionRiesgosTest {
    @Test
    void sinIncidentesNiHistoricoElRiesgoOperativoEsCero() {
        assertEquals(0.0, PrediccionService.riesgoOperativo(0, 0.0));
    }

    @Test
    void combinaIncidentesAbiertosYTasaDeLaRuta() {
        assertEquals(0.40, PrediccionService.riesgoOperativo(1, 0.5));
    }

    @Test
    void elRiesgoOperativoNoPasaDeUno() {
        assertEquals(1.0, PrediccionService.riesgoOperativo(9, 7.0));
    }

    @Test
    void elRiesgoClimaticoDependeDelMedio() {
        assertEquals(0.50, PrediccionService.riesgoClimatico("MARITIMO"));
        assertEquals(0.35, PrediccionService.riesgoClimatico("AEREO"));
        assertEquals(0.20, PrediccionService.riesgoClimatico("TERRESTRE"));
        assertEquals(0.25, PrediccionService.riesgoClimatico(null));
    }
}
