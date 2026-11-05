package ar.edu.uade.logistica.iot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ar.edu.uade.logistica.alertas.AlertaService;
import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IotServiceTest {
    private static final UUID CONTENEDOR = UUID.randomUUID();

    @Mock JdbcTemplate jdbcTemplate;
    @Mock LecturaCassandraRepository lecturas;
    @Mock ObjectProvider<AlertaService> alertas;

    IotService service;
    LecturaIotRequest lectura;

    @BeforeEach
    void setUp() {
        service = new IotService(jdbcTemplate, lecturas, alertas);
        lectura = new LecturaIotRequest(UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-10-21T12:00:00Z"),
                6.2, null, null, null, null, null);
    }

    @Test
    void savesTheReadingWithTheContainerAssignedAtThatInstant() {
        sensorExiste(1);
        when(jdbcTemplate.queryForList(anyString(), eq(UUID.class), any(), any(), any())).thenReturn(List.of(CONTENEDOR));

        var respuesta = service.registrar(lectura);

        verify(lecturas).guardar(lectura, CONTENEDOR);
        assertThat(respuesta.idLectura()).isEqualTo(lectura.idLectura());
        assertThat(respuesta.idContenedor()).isEqualTo(CONTENEDOR);
    }

    @Test
    void rejectsAnUnknownSensor() {
        sensorExiste(0);

        assertThatThrownBy(() -> service.registrar(lectura)).isInstanceOf(NotFoundException.class);
        verify(lecturas, never()).guardar(any(), any());
    }

    @Test
    void rejectsASensorWithoutAssignmentAtThatInstant() {
        sensorExiste(1);
        when(jdbcTemplate.queryForList(anyString(), eq(UUID.class), any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.registrar(lectura)).isInstanceOf(ConflictException.class);
        verify(lecturas, never()).guardar(any(), any());
    }

    private void sensorExiste(int cantidad) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(cantidad);
    }
}
