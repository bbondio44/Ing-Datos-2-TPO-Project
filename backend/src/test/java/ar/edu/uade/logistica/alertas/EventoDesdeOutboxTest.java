package ar.edu.uade.logistica.alertas;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventoDesdeOutboxTest {
    private static Map<String, Object> envio(String estado) {
        return Map.of("id_envio", "e1", "estado", estado, "origen", "Buenos Aires", "destino", "Santos",
                "pais_origen", "Argentina", "pais_destino", "Brasil");
    }

    @Test
    void cambioDeEstadoDelEnvioGeneraSuEvento() {
        var cambio = EventoDesdeOutbox.desde("envio", "ENVIO_UPDATE", envio("EN_PREPARACION")).orElseThrow();

        assertEquals("CARGA_INICIADA", cambio.tipo());
        assertEquals("Buenos Aires", cambio.lugar());
    }

    @Test
    void entregadoUsaElDestino() {
        var cambio = EventoDesdeOutbox.desde("envio", "ENVIO_UPDATE", envio("ENTREGADO")).orElseThrow();

        assertEquals("Santos", cambio.lugar());
        assertEquals("Brasil", cambio.pais());
    }

    @Test
    void elMismoCambioTieneElMismoId() {
        var primero = EventoDesdeOutbox.desde("envio", "ENVIO_UPDATE", envio("EN_TRANSITO")).orElseThrow();
        var repetido = EventoDesdeOutbox.desde("envio", "ENVIO_UPDATE", envio("EN_TRANSITO")).orElseThrow();

        assertEquals(primero.id(), repetido.id());
    }

    @Test
    void laCargaInicialYLosEstadosSinEventoNoGeneranNada() {
        assertTrue(EventoDesdeOutbox.desde("envio", "ENVIO_INSERT", envio("ENTREGADO")).isEmpty());
        assertTrue(EventoDesdeOutbox.desde("envio", "ENVIO_UPDATE", envio("CANCELADO")).isEmpty());
        assertTrue(EventoDesdeOutbox.desde("cliente", "CLIENTE_UPDATE", Map.of("estado", "ACTIVO")).isEmpty());
    }

    @Test
    void tramoEnCursoEsSalidaYFinalizadoEsDescarga() {
        Map<String, Object> tramo = Map.of("id_tramo", "t1", "id_envio", "e1", "numero_tramo", 2, "estado", "EN_CURSO",
                "origen", "Santos", "destino", "Valencia");
        var salida = EventoDesdeOutbox.desde("tramo", "TRAMO_UPDATE", tramo).orElseThrow();
        assertEquals("SALIDA", salida.tipo());
        assertEquals("Santos", salida.lugar());
        assertEquals("t1", salida.idTramo());

        Map<String, Object> fin = Map.of("id_tramo", "t1", "id_envio", "e1", "numero_tramo", 2, "estado", "FINALIZADO",
                "origen", "Santos", "destino", "Valencia");
        var descarga = EventoDesdeOutbox.desde("tramo", "TRAMO_UPDATE", fin).orElseThrow();
        assertEquals("DESCARGA", descarga.tipo());
        assertEquals("Valencia", descarga.lugar());
    }
}
