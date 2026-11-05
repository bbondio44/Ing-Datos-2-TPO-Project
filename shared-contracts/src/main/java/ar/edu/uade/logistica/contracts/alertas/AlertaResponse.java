package ar.edu.uade.logistica.contracts.alertas;

import java.time.Instant;

public record AlertaResponse(
        String id, String idRegla, String tipoRegla, String idSensor, String idContenedor, String idEnvio,
        String idCliente, String idLectura, String idIncidente, Double valorObservado, Double umbral,
        String severidad, String estado, boolean activa, String mensaje, Instant fecha,
        Instant ultimaOcurrencia, int ocurrencias) {
}
