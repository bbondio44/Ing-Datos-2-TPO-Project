package ar.edu.uade.logistica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Duration sessionTtl) {
    public AppProperties {
        if (sessionTtl == null || sessionTtl.isNegative() || sessionTtl.isZero()) {
            sessionTtl = Duration.ofHours(8);
        }
    }
}

