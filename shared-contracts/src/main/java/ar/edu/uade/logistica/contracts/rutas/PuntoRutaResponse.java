package ar.edu.uade.logistica.contracts.rutas;

/** tipo: CIUDAD, PUERTO, AEROPUERTO o ALTA_MAR. Un punto de alta mar no puede ser origen ni destino. */
public record PuntoRutaResponse(String codigo, String nombre, String pais, String tipo, double latitud, double longitud) {
}
