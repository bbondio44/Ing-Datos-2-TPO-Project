package ar.edu.uade.logistica.contracts.mensajeria;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** El usuario que la crea se suma solo como participante. */
public record ConversacionRequest(
        @NotBlank @Pattern(regexp = "PRIVADA|GRUPAL") String tipo,
        @NotBlank @Size(max = 200) String asunto,
        @Size(max = 120) String nombreGrupo,
        UUID idEnvio,
        @NotEmpty @Valid List<ParticipanteRequest> participantes) {

    @AssertTrue(message = "Una conversación privada tiene un solo destinatario")
    public boolean isPrivadaValida() {
        return !"PRIVADA".equals(tipo) || participantes == null || participantes.size() == 1;
    }
}
