package ar.edu.uade.logistica.desktop.http;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiClientReportPathTest {
    @Test
    void r35SendsClientFilterCombinedWithTheOthers() {
        UUID client = UUID.randomUUID();

        String path = ApiClient.shipmentReportPath("Brasil", "EN_TRANSITO", client, true, true);

        assertEquals("/api/v1/reportes/envios?pais=Brasil&estado=EN_TRANSITO&clienteId=" + client
                + "&demorado=true&incidentesAbiertos=true", path);
    }

    @Test
    void r35OmitsClientWhenNoneIsSelected() {
        String path = ApiClient.shipmentReportPath("", "", null, null, null);

        assertFalse(path.contains("clienteId"));
        assertTrue(path.startsWith("/api/v1/reportes/envios?"));
    }
}
