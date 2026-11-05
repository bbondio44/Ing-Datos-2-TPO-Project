package ar.edu.uade.logistica.contracts.mensajeria;

import java.time.Instant;

public record MensajeResponse(String id, String remitente, Instant fechaEnvio, String contenido) {
}
