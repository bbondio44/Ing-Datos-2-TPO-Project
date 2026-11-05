package ar.edu.uade.logistica.mensajeria;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import ar.edu.uade.logistica.common.BadRequestException;
import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.common.ServiceUnavailableException;
import ar.edu.uade.logistica.contracts.mensajeria.ConversacionRequest;
import ar.edu.uade.logistica.contracts.mensajeria.ConversacionResponse;
import ar.edu.uade.logistica.contracts.mensajeria.MensajeRequest;
import ar.edu.uade.logistica.contracts.mensajeria.MensajeResponse;
import ar.edu.uade.logistica.contracts.mensajeria.ParticipanteRequest;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Conversaciones y mensajes en colecciones separadas: una conversación puede crecer sin límite y un
 * documento no puede pasar los 16 MB. En la conversación solo se copia el último mensaje y el contador.
 * Toda consulta filtra por ids_participantes, así un usuario solo ve sus conversaciones.
 */
@Service
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "true")
public class MensajeriaService {
    /** Los usuarios internos participan en nombre de la empresa (código EMP de catalogo_ids.csv). */
    private static final String EMPRESA = "b1052e98-971b-52d6-9913-aed4e195c326";
    private static final String NOMBRE_EMPRESA = "Grupo 4 Logística";
    private static final int TAMANIO_MAXIMO = 100;
    private static final int LARGO_EXTRACTO = 60;

    private static final Map<String, String> TABLAS = Map.of(
            "CLIENTE", "SELECT razon_social FROM tp_logistica.cliente WHERE id_cliente = ?",
            "PROVEEDOR", "SELECT razon_social FROM tp_logistica.proveedor WHERE id_proveedor = ?",
            "OPERADOR", "SELECT razon_social FROM tp_logistica.operador_logistico WHERE id_operador = ?");

    private final JdbcTemplate jdbcTemplate;
    private final MongoCollection<Document> conversaciones;
    private final MongoCollection<Document> mensajes;

    public MensajeriaService(JdbcTemplate jdbcTemplate, MongoDatabase database) {
        this.jdbcTemplate = jdbcTemplate;
        this.conversaciones = database.getCollection("conversaciones");
        this.mensajes = database.getCollection("mensajes");
    }

    public List<ConversacionResponse> bandeja(AuthenticatedUser user) {
        String clave = identidad(user).clave();
        return ejecutar(() -> conversaciones.find(eq("ids_participantes", clave))
                .sort(descending("ultimo_mensaje.fecha"))
                .map(this::conversacion).into(new ArrayList<>()));
    }

    public ConversacionResponse crear(ConversacionRequest pedido, AuthenticatedUser user) {
        Identidad creador = identidad(user);
        Map<String, Document> participantes = new LinkedHashMap<>();
        participantes.put(creador.clave(), participante(creador.tipo(), creador.idEntidad(), user.userId().toString(),
                creador.nombre()));
        for (ParticipanteRequest invitado : pedido.participantes()) {
            String nombre = nombreDe(invitado);
            participantes.putIfAbsent(invitado.tipo() + ":" + invitado.idEntidad(),
                    participante(invitado.tipo(), invitado.idEntidad().toString(), null, nombre));
        }
        if (participantes.size() < 2) {
            throw new BadRequestException("La conversación necesita otro participante");
        }
        Document documento = new Document("_id", UUID.randomUUID().toString())
                .append("tipo", pedido.tipo())
                .append("asunto", pedido.asunto())
                .append("nombre_grupo", pedido.nombreGrupo())
                .append("id_envio", pedido.idEnvio() == null ? null : pedido.idEnvio().toString())
                .append("participantes", new ArrayList<>(participantes.values()))
                .append("ids_participantes", new ArrayList<>(participantes.keySet()))
                .append("creada_por", user.userId().toString())
                .append("fecha_creacion", new Date())
                .append("estado", "ABIERTA")
                .append("cantidad_mensajes", 0);
        ejecutar(() -> conversaciones.insertOne(documento));
        return conversacion(documento);
    }

