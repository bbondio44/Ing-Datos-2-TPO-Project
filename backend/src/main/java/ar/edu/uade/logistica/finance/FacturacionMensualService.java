package ar.edu.uade.logistica.finance;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.uade.logistica.contracts.finance.AutomaticInvoiceResponse;
import ar.edu.uade.logistica.contracts.finance.MonthlyCollectionResponse;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;

/**
 * Una factura por cliente por mes, con una línea por envío entregado en ese mes. Lo que evita duplicar es
 * la línea: cada una lleva el envío en el concepto, así repetir el proceso no vuelve a facturar un envío,
 * y un envío que entra después del proceso se factura en la corrida siguiente.
 */
@Service
public class FacturacionMensualService {
    static final int DIA_DE_COBRO = 10;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final String ENVIOS_SIN_FACTURAR = """
            SELECT e.id_cliente, e.id_envio, e.origen, e.destino, e.fecha_entrega_real,
                   COALESCE((SELECT SUM(COALESCE(t.costo_real, t.costo_estimado)) FROM tp_logistica.tramo t
                              WHERE t.id_envio = e.id_envio), 0) AS monto,
                   (SELECT t.id_tramo FROM tp_logistica.tramo t WHERE t.id_envio = e.id_envio
                     ORDER BY t.numero_tramo LIMIT 1) AS primer_tramo
              FROM tp_logistica.envio e
             WHERE e.estado = 'ENTREGADO' AND e.fecha_entrega_real >= ? AND e.fecha_entrega_real < ?
               AND NOT EXISTS (SELECT 1 FROM tp_logistica.factura f
                                WHERE f.id_envio = e.id_envio AND f.estado <> 'BORRADOR')
               AND NOT EXISTS (SELECT 1 FROM tp_logistica.factura_detalle d
                                WHERE d.concepto LIKE 'Envío ' || left(e.id_envio::text, 8) || '%')
             ORDER BY e.id_cliente, e.fecha_entrega_real
            """;
    private static final String MES_CERRADO = """
            SELECT EXISTS(SELECT 1 FROM tp_logistica.cierre_cuenta WHERE id_cuenta = ?
                           AND periodo >= date_trunc('month', now() AT TIME ZONE 'UTC')::date)
            """;
    private static final String TARIFA_FIJA = """
            SELECT precio FROM tp_logistica.tarifa_servicio
             WHERE activa AND tipo_servicio = 'TRANSPORTE' AND criterio_calculo = 'FIJO' AND moneda = 'ARS'
               AND vigencia_desde <= current_date AND (vigencia_hasta IS NULL OR vigencia_hasta >= current_date)
             ORDER BY vigencia_desde DESC LIMIT 1
            """;
    private static final String PENDIENTES_DE_COBRO = """
            SELECT s.id_factura, s.id_cuenta, s.pendiente FROM tp_logistica.v_saldo_factura s
              JOIN tp_logistica.factura f USING (id_factura)
             WHERE f.clave_idempotencia LIKE ? AND s.pendiente > 0
            """;

    record EnvioFacturable(UUID cliente, UUID envio, String origen, String destino, Instant entrega,
                           BigDecimal monto, UUID primerTramo) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseAuditContext auditContext;
    private final FinanceService finanzas;

