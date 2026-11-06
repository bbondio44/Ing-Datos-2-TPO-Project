package ar.edu.uade.logistica.contracts.rutas;

public record TramoRutaResponse(PuntoRutaResponse origen, PuntoRutaResponse destino, String medio, int km) {
}
