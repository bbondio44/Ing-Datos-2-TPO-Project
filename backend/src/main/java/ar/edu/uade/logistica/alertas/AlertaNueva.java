package ar.edu.uade.logistica.alertas;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaResponse;

/** Una violación de regla detectada. idLectura es null en las alertas de inactividad. */
record AlertaNueva(
        ReglaAlertaResponse regla, UUID idSensor, UUID idContenedor, UUID idEnvio, UUID idCliente,
        UUID idLectura, double valorObservado, double umbral, String mensaje, Instant fechaLectura, Instant ahora) {

    String claveDedup() {
        return regla.idRegla() + "|" + idSensor;
    }

    /** Id determinístico: reprocesar la misma violación produce el mismo documento (D04). */
    String idAlerta() {
        return derivar("alerta", claveDedup(), String.valueOf(idLectura), String.valueOf(fechaLectura));
    }

    String idEvento() {
        return derivar("evento", idAlerta());
    }

    String idIncidente() {
        return derivar("incidente", idAlerta());
    }

    boolean generaIncidente() {
        return idEnvio != null && ("ALTA".equals(regla.severidad()) || "CRITICA".equals(regla.severidad()));
    }

    private static String derivar(String... partes) {
        return UUID.nameUUIDFromBytes(String.join("|", partes).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
