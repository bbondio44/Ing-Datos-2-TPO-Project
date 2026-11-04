package ar.edu.uade.logistica.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.cql.Row;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IntegrationStatusControllerTest {
    private static final String COLLECTION = "eventos_logisticos";

    @Mock JdbcTemplate jdbcTemplate;
    @Mock ObjectProvider<Driver> neo4jDriver;
    @Mock ObjectProvider<MongoDatabase> mongoProvider;
    @Mock ObjectProvider<CqlSession> cassandraProvider;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MongoDatabase database;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) CqlSession session;

    IntegrationStatusController controller;

    @BeforeEach
    void setUp() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("logistica");
        controller = new IntegrationStatusController(jdbcTemplate, neo4jDriver, mongoProvider, cassandraProvider);
    }

    @Test
    void reportsNotConfiguredWhenTheAdaptersAreDisabled() {
        assertThat(store("mongodb")).containsEntry("estado", "NO_CONFIGURADA");
        assertThat(store("cassandra")).containsEntry("estado", "NO_CONFIGURADA");
    }

    @Test
    void reportsOperationalAfterARealReadOnBothStores() {
        mongoWithCollections(COLLECTION);
        when(database.getCollection(COLLECTION).find().first()).thenReturn(new Document("_id", "evento"));
        when(cassandraProvider.getIfAvailable()).thenReturn(session);
        when(session.execute(anyString()).one()).thenReturn(mock(Row.class));

        assertThat(store("mongodb"))
                .containsEntry("estado", "OPERATIVA")
                .containsEntry("detalle", "Lectura verificada en eventos_logisticos");
        assertThat(store("cassandra"))
                .containsEntry("estado", "OPERATIVA")
                .containsEntry("detalle", "Lectura verificada en ultima_lectura_por_sensor");
    }

    @Test
    void anEmptySchemaIsOperationalButSaysItHasNoData() {
        mongoWithCollections(COLLECTION);
        when(database.getCollection(COLLECTION).find().first()).thenReturn(null);
        when(cassandraProvider.getIfAvailable()).thenReturn(session);
        when(session.execute(anyString()).one()).thenReturn(null);

        assertThat(store("mongodb"))
                .containsEntry("estado", "OPERATIVA")
                .containsEntry("detalle", "Lectura verificada en eventos_logisticos (sin datos)");
        assertThat(store("cassandra"))
                .containsEntry("estado", "OPERATIVA")
                .containsEntry("detalle", "Lectura verificada en ultima_lectura_por_sensor (sin datos)");
    }

    @Test
    void reportsUnavailableWhenTheMongoSchemaWasNotLoaded() {
        mongoWithCollections("otra_coleccion");

        assertThat(store("mongodb"))
                .containsEntry("estado", "NO_DISPONIBLE")
                .containsEntry("detalle", "Falta la colección eventos_logisticos");
    }

    @Test
    void reportsUnavailableWhenMongoIsDown() {
        when(mongoProvider.getIfAvailable()).thenReturn(database);
        when(database.listCollectionNames().into(any())).thenThrow(new MongoTimeoutException("caída"));

        assertThat(store("mongodb")).containsEntry("estado", "NO_DISPONIBLE");
    }

    @Test
    void reportsUnavailableWhenTheLazyCassandraSessionCannotBeCreated() {
        when(cassandraProvider.getIfAvailable()).thenThrow(new IllegalStateException("sin conexión"));

        assertThat(store("cassandra")).containsEntry("estado", "NO_DISPONIBLE");
    }

    @Test
    void reportsUnavailableWhenTheCassandraReadFails() {
        when(cassandraProvider.getIfAvailable()).thenReturn(session);
        when(session.execute(anyString())).thenThrow(new DriverTimeoutException("timeout"));

        assertThat(store("cassandra")).containsEntry("estado", "NO_DISPONIBLE");
    }

    private void mongoWithCollections(String... names) {
        when(mongoProvider.getIfAvailable()).thenReturn(database);
        when(database.listCollectionNames().into(any())).thenReturn(new ArrayList<>(List.of(names)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> store(String name) {
        Map<String, Object> stores = (Map<String, Object>) controller.status().get("stores");
        return (Map<String, Object>) stores.get(name);
    }
}
