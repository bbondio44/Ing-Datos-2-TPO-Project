package ar.edu.uade.logistica.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

/** Proceso periódico del enunciado: el primer día de cada mes cierra el mes anterior de todas las cuentas. */
@Component
@ConditionalOnProperty(name = "app.cierre.auto-enabled", havingValue = "true")
public class MonthlyCloseJob {
    private static final Logger log = LoggerFactory.getLogger(MonthlyCloseJob.class);
    private final FinanceService financeService;

    public MonthlyCloseJob(FinanceService financeService) {
        this.financeService = financeService;
    }

    @Scheduled(cron = "${app.cierre.cron:0 0 3 1 * *}", zone = "UTC")
    public void run() {
        LocalDate period = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(1);
        var result = financeService.closeAllAccounts(period);
        log.info("Cierre mensual {}: cerradas={}, salteadas={}", period, result.closed(), result.skipped());
    }
}
