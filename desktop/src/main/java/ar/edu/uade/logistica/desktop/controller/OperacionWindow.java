package ar.edu.uade.logistica.desktop.controller;

import ar.edu.uade.logistica.contracts.alertas.AlertaResponse;
import ar.edu.uade.logistica.contracts.alertas.EventoResponse;
import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaRequest;
import ar.edu.uade.logistica.contracts.alertas.ReglaAlertaResponse;
import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.contracts.mensajeria.ConversacionRequest;
import ar.edu.uade.logistica.contracts.mensajeria.ConversacionResponse;
import ar.edu.uade.logistica.contracts.mensajeria.MensajeResponse;
import ar.edu.uade.logistica.contracts.mensajeria.ParticipanteRequest;
import ar.edu.uade.logistica.desktop.AppContext;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/** Alertas (R09, R23, R26, R32), reglas (R09), monitoreo de un envío (R17, R25) y mensajería (R40). */
public final class OperacionWindow {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    private final TableView<AlertaResponse> alertas = new TableView<>();
    private final ComboBox<String> estado = new ComboBox<>(FXCollections.observableArrayList(
            "", "ABIERTA", "RECONOCIDA", "CERRADA"));
    private final Label estadoAlertas = new Label();

    private final TableView<ReglaAlertaResponse> reglas = new TableView<>();
    private final TextField nombreRegla = new TextField();
    private final ComboBox<String> tipoRegla = new ComboBox<>(FXCollections.observableArrayList(
            "TEMPERATURA", "HUMEDAD", "VIBRACION", "BATERIA", "INACTIVIDAD", "ANOMALIA_TEMPERATURA"));
    private final ComboBox<String> operador = new ComboBox<>(FXCollections.observableArrayList(">", ">=", "<", "<="));
    private final TextField valorRegla = new TextField();
    private final ComboBox<String> severidad = new ComboBox<>(FXCollections.observableArrayList(
            "BAJA", "MEDIA", "ALTA", "CRITICA"));
    private final ComboBox<String> tipoContenedor = new ComboBox<>(FXCollections.observableArrayList(
            "Todos", "REEFER", "DRY", "OPEN_TOP"));
    private final Label estadoReglas = new Label();

    private final TextField idEnvio = new TextField();
    private final Label resumenEnvio = new Label();
    private final TableView<LecturaHistoricaResponse> lecturasEnvio = new TableView<>();
    private final TableView<EventoResponse> eventosEnvio = new TableView<>();

    private final TableView<ConversacionResponse> conversaciones = new TableView<>();
    private final TableView<MensajeResponse> mensajes = new TableView<>();
    private final TextField nuevoMensaje = new TextField();
    private final Label estadoMensajes = new Label();
    private final ComboBox<String> tipoConversacion = new ComboBox<>(FXCollections.observableArrayList("PRIVADA", "GRUPAL"));
    private final ComboBox<String> tipoDestinatario = new ComboBox<>(FXCollections.observableArrayList(
            "CLIENTE", "PROVEEDOR", "OPERADOR"));
    private final TextField asunto = new TextField();
    private final TextField destinatario = new TextField();

    public static void show() {
        new OperacionWindow().abrir();
    }

