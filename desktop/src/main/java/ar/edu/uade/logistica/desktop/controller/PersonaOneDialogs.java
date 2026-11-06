package ar.edu.uade.logistica.desktop.controller;

import ar.edu.uade.logistica.contracts.admin.PermissionResponse;
import ar.edu.uade.logistica.contracts.admin.RoleRequest;
import ar.edu.uade.logistica.contracts.admin.RoleResponse;
import ar.edu.uade.logistica.contracts.admin.UserRequest;
import ar.edu.uade.logistica.contracts.admin.UserResponse;
import ar.edu.uade.logistica.contracts.auth.LoginResponse;
import ar.edu.uade.logistica.contracts.client.ClientResponse;
import ar.edu.uade.logistica.contracts.finance.*;
import ar.edu.uade.logistica.contracts.logistics.*;
import ar.edu.uade.logistica.contracts.master.LocationResponse;
import ar.edu.uade.logistica.contracts.master.VehicleResponse;
import ar.edu.uade.logistica.contracts.organization.OrganizationResponse;
import ar.edu.uade.logistica.contracts.report.*;
import ar.edu.uade.logistica.contracts.route.RouteLegRequest;
import ar.edu.uade.logistica.contracts.route.RouteLegResponse;
import ar.edu.uade.logistica.contracts.route.RouteSupplierRequest;
import ar.edu.uade.logistica.desktop.http.ApiClient;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

final class PersonaOneDialogs {
    private final ApiClient api;
    private final LoginResponse session;

    PersonaOneDialogs(ApiClient api, LoginResponse session) {
        this.api = api;
        this.session = session;
    }

    void showAdministration() {
        if (!session.permissions().contains("USUARIOS_GESTIONAR")) {
            info("Administración", "Su rol no tiene permiso para administrar usuarios.");
            return;
        }
        Dialog<Void> dialog = baseDialog("Usuarios y roles", 1040, 640);
        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Usuarios", usersPane()));
        if (session.permissions().contains("ROLES_GESTIONAR")) {
            tabs.getTabs().add(new Tab("Roles y permisos", rolesPane()));
        }
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        dialog.getDialogPane().setContent(tabs);
        dialog.showAndWait();
    }

    private Node usersPane() {
        TableView<UserResponse> table = new TableView<>();
        table.getColumns().add(column("Nombre", u -> u.firstName() + " " + u.lastName()));
        table.getColumns().add(column("Email", UserResponse::email));
        table.getColumns().add(column("Roles", u -> u.roles().stream().map(RoleResponse::description)
                .sorted().reduce((a, b) -> a + ", " + b).orElse("")));
        table.getColumns().add(column("Estado", UserResponse::state));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Label status = new Label();
        TextField search = new TextField();
        search.setPromptText("Buscar nombre o email");
        Runnable reload = () -> run(() -> {
            var page = api.users(search.getText()).join();
            table.setItems(FXCollections.observableArrayList(page.content()));
            status.setText(page.totalElements() + " usuario(s)");
        });
        Button find = new Button("Buscar");
        find.setOnAction(event -> reload.run());
        Button create = new Button("Nuevo usuario");
        create.setOnAction(event -> run(() -> {
            List<RoleResponse> roles = api.roles().join();
            userDialog(null, roles).ifPresent(request -> api.createUser(request).join());
            reload.run();
        }));
        Button edit = new Button("Editar");
        edit.setOnAction(event -> {
            UserResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            run(() -> {
                List<RoleResponse> roles = api.roles().join();
                userDialog(selected, roles).ifPresent(request -> api.updateUser(selected.id(), request).join());
                reload.run();
            });
        });
        Button state = new Button("Activar / bloquear");
        state.setOnAction(event -> {
            UserResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            ChoiceDialog<String> choice = new ChoiceDialog<>(selected.state(), "ACTIVO", "BLOQUEADO", "INACTIVO");
            choice.setTitle("Estado del usuario");
            choice.setHeaderText(selected.email());
            choice.showAndWait().ifPresent(value -> run(() -> {
                api.changeUserState(selected.id(), value).join();
                reload.run();
            }));
        });
        HBox actions = new HBox(8, search, find, create, edit, state);
        HBox.setHgrow(search, Priority.ALWAYS);
        VBox box = pane(actions, table, status);
        reload.run();
        return box;
    }

