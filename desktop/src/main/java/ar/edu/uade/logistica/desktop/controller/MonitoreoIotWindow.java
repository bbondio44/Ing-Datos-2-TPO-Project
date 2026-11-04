package ar.edu.uade.logistica.desktop.controller;

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
import javafx.scene.web.WebView;
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
import java.util.stream.Collectors;

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
    private final WebView mapa = new WebView();
    private final Label estadoEstados = new Label();
    private final Label estadoHistorico = new Label();
    private final Label estadoMapa = new Label();

    public static void show() {
        new MonitoreoIotWindow().abrir();
    }

    private void abrir() {
        TabPane tabs = new TabPane(
                new Tab("Estado de sensores", pestaniaEstados()),
                new Tab("Histórico de lecturas", pestaniaHistorico()),
                new Tab("Recorrido y mapa", pestaniaMapa()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Stage stage = new Stage();
        stage.setTitle("Monitoreo IoT");
        stage.setScene(new Scene(tabs, 1120, 720));
        stage.show();
        cargarEstados();
    }

    private VBox pestaniaEstados() {
        estados.getColumns().add(columna("Sensor", s -> corto(s.idSensor())));
        estados.getColumns().add(columna("Tipo", SensorEstadoResponse::tipo));
        estados.getColumns().add(columna("Conexión", SensorEstadoResponse::conexion));
        estados.getColumns().add(columna("Batería %", s -> numero(s.bateria())));
        estados.getColumns().add(columna("Última señal", s -> fecha(s.ultimaSenal())));
        estados.getColumns().add(columna("Min. sin señal", s -> s.minutosSinSenal() == null ? "" : s.minutosSinSenal().toString()));
        estados.getColumns().add(columna("Contenedor", s -> corto(s.idContenedor())));
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
        Button consultar = new Button("Consultar");
        consultar.setOnAction(event -> consultarHistorico());
        HBox filtros = new HBox(10, tipoConsulta, idConsulta, new Label("Desde"), desdeHistorico,
                new Label("Hasta"), hastaHistorico, consultar);
        return panel(filtros, lecturas, estadoHistorico);
    }

    private VBox pestaniaMapa() {
        contenedorMapa.setPrefWidth(320);
        Button ver = new Button("Ver recorrido");
        ver.setOnAction(event -> consultarRecorrido());
        HBox filtros = new HBox(10, new Label("Contenedor"), contenedorMapa, new Label("Desde"), desdeMapa,
                new Label("Hasta"), hastaMapa, ver);
        return panel(filtros, mapa, estadoMapa);
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
        estadoMapa.setText("Consultando...");
        AppContext.get().api().route(id, inicio(desdeMapa), fin(hastaMapa))
                .whenComplete((posiciones, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        estadoMapa.setText(mensaje(error));
                        return;
                    }
                    if (posiciones.isEmpty()) {
                        mapa.getEngine().loadContent("<p>Sin posiciones en ese rango.</p>");
                        estadoMapa.setText("Sin posiciones en ese rango");
                        return;
                    }
                    mapa.getEngine().loadContent(html(posiciones));
                    PosicionResponse ultima = posiciones.get(posiciones.size() - 1);
                    estadoMapa.setText(posiciones.size() + " posición(es). Última: " + fecha(ultima.fechaHora()));
                }));
    }

    /** Mapa con Leaflet: la línea es el recorrido en orden cronológico y el marcador, la última posición. */
    static String html(List<PosicionResponse> posiciones) {
        String puntos = posiciones.stream()
                .map(p -> String.format(Locale.ROOT, "[%f,%f]", p.latitud(), p.longitud()))
                .collect(Collectors.joining(","));
        return """
                <!doctype html><html><head><meta charset="utf-8">
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
                <style>html,body,#mapa{height:100%;margin:0}</style></head>
                <body><div id="mapa"></div><script>
                var puntos=[PUNTOS];
                var mapa=L.map('mapa');
                L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',
                  {attribution:'OpenStreetMap, CARTO',maxZoom:18}).addTo(mapa);
                var linea=L.polyline(puntos,{color:'#1769d2',weight:4}).addTo(mapa);
                puntos.forEach(function(p){L.circleMarker(p,{radius:3,color:'#1769d2'}).addTo(mapa);});
                L.circleMarker(puntos[0],{radius:7,color:'#2e9e5b',fillOpacity:1}).addTo(mapa).bindTooltip('Inicio');
                L.circleMarker(puntos[puntos.length-1],{radius:8,color:'#d2352b',fillOpacity:1}).addTo(mapa).bindTooltip('Última posición');
                if(puntos.length>1){mapa.fitBounds(linea.getBounds(),{padding:[30,30]});}else{mapa.setView(puntos[0],10);}
                </script></body></html>
                """.replace("PUNTOS", puntos);
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
