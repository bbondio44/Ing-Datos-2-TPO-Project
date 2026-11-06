package ar.edu.uade.logistica.common;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/prueba");

    @Test
    void conflictoDeNegocioMuestraSuMotivo() {
        var respuesta = handler.conflict(new ConflictException("La tarifa no está vigente"), request);

        assertEquals("La tarifa no está vigente", respuesta.getBody().mensaje());
    }

    @Test
    void reglaDeLaBaseMuestraElMensajeSinDetallesTecnicos() {
        SQLException causa = new SQLException("ERROR: Pago excede saldo pendiente de la factura\n  Where: PL/pgSQL function", "P0001");

        var respuesta = handler.conflict(new UncategorizedSQLException("pago", "SELECT 1", causa), request);

        assertEquals("Pago excede saldo pendiente de la factura", respuesta.getBody().mensaje());
    }

    @Test
    void otroErrorDeBaseNoExponeDetalles() {
        var respuesta = handler.conflict(new DataIntegrityViolationException("duplicate key cliente_cuit_key"), request);

        assertTrue(respuesta.getBody().mensaje().startsWith("La operación entra en conflicto"));
    }
}
