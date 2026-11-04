package ar.edu.uade.logistica.integration.mongo;

import java.util.concurrent.TimeUnit;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "true")
public class MongoConfiguration {
    private static final long SERVER_SELECTION_TIMEOUT_SECONDS = 3;

    @Bean(destroyMethod = "close")
    MongoClient mongoClient(@Value("${app.mongo.uri}") String uri) {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                // Por defecto espera 30 s: con Mongo caído dejaría colgado el endpoint de estado.
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(
                        SERVER_SELECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    MongoDatabase mongoDatabase(MongoClient client, @Value("${app.mongo.database}") String database) {
        return client.getDatabase(database);
    }
}
