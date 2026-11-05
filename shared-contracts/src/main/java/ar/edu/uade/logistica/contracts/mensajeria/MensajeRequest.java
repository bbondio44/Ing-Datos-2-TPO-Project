package ar.edu.uade.logistica.contracts.mensajeria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** idMensaje lo genera quien envía: un reintento con el mismo id no duplica el mensaje. */
public record MensajeRequest(@NotNull UUID idMensaje, @NotBlank @Size(max = 4000) String contenido) {
}
