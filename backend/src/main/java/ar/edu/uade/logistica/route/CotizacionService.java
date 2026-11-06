package ar.edu.uade.logistica.route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ar.edu.uade.logistica.common.BadRequestException;
import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.contracts.rutas.CotizacionResponse;
import ar.edu.uade.logistica.contracts.rutas.RutaResponse;
import ar.edu.uade.logistica.contracts.rutas.TramoCotizadoResponse;
import ar.edu.uade.logistica.contracts.rutas.TramoRutaResponse;
import ar.edu.uade.logistica.integration.neo4j.RutasService;

/** Precio de un viaje: por cada tramo, km × m³ × precio del medio de transporte. Los km salen de Neo4j y los precios de PostgreSQL. */
@Service
@ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
public class CotizacionService {
    static final String PREFIJO_CRITERIO = "KM_M3_";
    private static final String MONEDA = "ARS";
    private static final String PRECIOS = """
            SELECT criterio_calculo, precio FROM tp_logistica.tarifa_servicio
             WHERE activa AND tipo_servicio = 'TRANSPORTE' AND moneda = ? AND criterio_calculo LIKE ?
               AND vigencia_desde <= current_date AND (vigencia_hasta IS NULL OR vigencia_hasta >= current_date)
            """;

    private final RutasService rutas;
    private final JdbcTemplate jdbcTemplate;

    public CotizacionService(RutasService rutas, JdbcTemplate jdbcTemplate) {
        this.rutas = rutas;
        this.jdbcTemplate = jdbcTemplate;
    }

    public CotizacionResponse cotizar(String origen, String destino, BigDecimal volumen, boolean aereo) {
        if (volumen.signum() <= 0) throw new BadRequestException("El volumen debe ser mayor a 0");
        return cotizar(rutas.ruta(origen, destino, aereo), volumen, precios());
    }

    private Map<String, BigDecimal> precios() {
        Map<String, BigDecimal> precios = new HashMap<>();
        jdbcTemplate.query(PRECIOS, fila -> {
            precios.put(fila.getString("criterio_calculo").substring(PREFIJO_CRITERIO.length()), fila.getBigDecimal("precio"));
        }, MONEDA, PREFIJO_CRITERIO + "%");
        return precios;
    }

    static CotizacionResponse cotizar(RutaResponse ruta, BigDecimal volumen, Map<String, BigDecimal> precios) {
        List<TramoCotizadoResponse> tramos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (TramoRutaResponse tramo : ruta.tramos()) {
            BigDecimal precio = precios.get(tramo.medio());
            if (precio == null) throw new ConflictException("No hay una tarifa vigente para el transporte " + tramo.medio());
            BigDecimal costo = BigDecimal.valueOf(tramo.km()).multiply(volumen).multiply(precio)
                    .setScale(2, RoundingMode.HALF_UP);
            tramos.add(new TramoCotizadoResponse(tramo.origen(), tramo.destino(), tramo.medio(), tramo.km(), precio, costo));
            total = total.add(costo);
        }
        return new CotizacionResponse(ruta.origen(), ruta.destino(), ruta.km(), volumen, total, MONEDA, tramos);
    }
}
