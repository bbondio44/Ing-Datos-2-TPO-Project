package ar.edu.uade.logistica.integration.neo4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProyeccionGrafoTest {
    @Test
    void everyWriteUsesMergeSoReprocessingDoesNotDuplicate() {
        Map<String, Object> envio = Map.of("id_envio", "e", "id_cliente", "c");

        var sentencias = ProyeccionGrafo.sentencias("envio", "ENVIO_INSERT", envio);

        assertThat(sentencias).hasSize(1);
        assertThat(sentencias.get(0).cypher()).contains("MERGE (e:Envio").contains("MERGE (c)-[:SOLICITA]->(e)")
                .doesNotContain("CREATE");
    }

    @Test
    void aSegmentWithoutOptionalReferencesOnlyWritesTheSegment() {
        Map<String, Object> tramo = new HashMap<>();
        tramo.put("id_tramo", "t");
        tramo.put("id_envio", "e");
        tramo.put("id_vehiculo", null);
        tramo.put("id_operador", null);

        assertThat(ProyeccionGrafo.sentencias("tramo", "TRAMO_INSERT", tramo)).hasSize(1);
    }

    @Test
    void aSegmentLinksItsOperatorEvenWithoutAVehicle() {
        Map<String, Object> tramo = new HashMap<>();
        tramo.put("id_tramo", "t");
        tramo.put("id_operador", "o");

        List<ProyeccionGrafo.Sentencia> sentencias = ProyeccionGrafo.sentencias("tramo", "TRAMO_INSERT", tramo);

        assertThat(sentencias).hasSize(2);
        assertThat(sentencias.get(1).cypher()).contains("OPERADO_POR");
    }

    @Test
    void aDeleteRemovesTheNodeWithItsRelationships() {
        var sentencias = ProyeccionGrafo.sentencias("cliente", "CLIENTE_DELETE", Map.of("id_cliente", "c"));

        assertThat(sentencias.get(0).cypher()).contains("DETACH DELETE");
    }

    @Test
    void entitiesOutsideTheGraphAreIgnored() {
        assertThat(ProyeccionGrafo.sentencias("sensor_iot", "SENSOR_IOT_INSERT", Map.of())).isEmpty();
        assertThat(ProyeccionGrafo.sentencias("proveedor", "PROVEEDOR_INSERT", Map.of())).isEmpty();
    }

    @Test
    void theRetryWaitDoublesUpToFiveMinutes() {
        assertThat(OutboxNeo4jWorker.esperaSegundos(0)).isEqualTo(5);
        assertThat(OutboxNeo4jWorker.esperaSegundos(1)).isEqualTo(10);
        assertThat(OutboxNeo4jWorker.esperaSegundos(2)).isEqualTo(20);
        assertThat(OutboxNeo4jWorker.esperaSegundos(20)).isEqualTo(300);
    }
}
