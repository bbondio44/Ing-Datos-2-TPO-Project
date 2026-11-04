package ar.edu.uade.logistica.contracts.logistics;

import java.math.BigDecimal;
import java.util.UUID;

public record ContainerResponse(UUID id, String internationalCode, String type,
                                BigDecimal capacity, String state) {
}
