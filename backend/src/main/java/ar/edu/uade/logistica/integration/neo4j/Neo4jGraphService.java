package ar.edu.uade.logistica.integration.neo4j;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.security.AuthenticatedUser;

@Service
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class Neo4jGraphService {
    private static final String CLIENT_GRAPH_QUERY = """
            MATCH (c:Cliente {id_cliente: $clientId})-[:SOLICITA]->(e:Envio)
            OPTIONAL MATCH (e)-[comp:COMPUESTO_POR]->(t:Tramo)
            OPTIONAL MATCH (t)-[:ORIGEN]->(origen:Ubicacion)
            OPTIONAL MATCH (t)-[:DESTINO]->(destino:Ubicacion)
            OPTIONAL MATCH (t)-[:EJECUTADO_POR]->(v:Vehiculo)-[:PERTENECE_A]->(o:Operador)
            OPTIONAL MATCH (e)-[:ASIGNADO_A]->(contenedor:Contenedor)
            OPTIONAL MATCH (e)-[:REGISTRA]->(incidenteEnvio:Incidente)
            OPTIONAL MATCH (t)-[:REGISTRA]->(incidenteTramo:Incidente)
            RETURN properties(c) AS cliente, properties(e) AS envio,
                   collect(DISTINCT CASE WHEN t IS NULL THEN null ELSE {
                       tramo: properties(t), orden: comp.orden,
                       origen: properties(origen), destino: properties(destino),
                       vehiculo: properties(v), operador: properties(o)
                   } END) AS tramos,
                   collect(DISTINCT CASE WHEN contenedor IS NULL THEN null ELSE properties(contenedor) END) AS contenedores,
                   reduce(incidentes = [], incidente IN (
                       collect(DISTINCT CASE WHEN incidenteEnvio IS NULL THEN null ELSE properties(incidenteEnvio) END)
                           + collect(DISTINCT CASE WHEN incidenteTramo IS NULL THEN null ELSE properties(incidenteTramo) END)
                   ) | CASE WHEN incidente IS NULL OR incidente IN incidentes
                           THEN incidentes ELSE incidentes + incidente END) AS incidentes
            ORDER BY e.id_envio
            """;

    private final Driver driver;
    private final String database;

    public Neo4jGraphService(Driver driver, @Value("${app.neo4j.database:neo4j}") String database) {
        this.driver = driver;
        this.database = database;
    }

    public List<Map<String, Object>> graphForClient(String clientId, AuthenticatedUser user) {
        String authenticatedClientId = user.clientId() == null ? null : user.clientId().toString();
        if (!user.permissions().contains("ENVIOS_LEER_TODOS") && !clientId.equals(authenticatedClientId)) {
            throw new NotFoundException("El cliente no existe");
        }

        try (var session = driver.session(SessionConfig.forDatabase(database))) {
            return session.executeRead(transaction -> transaction
                    .run(CLIENT_GRAPH_QUERY, Map.of("clientId", clientId))
                    .list(record -> record.asMap(value -> value.asObject())));
        }
    }
}