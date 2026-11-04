package ar.edu.uade.logistica.integration;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integracion")
public class IntegrationStatusController {
    private final JdbcTemplate jdbcTemplate;

    public IntegrationStatusController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/estado")
    public Map<String, Object> status() {
        Map<String, Object> stores = new LinkedHashMap<>();
        stores.put("postgresql", store("OPERATIVA", jdbcTemplate.queryForObject("select current_database()", String.class)));
        stores.put("mongodb", store("NO_CONFIGURADA", null));
        stores.put("cassandra", store("NO_CONFIGURADA", null));
        stores.put("neo4j", store("NO_CONFIGURADA", null));
        return Map.of("checkedAt", Instant.now(), "stores", stores);
    }

    private Map<String, Object> store(String state, String detail) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("estado", state);
        value.put("detalle", detail);
        return value;
    }
}

