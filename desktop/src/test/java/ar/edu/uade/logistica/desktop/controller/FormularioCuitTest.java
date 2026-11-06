package ar.edu.uade.logistica.desktop.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void generaCuitsDeOnceDigitosConVerificadorCorrecto() {
        for (int i = 0; i < 1000; i++) {
            String cuit = Formulario.generarCuit();
            assertTrue(cuit.matches("30\\d{9}"), cuit);
            assertEquals(cuit.charAt(10) - '0', Formulario.digitoVerificador(cuit.substring(0, 10)));
        }
    }
}
