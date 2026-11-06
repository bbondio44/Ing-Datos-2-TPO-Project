package ar.edu.uade.logistica.contracts.rutas;

import java.math.BigDecimal;
import java.util.List;

public record CotizacionResponse(String origen, String destino, int km, BigDecimal volumenM3, BigDecimal total,
                                 String moneda, List<TramoCotizadoResponse> tramos) {
}
