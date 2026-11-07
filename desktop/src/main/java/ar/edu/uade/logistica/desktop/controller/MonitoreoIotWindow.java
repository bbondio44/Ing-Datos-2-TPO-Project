package ar.edu.uade.logistica.desktop.controller;

import ar.edu.uade.logistica.contracts.logistics.ShipmentResponse;
import ar.edu.uade.logistica.contracts.route.RouteLegResponse;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import ar.edu.uade.logistica.contracts.iot.LecturaHistoricaResponse;
import ar.edu.uade.logistica.contracts.iot.PosicionResponse;
import ar.edu.uade.logistica.contracts.iot.SensorEstadoResponse;
import ar.edu.uade.logistica.desktop.AppContext;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/** Monitoreo IoT: estado de sensores (R22), histórico (R18, R19), recorrido y mapa (R20, R21). */
public final class MonitoreoIotWindow {
    private static final String SENSOR = "Sensor";
    private static final String CONTENEDOR = "Contenedor";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    private final TableView<SensorEstadoResponse> estados = new TableView<>();
    private final TableView<LecturaHistoricaResponse> lecturas = new TableView<>();
    private final ComboBox<String> tipoConsulta = new ComboBox<>(FXCollections.observableArrayList(SENSOR, CONTENEDOR));
    private final ComboBox<UUID> idConsulta = new ComboBox<>();
    private final ComboBox<UUID> contenedorMapa = new ComboBox<>();
    private final DatePicker desdeHistorico = new DatePicker(hoyUtc().minusDays(1));
    private final DatePicker hastaHistorico = new DatePicker(hoyUtc());
    private final DatePicker desdeMapa = new DatePicker(hoyUtc().minusDays(1));
    private final DatePicker hastaMapa = new DatePicker(hoyUtc());
    private final ListView<String> ruta = new ListView<>();
    private final TableView<PosicionResponse> posiciones = new TableView<>();
    private final Label ubicacion = new Label("Elegí un envío para ver su viaje");
    private ShipmentResponse envioElegido;
    private UUID contenedorDelEnvio;
    private List<RouteLegResponse> tramos = List.of();
    private boolean siguiendo;
    private final Label estadoEstados = new Label();
    private final Label estadoHistorico = new Label();
    private final Label estadoMapa = new Label();
    private final Map<UUID, String> codigos = new ConcurrentHashMap<>();

    public static void show() {
        new MonitoreoIotWindow().abrir();
    }

