package ar.edu.uade.logistica.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.billing.auto-enabled", havingValue = "true")
public class AutomaticBillingJob {
    private static final Logger log = LoggerFactory.getLogger(AutomaticBillingJob.class);
    private final FinanceService financeService;
    private final JdbcTemplate jdbcTemplate;

    public AutomaticBillingJob(FinanceService financeService, JdbcTemplate jdbcTemplate) {
        this.financeService = financeService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${app.billing.cron:0 0 2 * * *}", zone = "${app.billing.zone:America/Buenos_Aires}")
    public void run() {
        List<UUID> tariffs = jdbcTemplate.queryForList("""
                SELECT id_tarifa FROM tp_logistica.tarifa_servicio
                 WHERE activa AND tipo_servicio IN ('TRANSPORTE','SERVICIO')
                   AND criterio_calculo='FIJO' AND moneda='ARS'
                   AND vigencia_desde<=current_date
                   AND (vigencia_hasta IS NULL OR vigencia_hasta>=current_date)
                 ORDER BY vigencia_desde DESC LIMIT 1
                """, UUID.class);
        if (tariffs.isEmpty()) {
            log.warn("Facturación automática omitida: no hay tarifa vigente");
            return;
        }
        var result = financeService.automaticInvoices(tariffs.getFirst());
        log.info("Facturación automática: procesados={}, creados={}", result.processed(), result.created());
    }
}
