package ar.edu.uade.logistica.alertas;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import ar.edu.uade.logistica.common.ConflictException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.common.ServiceUnavailableException;
import ar.edu.uade.logistica.contracts.alertas.AlertaResponse;
import ar.edu.uade.logistica.contracts.alertas.EventoResponse;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "true")
public class AlertaMongoRepository {
    private static final int LIMITE_LISTADO = 200;

    private final MongoCollection<Document> alertas;
    private final MongoCollection<Document> eventos;

    public AlertaMongoRepository(MongoDatabase database) {
        this.alertas = database.getCollection("alertas");
        this.eventos = database.getCollection("eventos_logisticos");
    }

    /**
     * Si ya hay una alerta activa para esa regla y sensor, suma una ocurrencia ($inc). El filtro por
     * ultima_id_lectura evita contar dos veces una lectura reenviada.
     *
     * @return true si existía una alerta activa, en cuyo caso no hay que crear otra.
     */
    public boolean sumarOcurrencia(AlertaNueva alerta) {
        return ejecutar(() -> {
            String lectura = alerta.idLectura() == null ? null : alerta.idLectura().toString();
            Bson activa = and(eq("clave_dedup", alerta.claveDedup()), eq("activa", true));
            Bson filtro = lectura == null ? activa : and(activa, ne("ultima_id_lectura", lectura));
            long actualizadas = alertas.updateOne(filtro, combine(
                    inc("ocurrencias", 1),
                    set("ultima_ocurrencia", Date.from(alerta.ahora())),
                    set("valor_observado", alerta.valorObservado()),
                    set("ultima_id_lectura", lectura))).getMatchedCount();
            return actualizadas > 0 || alertas.find(activa).first() != null;
        });
    }

    /**
     * El índice único parcial sobre clave_dedup (solo activa: true) impide dos alertas activas iguales,
     * aunque dos lecturas lleguen a la vez.
     *
     * @return false si otra escritura ganó y la alerta ya existía.
     */
    public boolean insertar(AlertaNueva alerta, boolean conIncidente) {
        Date ahora = Date.from(alerta.ahora());
        String lectura = alerta.idLectura() == null ? null : alerta.idLectura().toString();
        Document historial = new Document("estado", "ABIERTA").append("fecha", ahora)
                .append("id_usuario", null).append("comentario", null);
        Document documento = new Document("_id", alerta.idAlerta())
                .append("id_regla", alerta.regla().idRegla().toString())
                .append("tipo_regla", alerta.regla().tipoRegla())
                .append("id_sensor", alerta.idSensor().toString())
                .append("id_contenedor", texto(alerta.idContenedor()))
                .append("id_envio", texto(alerta.idEnvio()))
                .append("id_cliente", texto(alerta.idCliente()))
                .append("id_lectura", lectura)
                .append("ultima_id_lectura", lectura)
                .append("id_evento", alerta.idEnvio() == null ? null : alerta.idEvento())
                .append("id_incidente", conIncidente ? alerta.idIncidente() : null)
                .append("operador", alerta.regla().operador())
                .append("umbral", alerta.umbral())
                .append("valor_observado", alerta.valorObservado())
                .append("severidad", alerta.regla().severidad())
                .append("estado", "ABIERTA")
                .append("activa", true)
                .append("mensaje", alerta.mensaje())
                .append("fecha", ahora)
                .append("fecha_lectura", alerta.fechaLectura() == null ? null : Date.from(alerta.fechaLectura()))
                .append("ultima_ocurrencia", ahora)
                .append("ocurrencias", 1)
                .append("clave_dedup", alerta.claveDedup())
                .append("historial_estados", List.of(historial));
        return insertarSiNoExiste(alertas, documento);
    }

    public void insertarEvento(AlertaNueva alerta, boolean conIncidente) {
        Date ahora = Date.from(alerta.ahora());
        Document payload = new Document("tipo_regla", alerta.regla().tipoRegla())
                .append("severidad", alerta.regla().severidad())
                .append("valor_observado", alerta.valorObservado());
        Document evento = new Document("_id", alerta.idEvento())
                .append("id_envio", alerta.idEnvio().toString())
                .append("id_contenedor", texto(alerta.idContenedor()))
                .append("tipo", "ALERTA_GENERADA")
                .append("fecha", ahora)
                .append("descripcion", alerta.mensaje())
                .append("payload", payload)
                .append("origen", alerta.idLectura() == null ? "SISTEMA" : "SENSOR")
                .append("id_alerta", alerta.idAlerta())
                .append("id_incidente", conIncidente ? alerta.idIncidente() : null)
                .append("id_correlacion", correlacion())
                .append("creado_en", ahora);
        insertarSiNoExiste(eventos, evento);
    }

