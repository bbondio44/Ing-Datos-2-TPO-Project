package ar.edu.uade.logistica;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class LogisticaBackendApplication {
    public static void main(String[] args) {
        // D01: todo en UTC. Además, Java en Windows informa America/Buenos_Aires, que Postgres rechaza.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(LogisticaBackendApplication.class, args);
    }
}

