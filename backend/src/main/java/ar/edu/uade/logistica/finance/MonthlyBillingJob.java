package ar.edu.uade.logistica.finance;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** El día 1 factura los envíos entregados el mes anterior, antes del cierre, y el día 10 los cobra. */
@Component
@ConditionalOnProperty(name = "app.facturacion-mensual.enabled", havingValue = "true")
public class MonthlyBillingJob {
    private static final Logger log = LoggerFactory.getLogger(MonthlyBillingJob.class);
    private final FacturacionMensualService service;

    public MonthlyBillingJob(FacturacionMensualService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.facturacion-mensual.cron:0 0 2 1 * *}", zone = "UTC")
    public void facturar() {
        LocalDate mes = LocalDate.now(ZoneOffset.UTC).minusMonths(1);
        var resultado = service.facturar(mes);
        log.info("Facturación mensual {}: envíos={}, facturas={}", mes, resultado.processed(), resultado.created());
    }

    @Scheduled(cron = "${app.cobro-mensual.cron:0 0 4 10 * *}", zone = "UTC")
    public void cobrar() {
        LocalDate mes = LocalDate.now(ZoneOffset.UTC).minusMonths(1);
        var resultado = service.cobrar(mes);
        log.info("Cobro mensual {}: facturas={}, total={}", mes, resultado.collected(), resultado.total());
    }
}