    private void abrir() {
        TabPane tabs = new TabPane(
                new Tab("Alertas", pestaniaAlertas()),
                new Tab("Reglas", pestaniaReglas()),
                new Tab("Monitoreo de envío", pestaniaMonitoreo()),
                new Tab("Mensajería", pestaniaMensajeria()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Stage stage = new Stage();
        stage.setTitle("Alertas y operación");
        stage.setScene(new Scene(tabs, 1180, 720));
        stage.show();
        cargarAlertas();
        cargarReglas();
        cargarConversaciones();
    }

    private VBox pestaniaAlertas() {
        alertas.getColumns().add(columna("Fecha", a -> fecha(a.fecha())));
        alertas.getColumns().add(columna("Tipo", AlertaResponse::tipoRegla));
        alertas.getColumns().add(columna("Severidad", AlertaResponse::severidad));
        alertas.getColumns().add(columna("Estado", AlertaResponse::estado));
        alertas.getColumns().add(columna("Ocurrencias", a -> String.valueOf(a.ocurrencias())));
        alertas.getColumns().add(columna("Mensaje", AlertaResponse::mensaje));
        alertas.getColumns().add(columna("Incidente", a -> a.idIncidente() == null ? "" : a.idIncidente().substring(0, 8)));
        alertas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        estado.setValue("");
        Button filtrar = boton("Filtrar", this::cargarAlertas);
        Button reconocer = boton("Reconocer", () -> cambiarEstado(true));
        Button cerrar = boton("Cerrar", () -> cambiarEstado(false));
        return panel(new HBox(10, new Label("Estado"), estado, filtrar, reconocer, cerrar), alertas, estadoAlertas);
    }

    private VBox pestaniaReglas() {
        reglas.getColumns().add(columna("Nombre", ReglaAlertaResponse::nombre));
        reglas.getColumns().add(columna("Tipo", ReglaAlertaResponse::tipoRegla));
        reglas.getColumns().add(columna("Condición", r -> r.operador() + " " + (r.umbral() != null ? r.umbral() : r.minutosInactividad() + " min")));
        reglas.getColumns().add(columna("Severidad", ReglaAlertaResponse::severidad));
        reglas.getColumns().add(columna("Contenedor", r -> r.tipoContenedor() == null ? "Todos" : r.tipoContenedor()));
        reglas.getColumns().add(columna("Activa", r -> r.activa() ? "Sí" : "No"));
        reglas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        nombreRegla.setPromptText("Nombre");
        valorRegla.setPromptText("Umbral o minutos");
        valorRegla.setPrefWidth(130);
        tipoRegla.setValue("TEMPERATURA");
        operador.setValue(">");
        severidad.setValue("ALTA");
        tipoContenedor.setValue("Todos");
        Button crear = boton("Crear regla", this::crearRegla);
        Button desactivar = boton("Desactivar", this::desactivarRegla);
        HBox.setHgrow(nombreRegla, Priority.ALWAYS);
        return panel(new HBox(10, nombreRegla, tipoRegla, operador, valorRegla, severidad, tipoContenedor, crear, desactivar),
                reglas, estadoReglas);
    }

    private VBox pestaniaMonitoreo() {
        lecturasEnvio.getColumns().add(columna("Última señal", l -> fecha(l.fechaHora())));
        lecturasEnvio.getColumns().add(columna("Sensor", l -> l.idSensor().toString().substring(0, 8)));
        lecturasEnvio.getColumns().add(columna("Temp. °C", l -> numero(l.temperatura())));
        lecturasEnvio.getColumns().add(columna("Batería %", l -> numero(l.bateria())));
        lecturasEnvio.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        eventosEnvio.getColumns().add(columna("Fecha", e -> fecha(e.fecha())));
        eventosEnvio.getColumns().add(columna("Evento", EventoResponse::tipo));
        eventosEnvio.getColumns().add(columna("Descripción", EventoResponse::descripcion));
        eventosEnvio.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        idEnvio.setPromptText("UUID del envío");
        HBox.setHgrow(idEnvio, Priority.ALWAYS);
        HBox tablas = new HBox(12, lecturasEnvio, eventosEnvio);
        HBox.setHgrow(lecturasEnvio, Priority.ALWAYS);
        HBox.setHgrow(eventosEnvio, Priority.ALWAYS);
        return panel(new HBox(10, idEnvio, boton("Consultar", this::consultarEnvio)), tablas, resumenEnvio);
    }

    private VBox pestaniaMensajeria() {
        conversaciones.getColumns().add(columna("Asunto", ConversacionResponse::asunto));
        conversaciones.getColumns().add(columna("Tipo", ConversacionResponse::tipo));
        conversaciones.getColumns().add(columna("Mensajes", c -> String.valueOf(c.cantidadMensajes())));
        conversaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        conversaciones.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> cargarMensajes());
        mensajes.getColumns().add(columna("Fecha", m -> fecha(m.fechaEnvio())));
        mensajes.getColumns().add(columna("De", MensajeResponse::remitente));
        mensajes.getColumns().add(columna("Mensaje", MensajeResponse::contenido));
        mensajes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        nuevoMensaje.setPromptText("Escribí un mensaje");
        HBox.setHgrow(nuevoMensaje, Priority.ALWAYS);
        HBox tablas = new HBox(12, conversaciones, mensajes);
        HBox.setHgrow(conversaciones, Priority.ALWAYS);
        HBox.setHgrow(mensajes, Priority.ALWAYS);
        asunto.setPromptText("Asunto");
        destinatario.setPromptText("UUID del destinatario");
        tipoConversacion.setValue("PRIVADA");
        tipoDestinatario.setValue("CLIENTE");
        HBox.setHgrow(destinatario, Priority.ALWAYS);
        HBox nueva = new HBox(10, tipoConversacion, asunto, tipoDestinatario, destinatario,
                boton("Nueva conversación", this::crearConversacion));
        nueva.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox.setVgrow(tablas, Priority.ALWAYS);
        return panel(new HBox(10, nuevoMensaje, boton("Enviar", this::enviarMensaje)),
                new VBox(10, nueva, tablas), estadoMensajes);
    }

    private void crearConversacion() {
        UUID id;
        try {
            id = UUID.fromString(destinatario.getText().strip());
        } catch (IllegalArgumentException exception) {
            estadoMensajes.setText("El destinatario no es un UUID válido");
            return;
        }
        if (asunto.getText().isBlank()) {
            estadoMensajes.setText("Escribí un asunto");
            return;
        }
        ConversacionRequest conversacion = new ConversacionRequest(tipoConversacion.getValue(), asunto.getText().strip(),
                "GRUPAL".equals(tipoConversacion.getValue()) ? asunto.getText().strip() : null, null,
                List.of(new ParticipanteRequest(tipoDestinatario.getValue(), id)));
        al(AppContext.get().api().createConversation(conversacion), estadoMensajes, creada -> {
            asunto.clear();
            destinatario.clear();
            cargarConversaciones();
        });
    }

    private void cargarAlertas() {
        estadoAlertas.setText("Cargando...");
        al(AppContext.get().api().alerts(estado.getValue()), estadoAlertas, lista -> {
            alertas.setItems(FXCollections.observableArrayList(lista));
            estadoAlertas.setText(lista.size() + " alerta(s)");
        });
    }

    private void cambiarEstado(boolean reconocer) {
        AlertaResponse alerta = alertas.getSelectionModel().getSelectedItem();
        if (alerta == null) {
            estadoAlertas.setText("Elegí una alerta");
            return;
        }
        var api = AppContext.get().api();
        al(reconocer ? api.acknowledgeAlert(alerta.id()) : api.closeAlert(alerta.id()), estadoAlertas,
                ignorado -> cargarAlertas());
    }

    private void cargarReglas() {
        al(AppContext.get().api().alertRules(), estadoReglas, lista -> {
            reglas.setItems(FXCollections.observableArrayList(lista));
            estadoReglas.setText(lista.size() + " regla(s)");
        });
    }

    private void crearRegla() {
        boolean inactividad = "INACTIVIDAD".equals(tipoRegla.getValue());
        double valor;
        try {
            valor = Double.parseDouble(valorRegla.getText().strip().replace(',', '.'));
        } catch (NumberFormatException exception) {
            estadoReglas.setText("El umbral debe ser un número");
            return;
        }
        ReglaAlertaRequest regla = new ReglaAlertaRequest(null, null, nombreRegla.getText().strip(), tipoRegla.getValue(),
                operador.getValue(), inactividad ? null : valor, inactividad ? (int) valor : null, severidad.getValue(), true,
                "Todos".equals(tipoContenedor.getValue()) ? null : tipoContenedor.getValue());
        al(AppContext.get().api().createAlertRule(regla), estadoReglas, creada -> cargarReglas());
    }

    private void desactivarRegla() {
        ReglaAlertaResponse regla = reglas.getSelectionModel().getSelectedItem();
        if (regla == null) {
            estadoReglas.setText("Elegí una regla");
            return;
        }
        al(AppContext.get().api().deactivateAlertRule(regla.idRegla()), estadoReglas, ignorado -> cargarReglas());
    }

    private void consultarEnvio() {
        UUID id;
        try {
            id = UUID.fromString(idEnvio.getText().strip());
        } catch (IllegalArgumentException exception) {
            resumenEnvio.setText("El id del envío no es un UUID válido");
            return;
        }
        resumenEnvio.setText("Consultando...");
        al(AppContext.get().api().shipmentMonitoring(id), resumenEnvio, envio -> {
            lecturasEnvio.setItems(FXCollections.observableArrayList(envio.ultimasLecturas()));
            eventosEnvio.setItems(FXCollections.observableArrayList(envio.eventos()));
            resumenEnvio.setText(envio.origen() + " → " + envio.destino() + " · " + envio.estado() + " · "
                    + envio.contenedores().size() + " contenedor(es) · " + envio.alertas().size() + " alerta(s) · "
                    + envio.incidentes().size() + " incidente(s)");
        });
    }

    private void cargarConversaciones() {
        al(AppContext.get().api().conversations(), estadoMensajes, lista -> {
            conversaciones.setItems(FXCollections.observableArrayList(lista));
            estadoMensajes.setText(lista.size() + " conversación(es)");
        });
    }

    private void cargarMensajes() {
        ConversacionResponse conversacion = conversaciones.getSelectionModel().getSelectedItem();
        if (conversacion == null) return;
        al(AppContext.get().api().messages(conversacion.id()), estadoMensajes,
                lista -> mensajes.setItems(FXCollections.observableArrayList(lista)));
    }

    private void enviarMensaje() {
        ConversacionResponse conversacion = conversaciones.getSelectionModel().getSelectedItem();
        String texto = nuevoMensaje.getText().strip();
        if (conversacion == null || texto.isEmpty()) {
            estadoMensajes.setText("Elegí una conversación y escribí un mensaje");
            return;
        }
        al(AppContext.get().api().sendMessage(conversacion.id(), texto), estadoMensajes, enviado -> {
            nuevoMensaje.clear();
            cargarMensajes();
        });
    }

    private <T> void al(CompletableFuture<T> operacion, Label estadoLabel, java.util.function.Consumer<T> siFunciona) {
        operacion.whenComplete((resultado, error) -> Platform.runLater(() -> {
            if (error != null) estadoLabel.setText(mensaje(error));
            else siFunciona.accept(resultado);
        }));
    }

    private Button boton(String texto, Runnable accion) {
        Button boton = new Button(texto);
        boton.setOnAction(event -> accion.run());
        return boton;
    }

    private VBox panel(HBox filtros, javafx.scene.Node contenido, Label estadoLabel) {
        filtros.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        VBox panel = new VBox(12, filtros, contenido, estadoLabel);
        panel.setPadding(new Insets(14));
        return panel;
    }

    private static <T> TableColumn<T, String> columna(String titulo, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(fila -> new SimpleStringProperty(valor.apply(fila.getValue())));
        return columna;
    }

    private static String fecha(Instant instante) {
        return instante == null ? "" : FECHA.format(instante);
    }

    private static String numero(Double valor) {
        return valor == null ? "" : String.format(Locale.ROOT, "%.2f", valor);
    }

    private static String mensaje(Throwable error) {
        Throwable causa = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return causa.getMessage() == null ? "No se pudo completar la operación" : causa.getMessage();
    }
}