    private void abrir() {
        TabPane tabs = new TabPane(
                new Tab("Estado de sensores", pestaniaEstados()),
                new Tab("Histórico de lecturas", pestaniaHistorico()),
                new Tab("Seguimiento del viaje", pestaniaSeguimiento()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Stage stage = new Stage();
        stage.setTitle("Monitoreo IoT");
        stage.setScene(new Scene(tabs, 1120, Formulario.altoVentana(720)));
        stage.show();
        Timeline refresco = new Timeline(new KeyFrame(Duration.seconds(4), event -> refrescarViaje()));
        refresco.setCycleCount(Animation.INDEFINITE);
        refresco.play();
        stage.setOnHidden(event -> refresco.stop());
        cargarEstados();
    }

    private VBox pestaniaEstados() {
        estados.getColumns().add(columna("Sensor", s -> corto(s.idSensor())));
        estados.getColumns().add(columna("Tipo", SensorEstadoResponse::tipo));
        estados.getColumns().add(columna("Conexión", SensorEstadoResponse::conexion));
        estados.getColumns().add(columna("Batería %", s -> numero(s.bateria())));
        estados.getColumns().add(columna("Última señal", s -> fecha(s.ultimaSenal())));
        estados.getColumns().add(columna("Min. sin señal", s -> s.minutosSinSenal() == null ? "" : s.minutosSinSenal().toString()));
        estados.getColumns().add(columna("Contenedor", s -> nombreContenedor(s.idContenedor())));
        estados.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Button actualizar = new Button("Actualizar");
        actualizar.setOnAction(event -> cargarEstados());
        return panel(new HBox(10, actualizar), estados, estadoEstados);
    }

    private VBox pestaniaHistorico() {
        lecturas.getColumns().add(columna("Fecha y hora", l -> fecha(l.fechaHora())));
        lecturas.getColumns().add(columna("Sensor", l -> corto(l.idSensor())));
        lecturas.getColumns().add(columna("Temp. °C", l -> numero(l.temperatura())));
        lecturas.getColumns().add(columna("Humedad %", l -> numero(l.humedad())));
        lecturas.getColumns().add(columna("Vibración", l -> numero(l.vibracion())));
        lecturas.getColumns().add(columna("Batería %", l -> numero(l.bateria())));
        lecturas.getColumns().add(columna("Latitud", l -> numero(l.latitud())));
        lecturas.getColumns().add(columna("Longitud", l -> numero(l.longitud())));
        lecturas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tipoConsulta.setValue(SENSOR);
        tipoConsulta.setOnAction(event -> actualizarOpciones());
        idConsulta.setPrefWidth(320);
        idConsulta.setConverter(legible());
        Button consultar = new Button("Consultar");
        consultar.setOnAction(event -> consultarHistorico());
        HBox filtros = new HBox(10, tipoConsulta, idConsulta, new Label("Desde"), desdeHistorico,
                new Label("Hasta"), hastaHistorico, consultar);
        return panel(filtros, lecturas, estadoHistorico);
    }

    private VBox pestaniaSeguimiento() {
        posiciones.getColumns().add(columna("Fecha y hora", p -> fecha(p.fechaHora())));
        posiciones.getColumns().add(columna("Latitud", p -> coordenada(p.latitud())));
        posiciones.getColumns().add(columna("Longitud", p -> coordenada(p.longitud())));
        posiciones.getColumns().add(columna("Sensor", p -> corto(p.idSensor())));
        posiciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ruta.setPrefWidth(380);
        ubicacion.setStyle("-fx-font-weight: bold;");
        contenedorMapa.setPrefWidth(250);
        contenedorMapa.setConverter(legible());
        // Elegir a mano otro contenedor deja de mostrar el envío: la ruta de la izquierda ya no sería la suya.
        contenedorMapa.valueProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null && !ahora.equals(contenedorDelEnvio)) soltarEnvio();
        });
        Button elegir = new Button("Elegir envío…");
        elegir.setOnAction(event -> elegirEnvio());
        Button ver = new Button("Ver recorrido");
        ver.setOnAction(event -> consultarRecorrido());
        Button iniciar = new Button("Iniciar viaje");
        iniciar.setOnAction(event -> iniciarViaje());
        HBox filtros = new HBox(10, elegir, iniciar, new Label("Contenedor"), contenedorMapa, new Label("Desde"), desdeMapa,
                new Label("Hasta"), hastaMapa, ver);
        HBox cuerpo = new HBox(12, ruta, posiciones);
        HBox.setHgrow(posiciones, Priority.ALWAYS);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        return panel(filtros, new VBox(10, ubicacion, cuerpo), estadoMapa);
    }

    private void soltarEnvio() {
        envioElegido = null;
        contenedorDelEnvio = null;
        siguiendo = false;
        tramos = List.of();
        ruta.getItems().clear();
        posiciones.getItems().clear();
        ubicacion.setText("Consultá el recorrido del contenedor con Ver recorrido");
    }

    private void iniciarViaje() {
        if (envioElegido == null) {
            estadoMapa.setText("Elegí un envío con Elegir envío…");
            return;
        }
        AppContext.get().api().startTrip(envioElegido.id()).whenComplete((envio, error) -> Platform.runLater(() -> {
            if (error != null) {
                estadoMapa.setText(mensaje(error));
                return;
            }
            siguiendo = true;
            hastaMapa.setValue(hoyUtc());
            estadoMapa.setText("Viaje iniciado");
        }));
    }

    private void refrescarViaje() {
        if (!siguiendo || envioElegido == null) return;
        cargarTramos();
        if (contenedorMapa.getValue() != null) consultarRecorrido();
    }

    private void cargarTramos() {
        AppContext.get().api().routeLegs(envioElegido.id()).whenComplete((lista, error) -> Platform.runLater(() -> {
            if (error != null) {
                estadoMapa.setText(mensaje(error));
                return;
            }
            tramos = lista;
            ruta.setItems(FXCollections.observableArrayList(lineasDeRuta(lista)));
            if (!lista.isEmpty() && lista.stream().allMatch(t -> "FINALIZADO".equals(t.state()))) siguiendo = false;
            mostrarUbicacion();
        }));
    }

    /** El recorrido punto por punto: ✔ ya pasó, ● va hacia ahí, ○ falta. */
    static List<String> lineasDeRuta(List<RouteLegResponse> tramos) {
        if (tramos.isEmpty()) return List.of("Este envío no tiene tramos");
        List<String> lineas = new java.util.ArrayList<>();
        lineas.add(("PENDIENTE".equals(tramos.getFirst().state()) ? "○ " : "✔ ") + tramos.getFirst().origin());
        for (RouteLegResponse tramo : tramos) {
            String marca = switch (tramo.state()) {
                case "FINALIZADO" -> "✔ ";
                case "EN_CURSO" -> "● ";
                default -> "○ ";
            };
            lineas.add(marca + tramo.destination() + " — " + tramo.transportType());
        }
        return lineas;
    }

    private void mostrarUbicacion() {
        String texto;
        RouteLegResponse actual = tramos.stream().filter(t -> "EN_CURSO".equals(t.state())).findFirst().orElse(null);
        if (actual != null) texto = "Ubicación actual: entre " + actual.origin() + " y " + actual.destination();
        else if (tramos.isEmpty()) texto = "Última posición registrada";
        else if (tramos.stream().allMatch(t -> "FINALIZADO".equals(t.state()))) texto = "Llegó a " + tramos.getLast().destination();
        else if (tramos.stream().anyMatch(t -> "FINALIZADO".equals(t.state()))) texto = "Ubicación actual: en tránsito";
        else texto = "Todavía no salió";
        if (!posiciones.getItems().isEmpty()) {
            PosicionResponse ultima = posiciones.getItems().getFirst();
            texto += " (" + coordenada(ultima.latitud()) + ", " + coordenada(ultima.longitud()) + ")";
        }
        ubicacion.setText(texto);
    }

    private static String coordenada(Double valor) {
        return valor == null ? "" : String.format(Locale.ROOT, "%.4f", valor);
    }

    /** Elige el envío por cliente y ruta, y dibuja el recorrido de su contenedor. */
    private void elegirEnvio() {
        estadoMapa.setText("Cargando envíos...");
        AppContext.get().api().shipments("").whenComplete((pagina, error) -> Platform.runLater(() -> {
            if (error != null) {
                estadoMapa.setText(mensaje(error));
                return;
            }
            estadoMapa.setText("");
            Formulario.elegirDeLista("Envío", pagina.content(),
                    e -> e.clientName() + " — " + e.origin() + " → " + e.destination() + " — " + e.state())
                    .ifPresent(this::mostrarEnvio);
        }));
    }

    private void mostrarEnvio(ShipmentResponse envio) {
        AppContext.get().api().shipment(envio.id()).whenComplete((detalle, error) -> Platform.runLater(() -> {
            if (error != null) {
                estadoMapa.setText(mensaje(error));
                return;
            }
            envioElegido = envio;
            siguiendo = "EN_TRANSITO".equals(envio.state());
            posiciones.getItems().clear();
            cargarTramos();
            var asignaciones = detalle.containers();
            if (asignaciones.isEmpty()) {
                contenedorMapa.setValue(null);
                estadoMapa.setText("El envío no tiene contenedores asociados");
                return;
            }
            detalle.containers().forEach(c -> codigos.put(c.containerId(), c.containerCode()));
            var elegida = asignaciones.size() == 1 ? java.util.Optional.of(asignaciones.getFirst())
                    : Formulario.elegirDeLista("Contenedor del envío", asignaciones,
                    c -> c.containerCode() + " — " + c.containerType() + (c.active() ? " — vigente" : ""));
            elegida.ifPresent(c -> {
                // El rango cubre todo el viaje, hasta 31 días que es lo máximo que admite la consulta.
                LocalDate desde = c.startAt().atZone(ZoneOffset.UTC).toLocalDate();
                LocalDate hasta = c.endAt() == null ? hoyUtc() : c.endAt().atZone(ZoneOffset.UTC).toLocalDate();
                desdeMapa.setValue(desde.isBefore(hasta.minusDays(30)) ? hasta.minusDays(30) : desde);
                hastaMapa.setValue(hasta);
                contenedorDelEnvio = c.containerId();
                contenedorMapa.setValue(c.containerId());
                consultarRecorrido();
            });
        }));
    }

    private String nombreContenedor(UUID id) {
        return id == null ? "" : codigos.getOrDefault(id, corto(id));
    }

    /** Muestra cada UUID con lo que identifica: el código del contenedor o el tipo del sensor. */
    private StringConverter<UUID> legible() {
        return new StringConverter<>() {
            @Override public String toString(UUID id) {
                if (id == null) return "";
                if (codigos.containsKey(id)) return codigos.get(id) + " — " + corto(id);
                return estados.getItems().stream().filter(s -> id.equals(s.idSensor())).findFirst()
                        .map(s -> s.tipo() + " en " + nombreContenedor(s.idContenedor()) + " — " + corto(id))
                        .orElse(id.toString());
            }
            @Override public UUID fromString(String texto) { return null; }
        };
    }

    private VBox panel(HBox filtros, javafx.scene.Node contenido, Label estado) {
        filtros.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        VBox panel = new VBox(12, filtros, contenido, estado);
        panel.setPadding(new Insets(14));
        return panel;
    }

    private void cargarEstados() {
        estadoEstados.setText("Cargando...");
        AppContext.get().api().containers("").whenComplete((pagina, error) -> Platform.runLater(() -> {
            if (error == null) {
                pagina.content().forEach(c -> codigos.put(c.id(), c.internationalCode()));
                cargarSensores();
            } else {
                cargarCodigosDeSusEnvios();
            }
        }));
    }

    /** Un cliente no ve el catálogo de contenedores, pero sus envíos traen el código de los suyos. */
    private void cargarCodigosDeSusEnvios() {
        var api = AppContext.get().api();
        api.shipments("").thenCompose(pagina -> CompletableFuture.allOf(pagina.content().stream()
                        .map(envio -> api.shipment(envio.id()).thenAccept(detalle -> detalle.containers()
                                .forEach(c -> codigos.put(c.containerId(), c.containerCode()))))
                        .toArray(CompletableFuture[]::new)))
                .whenComplete((nada, error) -> Platform.runLater(this::cargarSensores));
    }

    private void cargarSensores() {
        AppContext.get().api().sensorStates().whenComplete((lista, error) -> Platform.runLater(() -> {
            if (error != null) {
                estadoEstados.setText(mensaje(error));
                return;
            }
            estados.setItems(FXCollections.observableArrayList(lista));
            long online = lista.stream().filter(s -> "ONLINE".equals(s.conexion())).count();
            estadoEstados.setText(lista.size() + " sensor(es), " + online + " online");
            actualizarOpciones();
            contenedorMapa.setItems(FXCollections.observableArrayList(contenedores()));
        }));
    }

    private void actualizarOpciones() {
        List<UUID> opciones = SENSOR.equals(tipoConsulta.getValue())
                ? estados.getItems().stream().map(SensorEstadoResponse::idSensor).toList()
                : contenedores();
        idConsulta.setItems(FXCollections.observableArrayList(opciones));
    }

    private List<UUID> contenedores() {
        return estados.getItems().stream().map(SensorEstadoResponse::idContenedor)
                .filter(Objects::nonNull).distinct().toList();
    }

    private void consultarHistorico() {
        UUID id = idConsulta.getValue();
        if (id == null) {
            estadoHistorico.setText("Elegí un " + tipoConsulta.getValue().toLowerCase(Locale.ROOT));
            return;
        }
        String recurso = SENSOR.equals(tipoConsulta.getValue()) ? "sensores" : "contenedores";
        estadoHistorico.setText("Consultando...");
        AppContext.get().api().readings(recurso, id, inicio(desdeHistorico), fin(hastaHistorico))
                .whenComplete((lista, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        estadoHistorico.setText(mensaje(error));
                        return;
                    }
                    lecturas.setItems(FXCollections.observableArrayList(lista));
                    estadoHistorico.setText(lista.size() + " lectura(s), ordenadas por fecha");
                }));
    }

    private void consultarRecorrido() {
        UUID id = contenedorMapa.getValue();
        if (id == null) {
            estadoMapa.setText("Elegí un contenedor");
            return;
        }
        if (posiciones.getItems().isEmpty()) estadoMapa.setText("Consultando...");
        AppContext.get().api().route(id, inicio(desdeMapa), fin(hastaMapa))
                .whenComplete((lista, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        estadoMapa.setText(mensaje(error));
                        return;
                    }
                    List<PosicionResponse> recientes = new java.util.ArrayList<>(lista);
                    java.util.Collections.reverse(recientes);
                    posiciones.setItems(FXCollections.observableArrayList(recientes));
                    mostrarUbicacion();
                    estadoMapa.setText(lista.isEmpty() ? "Sin posiciones en ese rango"
                            : lista.size() + " posición(es). Última: " + fecha(recientes.getFirst().fechaHora()));
                }));
    }

    private static <T> TableColumn<T, String> columna(String titulo, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(fila -> new SimpleStringProperty(valor.apply(fila.getValue())));
        return columna;
    }

    private static Instant inicio(DatePicker fecha) {
        return fecha.getValue().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant fin(DatePicker fecha) {
        return fecha.getValue().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
    }

    private static LocalDate hoyUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private static String corto(UUID id) {
        return id == null ? "" : id.toString().substring(0, 8);
    }

    private static String numero(Double valor) {
        return valor == null ? "" : String.format(Locale.ROOT, "%.2f", valor);
    }

    private static String fecha(Instant instante) {
        return instante == null ? "" : FECHA.format(instante);
    }

    private static String mensaje(Throwable error) {
        Throwable causa = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return causa.getMessage() == null ? "No se pudo completar la operación" : causa.getMessage();
    }
}
