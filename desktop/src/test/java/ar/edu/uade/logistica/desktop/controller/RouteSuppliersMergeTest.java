package ar.edu.uade.logistica.desktop.controller;

import ar.edu.uade.logistica.contracts.route.RouteSupplierRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteSuppliersMergeTest {
    private final RouteSupplierRequest other =
            new RouteSupplierRequest(UUID.randomUUID(), "ADUANA", BigDecimal.TEN);

    @Test
    void r04EditingOneSupplierKeepsTheOthers() {
        RouteSupplierRequest edited = new RouteSupplierRequest(UUID.randomUUID(), "DEPOSITO", BigDecimal.ONE);

        assertEquals(List.of(edited, other), PersonaOneDialogs.mergeSuppliers(edited, List.of(other)));
    }

    @Test
    void r04ClearingTheEditedSupplierKeepsTheOthers() {
        assertEquals(List.of(other), PersonaOneDialogs.mergeSuppliers(null, List.of(other)));
    }

    @Test
    void r04SameSupplierAndServiceIsNotDuplicated() {
        RouteSupplierRequest edited = new RouteSupplierRequest(other.supplierId(), "ADUANA", BigDecimal.ONE);

        assertEquals(List.of(edited), PersonaOneDialogs.mergeSuppliers(edited, List.of(other)));
    }
}
