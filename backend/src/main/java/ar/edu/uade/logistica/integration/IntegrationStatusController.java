package ar.edu.uade.logistica.integration;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integracion")
public class IntegrationStatusController {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<Driver> neo4jDriver;

    public IntegrationStatusController(JdbcTemplate jdbcTemplate, ObjectProvider<Driver> neo4jDriver) {
        this.jdbcTemplate = jdbcTemplate;
        this.neo4jDriver = neo4jDriver;
    }

    @GetMapping("/estado")
    public Map<String, Object> status() {
        Map<String, Object> stores = new LinkedHashMap<>();
        stores.put("postgresql", store("OPERATIVA", jdbcTemplate.queryForObject("select current_database()", String.class)));
        stores.put("mongodb", store("NO_CONFIGURADA", null));
        stores.put("cassandra", store("NO_CONFIGURADA", null));
        stores.put("neo4j", neo4jStore());
        return Map.of("checkedAt", Instant.now(), "stores", stores);
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

    private Map<String, Object> store(String state, String detail) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("estado", state);
        value.put("detalle", detail);
        return value;
    }
}

