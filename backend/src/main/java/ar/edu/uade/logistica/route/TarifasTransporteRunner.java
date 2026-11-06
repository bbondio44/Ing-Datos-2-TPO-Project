package ar.edu.uade.logistica.route;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Crea las tarifas por km y m³ si faltan. Con ON CONFLICT nunca pisa un precio que ya se haya cambiado. */
@Component
class TarifasTransporteRunner implements ApplicationRunner {
    private static final String CREAR = """
            INSERT INTO tp_logistica.tarifa_servicio
            (id_tarifa, concepto, tipo_servicio, criterio_calculo, precio, moneda, vigencia_desde, activa)
            VALUES (?::uuid, ?, 'TRANSPORTE', ?, ?, 'ARS', DATE '2025-01-01', true)
            ON CONFLICT (id_tarifa) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    TarifasTransporteRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        crear("7a71fa00-0000-4000-8000-000000000001", "Transporte marítimo por km y m³", "MARITIMO", 3);
        crear("7a71fa00-0000-4000-8000-000000000002", "Transporte terrestre por km y m³", "TERRESTRE", 15);
        crear("7a71fa00-0000-4000-8000-000000000003", "Transporte aéreo por km y m³", "AEREO", 60);
    }

    private void crear(String id, String concepto, String medio, int precio) {
        jdbcTemplate.update(CREAR, id, concepto, CotizacionService.PREFIJO_CRITERIO + medio, precio);
    }
}
