package ar.edu.uade.logistica.desktop.controller;

import ar.edu.uade.logistica.contracts.reportes.ReporteRequest;
import ar.edu.uade.logistica.contracts.reportes.ReporteResponse;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

/** Rankings y desempeño (R27, R28, R31), predicción (R29), reportes (R11, R33, R34) y red del cliente (R30). */
public final class AnaliticaWindow {
    private static final String RUTAS = "Rutas con más incidentes";
    private static final String CLIENTES = "Clientes por volumen";
    private static final String DESEMPENO = "Desempeño por período";

    private final ComboBox<String> consulta = new ComboBox<>(FXCollections.observableArrayList(RUTAS, CLIENTES, DESEMPENO));
    private final TableView<Map<String, Object>> resultado = new TableView<>();
    private final Label estadoAnalitica = new Label();

    private final TableView<Map<String, Object>> predicciones = new TableView<>();
    private final Label estadoPredicciones = new Label();

    private final ComboBox<String> tipoReporte = new ComboBox<>(FXCollections.observableArrayList(
            "RANKING_RUTAS", "RANKING_CLIENTES", "DESEMPENO", "PREDICTIVO"));
    private final TextField clienteReporte = new TextField();
    private final TableView<ReporteResponse> reportes = new TableView<>();
    private final TableView<Map<String, Object>> contenidoReporte = new TableView<>();
    private final Label estadoReportes = new Label();

    private final TextField clienteRed = new TextField();
    private final ComboBox<String> vistaRed = new ComboBox<>(FXCollections.observableArrayList(
            "envios", "tramos", "operadores", "contenedores", "incidentes"));
    private final TableView<Map<String, Object>> tablaRed = new TableView<>();
    private final Label estadoRed = new Label();
    private Map<String, Object> red;

    public static void show() {
        new AnaliticaWindow().abrir();
    }

