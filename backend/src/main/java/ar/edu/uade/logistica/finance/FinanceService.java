package ar.edu.uade.logistica.finance;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.finance.AccountCloseResponse;
import ar.edu.uade.logistica.contracts.finance.AccountResponse;
import ar.edu.uade.logistica.contracts.finance.AutomaticInvoiceResponse;
import ar.edu.uade.logistica.contracts.finance.InvoiceLineRequest;
import ar.edu.uade.logistica.contracts.finance.InvoiceLineResponse;
import ar.edu.uade.logistica.contracts.finance.InvoiceRequest;
import ar.edu.uade.logistica.contracts.finance.InvoiceResponse;
import ar.edu.uade.logistica.contracts.finance.MovementResponse;
import ar.edu.uade.logistica.contracts.finance.PaymentRequest;
import ar.edu.uade.logistica.contracts.finance.PaymentResponse;
import ar.edu.uade.logistica.contracts.finance.TariffResponse;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FinanceService {
    private static final Set<String> OWNER_TYPES = Set.of("CLIENTE", "PROVEEDOR", "OPERADOR");
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseAuditContext auditContext;

    public FinanceService(JdbcTemplate jdbcTemplate, DatabaseAuditContext auditContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditContext = auditContext;
    }

    @Transactional
    public List<AccountResponse> accounts(AuthenticatedUser user) {
        ensureAccountsForExistingOwners();
        String condition = "";
        List<Object> params = new ArrayList<>();
        if (!user.permissions().contains("FINANZAS_LEER_TODAS")) {
            List<String> owners = new ArrayList<>();
            if (user.clientId() != null) { owners.add("c.id_cliente=?"); params.add(user.clientId()); }
            if (user.supplierId() != null) { owners.add("c.id_proveedor=?"); params.add(user.supplierId()); }
            if (user.operatorId() != null) { owners.add("c.id_operador=?"); params.add(user.operatorId()); }
            if (owners.isEmpty()) return List.of();
            condition = " WHERE " + String.join(" OR ", owners);
        }
        return jdbcTemplate.query(ACCOUNT_SELECT + condition + " ORDER BY titular", this::mapAccount,
                params.toArray());
    }

    @Transactional(readOnly = true)
    public AccountResponse account(UUID id, AuthenticatedUser user) {
        List<AccountResponse> result = jdbcTemplate.query(ACCOUNT_SELECT + " WHERE c.id_cuenta=?",
                this::mapAccount, id);
        if (result.isEmpty()) throw new NotFoundException("La cuenta corriente no existe");
        authorize(result.getFirst().ownerType(), result.getFirst().ownerId(), user);
        return result.getFirst();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> invoices(UUID accountId, String state, AuthenticatedUser user) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (accountId != null) {
            account(accountId, user);
            where.append(" AND f.id_cuenta=?");
            params.add(accountId);
        } else if (!user.permissions().contains("FINANZAS_LEER_TODAS")) {
            List<String> owners = new ArrayList<>();
            if (user.clientId() != null) { owners.add("f.id_cliente=?"); params.add(user.clientId()); }
            if (user.supplierId() != null) { owners.add("f.id_proveedor=?"); params.add(user.supplierId()); }
            if (user.operatorId() != null) { owners.add("f.id_operador=?"); params.add(user.operatorId()); }
            if (owners.isEmpty()) return List.of();
            where.append(" AND (").append(String.join(" OR ", owners)).append(')');
        }
        if (state != null && !state.isBlank()) {
            where.append(" AND f.estado=?");
            params.add(upper(state));
        }
        return jdbcTemplate.query(INVOICE_SELECT + where + " ORDER BY f.fecha_emision DESC,f.numero_factura DESC NULLS LAST",
                this::mapInvoice, params.toArray());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse invoice(UUID id, AuthenticatedUser user) {
        List<InvoiceResponse> result = jdbcTemplate.query(INVOICE_SELECT + " WHERE f.id_factura=?",
                this::mapInvoice, id);
        if (result.isEmpty()) throw new NotFoundException("La factura no existe");
        authorize(result.getFirst().ownerType(), result.getFirst().ownerId(), user);
        return result.getFirst();
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        String ownerType = ownerType(request.ownerType());
        requireOwner(ownerType, request.ownerId());
        UUID accountId = ensureAccount(ownerType, request.ownerId());
        UUID id = UUID.randomUUID();
        String nature = "CLIENTE".equals(ownerType) ? "COBRAR" : "PAGAR";
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.factura
                (id_factura,id_cuenta,id_cliente,id_proveedor,id_operador,id_envio,fecha_vencimiento,
                 naturaleza,clave_idempotencia,estado)
                VALUES (?,?,?,?,?,?,?,?,?,'BORRADOR')
                """, id, accountId, "CLIENTE".equals(ownerType) ? request.ownerId() : null,
                "PROVEEDOR".equals(ownerType) ? request.ownerId() : null,
                "OPERADOR".equals(ownerType) ? request.ownerId() : null,
                request.shipmentId(), date(request.dueDate()), nature, nullable(request.idempotencyKey()));
        replaceLines(id, request.lines());
        return invoice(id, user);
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID id, InvoiceRequest request, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        InvoiceResponse current = invoice(id, user);
        if (!"BORRADOR".equals(current.state())) throw new ConflictException("Solo se editan facturas en borrador");
        String ownerType = ownerType(request.ownerType());
        if (!ownerType.equals(current.ownerType()) || !request.ownerId().equals(current.ownerId())) {
            throw new ConflictException("No se puede cambiar el titular de una factura");
        }
        jdbcTemplate.update("""
                UPDATE tp_logistica.factura SET id_envio=?,fecha_vencimiento=?,clave_idempotencia=?
                 WHERE id_factura=? AND estado='BORRADOR'
                """, request.shipmentId(), date(request.dueDate()), nullable(request.idempotencyKey()), id);
        replaceLines(id, request.lines());
        return invoice(id, user);
    }

    @Transactional
    public InvoiceResponse issueInvoice(UUID id, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        invoice(id, user);
        jdbcTemplate.queryForObject("SELECT tp_logistica.emitir_factura(?)", UUID.class, id);
        return invoice(id, user);
    }

    @Transactional
    public void deleteDraft(UUID id, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        InvoiceResponse invoice = invoice(id, user);
        if (!"BORRADOR".equals(invoice.state())) throw new ConflictException("Solo se eliminan facturas en borrador");
        jdbcTemplate.update("DELETE FROM tp_logistica.factura_detalle WHERE id_factura=?", id);
        jdbcTemplate.update("DELETE FROM tp_logistica.factura WHERE id_factura=?", id);
    }

    @Transactional
    public PaymentResponse registerPayment(UUID invoiceId, PaymentRequest request, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        invoice(invoiceId, user);
        UUID id = jdbcTemplate.queryForObject("SELECT tp_logistica.registrar_pago(?,?,?,?,?)", UUID.class,
                invoiceId, request.amount(), upper(request.paymentMethod()), request.idempotencyKey().strip(),
                nullable(request.externalReference()));
        return payment(id);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> payments(UUID invoiceId, AuthenticatedUser user) {
        invoice(invoiceId, user);
        return jdbcTemplate.query("""
                SELECT id_pago,id_factura,id_cuenta,fecha,monto,medio_pago,referencia_externa,clave_idempotencia
                  FROM tp_logistica.pago WHERE id_factura=? ORDER BY fecha DESC
                """, this::mapPayment, invoiceId);
    }

    @Transactional
    public AccountCloseResponse closeAccount(UUID accountId, LocalDate period, AuthenticatedUser user) {
        auditContext.applyToCurrentTransaction();
        account(accountId, user);
        UUID id = jdbcTemplate.queryForObject("SELECT tp_logistica.cerrar_cuenta(?,?)", UUID.class,
                accountId, date(period));
        return close(id);
    }

    @Transactional(readOnly = true)
    public List<AccountCloseResponse> closes(UUID accountId, AuthenticatedUser user) {
        account(accountId, user);
        return jdbcTemplate.query("""
                SELECT id_cierre,id_cuenta,periodo,saldo_inicial,total_debitos,total_creditos,
                       saldo_final,fecha_cierre FROM tp_logistica.cierre_cuenta
                 WHERE id_cuenta=? ORDER BY periodo DESC
                """, this::mapClose, accountId);
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> tariffs() {
        return jdbcTemplate.query("""
                SELECT id_tarifa,concepto,tipo_servicio,criterio_calculo,precio,moneda,
                       vigencia_desde,vigencia_hasta,activa
                  FROM tp_logistica.tarifa_servicio ORDER BY activa DESC,vigencia_desde DESC,concepto
                """, (rs, row) -> new TariffResponse(rs.getObject("id_tarifa", UUID.class),
                rs.getString("concepto"), rs.getString("tipo_servicio"), rs.getString("criterio_calculo"),
                rs.getBigDecimal("precio"), rs.getString("moneda"), rs.getDate("vigencia_desde").toLocalDate(),
                localDate(rs, "vigencia_hasta"), rs.getBoolean("activa")));
    }

    @Transactional
    public AutomaticInvoiceResponse automaticInvoices(UUID tariffId) {
        ensureAccountsForExistingOwners();
        Boolean validTariff = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM tp_logistica.tarifa_servicio
                               WHERE id_tarifa=? AND activa AND tipo_servicio IN ('TRANSPORTE','SERVICIO')
                                 AND criterio_calculo='FIJO' AND moneda='ARS'
                                 AND vigencia_desde<=current_date
                                 AND (vigencia_hasta IS NULL OR vigencia_hasta>=current_date))
                """, Boolean.class, tariffId);
        if (!Boolean.TRUE.equals(validTariff)) throw new ConflictException("La tarifa no está vigente");
        List<UUID> pending = jdbcTemplate.queryForList("""
                SELECT id_envio FROM tp_logistica.envio
                 WHERE estado IN ('EN_TRANSITO','DEMORADO','ENTREGADO')
                   AND NOT EXISTS (
                       SELECT 1 FROM tp_logistica.factura f
                        WHERE f.id_envio=envio.id_envio AND f.estado<>'BORRADOR'
                   )
                 ORDER BY fecha_creacion
                """, UUID.class);
        // Una cuenta con el mes cerrado rechaza movimientos: si entrara al lote, la transacción
        // se revertiría y ningún otro cliente quedaría facturado. Se saltea y se informa.
        List<UUID> closed = jdbcTemplate.queryForList("""
                SELECT e.id_envio FROM tp_logistica.envio e
                  JOIN tp_logistica.cuenta_corriente c ON c.id_cliente=e.id_cliente
                  JOIN tp_logistica.cierre_cuenta cc ON cc.id_cuenta=c.id_cuenta
                 WHERE cc.periodo>=date_trunc('month',now() AT TIME ZONE 'UTC')::date
                """, UUID.class);
        List<UUID> shipments = pending.stream().filter(id -> !closed.contains(id)).toList();
        List<UUID> invoiceIds = new ArrayList<>();
        int created = 0;
        for (UUID shipmentId : shipments) {
            String key = "AUTO-ENVIO-" + shipmentId + "-TARIFA-" + tariffId;
            Boolean existed = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM tp_logistica.factura WHERE clave_idempotencia=?)",
                    Boolean.class, key);
            UUID invoiceId = jdbcTemplate.queryForObject("SELECT tp_logistica.facturar_envio(?,?,?)", UUID.class,
                    shipmentId, tariffId, key);
            invoiceIds.add(invoiceId);
            if (!Boolean.TRUE.equals(existed)) created++;
        }
        return new AutomaticInvoiceResponse(shipments.size(), created, pending.size() - shipments.size(),
                List.copyOf(invoiceIds));
    }

    private AccountResponse mapAccount(ResultSet rs, int row) throws SQLException {
        UUID id = rs.getObject("id_cuenta", UUID.class);
        return new AccountResponse(id, rs.getString("tipo_titular"), rs.getObject("id_titular", UUID.class),
                rs.getString("titular"), rs.getString("naturaleza"), rs.getBigDecimal("saldo"),
                rs.getBigDecimal("saldo_calculado"), rs.getBoolean("saldo_conciliado"),
                rs.getTimestamp("fecha_actualizacion").toInstant(), movements(id));
    }

    private List<MovementResponse> movements(UUID accountId) {
        return jdbcTemplate.query("""
                SELECT id_movimiento,id_factura,id_pago,fecha,tipo_movimiento,origen,descripcion,importe,referencia
                  FROM tp_logistica.movimiento_cuenta WHERE id_cuenta=? ORDER BY fecha DESC,id_movimiento
                """, (rs, row) -> new MovementResponse(rs.getObject("id_movimiento", UUID.class),
                rs.getObject("id_factura", UUID.class), rs.getObject("id_pago", UUID.class),
                rs.getTimestamp("fecha").toInstant(), rs.getString("tipo_movimiento"), rs.getString("origen"),
                rs.getString("descripcion"), rs.getBigDecimal("importe"), rs.getString("referencia")), accountId);
    }

    private InvoiceResponse mapInvoice(ResultSet rs, int row) throws SQLException {
        UUID id = rs.getObject("id_factura", UUID.class);
        return new InvoiceResponse(id, rs.getString("numero_factura"), rs.getObject("id_cuenta", UUID.class),
                rs.getString("tipo_titular"), rs.getObject("id_titular", UUID.class), rs.getString("titular"),
                rs.getObject("id_envio", UUID.class), rs.getDate("fecha_emision").toLocalDate(),
                localDate(rs, "fecha_vencimiento"), rs.getBigDecimal("importe_total"),
                rs.getBigDecimal("total_pagado"), rs.getBigDecimal("pendiente"), rs.getString("naturaleza"),
                rs.getString("estado"), rs.getString("clave_idempotencia"), lines(id));
    }

    private List<InvoiceLineResponse> lines(UUID invoiceId) {
        return jdbcTemplate.query("""
                SELECT id_detalle,concepto,cantidad,precio_unitario,subtotal,id_tramo
                  FROM tp_logistica.factura_detalle WHERE id_factura=? ORDER BY concepto,id_detalle
                """, (rs, row) -> new InvoiceLineResponse(rs.getObject("id_detalle", UUID.class),
                rs.getString("concepto"), rs.getBigDecimal("cantidad"), rs.getBigDecimal("precio_unitario"),
                rs.getBigDecimal("subtotal"), rs.getObject("id_tramo", UUID.class)), invoiceId);
    }

    private PaymentResponse payment(UUID id) {
        List<PaymentResponse> result = jdbcTemplate.query("""
                SELECT id_pago,id_factura,id_cuenta,fecha,monto,medio_pago,referencia_externa,clave_idempotencia
                  FROM tp_logistica.pago WHERE id_pago=?
                """, this::mapPayment, id);
        if (result.isEmpty()) throw new NotFoundException("El pago no existe");
        return result.getFirst();
    }

    private PaymentResponse mapPayment(ResultSet rs, int row) throws SQLException {
        return new PaymentResponse(rs.getObject("id_pago", UUID.class), rs.getObject("id_factura", UUID.class),
                rs.getObject("id_cuenta", UUID.class), rs.getTimestamp("fecha").toInstant(),
                rs.getBigDecimal("monto"), rs.getString("medio_pago"), rs.getString("referencia_externa"),
                rs.getString("clave_idempotencia"));
    }

    private AccountCloseResponse close(UUID id) {
        List<AccountCloseResponse> result = jdbcTemplate.query("""
                SELECT id_cierre,id_cuenta,periodo,saldo_inicial,total_debitos,total_creditos,
                       saldo_final,fecha_cierre FROM tp_logistica.cierre_cuenta WHERE id_cierre=?
                """, this::mapClose, id);
        if (result.isEmpty()) throw new NotFoundException("El cierre no existe");
        return result.getFirst();
    }

    private AccountCloseResponse mapClose(ResultSet rs, int row) throws SQLException {
        return new AccountCloseResponse(rs.getObject("id_cierre", UUID.class),
                rs.getObject("id_cuenta", UUID.class), rs.getDate("periodo").toLocalDate(),
                rs.getBigDecimal("saldo_inicial"), rs.getBigDecimal("total_debitos"),
                rs.getBigDecimal("total_creditos"), rs.getBigDecimal("saldo_final"),
                rs.getTimestamp("fecha_cierre").toInstant());
    }

    private void replaceLines(UUID invoiceId, List<InvoiceLineRequest> lines) {
        if (lines == null || lines.isEmpty()) throw new ConflictException("La factura requiere al menos un detalle");
        jdbcTemplate.update("DELETE FROM tp_logistica.factura_detalle WHERE id_factura=?", invoiceId);
        for (InvoiceLineRequest line : lines) {
            jdbcTemplate.update("""
                    INSERT INTO tp_logistica.factura_detalle
                    (id_detalle,id_factura,concepto,cantidad,precio_unitario,id_tramo) VALUES (?,?,?,?,?,?)
                    """, UUID.randomUUID(), invoiceId, line.concept().strip(), line.quantity(),
                    line.unitPrice(), line.routeLegId());
        }
    }

    private UUID ensureAccount(String ownerType, UUID ownerId) {
        String column = ownerColumn(ownerType);
        List<UUID> existing = jdbcTemplate.queryForList(
                "SELECT id_cuenta FROM tp_logistica.cuenta_corriente WHERE " + column + "=?", UUID.class, ownerId);
        if (!existing.isEmpty()) return existing.getFirst();
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO tp_logistica.cuenta_corriente(id_cuenta," + column + ",naturaleza) VALUES (?,?,?)",
                id, ownerId, "CLIENTE".equals(ownerType) ? "COBRAR" : "PAGAR");
        return id;
    }

    private void ensureAccountsForExistingOwners() {
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.cuenta_corriente(id_cliente,naturaleza)
                SELECT id_cliente,'COBRAR' FROM tp_logistica.cliente c
                 WHERE NOT EXISTS (SELECT 1 FROM tp_logistica.cuenta_corriente cc WHERE cc.id_cliente=c.id_cliente)
                """);
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.cuenta_corriente(id_proveedor,naturaleza)
                SELECT id_proveedor,'PAGAR' FROM tp_logistica.proveedor p
                 WHERE NOT EXISTS (SELECT 1 FROM tp_logistica.cuenta_corriente cc WHERE cc.id_proveedor=p.id_proveedor)
                """);
        jdbcTemplate.update("""
                INSERT INTO tp_logistica.cuenta_corriente(id_operador,naturaleza)
                SELECT id_operador,'PAGAR' FROM tp_logistica.operador_logistico o
                 WHERE NOT EXISTS (SELECT 1 FROM tp_logistica.cuenta_corriente cc WHERE cc.id_operador=o.id_operador)
                """);
    }

    private void requireOwner(String ownerType, UUID ownerId) {
        String table = switch (ownerType) {
            case "CLIENTE" -> "cliente";
            case "PROVEEDOR" -> "proveedor";
            case "OPERADOR" -> "operador_logistico";
            default -> throw new ConflictException("Tipo de titular inválido");
        };
        String column = ownerColumn(ownerType);
        Boolean exists = jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM tp_logistica." + table
                + " WHERE " + column + "=? AND estado='ACTIVO')", Boolean.class, ownerId);
        if (!Boolean.TRUE.equals(exists)) throw new ConflictException("El titular no existe o está inactivo");
    }

    private void authorize(String ownerType, UUID ownerId, AuthenticatedUser user) {
        if (user.permissions().contains("FINANZAS_LEER_TODAS")) return;
        boolean own = switch (ownerType) {
            case "CLIENTE" -> ownerId.equals(user.clientId());
            case "PROVEEDOR" -> ownerId.equals(user.supplierId());
            case "OPERADOR" -> ownerId.equals(user.operatorId());
            default -> false;
        };
        if (!own) throw new NotFoundException("La cuenta corriente no existe");
    }

    private static String ownerType(String value) {
        String normalized = upper(value);
        if (!OWNER_TYPES.contains(normalized)) throw new ConflictException("Tipo de titular inválido");
        return normalized;
    }

    private static String ownerColumn(String ownerType) {
        return switch (ownerType) {
            case "CLIENTE" -> "id_cliente";
            case "PROVEEDOR" -> "id_proveedor";
            case "OPERADOR" -> "id_operador";
            default -> throw new IllegalArgumentException("Tipo de titular inválido");
        };
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }
    private static Date date(LocalDate value) { return value == null ? null : Date.valueOf(value); }
    private static String nullable(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private static String upper(String value) { return value.strip().toUpperCase(Locale.ROOT); }

    private static final String ACCOUNT_SELECT = """
            SELECT c.id_cuenta,c.naturaleza,c.saldo,c.fecha_actualizacion,
                   CASE WHEN c.id_cliente IS NOT NULL THEN 'CLIENTE'
                        WHEN c.id_proveedor IS NOT NULL THEN 'PROVEEDOR' ELSE 'OPERADOR' END AS tipo_titular,
                   COALESCE(c.id_cliente,c.id_proveedor,c.id_operador) AS id_titular,
                   COALESCE(cl.razon_social,p.razon_social,o.razon_social) AS titular,
                   vc.saldo_calculado,vc.saldo_conciliado
              FROM tp_logistica.cuenta_corriente c
              LEFT JOIN tp_logistica.cliente cl ON cl.id_cliente=c.id_cliente
              LEFT JOIN tp_logistica.proveedor p ON p.id_proveedor=c.id_proveedor
              LEFT JOIN tp_logistica.operador_logistico o ON o.id_operador=c.id_operador
              JOIN tp_logistica.v_conciliacion_cuentas vc ON vc.id_cuenta=c.id_cuenta
            """;

    private static final String INVOICE_SELECT = """
            SELECT f.id_factura,f.numero_factura,f.id_cuenta,f.id_envio,f.fecha_emision,f.fecha_vencimiento,
                   f.importe_total,f.naturaleza,f.estado,f.clave_idempotencia,
                   CASE WHEN f.id_cliente IS NOT NULL THEN 'CLIENTE'
                        WHEN f.id_proveedor IS NOT NULL THEN 'PROVEEDOR' ELSE 'OPERADOR' END AS tipo_titular,
                   COALESCE(f.id_cliente,f.id_proveedor,f.id_operador) AS id_titular,
                   COALESCE(cl.razon_social,p.razon_social,o.razon_social) AS titular,
                   COALESCE(sf.total_pagado,0) AS total_pagado,
                   COALESCE(sf.pendiente,f.importe_total) AS pendiente
              FROM tp_logistica.factura f
              LEFT JOIN tp_logistica.cliente cl ON cl.id_cliente=f.id_cliente
              LEFT JOIN tp_logistica.proveedor p ON p.id_proveedor=f.id_proveedor
              LEFT JOIN tp_logistica.operador_logistico o ON o.id_operador=f.id_operador
              LEFT JOIN tp_logistica.v_saldo_factura sf ON sf.id_factura=f.id_factura
            """;
}