    /** Paginado con sort, limit y un cursor por fecha: trae los mensajes anteriores a "antesDe". */
    public List<MensajeResponse> mensajes(String idConversacion, Instant antesDe, int tamanio, AuthenticatedUser user) {
        exigirParticipante(idConversacion, identidad(user).clave());
        Bson filtro = antesDe == null ? eq("id_conversacion", idConversacion)
                : and(eq("id_conversacion", idConversacion), lt("fecha_envio", Date.from(antesDe)));
        int limite = Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);
        return ejecutar(() -> mensajes.find(filtro).sort(descending("fecha_envio")).limit(limite)
                .map(this::mensaje).into(new ArrayList<>()));
    }

    public MensajeResponse enviar(String idConversacion, MensajeRequest pedido, AuthenticatedUser user) {
        Identidad remitente = identidad(user);
        exigirParticipante(idConversacion, remitente.clave());
        Date ahora = new Date();
        Document documento = new Document("_id", pedido.idMensaje().toString())
                .append("id_conversacion", idConversacion)
                .append("remitente", participante(remitente.tipo(), remitente.idEntidad(), user.userId().toString(),
                        remitente.nombre()))
                .append("fecha_envio", ahora)
                .append("contenido", pedido.contenido())
                .append("tipo", "TEXTO")
                .append("estado", "ENVIADO");
        if (insertarNuevo(documento)) {
            String extracto = pedido.contenido().length() <= LARGO_EXTRACTO ? pedido.contenido()
                    : pedido.contenido().substring(0, LARGO_EXTRACTO);
            Document ultimo = new Document("fecha", ahora).append("remitente", remitente.clave())
                    .append("extracto", extracto);
            ejecutar(() -> conversaciones.updateOne(eq("_id", idConversacion),
                    combine(inc("cantidad_mensajes", 1), set("ultimo_mensaje", ultimo))));
        }
        return mensaje(documento);
    }

    /** Si la conversación es ajena responde 404, igual que si no existiera. */
    private void exigirParticipante(String idConversacion, String clave) {
        Document encontrada = ejecutar(() -> conversaciones
                .find(and(eq("_id", idConversacion), eq("ids_participantes", clave))).first());
        if (encontrada == null) throw new NotFoundException("La conversación no existe");
    }

    private boolean insertarNuevo(Document documento) {
        return ejecutar(() -> {
            try {
                mensajes.insertOne(documento);
                return true;
            } catch (MongoWriteException exception) {
                if (exception.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) return false;
                throw exception;
            }
        });
    }

    private Identidad identidad(AuthenticatedUser user) {
        Map<String, Object> fila = jdbcTemplate.queryForMap("""
                SELECT id_cliente, id_proveedor, id_operador FROM tp_logistica.usuario WHERE id_usuario = ?
                """, user.userId());
        if (fila.get("id_cliente") != null) return externa("CLIENTE", (UUID) fila.get("id_cliente"));
        if (fila.get("id_proveedor") != null) return externa("PROVEEDOR", (UUID) fila.get("id_proveedor"));
        if (fila.get("id_operador") != null) return externa("OPERADOR", (UUID) fila.get("id_operador"));
        return new Identidad("INTERNO", EMPRESA, NOMBRE_EMPRESA);
    }

    private Identidad externa(String tipo, UUID id) {
        return new Identidad(tipo, id.toString(), nombreDe(new ParticipanteRequest(tipo, id)));
    }

    /** MongoDB no tiene FK: se valida contra PostgreSQL antes de escribir (D03). */
    private String nombreDe(ParticipanteRequest participante) {
        List<String> nombres = jdbcTemplate.queryForList(TABLAS.get(participante.tipo()), String.class,
                participante.idEntidad());
        if (nombres.isEmpty()) throw new NotFoundException("El participante no existe");
        return nombres.get(0);
    }

    private Document participante(String tipo, String idEntidad, String idUsuario, String nombre) {
        return new Document("tipo", tipo).append("id_entidad", idEntidad)
                .append("id_usuario", idUsuario).append("nombre", nombre);
    }

    private ConversacionResponse conversacion(Document d) {
        Document ultimo = d.get("ultimo_mensaje", Document.class);
        List<String> nombres = d.getList("participantes", Document.class).stream()
                .map(p -> p.getString("nombre")).toList();
        return new ConversacionResponse(d.getString("_id"), d.getString("tipo"), d.getString("asunto"),
                d.getString("nombre_grupo"), d.getString("id_envio"), nombres,
                ((Number) d.get("cantidad_mensajes")).intValue(),
                ultimo == null ? null : ultimo.getDate("fecha").toInstant(),
                ultimo == null ? null : ultimo.getString("extracto"));
    }

    private MensajeResponse mensaje(Document d) {
        return new MensajeResponse(d.getString("_id"), d.get("remitente", Document.class).getString("nombre"),
                d.getDate("fecha_envio").toInstant(), d.getString("contenido"));
    }

    private void ejecutar(Runnable operacion) {
        ejecutar(() -> {
            operacion.run();
            return null;
        });
    }

    private <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (MongoWriteException exception) {
            throw exception;
        } catch (MongoException exception) {
            throw new ServiceUnavailableException("MongoDB no está disponible", exception);
        }
    }

    private record Identidad(String tipo, String idEntidad, String nombre) {
        String clave() {
            return tipo + ":" + idEntidad;
        }
    }
}
