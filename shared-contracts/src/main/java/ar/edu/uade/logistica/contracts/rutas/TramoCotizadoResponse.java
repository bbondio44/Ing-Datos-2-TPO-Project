package ar.edu.uade.logistica.contracts.rutas;

import java.math.BigDecimal;

/** precioKm es el precio por km y por m³ del medio de transporte del tramo. */
public record TramoCotizadoResponse(PuntoRutaResponse origen, PuntoRutaResponse destino, String medio, int km,
                                    BigDecimal precioKm, BigDecimal costo) {
}
