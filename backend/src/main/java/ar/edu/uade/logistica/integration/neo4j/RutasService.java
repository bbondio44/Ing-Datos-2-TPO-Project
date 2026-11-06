package ar.edu.uade.logistica.integration.neo4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ar.edu.uade.logistica.common.BadRequestException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.rutas.PuntoRutaResponse;
import ar.edu.uade.logistica.contracts.rutas.RutaResponse;
import ar.edu.uade.logistica.contracts.rutas.TramoRutaResponse;

/**
 * Calcula el camino de un viaje sobre la red de rutas. Cypher busca los caminos con saltos limitados,
 * como se vio en clase, y la aplicación elige el más corto.
 */
@Service
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class RutasService {
    private static final Set<String> SIN_AEREO = Set.of("TERRESTRE", "MARITIMO");
    private static final Set<String> TODOS = Set.of("TERRESTRE", "MARITIMO", "AEREO");

    private static final String PUNTOS = """
            MATCH (p:Punto)
            RETURN p
            ORDER BY p.pais, p.nombre
            """;
    // La relación se guarda una sola vez y se recorre sin dirección: el viaje puede ir para los dos lados.
    // El viaje más largo de la red, Callao a Valencia, tiene 18 saltos.
    private static final String CAMINOS = """
            MATCH p=(a:Punto {codigo: $origen})-[:CONECTA*1..20]-(b:Punto {codigo: $destino})
            RETURN p
            """;

    private final Driver driver;
    private final String database;

    public RutasService(Driver driver, @Value("${app.neo4j.database:neo4j}") String database) {
        this.driver = driver;
        this.database = database;
    }

    public List<PuntoRutaResponse> puntos() {
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            return session.executeRead(tx -> tx.run(PUNTOS).list(fila -> punto(fila.get("p").asNode())));
        }
    }

    public RutaResponse ruta(String origen, String destino, boolean aereo) {
        if (origen.equals(destino)) throw new BadRequestException("El origen y el destino tienen que ser distintos");
        List<List<TramoRutaResponse>> caminos;
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            caminos = session.executeRead(tx -> tx.run(CAMINOS, Map.of("origen", origen, "destino", destino))
                    .list(fila -> tramos(fila.get("p").asPath())));
        }
        return masCorto(caminos, aereo ? TODOS : SIN_AEREO)
                .orElseThrow(() -> new NotFoundException("No hay una ruta entre esos puntos"));
    }

    /** El camino de menos km que usa solo esos transportes y no pasa dos veces por el mismo punto. */
    static Optional<RutaResponse> masCorto(List<List<TramoRutaResponse>> caminos, Set<String> medios) {
        return caminos.stream()
                .filter(camino -> valido(camino, medios))
                .min(Comparator.comparingInt(RutasService::km))
                .map(camino -> new RutaResponse(camino.getFirst().origen().codigo(), camino.getLast().destino().codigo(),
                        km(camino), camino));
    }

    private static boolean valido(List<TramoRutaResponse> camino, Set<String> medios) {
        if ("ALTA_MAR".equals(camino.getFirst().origen().tipo())
                || "ALTA_MAR".equals(camino.getLast().destino().tipo())) return false;
        Set<String> visitados = new HashSet<>(Set.of(camino.getFirst().origen().codigo()));
        for (TramoRutaResponse tramo : camino) {
            if (!medios.contains(tramo.medio()) || !visitados.add(tramo.destino().codigo())) return false;
        }
        return true;
    }

    private static int km(List<TramoRutaResponse> camino) {
        return camino.stream().mapToInt(TramoRutaResponse::km).sum();
    }

    private static List<TramoRutaResponse> tramos(Path camino) {
        List<TramoRutaResponse> tramos = new ArrayList<>();
        for (Path.Segment segmento : camino) {
            tramos.add(new TramoRutaResponse(punto(segmento.start()), punto(segmento.end()),
                    segmento.relationship().get("medio").asString(), segmento.relationship().get("km").asInt()));
        }
        return tramos;
    }

    private static PuntoRutaResponse punto(Node nodo) {
        return new PuntoRutaResponse(nodo.get("codigo").asString(), nodo.get("nombre").asString(),
                nodo.get("pais").asString(), nodo.get("tipo").asString(),
                nodo.get("latitud").asDouble(), nodo.get("longitud").asDouble());
    }
}
