package ar.edu.uade.logistica.iot;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ar.edu.uade.logistica.common.BadRequestException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IotConsultaServiceTest {
    private static final Instant DESDE = Instant.parse("2026-10-20T22:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-10-21T02:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-10-21T12:00:00Z");
    private static final UUID SENSOR = UUID.randomUUID();

    @Mock JdbcTemplate jdbcTemplate;
    @Mock LecturaConsultaCassandraRepository lecturas;

    AuthenticatedUser admin;
    AuthenticatedUser cliente;
    IotConsultaService service;

    @BeforeEach
    void setUp() {
        service = new IotConsultaService(jdbcTemplate, lecturas);
        admin = mock(AuthenticatedUser.class);
        when(admin.permissions()).thenReturn(Set.of("ENVIOS_LEER_TODOS"));
        cliente = mock(AuthenticatedUser.class);
        when(cliente.permissions()).thenReturn(Set.of("LOGISTICA_LEER"));
        when(cliente.clientId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void aRangeThatCrossesMidnightQueriesOnePartitionPerDay() {
        assertThat(LecturaConsultaCassandraRepository.dias(DESDE, HASTA))
                .containsExactly(LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-21"));
    }

    @Test
    void rejectsAnInvertedRange() {
        assertThatThrownBy(() -> service.lecturasPorSensor(SENSOR, HASTA, DESDE, admin))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsARangeLongerThan31Days() {
        assertThatThrownBy(() -> service.lecturasPorSensor(SENSOR, DESDE, DESDE.plus(Duration.ofDays(32)), admin))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aClientCannotReadASensorOfAnotherClient() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(0);

        assertThatThrownBy(() -> service.lecturasPorSensor(SENSOR, DESDE, HASTA, cliente))
                .isInstanceOf(NotFoundException.class);
        verify(lecturas, never()).lecturasPorSensor(any(), any(), any());
    }

    @Test
    void aClientCanReadItsOwnSensor() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);
        when(lecturas.lecturasPorSensor(SENSOR, DESDE, HASTA)).thenReturn(List.of());

        assertThat(service.lecturasPorSensor(SENSOR, DESDE, HASTA, cliente)).isEmpty();
    }

    @Test
    void aSensorThatNeverReportedHasNoData() {
        var estado = IotConsultaService.estado(sensor(), Optional.empty(), 120, AHORA);

        assertThat(estado.conexion()).isEqualTo("SIN_DATOS");
        assertThat(estado.ultimaSenal()).isNull();
    }

    @Test
    void aSensorIsOnlineUpToTheInactivityLimit() {
        var estado = IotConsultaService.estado(sensor(), Optional.of(lectura(AHORA.minus(Duration.ofMinutes(120)))), 120, AHORA);

        assertThat(estado.conexion()).isEqualTo("ONLINE");
        assertThat(estado.bateria()).isEqualTo(80.0);
    }

    @Test
    void aSensorIsOfflineAfterTheInactivityLimit() {
        var estado = IotConsultaService.estado(sensor(), Optional.of(lectura(AHORA.minus(Duration.ofMinutes(121)))), 120, AHORA);

        assertThat(estado.conexion()).isEqualTo("OFFLINE");
        assertThat(estado.minutosSinSenal()).isEqualTo(121);
    }

    private IotConsultaService.SensorBasico sensor() {
        return new IotConsultaService.SensorBasico(SENSOR, "MULTIPARAMETRO");
    }

    private LecturaHistoricaResponse lectura(Instant fechaHora) {
        return new LecturaHistoricaResponse(UUID.randomUUID(), SENSOR, UUID.randomUUID(), fechaHora,
                6.0, null, null, 80.0, null, null);
    }
}
