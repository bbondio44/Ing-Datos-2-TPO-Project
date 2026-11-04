package ar.edu.uade.logistica.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DesktopApplication extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("Logística IoT - Grupo 4");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        show("login.fxml", 1080, 700);
    }

    public static void showMain() {
        try {
            show("main.fxml", 1280, 780);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo abrir la pantalla principal", exception);
        }
    }

    public static void showLogin() {
        try {
            show("login.fxml", 1080, 700);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo abrir el login", exception);
        }
    }

    private static void show(String fxml, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(DesktopApplication.class.getResource("/fxml/" + fxml));
        Parent root = loader.load();
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(DesktopApplication.class.getResource("/css/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