    private void abrir() {
        TabPane tabs = new TabPane(
                new Tab("Rankings y desempeño", pestaniaAnalitica()),
                new Tab("Predicción de atrasos", pestaniaPredicciones()),
                new Tab("Reportes", pestaniaReportes()),
                new Tab("Red del cliente", pestaniaRed()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Stage stage = new Stage();
        stage.setTitle("Analítica y reportes");
        stage.setScene(new Scene(tabs, 1180, Formulario.altoVentana(720)));
        stage.show();
        consultar();
        cargarPredicciones();
        cargarReportes();
    }

    private VBox pestaniaAnalitica() {
        consulta.setValue(CLIENTES);
        consulta.setOnAction(event -> consultar());
        return panel(new HBox(10, new Label("Consulta"), consulta), resultado, estadoAnalitica);
    }

    private VBox pestaniaPredicciones() {
        return panel(new HBox(10, boton("Generar predicción del día", this::generarPredicciones),
                boton("Ver validación", this::verValidacion)), predicciones, estadoPredicciones);
    }

    private VBox pestaniaReportes() {
        reportes.getColumns().add(columna("Fecha", r -> r.fechaSolicitud().toString()));
        reportes.getColumns().add(columna("Tipo", ReporteResponse::tipo));
        reportes.getColumns().add(columna("Estado", ReporteResponse::estado));
        reportes.getColumns().add(columna("Precio", r -> r.precio().toPlainString()));
        reportes.getColumns().add(columna("Factura", r -> r.idFactura() == null ? "" : r.idFactura().toString().substring(0, 8)));
        reportes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        reportes.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> verResultado());
        tipoReporte.setValue("PREDICTIVO");
        clienteReporte.setPromptText("UUID del cliente titular");
        HBox.setHgrow(clienteReporte, Priority.ALWAYS);
        HBox tablas = new HBox(12, reportes, contenidoReporte);
        HBox.setHgrow(reportes, Priority.ALWAYS);
        HBox.setHgrow(contenidoReporte, Priority.ALWAYS);
        return panel(new HBox(10, tipoReporte, clienteReporte, boton("Elegir…", this::elegirClienteReporte),
                boton("Solicitar", this::solicitarReporte), boton("Facturar", this::facturarReporte)), tablas, estadoReportes);
    }

    /** Lista los clientes activos con su nombre y su UUID. */
    private void elegirClienteReporte() {
        al(AppContext.get().api().clients(""), estadoReportes, pagina -> Formulario.elegirDeLista("Cliente titular",
                pagina.content().stream().filter(c -> "ACTIVO".equals(c.state())).toList(),
                c -> c.businessName() + " — " + c.id()).ifPresent(c -> clienteReporte.setText(c.id().toString())));
    }

    private VBox pestaniaRed() {
        clienteRed.setPromptText("UUID del cliente");
        HBox.setHgrow(clienteRed, Priority.ALWAYS);
        vistaRed.setValue("envios");
        vistaRed.setOnAction(event -> mostrarRed());
        return panel(new HBox(10, clienteRed, boton("Consultar", this::consultarRed), vistaRed), tablaRed, estadoRed);
    }

    private void consultar() {
        var api = AppContext.get().api();
        var operacion = switch (consulta.getValue()) {
            case RUTAS -> api.analytics("ranking-rutas");
            case DESEMPENO -> api.analytics("desempeno");
            default -> api.analytics("ranking-clientes");
        };
        al(operacion, estadoAnalitica, filas -> {
            llenar(resultado, filas);
            estadoAnalitica.setText(filas.size() + " fila(s)");
        });
    }

    private void cargarPredicciones() {
        al(AppContext.get().api().analytics("predicciones"), estadoPredicciones, filas -> {
            llenar(predicciones, filas);
            estadoPredicciones.setText(filas.size() + " envío(s) en tránsito con predicción");
        });
    }

    private void generarPredicciones() {
        al(AppContext.get().api().generatePredictions(), estadoPredicciones, ignorado -> cargarPredicciones());
    }

    private void verValidacion() {
        al(AppContext.get().api().analytics("predicciones/validacion"), estadoPredicciones, filas -> {
            if (filas.isEmpty()) estadoPredicciones.setText("Sin envíos entregados para validar");
            else estadoPredicciones.setText("Evaluados: " + filas.get(0).get("envios_evaluados")
                    + " · error absoluto medio: " + filas.get(0).get("error_absoluto_medio_horas") + " horas");
        });
    }

    private void cargarReportes() {
        al(AppContext.get().api().reports(), estadoReportes, lista -> {
            reportes.setItems(FXCollections.observableArrayList(lista));
            estadoReportes.setText(lista.size() + " reporte(s)");
        });
    }

    private void solicitarReporte() {
        UUID cliente = uuid(clienteReporte.getText());
        if (!clienteReporte.getText().isBlank() && cliente == null) {
            estadoReportes.setText("El cliente no es un UUID válido");
            return;
        }
        estadoReportes.setText("Generando...");
        al(AppContext.get().api().requestReport(new ReporteRequest(tipoReporte.getValue(), cliente, null, null)),
                estadoReportes, creado -> cargarReportes());
    }

    private void facturarReporte() {
        ReporteResponse reporte = reportes.getSelectionModel().getSelectedItem();
        if (reporte == null) {
            estadoReportes.setText("Elegí un reporte");
            return;
        }
        al(AppContext.get().api().invoiceReport(reporte.idSolicitud()), estadoReportes, facturado -> cargarReportes());
    }

    @SuppressWarnings("unchecked")
    private void verResultado() {
        ReporteResponse reporte = reportes.getSelectionModel().getSelectedItem();
        if (reporte == null) return;
        al(AppContext.get().api().reportResult(reporte.idSolicitud()), estadoReportes, contenido -> {
            if (contenido instanceof List<?> lista) llenar(contenidoReporte, (List<Map<String, Object>>) lista);
            else if (contenido instanceof Map<?, ?> mapa) llenar(contenidoReporte, List.of((Map<String, Object>) mapa));
        });
    }

    private void consultarRed() {
        UUID cliente = uuid(clienteRed.getText());
        if (cliente == null) {
            estadoRed.setText("El cliente no es un UUID válido");
            return;
        }
        estadoRed.setText("Consultando...");
        al(AppContext.get().api().clientNetwork(cliente), estadoRed, respuesta -> {
            red = respuesta;
            mostrarRed();
        });
    }

    @SuppressWarnings("unchecked")
    private void mostrarRed() {
        if (red == null) return;
        Map<String, Object> cliente = (Map<String, Object>) red.get("cliente");
        llenar(tablaRed, (List<Map<String, Object>>) red.get(vistaRed.getValue()));
        estadoRed.setText(cliente.get("razon_social") + " · " + ((List<?>) red.get("envios")).size() + " envío(s) · "
                + ((List<?>) red.get("operadores")).size() + " operador(es)");
    }

    /** Las columnas salen de la primera fila: sirve para cualquier consulta. */
    static void llenar(TableView<Map<String, Object>> tabla, List<Map<String, Object>> filas) {
        tabla.getColumns().clear();
        if (!filas.isEmpty()) {
            for (String clave : filas.get(0).keySet()) {
                TableColumn<Map<String, Object>, String> columna = new TableColumn<>(clave);
                columna.setCellValueFactory(fila -> new SimpleStringProperty(
                        fila.getValue().get(clave) == null ? "" : String.valueOf(fila.getValue().get(clave))));
                tabla.getColumns().add(columna);
            }
        }
        tabla.setItems(FXCollections.observableArrayList(filas));
    }

    private <T> void al(CompletableFuture<T> operacion, Label estado, Consumer<T> siFunciona) {
        operacion.whenComplete((valor, error) -> Platform.runLater(() -> {
            if (error != null) estado.setText(mensaje(error));
            else siFunciona.accept(valor);
        }));
    }

    private Button boton(String texto, Runnable accion) {
        Button boton = new Button(texto);
        boton.setOnAction(event -> accion.run());
        return boton;
    }

    private VBox panel(HBox filtros, javafx.scene.Node contenido, Label estado) {
        filtros.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        VBox panel = new VBox(12, filtros, contenido, estado);
        panel.setPadding(new Insets(14));
        return panel;
    }

    private static <T> TableColumn<T, String> columna(String titulo, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(fila -> new SimpleStringProperty(valor.apply(fila.getValue())));
        return columna;
    }

    private static UUID uuid(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : UUID.fromString(texto.strip());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String mensaje(Throwable error) {
        Throwable causa = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return causa.getMessage() == null ? "No se pudo completar la operación" : causa.getMessage();
    }
}
