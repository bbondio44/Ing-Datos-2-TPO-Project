package ar.edu.uade.logistica.integration.neo4j;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ar.edu.uade.logistica.contracts.rutas.PuntoRutaResponse;
import ar.edu.uade.logistica.contracts.rutas.TramoRutaResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RutasServiceTest {
    private static final Set<String> SIN_AEREO = Set.of("TERRESTRE", "MARITIMO");
    private static final PuntoRutaResponse A = punto("A", "CIUDAD");
    private static final PuntoRutaResponse B = punto("B", "CIUDAD");
    private static final PuntoRutaResponse C = punto("C", "CIUDAD");
    private static final PuntoRutaResponse MAR = punto("M", "ALTA_MAR");

    private static PuntoRutaResponse punto(String codigo, String tipo) {
        return new PuntoRutaResponse(codigo, codigo, "AR", tipo, 0, 0);
    }

    private static TramoRutaResponse tramo(PuntoRutaResponse origen, PuntoRutaResponse destino, String medio, int km) {
        return new TramoRutaResponse(origen, destino, medio, km);
    }

    @Test
    void eligeElCaminoDeMenosKilometrosYSumaSusTramos() {
        var largo = List.of(tramo(A, C, "TERRESTRE", 500));
        var corto = List.of(tramo(A, B, "TERRESTRE", 100), tramo(B, C, "MARITIMO", 150));

        var ruta = RutasService.masCorto(List.of(largo, corto), SIN_AEREO).orElseThrow();

        assertThat(ruta.km()).isEqualTo(250);
        assertThat(ruta.origen()).isEqualTo("A");
        assertThat(ruta.destino()).isEqualTo("C");
        assertThat(ruta.tramos()).hasSize(2);
    }

    @Test
    void noUsaAvionSiNoSeLoPide() {
        var aereo = List.of(tramo(A, C, "AEREO", 10));
        var terrestre = List.of(tramo(A, C, "TERRESTRE", 900));

        assertThat(RutasService.masCorto(List.of(aereo, terrestre), SIN_AEREO).orElseThrow().km()).isEqualTo(900);
        assertThat(RutasService.masCorto(List.of(aereo), SIN_AEREO)).isEmpty();
    }

    @Test
    void descartaLosCaminosQueRepitenUnPunto() {
        var conVuelta = List.of(tramo(A, B, "TERRESTRE", 1), tramo(B, A, "TERRESTRE", 1), tramo(A, C, "TERRESTRE", 1));

        assertThat(RutasService.masCorto(List.of(conVuelta), SIN_AEREO)).isEmpty();
    }

    @Test
    void unViajeNoPuedeEmpezarNiTerminarEnAltaMar() {
        assertThat(RutasService.masCorto(List.of(List.of(tramo(MAR, A, "MARITIMO", 5))), SIN_AEREO)).isEmpty();
        assertThat(RutasService.masCorto(List.of(List.of(tramo(A, MAR, "MARITIMO", 5))), SIN_AEREO)).isEmpty();
    }

    @Test
    void puedePasarPorAltaMarEnElCamino() {
        var ruta = List.of(tramo(A, MAR, "MARITIMO", 5), tramo(MAR, B, "MARITIMO", 5));

        assertThat(RutasService.masCorto(List.of(ruta), SIN_AEREO)).isPresent();
    }

    @Test
    void laRedDeRutasUsaMergeParaNoDuplicarAlRecargar() {
        List<String> sentencias = RedRutas.sentencias();

        assertThat(sentencias).isNotEmpty().allMatch(s -> s.contains("MERGE")).noneMatch(s -> s.contains("CREATE"));
    }

    @Test
    void ignoraComentariosYLineasVacias() {
        assertThat(RedRutas.sentencias("// nota\n\nMERGE (p:Punto {codigo: 'A'});\n"))
                .containsExactly("MERGE (p:Punto {codigo: 'A'})");
    }
}
