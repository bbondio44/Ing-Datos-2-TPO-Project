package ar.edu.uade.logistica.contracts.mensajeria;

import java.time.Instant;
import java.util.List;

public record ConversacionResponse(
        String id, String tipo, String asunto, String nombreGrupo, String idEnvio,
        List<String> participantes, int cantidadMensajes, Instant ultimoMensajeFecha, String ultimoMensajeExtracto) {
}
