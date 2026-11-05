package ar.edu.uade.logistica.analitica;

import static com.mongodb.client.model.Filters.eq;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ar.edu.uade.logistica.common.BadRequestException;
import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.common.ServiceUnavailableException;
import ar.edu.uade.logistica.contracts.reportes.ReporteRequest;
import ar.edu.uade.logistica.contracts.reportes.ReporteResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * R11, R33, R34. La solicitud, el precio y la factura viven en PostgreSQL; el resultado, cuya forma cambia
 * según el tipo de reporte, va a MongoDB. Se vinculan por id_solicitud.
 */
@Service
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "true")
public class ReporteService {
    private static final String VER_TODO = "ENVIOS_LEER_TODOS";
    private static final String COLUMNAS = """
            id_solicitud, id_cliente, tipo_reporte, estado, precio, fecha_solicitud, fecha_generacion, id_factura
            """;
    private static final String TARIFA_VIGENTE = """
            SELECT id_tarifa, precio FROM tp_logistica.tarifa_servicio
             WHERE tipo_servicio = 'REPORTE' AND activa AND criterio_calculo = 'FIJO' AND moneda = 'ARS'
               AND vigencia_desde <= current_date AND (vigencia_hasta IS NULL OR vigencia_hasta >= current_date)
             ORDER BY vigencia_desde DESC LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AnaliticaRepository analitica;
    private final PrediccionService predicciones;
    private final MongoCollection<Document> resultados;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReporteService(JdbcTemplate jdbcTemplate, AnaliticaRepository analitica, PrediccionService predicciones,
                          MongoDatabase database) {
        this.jdbcTemplate = jdbcTemplate;
        this.analitica = analitica;
        this.predicciones = predicciones;
        this.resultados = database.getCollection("reportes_generados");
    }

    public ReporteResponse solicitar(ReporteRequest pedido, AuthenticatedUser user) {
        UUID idCliente = titular(pedido, user);
        Map<String, Object> parametros = parametros(pedido, idCliente);
        UUID idSolicitud = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.solicitud_reporte
                  (id_solicitud, id_cliente, id_usuario, tipo_reporte, parametros, precio)
                VALUES (?, ?, ?, ?, ?::jsonb, ?)
                """, idSolicitud, idCliente, user.userId(), pedido.tipo(), json(parametros), precioVigente());
        try {
            List<Map<String, Object>> filas = ejecutar(pedido, idCliente);
            UUID idResultado = guardarResultado(idSolicitud, idCliente, user, pedido.tipo(), parametros, filas);
            jdbcTemplate.update("""
                    UPDATE tp_logistica.solicitud_reporte
                       SET estado = 'GENERADO', fecha_generacion = now(), id_resultado_externo = ?
                     WHERE id_solicitud = ?
                    """, idResultado, idSolicitud);
        } catch (RuntimeException exception) {
            jdbcTemplate.update("UPDATE tp_logistica.solicitud_reporte SET estado = 'ERROR' WHERE id_solicitud = ?",
                    idSolicitud);
            throw exception;
        }
        return buscar(idSolicitud);
    }

    public List<ReporteResponse> listar(AuthenticatedUser user) {
        if (user.permissions().contains(VER_TODO)) {
            return jdbcTemplate.query("SELECT " + COLUMNAS
                    + " FROM tp_logistica.solicitud_reporte ORDER BY fecha_solicitud DESC LIMIT 200", this::mapear);
        }
        if (user.clientId() == null) return List.of();
        return jdbcTemplate.query("SELECT " + COLUMNAS
                + " FROM tp_logistica.solicitud_reporte WHERE id_cliente = ? ORDER BY fecha_solicitud DESC LIMIT 200",
                this::mapear, user.clientId());
    }

    public Object resultado(UUID idSolicitud, AuthenticatedUser user) {
        exigirAcceso(buscar(idSolicitud), user);
        Document documento = leer(idSolicitud);
        if (documento == null) throw new NotFoundException("El reporte todavía no tiene resultado");
        return documento.get("resultado");
    }

    /** facturar_reporte es idempotente: si ya tiene factura, devuelve la misma. */
    public ReporteResponse facturar(UUID idSolicitud, AuthenticatedUser user) {
        exigirAcceso(buscar(idSolicitud), user);
        List<UUID> tarifas = jdbcTemplate.query(TARIFA_VIGENTE, (rs, fila) -> rs.getObject("id_tarifa", UUID.class));
        if (tarifas.isEmpty()) throw new ConflictException("No hay una tarifa de reporte vigente");
        jdbcTemplate.queryForObject("SELECT tp_logistica.facturar_reporte(?, ?, ?)", UUID.class,
                idSolicitud, tarifas.get(0), "reporte:" + idSolicitud);
        return buscar(idSolicitud);
    }

    private List<Map<String, Object>> ejecutar(ReporteRequest pedido, UUID idCliente) {
        return switch (pedido.tipo()) {
            case "RANKING_RUTAS" -> analitica.rankingRutas();
            case "RANKING_CLIENTES" -> analitica.rankingClientes();
            case "DESEMPENO" -> analitica.desempeno(idCliente, pedido.desde(), pedido.hasta());
            case "PREDICTIVO" -> {
                predicciones.generarDelDia();
                yield predicciones.vigentes(idCliente);
            }
            default -> throw new BadRequestException("Tipo de reporte inválido");
        };
    }

    /** _id determinístico e índice único en id_solicitud: reintentar no duplica el resultado. */
    private UUID guardarResultado(UUID idSolicitud, UUID idCliente, AuthenticatedUser user, String tipo,
                                  Map<String, Object> parametros, List<Map<String, Object>> filas) {
        UUID idResultado = UUID.nameUUIDFromBytes(("reporte|" + idSolicitud).getBytes(StandardCharsets.UTF_8));
        List<Document> resultado = new ArrayList<>();
        filas.forEach(fila -> resultado.add(documento(fila)));
        Document documento = new Document("_id", idResultado.toString())
                .append("id_solicitud", idSolicitud.toString())
                .append("id_cliente", idCliente.toString())
                .append("solicitado_por", user.userId().toString())
                .append("tipo", tipo)
                .append("parametros", new Document(parametros))
                .append("formato", "JSON")
                .append("estado", "GENERADO")
                .append("resultado", resultado)
                .append("ubicacion", null)
                .append("generado_en", new Date());
        try {
            resultados.insertOne(documento);
        } catch (MongoWriteException exception) {
            if (exception.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) throw exception;
        } catch (MongoException exception) {
            throw new ServiceUnavailableException("MongoDB no está disponible", exception);
        }
        return idResultado;
    }

    private Document leer(UUID idSolicitud) {
        try {
            return resultados.find(eq("id_solicitud", idSolicitud.toString())).first();
        } catch (MongoException exception) {
            throw new ServiceUnavailableException("MongoDB no está disponible", exception);
        }
    }

    /** BSON no acepta directamente algunos tipos de JDBC: se convierten a texto, número o fecha. */
    private Document documento(Map<String, Object> fila) {
        Document documento = new Document();
        fila.forEach((columna, valor) -> documento.append(columna, switch (valor) {
            case null -> null;
            case UUID uuid -> uuid.toString();
            case BigDecimal decimal -> decimal.doubleValue();
            case Timestamp fecha -> Date.from(fecha.toInstant());
            case java.sql.Date fecha -> fecha.toString();
            default -> valor;
        }));
        return documento;
    }

    private UUID titular(ReporteRequest pedido, AuthenticatedUser user) {
        if (!user.permissions().contains(VER_TODO)) {
            if (user.clientId() == null) throw new BadRequestException("El usuario no pertenece a un cliente");
            return user.clientId();
        }
        if (pedido.idCliente() == null) throw new BadRequestException("Indicá el cliente titular del reporte");
        Integer clientes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tp_logistica.cliente WHERE id_cliente = ?", Integer.class, pedido.idCliente());
        if (clientes == null || clientes == 0) throw new NotFoundException("El cliente no existe");
        return pedido.idCliente();
    }

    private Map<String, Object> parametros(ReporteRequest pedido, UUID idCliente) {
        if (pedido.desde() != null && pedido.hasta() != null && pedido.desde().isAfter(pedido.hasta())) {
            throw new BadRequestException("La fecha desde debe ser anterior a la fecha hasta");
        }
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("id_cliente", idCliente.toString());
        if (pedido.desde() != null) parametros.put("desde", pedido.desde().toString());
        if (pedido.hasta() != null) parametros.put("hasta", pedido.hasta().toString());
        return parametros;
    }

    private BigDecimal precioVigente() {
        List<BigDecimal> precios = jdbcTemplate.query(TARIFA_VIGENTE, (rs, fila) -> rs.getBigDecimal("precio"));
        return precios.isEmpty() ? BigDecimal.ZERO : precios.get(0);
    }

    private String json(Map<String, Object> valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron serializar los parámetros", exception);
        }
    }

    private ReporteResponse buscar(UUID id) {
        List<ReporteResponse> filas = jdbcTemplate.query("SELECT " + COLUMNAS
                + " FROM tp_logistica.solicitud_reporte WHERE id_solicitud = ?", this::mapear, id);
        if (filas.isEmpty()) throw new NotFoundException("El reporte no existe");
        return filas.get(0);
    }

    private void exigirAcceso(ReporteResponse reporte, AuthenticatedUser user) {
        if (!user.permissions().contains(VER_TODO) && !reporte.idCliente().equals(user.clientId())) {
            throw new NotFoundException("El reporte no existe");
        }
    }

    private ReporteResponse mapear(ResultSet rs, int fila) throws SQLException {
        Timestamp generacion = rs.getTimestamp("fecha_generacion");
        return new ReporteResponse(rs.getObject("id_solicitud", UUID.class), rs.getObject("id_cliente", UUID.class),
                rs.getString("tipo_reporte"), rs.getString("estado"), rs.getBigDecimal("precio"),
                rs.getTimestamp("fecha_solicitud").toInstant(), generacion == null ? null : generacion.toInstant(),
                rs.getObject("id_factura", UUID.class));
    }
}
