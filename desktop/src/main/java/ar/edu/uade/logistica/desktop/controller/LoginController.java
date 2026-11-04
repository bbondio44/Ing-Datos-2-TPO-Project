package ar.edu.uade.logistica.desktop.controller;

import ar.edu.uade.logistica.desktop.AppContext;
import ar.edu.uade.logistica.desktop.DesktopApplication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletionException;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator progress;
    @FXML private Label errorLabel;

    @FXML
    private void login() {
        errorLabel.setText("");
        setLoading(true);
        AppContext.get().api().login(emailField.getText().strip(), passwordField.getText())
                .whenComplete((session, error) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (error != null) {
                        errorLabel.setText(message(error));
                        return;
                    }
                    AppContext.get().setSession(session);
                    DesktopApplication.showMain();
                }));
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        progress.setVisible(loading);
    }

    private String message(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause.getMessage() == null ? "No se pudo conectar con el backend" : cause.getMessage();
    }
}

