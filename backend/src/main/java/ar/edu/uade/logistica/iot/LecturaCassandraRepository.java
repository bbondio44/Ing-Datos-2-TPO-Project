package ar.edu.uade.logistica.iot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import ar.edu.uade.logistica.common.ServiceUnavailableException;
import ar.edu.uade.logistica.contracts.iot.LecturaIotRequest;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/**
 * Escribe una lectura en las tablas de Cassandra: una tabla por consulta, con los datos desnormalizados.
 * Las escrituras son INSERT separados. Si una falla, las tablas quedan distintas hasta reintentar: el
 * emisor reenvía con el mismo id_lectura y, como INSERT es un upsert por primary key, no se duplica.
 */
@Repository
@ConditionalOnProperty(prefix = "app.cassandra", name = "enabled", havingValue = "true")
public class LecturaCassandraRepository {
    private final ObjectProvider<CqlSession> sessionProvider;

    public LecturaCassandraRepository(ObjectProvider<CqlSession> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    public void guardar(LecturaIotRequest lectura, UUID idContenedor) {
        try {
            CqlSession session = sessionProvider.getObject();
            escribirHistorial(session, lectura, idContenedor);
            escribirUltimoEstado(session, lectura, idContenedor);
        } catch (BeansException | DriverException exception) {
            throw new ServiceUnavailableException("Cassandra no está disponible, reintente la lectura", exception);
        }
    }

    private void escribirHistorial(CqlSession session, LecturaIotRequest lectura, UUID idContenedor) {
        Instant fechaHora = lectura.timestamp();
        // Partition key con bucket diario, siempre en UTC: acota el tamaño de la partición.
        LocalDate dia = fechaHora.atZone(ZoneOffset.UTC).toLocalDate();

        Map<String, Object> porSensor = new LinkedHashMap<>();
        porSensor.put("id_sensor", lectura.idSensor());
        porSensor.put("dia", dia);
        porSensor.put("fecha_hora", fechaHora);
        porSensor.put("id_lectura", lectura.idLectura());
        porSensor.put("id_contenedor", idContenedor);
        agregarMediciones(porSensor, lectura);
        insertar(session, "lecturas_por_sensor_dia", porSensor, null);

        Map<String, Object> porContenedor = new LinkedHashMap<>();
        porContenedor.put("id_contenedor", idContenedor);
        porContenedor.put("dia", dia);
        porContenedor.put("fecha_hora", fechaHora);
        porContenedor.put("id_sensor", lectura.idSensor());
        porContenedor.put("id_lectura", lectura.idLectura());
        agregarMediciones(porContenedor, lectura);
        insertar(session, "lecturas_por_contenedor_dia", porContenedor, null);

        if (lectura.tieneGps()) {
            Map<String, Object> posicion = new LinkedHashMap<>();
            posicion.put("id_contenedor", idContenedor);
            posicion.put("dia", dia);
            posicion.put("fecha_hora", fechaHora);
            posicion.put("id_sensor", lectura.idSensor());
            posicion.put("id_lectura", lectura.idLectura());
            posicion.put("latitud", lectura.latitud());
            posicion.put("longitud", lectura.longitud());
            insertar(session, "posiciones_por_contenedor_dia", posicion, null);
        }
    }

    private void escribirUltimoEstado(CqlSession session, LecturaIotRequest lectura, UUID idContenedor) {
        long microsegundos = ChronoUnit.MICROS.between(Instant.EPOCH, lectura.timestamp());

        Map<String, Object> ultimaLectura = new LinkedHashMap<>();
        ultimaLectura.put("id_sensor", lectura.idSensor());
        ultimaLectura.put("id_contenedor", idContenedor);
        ultimaLectura.put("id_lectura", lectura.idLectura());
        ultimaLectura.put("fecha_hora", lectura.timestamp());
        agregarMediciones(ultimaLectura, lectura);
        insertar(session, "ultima_lectura_por_sensor", ultimaLectura, microsegundos);

        // Sin GPS válido no se actualiza la última posición (C04).
        if (lectura.tieneGps()) {
            Map<String, Object> ultimaPosicion = new LinkedHashMap<>();
            ultimaPosicion.put("id_contenedor", idContenedor);
            ultimaPosicion.put("id_sensor", lectura.idSensor());
            ultimaPosicion.put("id_lectura", lectura.idLectura());
            ultimaPosicion.put("fecha_hora", lectura.timestamp());
            ultimaPosicion.put("latitud", lectura.latitud());
            ultimaPosicion.put("longitud", lectura.longitud());
            insertar(session, "ultima_posicion_por_contenedor", ultimaPosicion, microsegundos);
        }
    }

    /** Solo agrega las columnas con valor: escribir un null en Cassandra genera un tombstone. */
    private void agregarMediciones(Map<String, Object> columnas, LecturaIotRequest lectura) {
        agregarSiTieneValor(columnas, "temperatura", lectura.temperatura());
        agregarSiTieneValor(columnas, "humedad", lectura.humedad());
        agregarSiTieneValor(columnas, "vibracion", lectura.vibracion());
        agregarSiTieneValor(columnas, "latitud", lectura.latitud());
        agregarSiTieneValor(columnas, "longitud", lectura.longitud());
        agregarSiTieneValor(columnas, "bateria", lectura.bateria());
    }

    private void agregarSiTieneValor(Map<String, Object> columnas, String columna, Double valor) {
        if (valor != null) {
            columnas.put(columna, valor);
        }
    }

    private void insertar(CqlSession session, String tabla, Map<String, Object> columnas, Long timestampMicros) {
        String nombres = String.join(", ", columnas.keySet());
        String marcadores = String.join(", ", java.util.Collections.nCopies(columnas.size(), "?"));
        String cql = "INSERT INTO " + tabla + " (" + nombres + ") VALUES (" + marcadores + ")";
        if (timestampMicros != null) {
            // FUERA DE CLASE: USING TIMESTAMP. El timestamp por columna y last write wins se vieron en
            // clase; con esta sintaxis la aplicación lo provee, y así una lectura tardía no pisa a una nueva.
            cql += " USING TIMESTAMP " + timestampMicros;
        }
        session.execute(SimpleStatement.newInstance(cql, new ArrayList<>(columnas.values()).toArray()));
    }
}
