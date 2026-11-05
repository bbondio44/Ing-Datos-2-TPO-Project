package ar.edu.uade.logistica.contracts.route;

import java.math.BigDecimal;
import java.util.UUID;

public record RouteSupplierResponse(UUID supplierId, String supplierName, String service, BigDecimal cost) {
}
