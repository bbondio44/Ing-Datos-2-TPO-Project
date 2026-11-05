package ar.edu.uade.logistica.integration.neo4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.security.AuthenticatedUser;

/** R30: red de un cliente. Una consulta MATCH por tipo de relación, y la aplicación arma el resultado. */
@Service
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class Neo4jGraphService {
    private static final String CLIENTE = """
            MATCH (c:Cliente {id_cliente: $id})
            RETURN c.id_cliente AS id_cliente, c.razon_social AS razon_social, c.pais AS pais
            """;
    private static final String ENVIOS = """
            MATCH (:Cliente {id_cliente: $id})-[:SOLICITA]->(e:Envio)
            RETURN e.id_envio AS id_envio, e.origen AS origen, e.destino AS destino, e.estado AS estado
            ORDER BY e.id_envio
            """;
    private static final String TRAMOS = """
            MATCH (:Cliente {id_cliente: $id})-[:SOLICITA]->(e:Envio)-[r:COMPUESTO_POR]->(t:Tramo)
            RETURN e.id_envio AS id_envio, r.orden AS orden, t.id_tramo AS id_tramo, t.origen AS origen,
                   t.destino AS destino, t.medio_transporte AS medio
            ORDER BY e.id_envio, r.orden
            """;
    private static final String OPERADORES = """
            MATCH (:Cliente {id_cliente: $id})-[:SOLICITA]->(:Envio)-[:COMPUESTO_POR]->(:Tramo)-[:OPERADO_POR]->(o:Operador)
            RETURN DISTINCT o.id_operador AS id_operador, o.razon_social AS razon_social
            ORDER BY o.razon_social
            """;
    private static final String CONTENEDORES = """
            MATCH (:Cliente {id_cliente: $id})-[:SOLICITA]->(e:Envio)-[:ASIGNADO_A]->(k:Contenedor)
            RETURN DISTINCT e.id_envio AS id_envio, k.id_contenedor AS id_contenedor,
                   k.codigo_internacional AS codigo
            ORDER BY e.id_envio
            """;
    private static final String INCIDENTES = """
            MATCH (:Cliente {id_cliente: $id})-[:SOLICITA]->(e:Envio)-[:REGISTRA]->(i:Incidente)
            RETURN e.id_envio AS id_envio, i.id_incidente AS id_incidente, i.tipo AS tipo,
                   i.severidad AS severidad, i.estado AS estado
            ORDER BY e.id_envio
            """;

    private final Driver driver;
    private final String database;

    public Neo4jGraphService(Driver driver, @Value("${app.neo4j.database:neo4j}") String database) {
        this.driver = driver;
        this.database = database;
    }

    public Map<String, Object> graphForClient(String clientId, AuthenticatedUser user) {
        String authenticatedClientId = user.clientId() == null ? null : user.clientId().toString();
        if (!user.permissions().contains("ENVIOS_LEER_TODOS") && !clientId.equals(authenticatedClientId)) {
            throw new NotFoundException("El cliente no existe");
        }
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            List<Map<String, Object>> cliente = consultar(session, CLIENTE, clientId);
            if (cliente.isEmpty()) throw new NotFoundException("El cliente no existe");
            Map<String, Object> red = new LinkedHashMap<>();
            red.put("cliente", cliente.get(0));
            red.put("envios", consultar(session, ENVIOS, clientId));
            red.put("tramos", consultar(session, TRAMOS, clientId));
            red.put("operadores", consultar(session, OPERADORES, clientId));
            red.put("contenedores", consultar(session, CONTENEDORES, clientId));
            red.put("incidentes", consultar(session, INCIDENTES, clientId));
            return red;
        }
    }

    private List<Map<String, Object>> consultar(Session session, String cypher, String clientId) {
        return session.executeRead(transaccion -> transaccion.run(cypher, Map.of("id", clientId))
                .list(registro -> registro.asMap(valor -> valor.asObject())));
    }
}
