package ar.edu.uade.logistica.alertas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** R23: revisa periódicamente la inactividad y la batería de los sensores asignados. */
@Component
@ConditionalOnProperty(name = {"app.mongo.enabled", "app.cassandra.enabled"}, havingValue = "true")
public class AlertaScheduler {
    private static final Logger log = LoggerFactory.getLogger(AlertaScheduler.class);

    private final AlertaService service;

    public AlertaScheduler(AlertaService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.alertas.intervalo-ms:60000}",
            initialDelayString = "${app.alertas.intervalo-ms:60000}")
    public void revisarSensores() {
        try {
            service.evaluarSensoresVigentes();
        } catch (RuntimeException exception) {
            log.warn("No se pudo revisar el estado de los sensores", exception);
        }
    }
}
