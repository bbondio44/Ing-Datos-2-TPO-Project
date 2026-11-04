package ar.edu.uade.logistica.iot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import ar.edu.uade.logistica.common.ServiceUnavailableException;
import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.contracts.iot.PosicionResponse;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/**
 * Consultas de telemetría. El WHERE siempre incluye toda la partition key (id + dia). Un rango de varios
 * días consulta cada partición diaria y la aplicación une y ordena el resultado (C02): así no hace falta
 * ALLOW FILTERING.
 */
@Repository
@ConditionalOnProperty(prefix = "app.cassandra", name = "enabled", havingValue = "true")
public class LecturaConsultaCassandraRepository {
    private static final String LECTURAS_POR_SENSOR = """
            SELECT * FROM lecturas_por_sensor_dia
             WHERE id_sensor = ? AND dia = ? AND fecha_hora >= ? AND fecha_hora <= ?
            """;
    private static final String LECTURAS_POR_CONTENEDOR = """
            SELECT * FROM lecturas_por_contenedor_dia
             WHERE id_contenedor = ? AND dia = ? AND fecha_hora >= ? AND fecha_hora <= ?
            """;
    private static final String POSICIONES_POR_CONTENEDOR = """
            SELECT * FROM posiciones_por_contenedor_dia
             WHERE id_contenedor = ? AND dia = ? AND fecha_hora >= ? AND fecha_hora <= ?
            """;
    private static final String ULTIMA_LECTURA = "SELECT * FROM ultima_lectura_por_sensor WHERE id_sensor = ?";
    private static final String ULTIMA_POSICION = "SELECT * FROM ultima_posicion_por_contenedor WHERE id_contenedor = ?";

    private final ObjectProvider<CqlSession> sessionProvider;

    public LecturaConsultaCassandraRepository(ObjectProvider<CqlSession> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    public List<LecturaHistoricaResponse> lecturasPorSensor(UUID idSensor, Instant desde, Instant hasta) {
        return porDias(LECTURAS_POR_SENSOR, idSensor, desde, hasta, this::lectura).stream()
                .sorted(Comparator.comparing(LecturaHistoricaResponse::fechaHora))
                .toList();
    }

    public List<LecturaHistoricaResponse> lecturasPorContenedor(UUID idContenedor, Instant desde, Instant hasta) {
        return porDias(LECTURAS_POR_CONTENEDOR, idContenedor, desde, hasta, this::lectura).stream()
                .sorted(Comparator.comparing(LecturaHistoricaResponse::fechaHora))
                .toList();
    }

    public List<PosicionResponse> recorrido(UUID idContenedor, Instant desde, Instant hasta) {
        return porDias(POSICIONES_POR_CONTENEDOR, idContenedor, desde, hasta, this::posicion).stream()
                .sorted(Comparator.comparing(PosicionResponse::fechaHora))
                .toList();
    }

    public Optional<LecturaHistoricaResponse> ultimaLectura(UUID idSensor) {
        return leer(session -> Optional.ofNullable(
                session.execute(SimpleStatement.newInstance(ULTIMA_LECTURA, idSensor)).one()).map(this::lectura));
    }

    public Optional<PosicionResponse> ultimaPosicion(UUID idContenedor) {
        return leer(session -> Optional.ofNullable(
                session.execute(SimpleStatement.newInstance(ULTIMA_POSICION, idContenedor)).one()).map(this::posicion));
    }

    private <T> List<T> porDias(String cql, UUID id, Instant desde, Instant hasta, Function<Row, T> mapear) {
        return leer(session -> {
            List<T> resultado = new ArrayList<>();
            for (LocalDate dia : dias(desde, hasta)) {
                session.execute(SimpleStatement.newInstance(cql, id, dia, desde, hasta))
                        .forEach(row -> resultado.add(mapear.apply(row)));
            }
            return resultado;
        });
    }

    /** Una partición por día en UTC, igual que al escribir. */
    static List<LocalDate> dias(Instant desde, Instant hasta) {
        LocalDate ultimo = hasta.atZone(ZoneOffset.UTC).toLocalDate();
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate dia = desde.atZone(ZoneOffset.UTC).toLocalDate(); !dia.isAfter(ultimo); dia = dia.plusDays(1)) {
            dias.add(dia);
        }
        return dias;
    }

    private LecturaHistoricaResponse lectura(Row row) {
        return new LecturaHistoricaResponse(
                row.getUuid("id_lectura"), row.getUuid("id_sensor"), row.getUuid("id_contenedor"),
                row.getInstant("fecha_hora"), valor(row, "temperatura"), valor(row, "humedad"),
                valor(row, "vibracion"), valor(row, "bateria"), valor(row, "latitud"), valor(row, "longitud"));
    }

    private PosicionResponse posicion(Row row) {
        return new PosicionResponse(row.getUuid("id_lectura"), row.getUuid("id_sensor"), row.getInstant("fecha_hora"),
                valor(row, "latitud"), valor(row, "longitud"));
    }

    private Double valor(Row row, String columna) {
        return row.isNull(columna) ? null : row.getDouble(columna);
    }

    private <T> T leer(Function<CqlSession, T> consulta) {
        try {
            return consulta.apply(sessionProvider.getObject());
        } catch (BeansException | DriverException exception) {
            throw new ServiceUnavailableException("Cassandra no está disponible", exception);
        }
    }
}
