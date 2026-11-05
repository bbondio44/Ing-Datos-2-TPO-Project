package ar.edu.uade.logistica.desktop.controller;

import ar.edu.uade.logistica.contracts.auth.SessionResponse;
import ar.edu.uade.logistica.contracts.client.ClientRequest;
import ar.edu.uade.logistica.contracts.client.ClientResponse;
import ar.edu.uade.logistica.contracts.organization.OrganizationRequest;
import ar.edu.uade.logistica.contracts.organization.OrganizationResponse;
import ar.edu.uade.logistica.contracts.master.LocationRequest;
import ar.edu.uade.logistica.contracts.master.LocationResponse;
import ar.edu.uade.logistica.contracts.master.VehicleRequest;
import ar.edu.uade.logistica.contracts.master.VehicleResponse;
import ar.edu.uade.logistica.desktop.AppContext;
import ar.edu.uade.logistica.desktop.DesktopApplication;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

public class MainController {
    @FXML private Label userLabel;
    @FXML private Label statusLabel;
    @FXML private TextField searchField;
    @FXML private TableView<ClientResponse> clientsTable;
    @FXML private TableColumn<ClientResponse, String> nameColumn;
    @FXML private TableColumn<ClientResponse, String> cuitColumn;
    @FXML private TableColumn<ClientResponse, String> countryColumn;
    @FXML private TableColumn<ClientResponse, String> stateColumn;
    @FXML private Button editButton;
    @FXML private Button deactivateButton;

