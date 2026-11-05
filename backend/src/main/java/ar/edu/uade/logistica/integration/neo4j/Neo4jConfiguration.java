package ar.edu.uade.logistica.integration.neo4j;

import java.util.concurrent.TimeUnit;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class Neo4jConfiguration {
    @Bean(destroyMethod = "close")
    Driver neo4jDriver(@Value("${app.neo4j.uri}") String uri,
                       @Value("${app.neo4j.username}") String username,
                       @Value("${app.neo4j.password}") String password) {
        // Por defecto reintenta 30 s: el worker del outbox tiene sus propios reintentos y necesita fallar rápido.
        Config config = Config.builder()
                .withMaxTransactionRetryTime(3, TimeUnit.SECONDS)
                .withConnectionTimeout(3, TimeUnit.SECONDS)
                .build();
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password), config);
    }
}