    public FacturacionMensualService(JdbcTemplate jdbcTemplate, DatabaseAuditContext auditContext, FinanceService finanzas) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditContext = auditContext;
        this.finanzas = finanzas;
    }

    @Transactional
    public AutomaticInvoiceResponse facturar(LocalDate mes) {
        auditContext.applyToCurrentTransaction();
        finanzas.ensureAccountsForExistingOwners();
        LocalDate desde = mes.withDayOfMonth(1);
        Map<UUID, List<EnvioFacturable>> porCliente = agrupar(jdbcTemplate.query(ENVIOS_SIN_FACTURAR, (fila, numero) -> new EnvioFacturable(
                fila.getObject("id_cliente", UUID.class), fila.getObject("id_envio", UUID.class), fila.getString("origen"),
                fila.getString("destino"), fila.getTimestamp("fecha_entrega_real").toInstant(), fila.getBigDecimal("monto"),
                fila.getObject("primer_tramo", UUID.class)),
                Timestamp.from(desde.atStartOfDay(ZoneOffset.UTC).toInstant()),
                Timestamp.from(desde.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant())));
        List<UUID> facturas = new ArrayList<>();
        int envios = 0;
        int salteados = 0;
        for (Map.Entry<UUID, List<EnvioFacturable>> cliente : porCliente.entrySet()) {
            UUID cuenta = jdbcTemplate.queryForObject(
                    "SELECT id_cuenta FROM tp_logistica.cuenta_corriente WHERE id_cliente = ?", UUID.class, cliente.getKey());
            if (Boolean.TRUE.equals(jdbcTemplate.queryForObject(MES_CERRADO, Boolean.class, cuenta))) {
                salteados += cliente.getValue().size();
                continue;
            }
            facturas.add(crearFactura(cuenta, cliente.getKey(), desde, cliente.getValue()));
            envios += cliente.getValue().size();
        }
        return new AutomaticInvoiceResponse(envios, facturas.size(), salteados, List.copyOf(facturas));
    }

    static Map<UUID, List<EnvioFacturable>> agrupar(List<EnvioFacturable> envios) {
        Map<UUID, List<EnvioFacturable>> porCliente = new LinkedHashMap<>();
        envios.forEach(envio -> porCliente.computeIfAbsent(envio.cliente(), cliente -> new ArrayList<>()).add(envio));
        return porCliente;
    }

    static String concepto(EnvioFacturable envio) {
        String texto = "Envío " + envio.envio().toString().substring(0, 8) + ": " + envio.origen() + " → " + envio.destino()
                + ", entregado " + DIA.format(envio.entrega().atZone(ZoneOffset.UTC));
        return texto.length() <= 180 ? texto : texto.substring(0, 180);
    }

    /** El día de cobro siguiente a la emisión: la factura vence ese día. */
    static LocalDate vencimiento(LocalDate emision) {
        LocalDate esteMes = emision.withDayOfMonth(DIA_DE_COBRO);
        return emision.isAfter(esteMes) ? esteMes.plusMonths(1) : esteMes;
    }

    private UUID crearFactura(UUID cuenta, UUID cliente, LocalDate mes, List<EnvioFacturable> envios) {
        String prefijo = "MENSUAL-" + cliente + "-" + MES.format(mes) + "-";
        Integer previas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tp_logistica.factura WHERE clave_idempotencia LIKE ?", Integer.class, prefijo + "%");
        UUID factura = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.factura
                (id_factura, id_cuenta, id_cliente, fecha_vencimiento, naturaleza, clave_idempotencia, estado)
                VALUES (?, ?, ?, ?, 'COBRAR', ?, 'BORRADOR')
                """, factura, cuenta, cliente, Date.valueOf(vencimiento(LocalDate.now(ZoneOffset.UTC))),
                prefijo + ((previas == null ? 0 : previas) + 1));
        BigDecimal fija = null;
        for (EnvioFacturable envio : envios) {
            BigDecimal monto = envio.monto();
            if (monto.signum() == 0) {
                if (fija == null) fija = jdbcTemplate.queryForObject(TARIFA_FIJA, BigDecimal.class);
                monto = fija;
            }
            jdbcTemplate.update("""
                    INSERT INTO tp_logistica.factura_detalle (id_detalle, id_factura, concepto, cantidad, precio_unitario, id_tramo)
                    VALUES (?, ?, ?, 1, ?, ?)
                    """, UUID.randomUUID(), factura, concepto(envio), monto, envio.primerTramo());
        }
        jdbcTemplate.queryForObject("SELECT tp_logistica.emitir_factura(?)", UUID.class, factura);
        return factura;
    }

    /** Cobra lo pendiente de las facturas mensuales de ese mes. La clave del pago hace que repetirlo no cobre dos veces. */
    @Transactional
    public MonthlyCollectionResponse cobrar(LocalDate mes) {
        auditContext.applyToCurrentTransaction();
        int cobradas = 0;
        int salteadas = 0;
        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> pendientes = jdbcTemplate.queryForList(PENDIENTES_DE_COBRO,
                "MENSUAL-%-" + MES.format(mes.withDayOfMonth(1)) + "-%");
        for (Map<String, Object> factura : pendientes) {
            if (Boolean.TRUE.equals(jdbcTemplate.queryForObject(MES_CERRADO, Boolean.class, factura.get("id_cuenta")))) {
                salteadas++;
                continue;
            }
            BigDecimal pendiente = (BigDecimal) factura.get("pendiente");
            jdbcTemplate.queryForObject("SELECT tp_logistica.registrar_pago(?, ?, ?, ?, ?)", UUID.class,
                    factura.get("id_factura"), pendiente, "TRANSFERENCIA", "COBRO-MENSUAL-" + factura.get("id_factura"), null);
            cobradas++;
            total = total.add(pendiente);
        }
        return new MonthlyCollectionResponse(cobradas, total, salteadas);
    }
}