    private Optional<UserRequest> userDialog(UserResponse initial, List<RoleResponse> roles) {
        Dialog<UserRequest> dialog = resultDialog(initial == null ? "Nuevo usuario" : "Editar usuario");
        TextField firstName = text(initial == null ? null : initial.firstName());
        TextField lastName = text(initial == null ? null : initial.lastName());
        TextField email = text(initial == null ? null : initial.email());
        PasswordField password = new PasswordField();
        password.setPromptText(initial == null ? "Mínimo 12 caracteres" : "Vacío conserva la actual");
        ComboBox<String> ownerType = new ComboBox<>(FXCollections.observableArrayList(
                "SIN ORGANIZACIÓN", "CLIENTE", "PROVEEDOR", "OPERADOR"));
        TextField ownerId = new TextField();
        ownerId.setPromptText("UUID del titular (si corresponde)");
        if (initial == null) ownerType.setValue("SIN ORGANIZACIÓN");
        else if (initial.clientId() != null) { ownerType.setValue("CLIENTE"); ownerId.setText(initial.clientId().toString()); }
        else if (initial.supplierId() != null) { ownerType.setValue("PROVEEDOR"); ownerId.setText(initial.supplierId().toString()); }
        else if (initial.operatorId() != null) { ownerType.setValue("OPERADOR"); ownerId.setText(initial.operatorId().toString()); }
        else ownerType.setValue("SIN ORGANIZACIÓN");
        ownerType.valueProperty().addListener((obs, before, now) -> ownerId.clear());
        Button chooseOwner = new Button("Elegir…");
        chooseOwner.disableProperty().bind(ownerType.valueProperty().isEqualTo("SIN ORGANIZACIÓN"));
        chooseOwner.setOnAction(event -> run(() -> {
            if ("CLIENTE".equals(ownerType.getValue())) {
                choice("Cliente titular", api.clients("").join().content().stream()
                        .filter(c -> "ACTIVO".equals(c.state())).toList(),
                        c -> c.businessName() + " — " + value(c.cuit()))
                        .showAndWait().ifPresent(c -> ownerId.setText(c.id().toString()));
            } else {
                String resource = "PROVEEDOR".equals(ownerType.getValue()) ? "proveedores" : "operadores";
                choice("Organización titular", api.organizations(resource, "").join().content().stream()
                        .filter(o -> "ACTIVO".equals(o.state())).toList(), OrganizationResponse::businessName)
                        .showAndWait().ifPresent(o -> ownerId.setText(o.id().toString()));
            }
        }));
        ListView<RoleResponse> roleList = new ListView<>(FXCollections.observableArrayList(roles));
        roleList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        roleList.setCellFactory(ignored -> listCell(r -> r.description() + " — " + r.permissions().size() + " permisos"));
        roleList.setPrefHeight(150);
        if (initial != null) {
            Set<UUID> current = initial.roles().stream().map(RoleResponse::id).collect(
                    java.util.stream.Collectors.toSet());
            for (int i = 0; i < roles.size(); i++) if (current.contains(roles.get(i).id())) {
                roleList.getSelectionModel().select(i);
            }
        }
        GridPane grid = grid();
        grid.addRow(0, new Label("Nombre"), firstName);
        grid.addRow(1, new Label("Apellido"), lastName);
        grid.addRow(2, new Label("Email"), email);
        grid.addRow(3, new Label("Contraseña"), password);
        grid.addRow(4, new Label("Tipo titular"), ownerType);
        grid.addRow(5, new Label("UUID titular"), ownerId);
        grid.addRow(6, new Label("Roles"), roleList);
        Formulario.ayuda(grid, 2, "Formato nombre@dominio.com, no repetido");
        Formulario.ayuda(grid, 3, initial == null ? "Mínimo 12 caracteres" : "Mínimo 12 caracteres. Vacía conserva la actual");
        Formulario.conBoton(grid, 5, chooseOwner, "Solo si pertenece a un cliente, proveedor u operador");
        Formulario.ayuda(grid, 6, "Al menos uno. Ctrl + clic para elegir varios");
        if (initial == null) Formulario.obligatorios(grid, "Nombre", "Apellido", "Email", "Contraseña", "Roles");
        else Formulario.obligatorios(grid, "Nombre", "Apellido", "Email", "Roles");
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
            try {
                UUID id = ownerId.getText().isBlank() ? null : UUID.fromString(ownerId.getText().strip());
                Set<UUID> selectedRoles = new LinkedHashSet<>(roleList.getSelectionModel()
                        .getSelectedItems().stream().map(RoleResponse::id).toList());
                return new UserRequest("CLIENTE".equals(ownerType.getValue()) ? id : null,
                        "PROVEEDOR".equals(ownerType.getValue()) ? id : null,
                        "OPERADOR".equals(ownerType.getValue()) ? id : null,
                        firstName.getText(), lastName.getText(), email.getText(),
                        password.getText().isBlank() ? null : password.getText(), selectedRoles);
            } catch (IllegalArgumentException exception) {
                showError("El UUID del titular no es válido");
                return null;
            }
        });
        return dialog.showAndWait();
    }

    private Node rolesPane() {
        TableView<RoleResponse> table = new TableView<>();
        // Sin ancho mínimo, la lista larga de permisos deja a estas dos columnas casi sin lugar.
        TableColumn<RoleResponse, String> roleName = column("Rol", RoleResponse::description);
        roleName.setMinWidth(160);
        TableColumn<RoleResponse, String> roleLevel = column("Nivel", r -> Integer.toString(r.level()));
        roleLevel.setMinWidth(70);
        table.getColumns().add(roleName);
        table.getColumns().add(roleLevel);
        table.getColumns().add(column("Permisos", r -> String.join(", ", r.permissions())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Label status = new Label();
        Runnable reload = () -> run(() -> {
            List<RoleResponse> roles = api.roles().join();
            table.setItems(FXCollections.observableArrayList(roles));
            status.setText(roles.size() + " rol(es)");
        });
        Button create = new Button("Nuevo rol");
        create.setOnAction(event -> run(() -> {
            roleDialog(null, api.permissions().join()).ifPresent(request -> api.createRole(request).join());
            reload.run();
        }));
        Button edit = new Button("Editar");
        edit.setOnAction(event -> {
            RoleResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            run(() -> {
                roleDialog(selected, api.permissions().join())
                        .ifPresent(request -> api.updateRole(selected.id(), request).join());
                reload.run();
            });
        });
        Button delete = new Button("Eliminar");
        delete.setOnAction(event -> {
            RoleResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) run(() -> { api.deleteRole(selected.id()).join(); reload.run(); });
        });
        VBox box = pane(new HBox(8, create, edit, delete), table, status);
        reload.run();
        return box;
    }

    private Optional<RoleRequest> roleDialog(RoleResponse initial, List<PermissionResponse> permissions) {
        Dialog<RoleRequest> dialog = resultDialog(initial == null ? "Nuevo rol" : "Editar rol");
        TextField name = text(initial == null ? null : initial.description());
        Spinner<Integer> level = new Spinner<>(0, 100, initial == null ? 50 : initial.level());
        ListView<PermissionResponse> list = new ListView<>(FXCollections.observableArrayList(permissions));
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setCellFactory(ignored -> listCell(PersonaOneDialogs::permissionLabel));
        list.setPrefWidth(470);
        if (initial != null) for (int i = 0; i < permissions.size(); i++) {
            if (initial.permissions().contains(permissions.get(i).code())) list.getSelectionModel().select(i);
        }
        GridPane grid = grid();
        grid.addRow(0, new Label("Nombre"), name);
        grid.addRow(1, new Label("Nivel"), level);
        grid.addRow(2, new Label("Permisos"), list);
        Formulario.ayuda(grid, 0, "No repetido");
        Formulario.ayuda(grid, 1, "De 0 a 100");
        Formulario.ayuda(grid, 2, "Ctrl + clic para elegir varios");
        Formulario.obligatorios(grid, "Nombre", "Nivel");
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE
                ? new RoleRequest(name.getText(), level.getValue(), list.getSelectionModel().getSelectedItems()
                .stream().map(PermissionResponse::code).collect(java.util.stream.Collectors.toSet())) : null);
        return dialog.showAndWait();
    }

    /** El código del permiso, con su descripción solo cuando agrega algo: varias repiten "Permiso CODIGO". */
    static String permissionLabel(PermissionResponse permission) {
        String description = permission.description();
        return description == null || description.equals("Permiso " + permission.code())
                ? permission.code() : permission.code() + " — " + description;
    }

    void showShipments() {
        Dialog<Void> dialog = baseDialog("Envíos, contenedores y sensores", 1120, 700);
        TableView<ShipmentResponse> table = new TableView<>();
        table.getColumns().add(column("Cliente", ShipmentResponse::clientName));
        table.getColumns().add(column("Origen", ShipmentResponse::origin));
        table.getColumns().add(column("Destino", ShipmentResponse::destination));
        table.getColumns().add(column("Estado", ShipmentResponse::state));
        table.getColumns().add(column("Prioridad", ShipmentResponse::priority));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TextField search = new TextField();
        search.setPromptText("Buscar envío, cliente, origen o destino");
        Label status = new Label();
        Runnable reload = () -> run(() -> {
            var page = api.shipments(search.getText()).join();
            table.setItems(FXCollections.observableArrayList(page.content()));
            status.setText(page.totalElements() + " envío(s)");
        });
        Button find = new Button("Buscar"); find.setOnAction(e -> reload.run());
        Button create = new Button("Nuevo envío");
        create.setDisable(!session.permissions().contains("ENVIOS_ESCRIBIR"));
        create.setOnAction(e -> run(() -> {
            shipmentDialog(null, api.clients("").join().content()).ifPresent(req -> api.createShipment(req).join());
            reload.run();
        }));
        Button edit = new Button("Editar");
        edit.setDisable(!session.permissions().contains("ENVIOS_ESCRIBIR"));
        edit.setOnAction(e -> {
            ShipmentResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) run(() -> {
                shipmentDialog(selected, api.clients("").join().content())
                        .ifPresent(req -> api.updateShipment(selected.id(), req).join());
                reload.run();
            });
        });
        Button changeState = new Button("Cambiar estado");
        changeState.setDisable(!session.permissions().contains("ENVIOS_ESCRIBIR"));
        changeState.setOnAction(e -> {
            ShipmentResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            ChoiceDialog<String> choice = new ChoiceDialog<>(selected.state(),
                    "CREADO", "EN_PREPARACION", "EN_TRANSITO", "DEMORADO", "ENTREGADO", "CANCELADO");
            choice.setHeaderText(selected.origin() + " → " + selected.destination());
            choice.showAndWait().ifPresent(value -> run(() -> {
                api.changeShipmentState(selected.id(), new ShipmentStateRequest(value,
                        "ENTREGADO".equals(value) ? Instant.now() : null)).join();
                reload.run();
            }));
        });
        Button detail = new Button("Detalle y trazabilidad");
        detail.setOnAction(e -> {
            ShipmentResponse selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showShipmentDetail(selected);
        });
        Button catalogs = new Button("Catálogos contenedores/sensores");
        catalogs.setOnAction(e -> showLogisticsCatalogs());
        HBox actions = new HBox(8, search, find, create, edit, changeState, detail, catalogs);
        HBox.setHgrow(search, Priority.ALWAYS);
        dialog.getDialogPane().setContent(pane(actions, table, status));
        reload.run();
        dialog.showAndWait();
    }

    private Optional<ShipmentRequest> shipmentDialog(ShipmentResponse initial, List<ClientResponse> clients) {
        Dialog<ShipmentRequest> dialog = resultDialog(initial == null ? "Nuevo envío" : "Editar envío");
        ComboBox<ClientResponse> client = combo(clients, c -> c.businessName() + " — " + value(c.cuit()));
        if (initial != null) clients.stream().filter(c -> c.id().equals(initial.clientId())).findFirst()
                .ifPresent(client::setValue);
        TextField origin = text(initial == null ? null : initial.origin());
        TextField destination = text(initial == null ? null : initial.destination());
        TextField originCountry = text(initial == null ? "Argentina" : initial.originCountry());
        TextField destinationCountry = text(initial == null ? null : initial.destinationCountry());
        TextField estimated = text(initial == null || initial.estimatedDelivery() == null
                ? null : initial.estimatedDelivery().toString());
        estimated.setPromptText("2026-12-01T15:00:00Z");
        ComboBox<String> priority = new ComboBox<>(FXCollections.observableArrayList("BAJA", "NORMAL", "ALTA", "URGENTE"));
        priority.setValue(initial == null ? "NORMAL" : initial.priority());
        TextField volume = text(initial == null ? "0" : initial.volumeM3().toPlainString());
        GridPane grid = grid();
        grid.addRow(0, new Label("Cliente"), client);
        grid.addRow(1, new Label("Origen"), origin);
        grid.addRow(2, new Label("Destino"), destination);
        grid.addRow(3, new Label("País origen"), originCountry);
        grid.addRow(4, new Label("País destino"), destinationCountry);
        grid.addRow(5, new Label("Entrega estimada UTC"), estimated);
        grid.addRow(6, new Label("Prioridad"), priority);
        grid.addRow(7, new Label("Volumen m³"), volume);
        Formulario.ayuda(grid, 0, "Debe estar activo");
        Formulario.ayuda(grid, 5, "Opcional. Ejemplo: 2026-12-01T15:00:00Z");
        Formulario.ayuda(grid, 7, "0 o más. Decimales con punto");
        Formulario.obligatorios(grid, "Cliente", "Origen", "Destino", "País origen", "País destino", "Prioridad", "Volumen m³");
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
            try {
                return new ShipmentRequest(client.getValue().id(), origin.getText(), destination.getText(),
                        originCountry.getText(), destinationCountry.getText(), parseInstant(estimated.getText()),
                        priority.getValue(), new BigDecimal(volume.getText().strip()));
            } catch (Exception exception) { showError("Revise cliente, fecha y volumen"); return null; }
        });
        return dialog.showAndWait();
    }

    /** Reabre el detalle cuando el anterior ya cerró: abrirlo adentro de su evento apila diálogos y frena Platform.runLater. */
    private void reabrirDetalle(ShipmentResponse shipment) {
        javafx.application.Platform.runLater(() -> showShipmentDetail(shipment));
    }

    private void showShipmentDetail(ShipmentResponse shipment) {
        run(() -> {
            Dialog<Void> dialog = baseDialog("Trazabilidad del envío", 980, 620);
            ShipmentDetailResponse detail = api.shipment(shipment.id()).join();
            TableView<ContainerAssignmentResponse> containers = new TableView<>();
            containers.getColumns().add(column("Contenedor", ContainerAssignmentResponse::containerCode));
            containers.getColumns().add(column("Tipo", ContainerAssignmentResponse::containerType));
            containers.getColumns().add(column("Desde", a -> a.startAt().toString()));
            containers.getColumns().add(column("Hasta", a -> a.endAt() == null ? "Vigente" : a.endAt().toString()));
            containers.setItems(FXCollections.observableArrayList(detail.containers()));
            containers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            TableView<SensorAssignmentResponse> sensors = new TableView<>();
            sensors.getColumns().add(column("Sensor", a -> a.sensorId().toString()));
            sensors.getColumns().add(column("Tipo", SensorAssignmentResponse::sensorType));
            sensors.getColumns().add(column("Contenedor", SensorAssignmentResponse::containerCode));
            sensors.getColumns().add(column("Desde", a -> a.startAt().toString()));
            sensors.getColumns().add(column("Hasta", a -> a.endAt() == null ? "Vigente" : a.endAt().toString()));
            sensors.setItems(FXCollections.observableArrayList(detail.sensors()));
            sensors.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            Button assignContainer = new Button("Asociar contenedor");
            assignContainer.setOnAction(e -> run(() -> {
                List<ContainerResponse> available = api.containers("").join().content().stream()
                        .filter(c -> "DISPONIBLE".equals(c.state())).toList();
                Dialog<ContainerResponse> choice = choice("Contenedor disponible", available,
                        c -> c.internationalCode() + " — " + c.type());
                choice.showAndWait().ifPresent(c -> api.assignContainer(shipment.id(),
                        new ContainerAssignmentRequest(c.id(), Instant.now(), null)).join());
                dialog.close();
                reabrirDetalle(shipment);
            }));
            Button unassignContainer = new Button("Desasociar");
            unassignContainer.setOnAction(e -> {
                ContainerAssignmentResponse selected = containers.getSelectionModel().getSelectedItem();
                if (selected != null && selected.active()) run(() -> {
                    api.unassignContainer(shipment.id(), selected.containerId(), selected.startAt()).join();
                    dialog.close(); reabrirDetalle(shipment);
                });
            });
            Button assignSensor = new Button("Asociar sensor");
            assignSensor.setOnAction(e -> {
                ContainerAssignmentResponse selected = containers.getSelectionModel().getSelectedItem();
                if (selected == null || !selected.active()) return;
                run(() -> {
                    List<SensorResponse> available = api.sensors("").join().content().stream()
                            .filter(s -> "ACTIVO".equals(s.state())).toList();
                    Dialog<SensorResponse> choice = choice("Sensor activo", available,
                            s -> s.type() + " — " + s.id());
                    choice.showAndWait().ifPresent(s -> api.assignSensor(new SensorAssignmentRequest(
                            s.id(), selected.containerId(), Instant.now(), null)).join());
                    dialog.close(); reabrirDetalle(shipment);
                });
            });
            Button unassignSensor = new Button("Desasociar sensor");
            unassignSensor.setOnAction(e -> {
                SensorAssignmentResponse selected = sensors.getSelectionModel().getSelectedItem();
                if (selected != null && selected.active()) run(() -> {
                    api.unassignSensor(selected.assignmentId()).join();
                    dialog.close(); reabrirDetalle(shipment);
                });
            });
            VBox content = new VBox(8, new Label("Contenedores (vigentes e históricos)"),
                    containers, new HBox(8, assignContainer, unassignContainer, assignSensor),
                    new Label("Sensores del envío"), sensors, new HBox(8, unassignSensor));
            content.setPadding(new Insets(8));
            VBox.setVgrow(containers, Priority.ALWAYS); VBox.setVgrow(sensors, Priority.ALWAYS);
            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();
        });
    }

    private void showLogisticsCatalogs() {
        Dialog<Void> dialog = baseDialog("Catálogos logísticos", 920, 600);
        TabPane tabs = new TabPane(containerCatalog(), sensorCatalog());
        tabs.getTabs().forEach(t -> t.setClosable(false));
        dialog.getDialogPane().setContent(tabs);
        dialog.showAndWait();
    }

    private Tab containerCatalog() {
        TableView<ContainerResponse> table = new TableView<>();
        table.getColumns().add(column("Código", ContainerResponse::internationalCode));
        table.getColumns().add(column("Tipo", ContainerResponse::type));
        table.getColumns().add(column("Capacidad", c -> c.capacity().toPlainString()));
        table.getColumns().add(column("Estado", ContainerResponse::state));
        Runnable reload = () -> run(() -> table.setItems(FXCollections.observableArrayList(api.containers("").join().content())));
        Button create = new Button("Nuevo contenedor");
        create.setOnAction(e -> containerDialog(null).ifPresent(req -> run(() -> { api.createContainer(req).join(); reload.run(); })));
        Button edit = new Button("Editar");
        edit.setOnAction(e -> {
            var selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) containerDialog(selected).ifPresent(req -> run(() -> {
                api.updateContainer(selected.id(), req).join(); reload.run();
            }));
        });
        Button history = new Button("Historial de movimientos");
        history.setOnAction(e -> {
            var selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showContainerHistory(selected);
        });
        reload.run();
        return new Tab("Contenedores", pane(new HBox(8, create, edit, history), table, new Label()));
    }

    private void showContainerHistory(ContainerResponse container) {
        run(() -> {
            Dialog<Void> dialog = baseDialog("Historial de " + container.internationalCode(), 860, 460);
            TableView<ContainerHistoryResponse> table = new TableView<>();
            table.getColumns().add(column("Cliente", ContainerHistoryResponse::client));
            table.getColumns().add(column("Origen", ContainerHistoryResponse::origin));
            table.getColumns().add(column("Destino", ContainerHistoryResponse::destination));
            table.getColumns().add(column("Estado del envío", ContainerHistoryResponse::shipmentState));
            table.getColumns().add(column("Desde", h -> h.startAt().toString()));
            table.getColumns().add(column("Hasta", h -> h.endAt() == null ? "Vigente" : h.endAt().toString()));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.setItems(FXCollections.observableArrayList(api.containerHistory(container.id()).join()));
            dialog.getDialogPane().setContent(pane(new HBox(), table, new Label(
                    "Envíos por los que pasó el contenedor. El recorrido GPS está en Monitoreo IoT.")));
            dialog.showAndWait();
        });
    }

    private Optional<ContainerRequest> containerDialog(ContainerResponse initial) {
        Dialog<ContainerRequest> dialog = resultDialog(initial == null ? "Nuevo contenedor" : "Editar contenedor");
        TextField code = text(initial == null ? null : initial.internationalCode());
        TextField type = text(initial == null ? null : initial.type());
        TextField capacity = text(initial == null ? null : initial.capacity().toPlainString());
        ComboBox<String> state = new ComboBox<>(FXCollections.observableArrayList(
                "DISPONIBLE", "EN_USO", "MANTENIMIENTO", "INACTIVO"));
        state.setValue(initial == null ? "DISPONIBLE" : initial.state());
        GridPane grid = grid(); grid.addRow(0, new Label("Código"), code); grid.addRow(1, new Label("Tipo"), type);
        grid.addRow(2, new Label("Capacidad"), capacity); grid.addRow(3, new Label("Estado"), state);
        Formulario.ayuda(grid, 0, "No repetido");
        Formulario.ayuda(grid, 1, "Ejemplo: REEFER, DRY u OPEN_TOP");
        Formulario.ayuda(grid, 2, "Mayor a 0. Decimales con punto");
        Formulario.obligatorios(grid, "Código", "Tipo", "Capacidad", "Estado");
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE
                ? new ContainerRequest(code.getText(), type.getText(), new BigDecimal(capacity.getText()), state.getValue()) : null);
        return dialog.showAndWait();
    }

    private Tab sensorCatalog() {
        TableView<SensorResponse> table = new TableView<>();
        table.getColumns().add(column("ID", s -> s.id().toString()));
        table.getColumns().add(column("Tipo", SensorResponse::type));
        table.getColumns().add(column("Fabricante", s -> value(s.manufacturer())));
        table.getColumns().add(column("Estado", SensorResponse::state));
        Runnable reload = () -> run(() -> table.setItems(FXCollections.observableArrayList(api.sensors("").join().content())));
        Button create = new Button("Nuevo sensor");
        create.setOnAction(e -> sensorDialog(null).ifPresent(req -> run(() -> { api.createSensor(req).join(); reload.run(); })));
        Button edit = new Button("Editar");
        edit.setOnAction(e -> {
            var selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) sensorDialog(selected).ifPresent(req -> run(() -> {
                api.updateSensor(selected.id(), req).join(); reload.run();
            }));
        });
        reload.run();
        return new Tab("Sensores", pane(new HBox(8, create, edit), table, new Label()));
    }

    private Optional<SensorRequest> sensorDialog(SensorResponse initial) {
        Dialog<SensorRequest> dialog = resultDialog(initial == null ? "Nuevo sensor" : "Editar sensor");
        TextField type = text(initial == null ? null : initial.type());
        TextField manufacturer = text(initial == null ? null : initial.manufacturer());
        ComboBox<String> state = new ComboBox<>(FXCollections.observableArrayList(
                "ACTIVO", "INACTIVO", "MANTENIMIENTO", "FALLA"));
        state.setValue(initial == null ? "ACTIVO" : initial.state());
        GridPane grid = grid(); grid.addRow(0, new Label("Tipo"), type);
        grid.addRow(1, new Label("Fabricante"), manufacturer); grid.addRow(2, new Label("Estado"), state);
        Formulario.ayuda(grid, 1, "Opcional");
        Formulario.obligatorios(grid, "Tipo", "Estado");
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE
                ? new SensorRequest(type.getText(), manufacturer.getText(),
                initial == null ? Instant.now() : initial.installationDate(), state.getValue()) : null);
        return dialog.showAndWait();
    }

    void showRoutes() {
        run(() -> {
            Dialog<Void> dialog = baseDialog("Trayectos", 1120, 680);
            List<ShipmentResponse> shipments = api.shipments("").join().content();
            ComboBox<ShipmentResponse> shipment = combo(shipments,
                    s -> s.clientName() + " — " + s.origin() + " → " + s.destination());
            TableView<RouteLegResponse> table = new TableView<>();
            table.getColumns().add(column("Orden", r -> Integer.toString(r.order())));
            table.getColumns().add(column("Origen", RouteLegResponse::origin));
            table.getColumns().add(column("Destino", RouteLegResponse::destination));
            table.getColumns().add(column("Medio", RouteLegResponse::transportType));
            table.getColumns().add(column("Operador", r -> value(r.operator())));
            table.getColumns().add(column("Vehículo", r -> value(r.vehicle())));
            table.getColumns().add(column("Proveedores", r -> String.join(", ",
                    r.suppliers().stream().map(s -> s.supplierName()).toList())));
            table.getColumns().add(column("Estado", RouteLegResponse::state));
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            Runnable reload = () -> {
                if (shipment.getValue() != null) run(() -> table.setItems(FXCollections.observableArrayList(
                        api.routeLegs(shipment.getValue().id()).join())));
            };
            shipment.setOnAction(e -> reload.run());
            Button create = new Button("Nuevo tramo");
            create.setOnAction(e -> {
                if (shipment.getValue() == null) return;
                routeDialog(null, shipment.getValue().id()).ifPresent(req -> run(() -> {
                    api.createRouteLeg(req).join(); reload.run();
                }));
            });
            Button edit = new Button("Editar");
            edit.setOnAction(e -> {
                var selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) routeDialog(selected, selected.shipmentId()).ifPresent(req -> run(() -> {
                    api.updateRouteLeg(selected.id(), req).join(); reload.run();
                }));
            });
            Button delete = new Button("Eliminar");
            delete.setOnAction(e -> {
                var selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) run(() -> { api.deleteRouteLeg(selected.id()).join(); reload.run(); });
            });
            HBox actions = new HBox(8, new Label("Envío"), shipment, create, edit, delete);
            HBox.setHgrow(shipment, Priority.ALWAYS);
            dialog.getDialogPane().setContent(pane(actions, table, new Label(
                    "Los tramos se ordenan por número y conservan operadores, vehículos, fechas, costos y proveedores.")));
            if (!shipments.isEmpty()) { shipment.setValue(shipments.getFirst()); reload.run(); }
            dialog.showAndWait();
        });
    }

    private Optional<RouteLegRequest> routeDialog(RouteLegResponse initial, UUID shipmentId) {
        return runResult(() -> {
            List<OrganizationResponse> operators = api.organizations("operadores", "").join().content();
            List<OrganizationResponse> suppliers = api.organizations("proveedores", "").join().content();
            List<VehicleResponse> vehicles = api.vehicles("").join().content();
            List<LocationResponse> locations = api.locations("").join().content();
            Dialog<RouteLegRequest> dialog = resultDialog(initial == null ? "Nuevo tramo" : "Editar tramo");
            Spinner<Integer> order = new Spinner<>(1, 999, initial == null ? 1 : initial.order());
            TextField origin = text(initial == null ? null : initial.origin());
            TextField destination = text(initial == null ? null : initial.destination());
            ComboBox<LocationResponse> originLocation = comboWithEmpty(locations, l -> l.code() + " — " + l.name());
            ComboBox<LocationResponse> destinationLocation = comboWithEmpty(locations, l -> l.code() + " — " + l.name());
            ComboBox<String> transport = new ComboBox<>(FXCollections.observableArrayList("TERRESTRE", "MARITIMO", "AEREO"));
            transport.setValue(initial == null ? "TERRESTRE" : initial.transportType());
            ComboBox<OrganizationResponse> operator = comboWithEmpty(operators, OrganizationResponse::businessName);
            ComboBox<VehicleResponse> vehicle = comboWithEmpty(vehicles, v -> v.identification() + " — " + v.operatorName());
            ComboBox<String> state = new ComboBox<>(FXCollections.observableArrayList("PENDIENTE", "EN_CURSO", "FINALIZADO", "CANCELADO"));
            state.setValue(initial == null ? "PENDIENTE" : initial.state());
            TextField departure = text(initial == null || initial.departureAt() == null ? null : initial.departureAt().toString());
            TextField estimatedArrival = text(initial == null || initial.estimatedArrivalAt() == null ? null : initial.estimatedArrivalAt().toString());
            TextField actualArrival = text(initial == null || initial.actualArrivalAt() == null ? null : initial.actualArrivalAt().toString());
            TextField estimatedCost = text(initial == null || initial.estimatedCost() == null ? null : initial.estimatedCost().toPlainString());
            TextField actualCost = text(initial == null || initial.actualCost() == null ? null : initial.actualCost().toPlainString());
            ComboBox<OrganizationResponse> supplier = comboWithEmpty(suppliers, OrganizationResponse::businessName);
            TextField service = new TextField(); TextField supplierCost = new TextField();
            // El formulario edita un proveedor; el resto se reenvía porque el backend reemplaza la lista entera.
            List<RouteSupplierRequest> otherSuppliers = new ArrayList<>();
            if (initial != null) initial.suppliers().stream().skip(1)
                    .map(s -> new RouteSupplierRequest(s.supplierId(), s.service(), s.cost())).forEach(otherSuppliers::add);
            Label otherLabel = new Label();
            Runnable showOthers = () -> otherLabel.setText(String.join(", ", otherSuppliers.stream()
                    .map(o -> suppliers.stream().filter(s -> s.id().equals(o.supplierId())).findFirst()
                            .map(OrganizationResponse::businessName).orElse("Proveedor") + " (" + o.service() + ")").toList()));
            showOthers.run();
            Button addSupplier = new Button("Agregar otro proveedor");
            addSupplier.setOnAction(e -> run(() -> {
                if (supplier.getValue() == null) return;
                List<RouteSupplierRequest> merged = mergeSuppliers(new RouteSupplierRequest(supplier.getValue().id(),
                        service.getText(), decimal(supplierCost.getText())), otherSuppliers);
                otherSuppliers.clear(); otherSuppliers.addAll(merged);
                supplier.setValue(null); service.clear(); supplierCost.clear(); showOthers.run();
            }));
            if (initial != null) {
                locations.stream().filter(l -> l.id().equals(initial.originLocationId())).findFirst().ifPresent(originLocation::setValue);
                locations.stream().filter(l -> l.id().equals(initial.destinationLocationId())).findFirst().ifPresent(destinationLocation::setValue);
                operators.stream().filter(o -> o.id().equals(initial.operatorId())).findFirst().ifPresent(operator::setValue);
                vehicles.stream().filter(v -> v.id().equals(initial.vehicleId())).findFirst().ifPresent(vehicle::setValue);
                if (!initial.suppliers().isEmpty()) {
                    var current = initial.suppliers().getFirst();
                    suppliers.stream().filter(s -> s.id().equals(current.supplierId())).findFirst().ifPresent(supplier::setValue);
                    service.setText(current.service()); supplierCost.setText(current.cost().toPlainString());
                }
            }
            GridPane grid = grid();
            grid.addRow(0, new Label("Orden"), order); grid.addRow(1, new Label("Origen"), origin);
            grid.addRow(2, new Label("Destino"), destination); grid.addRow(3, new Label("Ubicación origen"), originLocation);
            grid.addRow(4, new Label("Ubicación destino"), destinationLocation); grid.addRow(5, new Label("Medio"), transport);
            grid.addRow(6, new Label("Operador"), operator); grid.addRow(7, new Label("Vehículo"), vehicle);
            grid.addRow(8, new Label("Salida UTC"), departure); grid.addRow(9, new Label("Llegada estimada UTC"), estimatedArrival);
            grid.addRow(10, new Label("Llegada real UTC"), actualArrival); grid.addRow(11, new Label("Costo estimado"), estimatedCost);
            grid.addRow(12, new Label("Costo real"), actualCost); grid.addRow(13, new Label("Estado"), state);
            grid.addRow(14, new Label("Proveedor opcional"), supplier); grid.addRow(15, new Label("Servicio proveedor"), service);
            grid.addRow(16, new Label("Costo proveedor"), supplierCost);
            grid.addRow(17, addSupplier, otherLabel);
            Formulario.ayuda(grid, 0, "1 o más, sin repetir dentro del envío");
            Formulario.ayuda(grid, 7, "Requiere operador y debe pertenecerle");
            Formulario.ayuda(grid, 8, "Opcional. Ej.: 2026-12-01T15:00:00Z");
            Formulario.ayuda(grid, 9, "Opcional. No anterior a la salida");
            Formulario.ayuda(grid, 10, "Obligatoria si el estado es FINALIZADO");
            Formulario.ayuda(grid, 11, "Opcional. 0 o más, decimales con punto");
            Formulario.ayuda(grid, 12, "Opcional. 0 o más, decimales con punto");
            Formulario.ayuda(grid, 15, "Obligatorio si hay proveedor");
            Formulario.ayuda(grid, 16, "Obligatorio si hay proveedor. 0 o más");
            Formulario.obligatorios(grid, "Orden", "Origen", "Destino", "Medio", "Estado");
            ScrollPane scroll = new ScrollPane(grid); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(520);
            dialog.getDialogPane().setContent(scroll);
            dialog.setResultConverter(b -> {
                if (b.getButtonData() != ButtonBar.ButtonData.OK_DONE) return null;
                RouteSupplierRequest edited = supplier.getValue() == null ? null
                        : new RouteSupplierRequest(supplier.getValue().id(), service.getText(), decimal(supplierCost.getText()));
                List<RouteSupplierRequest> supplierRows = mergeSuppliers(edited, otherSuppliers);
                return new RouteLegRequest(shipmentId, order.getValue(), origin.getText(), destination.getText(),
                        id(originLocation.getValue()), id(destinationLocation.getValue()), transport.getValue(),
                        id(vehicle.getValue()), id(operator.getValue()), parseInstant(departure.getText()),
                        parseInstant(estimatedArrival.getText()), parseInstant(actualArrival.getText()),
                        decimalNullable(estimatedCost.getText()), decimalNullable(actualCost.getText()), state.getValue(), supplierRows);
            });
            return dialog.showAndWait();
        });
    }

    void showFinance() {
        run(() -> {
            Dialog<Void> dialog = baseDialog("Facturación y cuentas corrientes", 1160, 720);
            TableView<AccountResponse> accounts = new TableView<>();
            accounts.getColumns().add(column("Tipo", AccountResponse::ownerType));
            accounts.getColumns().add(column("Titular", AccountResponse::ownerName));
            accounts.getColumns().add(column("Naturaleza", AccountResponse::nature));
            accounts.getColumns().add(column("Saldo", a -> a.balance().toPlainString()));
            accounts.getColumns().add(column("Conciliada", a -> a.reconciled() ? "Sí" : "NO"));
            accounts.setItems(FXCollections.observableArrayList(api.accounts().join()));
            TableView<InvoiceResponse> invoices = new TableView<>();
            invoices.getColumns().add(column("Número", i -> value(i.number())));
            invoices.getColumns().add(column("Titular", InvoiceResponse::ownerName));
            invoices.getColumns().add(column("Total", i -> i.total().toPlainString()));
            invoices.getColumns().add(column("Pagado", i -> i.paid().toPlainString()));
            invoices.getColumns().add(column("Pendiente", i -> i.pending().toPlainString()));
            invoices.getColumns().add(column("Estado", InvoiceResponse::state));
            accounts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            invoices.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            Runnable reloadInvoices = () -> {
                AccountResponse selected = accounts.getSelectionModel().getSelectedItem();
                run(() -> invoices.setItems(FXCollections.observableArrayList(api.invoices(
                        selected == null ? null : selected.id()).join())));
            };
            accounts.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> reloadInvoices.run());
            Button newInvoice = new Button("Nueva factura");
            newInvoice.setOnAction(e -> {
                AccountResponse selected = accounts.getSelectionModel().getSelectedItem();
                if (selected != null) invoiceDialog(selected).ifPresent(req -> run(() -> {
                    api.createInvoice(req).join(); reloadInvoices.run();
                }));
            });
            Button issue = new Button("Emitir");
            issue.setOnAction(e -> {
                InvoiceResponse selected = invoices.getSelectionModel().getSelectedItem();
                if (selected != null) run(() -> { api.issueInvoice(selected.id()).join(); reloadInvoices.run(); });
            });
            Button pay = new Button("Registrar pago");
            pay.setOnAction(e -> {
                InvoiceResponse selected = invoices.getSelectionModel().getSelectedItem();
                if (selected != null) paymentDialog(selected).ifPresent(req -> run(() -> {
                    api.registerPayment(selected.id(), req).join();
                    accounts.setItems(FXCollections.observableArrayList(api.accounts().join()));
                    reloadInvoices.run();
                }));
            });
            Button close = new Button("Cierre mensual");
            close.setOnAction(e -> {
                AccountResponse selected = accounts.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                TextInputDialog input = new TextInputDialog(LocalDate.now().withDayOfMonth(1).minusMonths(1).toString());
                input.setHeaderText("Período a cerrar (primer día del mes)");
                input.showAndWait().ifPresent(value -> run(() -> {
                    AccountCloseResponse result = api.closeAccount(selected.id(), new CloseAccountRequest(LocalDate.parse(value))).join();
                    info("Cierre mensual", "Período " + result.period() + " cerrado. Saldo inicial: " + result.initialBalance()
                            + ". Débitos: " + result.debits() + ". Créditos: " + result.credits()
                            + ". Saldo final: " + result.finalBalance() + ". No admite movimientos retroactivos.");
                }));
            });
            Button closeAll = new Button("Cerrar todas las cuentas");
            closeAll.setOnAction(e -> {
                TextInputDialog input = new TextInputDialog(LocalDate.now().withDayOfMonth(1).minusMonths(1).toString());
                input.setHeaderText("Período a cerrar en todas las cuentas (primer día del mes)");
                input.showAndWait().ifPresent(value -> run(() -> {
                    AutomaticCloseResponse result = api.closeAllAccounts(new CloseAccountRequest(LocalDate.parse(value))).join();
                    info("Cierre mensual", "Cuentas cerradas: " + result.closed() + ". Ya cerradas: " + result.skipped());
                }));
            });
            Button automatic = new Button("Facturación automática");
            automatic.setOnAction(e -> run(() -> {
                List<TariffResponse> tariffs = api.tariffs().join().stream().filter(TariffResponse::active)
                        .filter(t -> List.of("TRANSPORTE", "SERVICIO").contains(t.serviceType())).toList();
                Dialog<TariffResponse> choice = choice("Tarifa vigente", tariffs,
                        t -> t.concept() + " — " + t.price() + " " + t.currency());
                choice.showAndWait().ifPresent(t -> {
                    AutomaticInvoiceResponse result = api.runAutomaticBilling(t.id()).join();
                    info("Facturación automática", "Procesados: " + result.processed() + ". Nuevos: " + result.created()
                            + ". Salteados por mes cerrado: " + result.skippedClosed());
                });
                reloadInvoices.run();
            }));
            SplitPane split = new SplitPane(new VBox(new Label("Cuentas corrientes"), accounts),
                    new VBox(new Label("Facturas de la cuenta"), invoices));
            split.setDividerPositions(.42); VBox.setVgrow(accounts, Priority.ALWAYS); VBox.setVgrow(invoices, Priority.ALWAYS);
            VBox content = new VBox(10, new HBox(8, newInvoice, issue, pay, close, closeAll, automatic), split);
            content.setPadding(new Insets(8)); VBox.setVgrow(split, Priority.ALWAYS);
            dialog.getDialogPane().setContent(content);
            reloadInvoices.run();
            dialog.showAndWait();
        });
    }

    private Optional<InvoiceRequest> invoiceDialog(AccountResponse account) {
        Dialog<InvoiceRequest> dialog = resultDialog("Nueva factura — " + account.ownerName());
        TextField concept = new TextField(); TextField quantity = text("1"); TextField price = new TextField();
        TextField due = text(LocalDate.now().plusDays(30).toString());
        TextField key = text("MANUAL-" + UUID.randomUUID());
        GridPane grid = grid(); grid.addRow(0, new Label("Concepto"), concept);
        grid.addRow(1, new Label("Cantidad"), quantity); grid.addRow(2, new Label("Precio unitario"), price);
        grid.addRow(3, new Label("Vencimiento"), due); grid.addRow(4, new Label("Clave idempotente"), key);
        Formulario.ayuda(grid, 1, "Mayor a 0. Decimales con punto");
        Formulario.ayuda(grid, 2, "0 o más. Decimales con punto");
        Formulario.ayuda(grid, 3, "Formato AAAA-MM-DD");
        Formulario.ayuda(grid, 4, "Generada sola: evita duplicar la factura");
        Formulario.obligatorios(grid, "Concepto", "Cantidad", "Precio unitario", "Vencimiento", "Clave idempotente");
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE
                ? new InvoiceRequest(account.ownerType(), account.ownerId(), null, LocalDate.parse(due.getText()),
                key.getText(), List.of(new InvoiceLineRequest(concept.getText(), decimal(quantity.getText()),
                decimal(price.getText()), null))) : null);
        return dialog.showAndWait();
    }

    private Optional<PaymentRequest> paymentDialog(InvoiceResponse invoice) {
        Dialog<PaymentRequest> dialog = resultDialog("Registrar pago — " + value(invoice.number()));
        TextField amount = text(invoice.pending().toPlainString());
        ComboBox<String> method = new ComboBox<>(FXCollections.observableArrayList(
                "TRANSFERENCIA", "TARJETA", "EFECTIVO", "OTRO")); method.setValue("TRANSFERENCIA");
        TextField reference = new TextField(); TextField key = text("PAGO-" + UUID.randomUUID());
        GridPane grid = grid(); grid.addRow(0, new Label("Monto"), amount);
        grid.addRow(1, new Label("Medio"), method); grid.addRow(2, new Label("Referencia"), reference);
        grid.addRow(3, new Label("Clave idempotente"), key);
        Formulario.ayuda(grid, 0, "Mayor a 0 y hasta el saldo pendiente. Decimales con punto");
        Formulario.ayuda(grid, 2, "Opcional, no repetida");
        Formulario.ayuda(grid, 3, "Generada sola: evita duplicar el pago");
        Formulario.obligatorios(grid, "Monto", "Medio", "Clave idempotente");
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE
                ? new PaymentRequest(decimal(amount.getText()), method.getValue(), key.getText(), reference.getText()) : null);
        return dialog.showAndWait();
    }

    void showReports() {
        run(() -> {
            Dialog<Void> dialog = baseDialog("Reportes SQL R35–R39", 1180, 720);
            TabPane tabs = new TabPane();
            tabs.getTabs().add(shipmentsReportTab());
            tabs.getTabs().add(deliveryTimesTab());
            tabs.getTabs().add(incidentAverageTab());
            tabs.getTabs().add(deliveryAverageTab());
            tabs.getTabs().add(transportCostsTab());
            tabs.getTabs().forEach(t -> t.setClosable(false));
            dialog.getDialogPane().setContent(tabs);
            dialog.showAndWait();
        });
    }

    private Tab shipmentsReportTab() {
        TableView<ShipmentReportResponse> table = new TableView<>();
        table.getColumns().add(column("Cliente", ShipmentReportResponse::client));
        table.getColumns().add(column("País destino", ShipmentReportResponse::destinationCountry));
        table.getColumns().add(column("Estado", ShipmentReportResponse::state));
        table.getColumns().add(column("Demorado", r -> r.delayed() ? "Sí" : "No"));
        table.getColumns().add(column("Incidentes abiertos", r -> Long.toString(r.openIncidents())));
        TextField country = new TextField(); country.setPromptText("País destino");
        TextField state = new TextField(); state.setPromptText("Estado");
        CheckBox delayed = new CheckBox("Solo demorados"); CheckBox incidents = new CheckBox("Con incidentes abiertos");
        // Un usuario CLIENTE no ve este filtro: el backend ya limita el reporte a su propio cliente.
        boolean canChooseClient = session.permissions().contains("REPORTES_LEER_TODOS")
                && session.permissions().contains("CLIENTES_LEER_TODOS");
        List<Optional<ClientResponse>> clientOptions = new ArrayList<>();
        clientOptions.add(Optional.empty());
        if (canChooseClient) run(() -> api.clients("").join().content().forEach(c -> clientOptions.add(Optional.of(c))));
        ComboBox<Optional<ClientResponse>> client = combo(clientOptions,
                c -> c.map(ClientResponse::businessName).orElse("Todos los clientes"));
        Runnable load = () -> run(() -> table.setItems(FXCollections.observableArrayList(api.shipmentReport(
                country.getText(), state.getText(), client.getValue().map(ClientResponse::id).orElse(null),
                delayed.isSelected() ? true : null, incidents.isSelected() ? true : null).join())));
        Button filter = new Button("Aplicar filtros"); filter.setOnAction(e -> load.run());
        load.run();
        HBox filters = new HBox(8, country, state, delayed, incidents, filter);
        if (canChooseClient) filters.getChildren().add(2, client);
        return new Tab("R35 Envíos", pane(filters, table,
                new Label("País se interpreta como país de destino; los filtros se combinan.")));
    }

    private Tab deliveryTimesTab() {
        TableView<ClientDeliveryTimeResponse> table = new TableView<>();
        table.getColumns().add(column("Cliente", ClientDeliveryTimeResponse::client));
        table.getColumns().add(column("Entregados", r -> Long.toString(r.deliveredShipments())));
        table.getColumns().add(column("Mínimo (h)", r -> value(r.minimumHours())));
        table.getColumns().add(column("Máximo (h)", r -> value(r.maximumHours())));
        table.getColumns().add(column("Promedio (h)", r -> value(r.averageHours())));
        table.setItems(FXCollections.observableArrayList(api.clientDeliveryTimes().join()));
        return new Tab("R36 Máx./mín.", pane(new HBox(), table, new Label("Solo incluye envíos entregados.")));
    }

    private Tab incidentAverageTab() {
        TableView<IncidentAverageResponse> table = new TableView<>();
        table.getColumns().add(column("País", IncidentAverageResponse::destinationCountry));
        table.getColumns().add(column("Estado", IncidentAverageResponse::shipmentState));
        table.getColumns().add(column("Cliente", IncidentAverageResponse::client));
        table.getColumns().add(column("Envíos", r -> Long.toString(r.shipments())));
        table.getColumns().add(column("Incidentes", r -> Long.toString(r.incidents())));
        table.getColumns().add(column("Promedio", r -> value(r.incidentsPerShipment())));
        table.setItems(FXCollections.observableArrayList(api.incidentAverages().join()));
        return new Tab("R37 Incidentes", pane(new HBox(), table,
                new Label("Denominador: envíos no cancelados de cada país destino, estado y cliente.")));
    }

    private Tab deliveryAverageTab() {
        DeliveryAverageResponse value = api.deliveryAverage().join();
        VBox box = new VBox(16, new Label("Envíos entregados: " + value.deliveredShipments()),
                new Label("Tiempo promedio de entrega: " + value(value.averageHours()) + " horas"),
                new Label("Los envíos no entregados quedan excluidos."));
        box.setPadding(new Insets(30));
        return new Tab("R38 Promedio", box);
    }

    private Tab transportCostsTab() {
        TableView<TransportCostResponse> table = new TableView<>();
        table.getColumns().add(column("Medio", TransportCostResponse::transportType));
        table.getColumns().add(column("Tramos con costo real", r -> Long.toString(r.legsWithActualCost())));
        table.getColumns().add(column("Costo total", r -> value(r.totalActualCost())));
        table.getColumns().add(column("Costo promedio", r -> value(r.averageActualCost())));
        table.setItems(FXCollections.observableArrayList(api.transportCosts().join()));
        return new Tab("R39 Costos", pane(new HBox(), table,
                new Label("Totales separados para transporte aéreo, marítimo y terrestre.")));
    }

    private static Dialog<Void> baseDialog(String title, double width, double height) {
        Dialog<Void> dialog = new Dialog<>(); dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(width, height); return dialog;
    }

    private static <T> Dialog<T> resultDialog(String title) {
        Dialog<T> dialog = new Dialog<>(); dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        return dialog;
    }

    private static GridPane grid() {
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(9); grid.setPadding(new Insets(12));
        return grid;
    }

    private static VBox pane(Node actions, Node content, Node status) {
        VBox box = new VBox(10, actions, content, status); box.setPadding(new Insets(10));
        VBox.setVgrow(content, Priority.ALWAYS); return box;
    }

    private static <T> TableColumn<T, String> column(String title, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue())));
        return column;
    }

    private static <T> ComboBox<T> combo(List<T> values, Function<T, String> label) {
        ComboBox<T> combo = new ComboBox<>(FXCollections.observableArrayList(values));
        combo.setConverter(converter(label)); if (!values.isEmpty()) combo.setValue(values.getFirst()); return combo;
    }

    private static <T> ComboBox<T> comboWithEmpty(List<T> values, Function<T, String> label) {
        ComboBox<T> combo = new ComboBox<>(FXCollections.observableArrayList(values));
        combo.setConverter(converter(label)); combo.setPromptText("Sin asignar"); return combo;
    }

    private static <T> Dialog<T> choice(String header, List<T> values, Function<T, String> label) {
        Dialog<T> dialog = new Dialog<>();
        dialog.setHeaderText(header);
        ButtonType accept = new ButtonType("Seleccionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(accept, ButtonType.CANCEL);
        ComboBox<T> combo = combo(values, label);
        combo.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(combo);
        dialog.getDialogPane().lookupButton(accept).setDisable(values.isEmpty());
        dialog.setResultConverter(button -> button == accept ? combo.getValue() : null);
        return dialog;
    }

    private static <T> StringConverter<T> converter(Function<T, String> label) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : label.apply(value); }
            @Override public T fromString(String string) { return null; }
        };
    }

    private static <T> ListCell<T> listCell(Function<T, String> label) {
        return new ListCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : label.apply(item));
            }
        };
    }

    private static TextField text(String value) { return new TextField(value == null ? "" : value); }
    private static Instant parseInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value.strip());
    }
    private static BigDecimal decimal(String value) { return new BigDecimal(value.strip()); }
    private static BigDecimal decimalNullable(String value) { return value == null || value.isBlank() ? null : decimal(value); }
    private static UUID id(Object value) {
        if (value == null) return null;
        if (value instanceof LocationResponse v) return v.id();
        if (value instanceof VehicleResponse v) return v.id();
        if (value instanceof OrganizationResponse v) return v.id();
        throw new IllegalArgumentException("Tipo sin UUID");
    }
    private static String value(Object value) { return value == null ? "" : value.toString(); }

    /** Une el proveedor editado con los demás sin repetir la PK (tramo, proveedor, servicio). */
    static List<RouteSupplierRequest> mergeSuppliers(RouteSupplierRequest edited, List<RouteSupplierRequest> others) {
        List<RouteSupplierRequest> rows = new ArrayList<>();
        if (edited != null) rows.add(edited);
        others.stream().filter(o -> edited == null || !(o.supplierId().equals(edited.supplierId())
                && o.service().equals(edited.service()))).forEach(rows::add);
        return rows;
    }

    private static void run(Runnable operation) {
        try { operation.run(); }
        catch (CompletionException exception) { showError(rootMessage(exception)); }
        catch (RuntimeException exception) { showError(rootMessage(exception)); }
    }

    private static <T> Optional<T> runResult(java.util.concurrent.Callable<Optional<T>> operation) {
        try { return operation.call(); }
        catch (Exception exception) { showError(rootMessage(exception)); return Optional.empty(); }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? "No se pudo completar la operación" : current.getMessage();
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("No se pudo completar la operación"); alert.showAndWait();
    }

    private static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(title); alert.showAndWait();
    }
}
