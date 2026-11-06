package ar.edu.uade.logistica.desktop.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormularioCuitTest {
    @Test
    void calculaElDigitoDeCuitsQueLaBaseAcepta() {
        assertEquals(3, Formulario.digitoVerificador("3075566677"));
        assertEquals(0, Formulario.digitoVerificador("3071012345"));
        assertEquals(3, Formulario.digitoVerificador("3050000000"));
        assertEquals(1, Formulario.digitoVerificador("3050000001"));
    }

    @Test
    void avisaCuandoElCuitEstaMalEscrito() {
        assertNull(Formulario.errorCuit(""));
        assertNull(Formulario.errorCuit("30755666773"));
        assertNull(Formulario.errorCuit(Formulario.generarCuit()));
        assertNotNull(Formulario.errorCuit("123"));
        assertNotNull(Formulario.errorCuit("30-75566677-3"));
        assertNotNull(Formulario.errorCuit("30710123451"));
    }

    @Test
    void avisaCuandoElEmailEstaMalEscrito() {
        assertNull(Formulario.errorEmail(""));
        assertNull(Formulario.errorEmail("ops@sur.test"));
        assertNull(Formulario.errorEmail(" ana.perez@grupo4.local "));
        assertNotNull(Formulario.errorEmail("sinarroba"));
        assertNotNull(Formulario.errorEmail("ana@"));
        assertNotNull(Formulario.errorEmail("@dominio.com"));
        assertNotNull(Formulario.errorEmail("ana perez@dominio.com"));
        assertNotNull(Formulario.errorEmail("ana@dominio..com"));
    }

    @Test
    void armaElCodigoDeUbicacionConPaisYNombre() {
        assertEquals("ARROS", Formulario.codigoUbicacion("Argentina", "Rosario", java.util.Set.of()));
        assertEquals("ESMAL", Formulario.codigoUbicacion("España", "Málaga", java.util.Set.of()));
        assertEquals("ARROS2", Formulario.codigoUbicacion("Argentina", "Rosario", java.util.Set.of("ARROS")));
        assertEquals("ARROS3", Formulario.codigoUbicacion("Argentina", "Rosario", java.util.Set.of("ARROS", "ARROS2")));
    }

    @Test
    void generaCuitsDeOnceDigitosConVerificadorCorrecto() {
        for (int i = 0; i < 1000; i++) {
            String cuit = Formulario.generarCuit();
            assertTrue(cuit.matches("30\\d{9}"), cuit);
            assertEquals(cuit.charAt(10) - '0', Formulario.digitoVerificador(cuit.substring(0, 10)));
        }
    }
}
