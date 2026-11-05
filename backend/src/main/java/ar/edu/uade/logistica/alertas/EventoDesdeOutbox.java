package ar.edu.uade.logistica.alertas;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduce un cambio de estado de PostgreSQL a un evento logístico de MongoDB (trazabilidad del envío).
 * Solo los UPDATE: un INSERT es la carga inicial, cuyos eventos ya vienen en el dataset. El _id es
 * determinístico por entidad y tipo, así reprocesar el outbox no duplica.
 */
public final class EventoDesdeOutbox {
    public record Cambio(String id, String idEnvio, String idTramo, String tipo, String descripcion,
                         String lugar, String pais, String estado) { }

    private static final Map<String, String> POR_ESTADO_ENVIO = Map.of(
            "EN_PREPARACION", "CARGA_INICIADA", "EN_TRANSITO", "EN_TRANSITO",
            "DEMORADO", "RETRASO", "ENTREGADO", "ENTREGADO");
    private static final Map<String, String> POR_ESTADO_TRAMO = Map.of(
            "EN_CURSO", "SALIDA", "FINALIZADO", "DESCARGA");

    private EventoDesdeOutbox() { }

    public static Optional<Cambio> desde(String agregado, String tipoEvento, Map<String, Object> fila) {
        String estado = texto(fila.get("estado"));
        if ("ENVIO_UPDATE".equals(tipoEvento) && "envio".equals(agregado)) {
            String tipo = POR_ESTADO_ENVIO.get(estado);
            if (tipo == null) return Optional.empty();
            String idEnvio = texto(fila.get("id_envio"));
            boolean destino = "ENTREGADO".equals(tipo);
            return Optional.of(new Cambio(id(idEnvio, tipo), idEnvio, null, tipo, "El envío pasó a " + estado,
                    texto(fila.get(destino ? "destino" : "origen")),
                    texto(fila.get(destino ? "pais_destino" : "pais_origen")), estado));
        }
        if ("TRAMO_UPDATE".equals(tipoEvento) && "tramo".equals(agregado)) {
            String tipo = POR_ESTADO_TRAMO.get(estado);
            if (tipo == null) return Optional.empty();
            String idTramo = texto(fila.get("id_tramo"));
            String lugar = texto(fila.get("SALIDA".equals(tipo) ? "origen" : "destino"));
            return Optional.of(new Cambio(id(idTramo, tipo), texto(fila.get("id_envio")), idTramo, tipo,
                    "Tramo " + fila.get("numero_tramo") + " pasó a " + estado, lugar, null, estado));
        }
        return Optional.empty();
    }

    private static String id(String entidad, String tipo) {
        return UUID.nameUUIDFromBytes(("evento|" + entidad + "|" + tipo).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
