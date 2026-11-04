package ar.edu.uade.logistica.integration.cassandra;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnProperty(prefix = "app.cassandra", name = "enabled", havingValue = "true")
public class CassandraConfiguration {
    /**
     * CqlSession conecta al crearse y falla si Cassandra no responde. Con @Lazy se crea en el primer
     * uso, así una demora de Cassandra no impide arrancar el backend. Si falla, se reintenta en el
     * siguiente uso, porque Spring no guarda un bean que no se pudo crear.
     */
    @Bean(destroyMethod = "close")
    @Lazy
    CqlSession cqlSession(@Value("${app.cassandra.contact-points}") String contactPoints,
                          @Value("${app.cassandra.local-datacenter}") String localDatacenter,
                          @Value("${app.cassandra.keyspace}") String keyspace) {
        return CqlSession.builder()
                .addContactPoints(parseContactPoints(contactPoints))
                .withLocalDatacenter(localDatacenter)
                .withKeyspace(keyspace)
                .build();
    }

    /** Convierte "host:puerto,host:puerto" en direcciones para el driver. */
    static List<InetSocketAddress> parseContactPoints(String contactPoints) {
        return Arrays.stream(contactPoints.split(","))
                .map(String::trim)
                .filter(point -> !point.isEmpty())
                .map(CassandraConfiguration::parseContactPoint)
                .toList();
    }

    private static InetSocketAddress parseContactPoint(String point) {
        int separator = point.lastIndexOf(':');
        if (separator <= 0 || separator == point.length() - 1) {
            throw new IllegalArgumentException("Contact point inválido, se espera host:puerto: " + point);
        }
        return new InetSocketAddress(point.substring(0, separator), Integer.parseInt(point.substring(separator + 1)));
    }
}
