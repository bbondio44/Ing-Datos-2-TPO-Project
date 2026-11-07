package ar.edu.uade.logistica.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ar.edu.uade.logistica.finance.FacturacionMensualService.EnvioFacturable;

import static org.assertj.core.api.Assertions.assertThat;

class FacturacionMensualServiceTest {
    private static EnvioFacturable envio(UUID cliente) {
        return new EnvioFacturable(cliente, UUID.fromString("12345678-0000-4000-8000-000000000000"), "Buenos Aires", "Santa Fe",
                Instant.parse("2026-11-06T12:00:00Z"), BigDecimal.TEN, null);
    }

    @Test
    void juntaLosEnviosDeCadaClienteEnUnaFactura() {
        UUID uno = UUID.randomUUID();
        UUID otro = UUID.randomUUID();

        var porCliente = FacturacionMensualService.agrupar(List.of(envio(uno), envio(uno), envio(otro)));

        assertThat(porCliente).hasSize(2);
        assertThat(porCliente.get(uno)).hasSize(2);
        assertThat(porCliente.get(otro)).hasSize(1);
    }

    @Test
    void elConceptoEmpiezaConElEnvioPorqueEvitaFacturarloDosVeces() {
        assertThat(FacturacionMensualService.concepto(envio(UUID.randomUUID())))
                .isEqualTo("Envío 12345678: Buenos Aires → Santa Fe, entregado 06/11/2026");
    }

    @Test
    void venceElProximoDiaDeCobro() {
        assertThat(FacturacionMensualService.vencimiento(LocalDate.of(2026, 11, 1))).isEqualTo("2026-11-10");
        assertThat(FacturacionMensualService.vencimiento(LocalDate.of(2026, 11, 10))).isEqualTo("2026-11-10");
        assertThat(FacturacionMensualService.vencimiento(LocalDate.of(2026, 11, 11))).isEqualTo("2026-12-10");
    }
}
