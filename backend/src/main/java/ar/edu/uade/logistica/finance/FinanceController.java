package ar.edu.uade.logistica.finance;

import ar.edu.uade.logistica.contracts.finance.AccountCloseResponse;
import ar.edu.uade.logistica.contracts.finance.AccountResponse;
import ar.edu.uade.logistica.contracts.finance.AutomaticCloseResponse;
import ar.edu.uade.logistica.contracts.finance.AutomaticInvoiceRequest;
import ar.edu.uade.logistica.contracts.finance.AutomaticInvoiceResponse;
import ar.edu.uade.logistica.contracts.finance.CloseAccountRequest;
import ar.edu.uade.logistica.contracts.finance.InvoiceRequest;
import ar.edu.uade.logistica.contracts.finance.InvoiceResponse;
import ar.edu.uade.logistica.contracts.finance.MonthlyCollectionResponse;
import ar.edu.uade.logistica.contracts.finance.PaymentRequest;
import ar.edu.uade.logistica.contracts.finance.PaymentResponse;
import ar.edu.uade.logistica.contracts.finance.TariffResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finanzas")
@PreAuthorize("hasAuthority('FINANZAS_LEER')")
public class FinanceController {
    private final FinanceService service;
    private final FacturacionMensualService monthly;

    public FinanceController(FinanceService service, FacturacionMensualService monthly) {
        this.service = service;
        this.monthly = monthly;
    }

    @GetMapping("/cuentas")
    public List<AccountResponse> accounts(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.accounts(user);
    }

    @GetMapping("/cuentas/{id}")
    public AccountResponse account(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.account(id, user);
    }

    @PostMapping("/cuentas/{id}/cierres")
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public AccountCloseResponse close(@PathVariable UUID id, @Valid @RequestBody CloseAccountRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return service.closeAccount(id, request.period(), user);
    }

    @GetMapping("/cuentas/{id}/cierres")
    public List<AccountCloseResponse> closes(@PathVariable UUID id,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return service.closes(id, user);
    }

    @GetMapping("/facturas")
    public List<InvoiceResponse> invoices(@RequestParam(required = false) UUID cuentaId,
                                          @RequestParam(required = false) String estado,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return service.invoices(cuentaId, estado, user);
    }

    @GetMapping("/facturas/{id}")
    public InvoiceResponse invoice(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.invoice(id, user);
    }

    @PostMapping("/facturas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public InvoiceResponse create(@Valid @RequestBody InvoiceRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return service.createInvoice(request, user);
    }

    @PutMapping("/facturas/{id}")
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public InvoiceResponse update(@PathVariable UUID id, @Valid @RequestBody InvoiceRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return service.updateInvoice(id, request, user);
    }

    @DeleteMapping("/facturas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.deleteDraft(id, user);
    }

    @PostMapping("/facturas/{id}/emitir")
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public InvoiceResponse issue(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.issueInvoice(id, user);
    }

    @PostMapping("/facturas/{id}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public PaymentResponse pay(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request,
                               @AuthenticationPrincipal AuthenticatedUser user) {
        return service.registerPayment(id, request, user);
    }

    @GetMapping("/facturas/{id}/pagos")
    public List<PaymentResponse> payments(@PathVariable UUID id,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return service.payments(id, user);
    }

    @PostMapping("/cierre-mensual")
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public AutomaticCloseResponse closeAll(@Valid @RequestBody CloseAccountRequest request) {
        return service.closeAllAccounts(request.period());
    }

    @GetMapping("/tarifas")
    public List<TariffResponse> tariffs() { return service.tariffs(); }

    @PostMapping("/facturacion-mensual")
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public AutomaticInvoiceResponse monthly(@Valid @RequestBody CloseAccountRequest request) {
        return monthly.facturar(request.period());
    }

    @PostMapping("/cobro-mensual")
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public MonthlyCollectionResponse collect(@Valid @RequestBody CloseAccountRequest request) {
        return monthly.cobrar(request.period());
    }

    @PostMapping("/facturacion-automatica")
    @PreAuthorize("hasAuthority('FINANZAS_ESCRIBIR')")
    public AutomaticInvoiceResponse automatic(@Valid @RequestBody AutomaticInvoiceRequest request) {
        return service.automaticInvoices(request.tariffId());
    }
}
