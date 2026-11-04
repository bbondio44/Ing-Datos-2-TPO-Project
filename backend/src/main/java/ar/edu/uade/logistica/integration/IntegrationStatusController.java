package ar.edu.uade.logistica.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.datastax.oss.driver.api.core.CqlSession;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integracion")
public class IntegrationStatusController {
    private static final Logger log = LoggerFactory.getLogger(IntegrationStatusController.class);
    private static final String MONGO_COLLECTION = "eventos_logisticos";
    private static final String CASSANDRA_TABLE = "ultima_lectura_por_sensor";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<Driver> neo4jDriver;
    private final ObjectProvider<MongoDatabase> mongoDatabase;
    private final ObjectProvider<CqlSession> cqlSession;

    public IntegrationStatusController(JdbcTemplate jdbcTemplate,
                                       ObjectProvider<Driver> neo4jDriver,
                                       ObjectProvider<MongoDatabase> mongoDatabase,
                                       ObjectProvider<CqlSession> cqlSession) {
        this.jdbcTemplate = jdbcTemplate;
        this.neo4jDriver = neo4jDriver;
        this.mongoDatabase = mongoDatabase;
        this.cqlSession = cqlSession;
    }

    @GetMapping("/estado")
    public Map<String, Object> status() {
        Map<String, Object> stores = new LinkedHashMap<>();
        stores.put("postgresql", store("OPERATIVA", jdbcTemplate.queryForObject("select current_database()", String.class)));
        stores.put("mongodb", mongoStore());
        stores.put("cassandra", cassandraStore());
        stores.put("neo4j", neo4jStore());
        return Map.of("checkedAt", Instant.now(), "stores", stores);
    }

    private Map<String, Object> mongoStore() {
        try {
            MongoDatabase database = mongoDatabase.getIfAvailable();
            if (database == null) return store("NO_CONFIGURADA", null);
            // Mongo no falla al leer una colección inexistente: hay que comprobar que exista.
            List<String> collections = database.listCollectionNames().into(new ArrayList<>());
            if (!collections.contains(MONGO_COLLECTION)) {
                return store("NO_DISPONIBLE", "Falta la colección " + MONGO_COLLECTION);
            }
            Document document = database.getCollection(MONGO_COLLECTION).find().first();
            return store("OPERATIVA", readDetail(MONGO_COLLECTION, document != null));
        } catch (RuntimeException exception) {
            log.warn("Falló la lectura de verificación en MongoDB", exception);
            return store("NO_DISPONIBLE", "No se pudo leer MongoDB");
        }
    }

    private Map<String, Object> cassandraStore() {
        try {
            // getIfAvailable crea la sesión diferida; si Cassandra no responde, lanza una excepción.
            CqlSession session = cqlSession.getIfAvailable();
            if (session == null) return store("NO_CONFIGURADA", null);
            boolean hasRows = session.execute("SELECT id_sensor FROM " + CASSANDRA_TABLE + " LIMIT 1").one() != null;
            return store("OPERATIVA", readDetail(CASSANDRA_TABLE, hasRows));
        } catch (RuntimeException exception) {
            log.warn("Falló la lectura de verificación en Cassandra", exception);
            return store("NO_DISPONIBLE", "No se pudo leer Cassandra");
        }
    }

    private Map<String, Object> neo4jStore() {
        Driver driver = neo4jDriver.getIfAvailable();
        if (driver == null) return store("NO_CONFIGURADA", null);
        try {
            driver.verifyConnectivity();
            return store("OPERATIVA", "Conexión Bolt verificada");
        } catch (RuntimeException exception) {
            return store("NO_DISPONIBLE", "No se pudo conectar con Neo4j");
        }
    }

    private String readDetail(String source, boolean hasData) {
        return hasData ? "Lectura verificada en " + source : "Lectura verificada en " + source + " (sin datos)";
    }

    private Map<String, Object> store(String state, String detail) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("estado", state);
        value.put("detalle", detail);
        return value;
    }
}
