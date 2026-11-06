package ar.edu.uade.logistica.contracts.rutas;

import java.util.List;

public record RutaResponse(String origen, String destino, int km, List<TramoRutaResponse> tramos) {
}