    @FXML
    private void initialize() {
        userLabel.setText(AppContext.get().session().displayName());
        nameColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().businessName()));
        cuitColumn.setCellValueFactory(row -> new SimpleStringProperty(value(row.getValue().cuit())));
        countryColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().country()));
        stateColumn.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().state()));

        boolean canWrite = AppContext.get().session().permissions().contains("CLIENTES_ESCRIBIR");
        editButton.disableProperty().bind(clientsTable.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> !canWrite)));
        deactivateButton.disableProperty().bind(clientsTable.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> !canWrite)));
        searchField.setOnAction(event -> refresh());
        refresh();
    }

    @FXML
    private void refresh() {
        statusLabel.setText("Cargando clientes...");
        AppContext.get().api().clients(searchField.getText())
                .whenComplete((page, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        statusLabel.setText(message(error));
                        return;
                    }
                    clientsTable.setItems(FXCollections.observableArrayList(page.content()));
                    statusLabel.setText(page.totalElements() + " cliente(s)");
                }));
    }

    @FXML
    private void createClient() {
        clientDialog("Alta de cliente", null).ifPresent(request -> saveClient(null, request));
    }

    @FXML
    private void editClient() {
        ClientResponse selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        clientDialog("Editar cliente", selected).ifPresent(request -> saveClient(selected, request));
    }

    private Optional<ClientRequest> clientDialog(String title, ClientResponse initial) {
        Dialog<ClientRequest> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Datos comerciales");
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField name = new TextField(initial == null ? "" : value(initial.businessName()));
        TextField cuit = new TextField(initial == null ? "" : value(initial.cuit()));
        TextField email = new TextField(initial == null ? "" : value(initial.email()));
        TextField phone = new TextField(initial == null ? "" : value(initial.phone()));
        TextField address = new TextField(initial == null ? "" : value(initial.address()));
        TextField country = new TextField(initial == null ? "Argentina" : value(initial.country()));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Razón social"), name);
        grid.addRow(1, new Label("CUIT"), cuit);
        grid.addRow(2, new Label("Email"), email);
        grid.addRow(3, new Label("Teléfono"), phone);
        grid.addRow(4, new Label("Dirección"), address);
        grid.addRow(5, new Label("País"), country);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.disableProperty().bind(name.textProperty().isEmpty().or(country.textProperty().isEmpty()));
        dialog.setResultConverter(button -> button == save
                ? new ClientRequest(name.getText().strip(), nullable(cuit.getText()), nullable(email.getText()),
                    nullable(phone.getText()), nullable(address.getText()), country.getText().strip())
                : null);
        return dialog.showAndWait();
    }

    private void saveClient(ClientResponse initial, ClientRequest request) {
        statusLabel.setText("Guardando cliente...");
        var operation = initial == null
                ? AppContext.get().api().createClient(request)
                : AppContext.get().api().updateClient(initial.id(), request);
        operation.whenComplete((saved, error) -> Platform.runLater(() -> {
            if (error != null) statusLabel.setText(message(error));
            else refresh();
        }));
    }

    @FXML
    private void deactivateClient() {
        ClientResponse selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null || "INACTIVO".equals(selected.state())) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "El cliente quedará inactivo y conservará su historial.", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Desactivar cliente");
        alert.setHeaderText(selected.businessName());
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        statusLabel.setText("Desactivando cliente...");
        AppContext.get().api().deactivateClient(selected.id()).whenComplete((ignored, error) ->
                Platform.runLater(() -> {
                    if (error != null) statusLabel.setText(message(error));
                    else refresh();
                }));
    }

    @FXML
    private void showSessions() {
        statusLabel.setText("Cargando sesiones...");
        AppContext.get().api().sessions().whenComplete((sessions, error) -> Platform.runLater(() -> {
            if (error != null) {
                statusLabel.setText(message(error));
                return;
            }
            showSessionDialog(sessions);
            statusLabel.setText("Sesiones actualizadas");
        }));
    }

    private void showSessionDialog(List<SessionResponse> sessions) {
        Dialog<SessionResponse> dialog = new Dialog<>();
        dialog.setTitle("Sesiones del usuario");
        dialog.setHeaderText("Seleccione una sesión activa para revocarla");
        ButtonType revoke = new ButtonType("Revocar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(revoke, ButtonType.CLOSE);

        ListView<SessionResponse> list = new ListView<>(FXCollections.observableArrayList(sessions));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        list.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(SessionResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.state() + "  •  " + formatter.format(item.startedAt())
                        + "  •  vence " + formatter.format(item.expiresAt()));
            }
        });
        list.setPrefSize(620, 280);
        dialog.getDialogPane().setContent(list);

        Node revokeButton = dialog.getDialogPane().lookupButton(revoke);
        revokeButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            SessionResponse selected = list.getSelectionModel().getSelectedItem();
            return selected == null || !"ACTIVA".equals(selected.state());
        }, list.getSelectionModel().selectedItemProperty()));
        dialog.setResultConverter(button -> button == revoke ? list.getSelectionModel().getSelectedItem() : null);
        dialog.showAndWait().ifPresent(session -> AppContext.get().api().revokeSession(session.id())
                .whenComplete((ignored, error) -> Platform.runLater(() ->
                        statusLabel.setText(error == null ? "Sesión revocada" : message(error)))));
    }

    @FXML
    private void showSuppliers() {
        showOrganizations("Proveedores", "proveedores", true);
    }

    @FXML
    private void showOperators() {
        showOrganizations("Operadores logísticos", "operadores", false);
    }

    private void showOrganizations(String title, String resource, boolean supplier) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Administración de " + title.toLowerCase());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField search = new TextField();
        search.setPromptText("Buscar por razón social o CUIT");
        Button searchButton = new Button("Buscar");
        Button create = new Button("Nuevo");
        Button edit = new Button("Editar");
        Button deactivate = new Button("Desactivar");
        Label status = new Label("Cargando...");

        TableView<OrganizationResponse> table = new TableView<>();
        TableColumn<OrganizationResponse, String> name = new TableColumn<>("Razón social");
        name.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().businessName()));
        TableColumn<OrganizationResponse, String> cuit = new TableColumn<>("CUIT");
        cuit.setCellValueFactory(row -> new SimpleStringProperty(value(row.getValue().cuit())));
        TableColumn<OrganizationResponse, String> country = new TableColumn<>("País");
        country.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().country()));
        TableColumn<OrganizationResponse, String> detail = new TableColumn<>(supplier ? "Servicio" : "Estado");
        detail.setCellValueFactory(row -> new SimpleStringProperty(
                supplier ? value(row.getValue().serviceType()) : row.getValue().state()));
        TableColumn<OrganizationResponse, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().state()));
        if (supplier) table.getColumns().addAll(name, cuit, country, detail, state);
        else table.getColumns().addAll(name, cuit, country, detail);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefSize(850, 430);

        boolean canWrite = AppContext.get().session().permissions().contains(
                supplier ? "PROVEEDORES_ESCRIBIR" : "OPERADORES_ESCRIBIR");
        create.setDisable(!canWrite);
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> !canWrite)));
        deactivate.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> !canWrite)));

        Runnable reload = () -> loadOrganizations(resource, search.getText(), table, status);
        searchButton.setOnAction(event -> reload.run());
        search.setOnAction(event -> reload.run());
        create.setOnAction(event -> organizationDialog("Nuevo " + singular(supplier), null, supplier)
                .ifPresent(request -> saveOrganization(resource, null, request, reload, status)));
        edit.setOnAction(event -> {
            OrganizationResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) organizationDialog("Editar " + singular(supplier), selected, supplier)
                    .ifPresent(request -> saveOrganization(resource, selected, request, reload, status));
        });
        deactivate.setOnAction(event -> {
            OrganizationResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || "INACTIVO".equals(selected.state())) return;
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "La organización conservará su historial.", ButtonType.OK, ButtonType.CANCEL);
            confirmation.setHeaderText("Desactivar " + selected.businessName());
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            status.setText("Desactivando...");
            AppContext.get().api().deactivateOrganization(resource, selected.id())
                    .whenComplete((ignored, error) -> Platform.runLater(() -> {
                        if (error != null) status.setText(message(error));
                        else reload.run();
                    }));
        });

        HBox filters = new HBox(10, search, searchButton, create, edit, deactivate);
        HBox.setHgrow(search, javafx.scene.layout.Priority.ALWAYS);
        VBox content = new VBox(12, filters, table, status);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(new BorderPane(content));
        reload.run();
        dialog.showAndWait();
    }

    private void loadOrganizations(String resource, String search, TableView<OrganizationResponse> table, Label status) {
        status.setText("Cargando...");
        AppContext.get().api().organizations(resource, search).whenComplete((page, error) ->
                Platform.runLater(() -> {
                    if (error != null) {
                        status.setText(message(error));
                        return;
                    }
                    table.setItems(FXCollections.observableArrayList(page.content()));
                    status.setText(page.totalElements() + " registro(s)");
                }));
    }

    private Optional<OrganizationRequest> organizationDialog(
            String title, OrganizationResponse initial, boolean supplier) {
        Dialog<OrganizationRequest> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Datos comerciales");
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField name = new TextField(initial == null ? "" : value(initial.businessName()));
        TextField cuit = new TextField(initial == null ? "" : value(initial.cuit()));
        TextField email = new TextField(initial == null ? "" : value(initial.email()));
        TextField phone = new TextField(initial == null ? "" : value(initial.phone()));
        TextField address = new TextField(initial == null ? "" : value(initial.address()));
        TextField country = new TextField(initial == null ? "Argentina" : value(initial.country()));
        TextField service = new TextField(initial == null ? "" : value(initial.serviceType()));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Razón social"), name);
        grid.addRow(1, new Label("CUIT"), cuit);
        grid.addRow(2, new Label("Email"), email);
        grid.addRow(3, new Label("Teléfono"), phone);
        grid.addRow(4, new Label("Dirección"), address);
        grid.addRow(5, new Label("País"), country);
        if (supplier) grid.addRow(6, new Label("Tipo de servicio"), service);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().lookupButton(save).disableProperty()
                .bind(name.textProperty().isEmpty().or(country.textProperty().isEmpty()));
        dialog.setResultConverter(button -> button == save
                ? new OrganizationRequest(name.getText().strip(), nullable(cuit.getText()), nullable(email.getText()),
                    nullable(phone.getText()), nullable(address.getText()), country.getText().strip(),
                    supplier ? nullable(service.getText()) : null)
                : null);
        return dialog.showAndWait();
    }

    private void saveOrganization(String resource, OrganizationResponse initial, OrganizationRequest request,
                                  Runnable reload, Label status) {
        status.setText("Guardando...");
        var operation = initial == null
                ? AppContext.get().api().createOrganization(resource, request)
                : AppContext.get().api().updateOrganization(resource, initial.id(), request);
        operation.whenComplete((saved, error) -> Platform.runLater(() -> {
            if (error != null) status.setText(message(error));
            else reload.run();
        }));
    }

    private String singular(boolean supplier) {
        return supplier ? "proveedor" : "operador";
    }

    @FXML
    private void showLocations() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ubicaciones");
        dialog.setHeaderText("Puertos, aeropuertos, depósitos y fronteras");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TextField search = new TextField();
        search.setPromptText("Buscar código, nombre o país");
        Button searchButton = new Button("Buscar");
        Button create = new Button("Nueva");
        Button edit = new Button("Editar");
        Button delete = new Button("Eliminar");
        Label status = new Label();
        TableView<LocationResponse> table = new TableView<>();
        table.getColumns().add(locationColumn("Código", LocationResponse::code));
        table.getColumns().add(locationColumn("Nombre", LocationResponse::name));
        table.getColumns().add(locationColumn("País", LocationResponse::country));
        table.getColumns().add(locationColumn("Tipo", LocationResponse::type));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefSize(820, 420);
        boolean canWrite = AppContext.get().session().permissions().contains("MAESTROS_ESCRIBIR");
        create.setDisable(!canWrite);
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> !canWrite)));
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> !canWrite)));
        Runnable reload = () -> loadLocations(search.getText(), table, status);
        searchButton.setOnAction(event -> reload.run());
        search.setOnAction(event -> reload.run());
        create.setOnAction(event -> locationDialog(null)
                .ifPresent(request -> saveLocation(null, request, reload, status)));
        edit.setOnAction(event -> {
            LocationResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) locationDialog(selected)
                    .ifPresent(request -> saveLocation(selected, request, reload, status));
        });
        delete.setOnAction(event -> {
            LocationResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Solo puede eliminarse si ningún tramo la utiliza.", ButtonType.OK, ButtonType.CANCEL);
            confirmation.setHeaderText("Eliminar " + selected.name());
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            AppContext.get().api().deleteLocation(selected.id()).whenComplete((ignored, error) ->
                    Platform.runLater(() -> {
                        if (error != null) status.setText(message(error));
                        else reload.run();
                    }));
        });
        HBox filters = new HBox(10, search, searchButton, create, edit, delete);
        HBox.setHgrow(search, javafx.scene.layout.Priority.ALWAYS);
        VBox content = new VBox(12, filters, table, status);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);
        reload.run();
        dialog.showAndWait();
    }

    private TableColumn<LocationResponse, String> locationColumn(
            String title, java.util.function.Function<LocationResponse, String> value) {
        TableColumn<LocationResponse, String> column = new TableColumn<>(title);
        column.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue())));
        return column;
    }

    private void loadLocations(String search, TableView<LocationResponse> table, Label status) {
        status.setText("Cargando...");
        AppContext.get().api().locations(search).whenComplete((page, error) -> Platform.runLater(() -> {
            if (error != null) status.setText(message(error));
            else {
                table.setItems(FXCollections.observableArrayList(page.content()));
                status.setText(page.totalElements() + " ubicación(es)");
            }
        }));
    }

    private Optional<LocationRequest> locationDialog(LocationResponse initial) {
        Dialog<LocationRequest> dialog = new Dialog<>();
        dialog.setTitle(initial == null ? "Nueva ubicación" : "Editar ubicación");
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField code = new TextField(initial == null ? "" : initial.code());
        TextField name = new TextField(initial == null ? "" : initial.name());
        TextField country = new TextField(initial == null ? "Argentina" : initial.country());
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
                "PUERTO", "AEROPUERTO", "DEPOSITO", "FRONTERA", "OTRO"));
        type.setValue(initial == null ? "PUERTO" : initial.type());
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Código"), code);
        grid.addRow(1, new Label("Nombre"), name);
        grid.addRow(2, new Label("País"), country);
        grid.addRow(3, new Label("Tipo"), type);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().lookupButton(save).disableProperty().bind(
                code.textProperty().isEmpty().or(name.textProperty().isEmpty()).or(country.textProperty().isEmpty()));
        dialog.setResultConverter(button -> button == save
                ? new LocationRequest(code.getText().strip(), name.getText().strip(),
                    country.getText().strip(), type.getValue()) : null);
        return dialog.showAndWait();
    }

    private void saveLocation(LocationResponse initial, LocationRequest request,
                              Runnable reload, Label status) {
        status.setText("Guardando...");
        var operation = initial == null ? AppContext.get().api().createLocation(request)
                : AppContext.get().api().updateLocation(initial.id(), request);
        operation.whenComplete((saved, error) -> Platform.runLater(() -> {
            if (error != null) status.setText(message(error));
            else reload.run();
        }));
    }

    @FXML
    private void showVehicles() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Vehículos");
        dialog.setHeaderText("Flota asociada a operadores logísticos");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TextField search = new TextField();
        search.setPromptText("Buscar identificación, tipo u operador");
        Button searchButton = new Button("Buscar");
        Button create = new Button("Nuevo");
        Button edit = new Button("Editar");
        Button deactivate = new Button("Desactivar");
        Label status = new Label("Cargando...");
        TableView<VehicleResponse> table = new TableView<>();
        table.getColumns().add(vehicleColumn("Identificación", VehicleResponse::identification));
        table.getColumns().add(vehicleColumn("Tipo", VehicleResponse::type));
        table.getColumns().add(vehicleColumn("Operador", VehicleResponse::operatorName));
        table.getColumns().add(vehicleColumn("Estado", VehicleResponse::state));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefSize(850, 420);
        var operators = FXCollections.<OrganizationResponse>observableArrayList();
        boolean canWrite = AppContext.get().session().permissions().contains("MAESTROS_ESCRIBIR");
        create.setDisable(true);
        edit.setDisable(true);
        deactivate.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull()
                .or(Bindings.createBooleanBinding(() -> !canWrite)));
        Runnable reload = () -> loadVehicles(search.getText(), table, status);
        searchButton.setOnAction(event -> reload.run());
        search.setOnAction(event -> reload.run());
        AppContext.get().api().organizations("operadores", "").whenComplete((page, error) ->
                Platform.runLater(() -> {
                    if (error != null) {
                        status.setText(message(error));
                        return;
                    }
                    operators.setAll(page.content().stream().filter(item -> "ACTIVO".equals(item.state())).toList());
                    create.setDisable(!canWrite || operators.isEmpty());
                    edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull()
                            .or(Bindings.createBooleanBinding(() -> !canWrite || operators.isEmpty())));
                }));
        create.setOnAction(event -> vehicleDialog(null, operators)
                .ifPresent(request -> saveVehicle(null, request, reload, status)));
        edit.setOnAction(event -> {
            VehicleResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) vehicleDialog(selected, operators)
                    .ifPresent(request -> saveVehicle(selected, request, reload, status));
        });
        deactivate.setOnAction(event -> {
            VehicleResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || "INACTIVO".equals(selected.state())) return;
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "El vehículo conservará su historial de tramos.", ButtonType.OK, ButtonType.CANCEL);
            confirmation.setHeaderText("Desactivar " + selected.identification());
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            AppContext.get().api().deactivateVehicle(selected.id()).whenComplete((ignored, error) ->
                    Platform.runLater(() -> {
                        if (error != null) status.setText(message(error));
                        else reload.run();
                    }));
        });
        HBox filters = new HBox(10, search, searchButton, create, edit, deactivate);
        HBox.setHgrow(search, javafx.scene.layout.Priority.ALWAYS);
        VBox content = new VBox(12, filters, table, status);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);
        reload.run();
        dialog.showAndWait();
    }

    private TableColumn<VehicleResponse, String> vehicleColumn(
            String title, java.util.function.Function<VehicleResponse, String> value) {
        TableColumn<VehicleResponse, String> column = new TableColumn<>(title);
        column.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue())));
        return column;
    }

    private void loadVehicles(String search, TableView<VehicleResponse> table, Label status) {
        status.setText("Cargando...");
        AppContext.get().api().vehicles(search).whenComplete((page, error) -> Platform.runLater(() -> {
            if (error != null) status.setText(message(error));
            else {
                table.setItems(FXCollections.observableArrayList(page.content()));
                status.setText(page.totalElements() + " vehículo(s)");
            }
        }));
    }

    private Optional<VehicleRequest> vehicleDialog(
            VehicleResponse initial, javafx.collections.ObservableList<OrganizationResponse> operators) {
        Dialog<VehicleRequest> dialog = new Dialog<>();
        dialog.setTitle(initial == null ? "Nuevo vehículo" : "Editar vehículo");
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        ComboBox<OrganizationResponse> operator = new ComboBox<>(operators);
        operator.setConverter(new StringConverter<>() {
            @Override public String toString(OrganizationResponse item) {
                return item == null ? "" : item.businessName() + " — " + value(item.cuit());
            }
            @Override public OrganizationResponse fromString(String text) { return null; }
        });
        if (initial != null) operators.stream().filter(item -> item.id().equals(initial.operatorId()))
                .findFirst().ifPresent(operator::setValue);
        else if (!operators.isEmpty()) operator.setValue(operators.getFirst());
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
                "CAMION", "BARCO", "AVION", "TREN", "OTRO"));
        type.setValue(initial == null ? "CAMION" : initial.type());
        TextField identification = new TextField(initial == null ? "" : initial.identification());
        ComboBox<String> state = new ComboBox<>(FXCollections.observableArrayList(
                "ACTIVO", "MANTENIMIENTO", "INACTIVO"));
        state.setValue(initial == null ? "ACTIVO" : initial.state());
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Operador"), operator);
        grid.addRow(1, new Label("Tipo"), type);
        grid.addRow(2, new Label("Identificación"), identification);
        grid.addRow(3, new Label("Estado"), state);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().lookupButton(save).disableProperty().bind(
                operator.valueProperty().isNull().or(identification.textProperty().isEmpty()));
        dialog.setResultConverter(button -> button == save
                ? new VehicleRequest(operator.getValue().id(), type.getValue(),
                    identification.getText().strip(), state.getValue()) : null);
        return dialog.showAndWait();
    }

    private void saveVehicle(VehicleResponse initial, VehicleRequest request,
                             Runnable reload, Label status) {
        status.setText("Guardando...");
        var operation = initial == null ? AppContext.get().api().createVehicle(request)
                : AppContext.get().api().updateVehicle(initial.id(), request);
        operation.whenComplete((saved, error) -> Platform.runLater(() -> {
            if (error != null) status.setText(message(error));
            else reload.run();
        }));
    }

    @FXML
    private void showIotMonitoring() {
        MonitoreoIotWindow.show();
    }

    @FXML
    private void showOperations() {
        OperacionWindow.show();
    }

    @FXML
    private void logout() {
        AppContext.get().api().logout().whenComplete((ignored, error) -> Platform.runLater(() -> {
            AppContext.get().setSession(null);
            DesktopApplication.showLogin();
        }));
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private String nullable(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    private String message(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause.getMessage() == null ? "No se pudo completar la operación" : cause.getMessage();
    }
}