    public List<AlertaResponse> listar(String estado, String idCliente) {
        List<Bson> filtros = new ArrayList<>();
        if (estado != null && !estado.isBlank()) filtros.add(eq("estado", estado));
        if (idCliente != null) filtros.add(eq("id_cliente", idCliente));
        Bson filtro = filtros.isEmpty() ? new Document() : and(filtros);
        return ejecutar(() -> alertas.find(filtro).sort(descending("fecha")).limit(LIMITE_LISTADO)
                .map(this::alerta).into(new ArrayList<>()));
    }

    public List<AlertaResponse> porEnvio(String idEnvio) {
        return ejecutar(() -> alertas.find(eq("id_envio", idEnvio)).sort(descending("fecha"))
                .map(this::alerta).into(new ArrayList<>()));
    }

    public List<EventoResponse> eventosPorEnvio(String idEnvio) {
        return ejecutar(() -> eventos.find(eq("id_envio", idEnvio)).sort(ascending("fecha"))
                .map(this::evento).into(new ArrayList<>()));
    }

    /** El filtro incluye el estado actual: una transición inválida no modifica nada. */
    public void reconocer(String id, String idUsuario, String comentario) {
        cambiarEstado(and(eq("_id", id), eq("estado", "ABIERTA")), id, combine(
                set("estado", "RECONOCIDA"),
                push("historial_estados", historial("RECONOCIDA", idUsuario, comentario))));
    }

    public void cerrar(String id, String idUsuario, String comentario) {
        cambiarEstado(and(eq("_id", id), eq("activa", true)), id, combine(
                set("estado", "CERRADA"),
                set("activa", false),
                push("historial_estados", historial("CERRADA", idUsuario, comentario))));
    }

    private void cambiarEstado(Bson filtro, String id, Bson cambio) {
        ejecutar(() -> {
            if (alertas.updateOne(filtro, cambio).getMatchedCount() > 0) return null;
            if (alertas.find(eq("_id", id)).first() == null) throw new NotFoundException("La alerta no existe");
            throw new ConflictException("La alerta no admite esa transición de estado");
        });
    }

    private Document historial(String estado, String idUsuario, String comentario) {
        return new Document("estado", estado).append("fecha", new Date())
                .append("id_usuario", idUsuario).append("comentario", comentario);
    }

    private boolean insertarSiNoExiste(MongoCollection<Document> coleccion, Document documento) {
        return ejecutar(() -> {
            try {
                coleccion.insertOne(documento);
                return true;
            } catch (MongoWriteException exception) {
                if (exception.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) return false;
                throw exception;
            }
        });
    }

    private AlertaResponse alerta(Document d) {
        return new AlertaResponse(d.getString("_id"), d.getString("id_regla"), d.getString("tipo_regla"),
                d.getString("id_sensor"), d.getString("id_contenedor"), d.getString("id_envio"),
                d.getString("id_cliente"), d.getString("id_lectura"), d.getString("id_incidente"),
                numero(d, "valor_observado"), numero(d, "umbral"), d.getString("severidad"), d.getString("estado"),
                Boolean.TRUE.equals(d.getBoolean("activa")), d.getString("mensaje"), instante(d, "fecha"),
                instante(d, "ultima_ocurrencia"), ((Number) d.get("ocurrencias")).intValue());
    }

    private EventoResponse evento(Document d) {
        return new EventoResponse(d.getString("_id"), d.getString("tipo"), instante(d, "fecha"),
                d.getString("descripcion"), d.getString("origen"), d.getString("id_alerta"), d.getString("id_incidente"));
    }

    private Double numero(Document documento, String campo) {
        Object valor = documento.get(campo);
        return valor instanceof Number numero ? numero.doubleValue() : null;
    }

    private Instant instante(Document documento, String campo) {
        Date fecha = documento.getDate(campo);
        return fecha == null ? null : fecha.toInstant();
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private String correlacion() {
        String id = MDC.get("correlationId");
        return id == null ? "sin-correlacion" : id;
    }

    private <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (MongoException exception) {
            throw new ServiceUnavailableException("MongoDB no está disponible", exception);
        }
    }
}
