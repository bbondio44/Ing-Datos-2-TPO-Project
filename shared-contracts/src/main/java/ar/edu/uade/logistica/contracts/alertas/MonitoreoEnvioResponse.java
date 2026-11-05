package ar.edu.uade.logistica.contracts.alertas;

import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.contracts.iot.PosicionResponse;

import java.util.List;
import java.util.UUID;

/** Vista de un envío armada desde tres motores: PostgreSQL, Cassandra y MongoDB (R17). */
public record MonitoreoEnvioResponse(
        UUID idEnvio, UUID idCliente, String origen, String destino, String estado,
        List<UUID> contenedores, List<LecturaHistoricaResponse> ultimasLecturas,
        List<PosicionResponse> ultimasPosiciones, List<EventoResponse> eventos,
        List<AlertaResponse> alertas, List<IncidenteResponse> incidentes) {
}
