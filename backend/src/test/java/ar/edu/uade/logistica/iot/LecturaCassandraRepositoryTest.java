package ar.edu.uade.logistica.iot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.common.ServiceUnavailableException;
import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LecturaCassandraRepositoryTest {
    private static final UUID SENSOR = UUID.randomUUID();
    private static final UUID CONTENEDOR = UUID.randomUUID();
    private static final Instant ANTES_DE_MEDIANOCHE_UTC = Instant.parse("2026-10-21T23:59:30Z");

    @Mock ObjectProvider<CqlSession> sessionProvider;
    @Mock CqlSession session;

    LecturaCassandraRepository repository;

    @BeforeEach
    void setUp() {
        when(sessionProvider.getObject()).thenReturn(session);
        repository = new LecturaCassandraRepository(sessionProvider);
    }

    @Test
    void writesFiveTablesWhenTheReadingHasGps() {
        repository.guardar(lectura(6.2, -34.6, -58.4), CONTENEDOR);

        assertThat(sentencias()).extracting(LecturaCassandraRepositoryTest::tabla).containsExactly(
                "lecturas_por_sensor_dia", "lecturas_por_contenedor_dia", "posiciones_por_contenedor_dia",
                "ultima_lectura_por_sensor", "ultima_posicion_por_contenedor");
    }

    @Test
    void doesNotTouchPositionTablesWithoutGps() {
        repository.guardar(lectura(6.2, null, null), CONTENEDOR);

        assertThat(sentencias()).extracting(LecturaCassandraRepositoryTest::tabla).containsExactly(
                "lecturas_por_sensor_dia", "lecturas_por_contenedor_dia", "ultima_lectura_por_sensor");
    }

    @Test
    void doesNotWriteColumnsWithoutValueToAvoidTombstones() {
        repository.guardar(lectura(null, null, null, 80.0), CONTENEDOR);

        SimpleStatement historial = sentencias().get(0);
        assertThat(historial.getQuery()).contains("bateria").doesNotContain("temperatura", "latitud");
        assertThat(historial.getPositionalValues()).doesNotContainNull();
    }

    @Test
    void usesTheEventInstantAsWriteTimestampOnlyInLatestStateTables() {
        repository.guardar(lectura(6.2, -34.6, -58.4), CONTENEDOR);

        long micros = ANTES_DE_MEDIANOCHE_UTC.getEpochSecond() * 1_000_000L;
        List<SimpleStatement> sentencias = sentencias();
        assertThat(sentencias.subList(0, 3)).allSatisfy(s -> assertThat(s.getQuery()).doesNotContain("USING TIMESTAMP"));
        assertThat(sentencias.subList(3, 5)).allSatisfy(s -> assertThat(s.getQuery()).endsWith("USING TIMESTAMP " + micros));
    }

    @Test
    void calculatesThePartitionDayInUtc() {
        repository.guardar(lectura(6.2, null, null), CONTENEDOR);

        assertThat(sentencias().get(0).getPositionalValues()).contains(LocalDate.parse("2026-10-21"));
    }

    @Test
    void reportsUnavailableWhenCassandraFails() {
        when(session.execute(any(SimpleStatement.class))).thenThrow(new DriverTimeoutException("timeout"));

        assertThatThrownBy(() -> repository.guardar(lectura(6.2, null, null), CONTENEDOR))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    private List<SimpleStatement> sentencias() {
        ArgumentCaptor<SimpleStatement> captor = ArgumentCaptor.forClass(SimpleStatement.class);
        verify(session, atLeastOnce()).execute(captor.capture());
        return captor.getAllValues();
    }

    private static String tabla(SimpleStatement sentencia) {
        return sentencia.getQuery().split(" ")[2];
    }

    private static LecturaIotRequest lectura(Double temperatura, Double latitud, Double longitud) {
        return lectura(temperatura, latitud, longitud, null);
    }

    private static LecturaIotRequest lectura(Double temperatura, Double latitud, Double longitud, Double bateria) {
        return new LecturaIotRequest(UUID.randomUUID(), SENSOR, ANTES_DE_MEDIANOCHE_UTC,
                temperatura, null, null, bateria, latitud, longitud);
    }
}
