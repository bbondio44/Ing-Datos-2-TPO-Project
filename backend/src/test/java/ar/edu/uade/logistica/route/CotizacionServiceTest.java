package ar.edu.uade.logistica.route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ar.edu.uade.logistica.common.BadRequestException;
import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.contracts.rutas.PuntoRutaResponse;
import ar.edu.uade.logistica.contracts.rutas.RutaResponse;
import ar.edu.uade.logistica.contracts.rutas.TramoRutaResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CotizacionServiceTest {
    private static final PuntoRutaResponse A = new PuntoRutaResponse("A", "A", "AR", "CIUDAD", 0, 0);
    private static final PuntoRutaResponse B = new PuntoRutaResponse("B", "B", "AR", "PUERTO", 0, 0);
    private static final PuntoRutaResponse C = new PuntoRutaResponse("C", "C", "ES", "PUERTO", 0, 0);
    private static final Map<String, BigDecimal> PRECIOS = Map.of(
            "TERRESTRE", new BigDecimal("15"), "MARITIMO", new BigDecimal("3"));

    private static RutaResponse rutaMixta() {
        return new RutaResponse("A", "C", 1100, List.of(
                new TramoRutaResponse(A, B, "TERRESTRE", 100), new TramoRutaResponse(B, C, "MARITIMO", 1000)));
    }

    @Test
    void cadaTramoCuestaKmPorVolumenPorPrecioDeSuTransporte() {
        var cotizacion = CotizacionService.cotizar(rutaMixta(), new BigDecimal("10"), PRECIOS);

        assertThat(cotizacion.tramos().get(0).costo()).isEqualByComparingTo("15000");
        assertThat(cotizacion.tramos().get(1).costo()).isEqualByComparingTo("30000");
        assertThat(cotizacion.total()).isEqualByComparingTo("45000");
        assertThat(cotizacion.km()).isEqualTo(1100);
    }

    @Test
    void elVolumenConDecimalesRedondeaACentavos() {
        var cotizacion = CotizacionService.cotizar(rutaMixta(), new BigDecimal("0.333"), PRECIOS);

        assertThat(cotizacion.tramos().get(0).costo()).isEqualByComparingTo("499.50");
        assertThat(cotizacion.total()).isEqualByComparingTo("1498.50");
    }

    @Test
    void sinTarifaParaUnTransporteNoSeCotiza() {
        var aereo = new RutaResponse("A", "B", 100, List.of(new TramoRutaResponse(A, B, "AEREO", 100)));

        assertThatThrownBy(() -> CotizacionService.cotizar(aereo, BigDecimal.ONE, PRECIOS))
                .isInstanceOf(ConflictException.class).hasMessageContaining("AEREO");
    }

    @Test
    void elVolumenTieneQueSerMayorACero() {
        var service = new CotizacionService(null, null);

        assertThatThrownBy(() -> service.cotizar("A", "B", BigDecimal.ZERO, false))
                .isInstanceOf(BadRequestException.class);
    }
}
