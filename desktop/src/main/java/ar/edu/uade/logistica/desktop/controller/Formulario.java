package ar.edu.uade.logistica.desktop.controller;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Aclara en cada formulario qué campos son obligatorios y qué requisitos tienen. */
final class Formulario {
    private static final String GRIS = "-fx-text-fill: #66748a; -fx-font-size: 12px;";

    private Formulario() {
    }

    /** Escribe el requisito de un campo a su derecha, en la misma fila de la grilla. */
    static void ayuda(GridPane grid, int fila, String texto) {
        Label ayuda = new Label(texto);
        ayuda.setStyle(GRIS);
        grid.add(ayuda, 2, fila);
    }

    /** Al tocar Guardar corre la validación: si hay un error lo muestra y deja el formulario abierto con lo cargado. */
    static void validarAlGuardar(Dialog<?> dialogo, Supplier<String> validacion) {
        for (ButtonType tipo : dialogo.getDialogPane().getButtonTypes()) {
            if (tipo.getButtonData() != ButtonBar.ButtonData.OK_DONE) continue;
            dialogo.getDialogPane().lookupButton(tipo).addEventFilter(ActionEvent.ACTION, evento -> {
                String error = validacion.get();
                if (error == null) return;
                evento.consume();
                Alert alerta = new Alert(Alert.AlertType.ERROR, error, ButtonType.OK);
                alerta.setHeaderText("Revisá los datos");
                alerta.showAndWait();
            });
        }
    }

    /** El mensaje del dato mal cargado, o null si el email está vacío o bien escrito. */
    static String errorEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.strip().matches("[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)*") ? null
                : "El email no es válido. Tiene que tener el formato nombre@dominio.com";
    }

    /** El mensaje del dato mal cargado, o null si el CUIT está vacío o es válido. */
    static String errorCuit(String cuit) {
        if (cuit == null || cuit.isBlank()) return null;
        String limpio = cuit.strip();
        if (!limpio.matches("\\d{11}")) return "El CUIT debe tener 11 dígitos, sin guiones ni espacios";
        return digitoVerificador(limpio) == limpio.charAt(10) - '0' ? null
                : "El CUIT no es válido: no coincide el último dígito. Podés usar el botón Generar";
    }

    /** El primer error de la lista, o null si todos los campos están bien. */
    static String primerError(String... errores) {
        for (String error : errores) if (error != null) return error;
        return null;
    }

    static String errorObligatorio(String texto, String campo) {
        return texto == null || texto.isBlank() ? "Completá " + campo : null;
    }

    /** Un número con mínimo. Con incluyeMinimo false el valor tiene que ser mayor; vacío solo vale si es opcional. */
    static String errorNumero(String texto, String campo, String minimo, boolean incluyeMinimo, boolean obligatorio) {
        if (texto == null || texto.isBlank()) return obligatorio ? "Completá " + campo : null;
        BigDecimal valor;
        try {
            valor = new BigDecimal(texto.strip());
        } catch (NumberFormatException exception) {
            return campo + " debe ser un número. Los decimales van con punto, por ejemplo 12.5";
        }
        int comparacion = valor.compareTo(new BigDecimal(minimo));
        if (incluyeMinimo ? comparacion >= 0 : comparacion > 0) return null;
        return campo + " debe ser " + (incluyeMinimo ? minimo + " o más" : "mayor a " + minimo);
    }

    /** Fecha y hora UTC opcional. */
    static String errorInstante(String texto, String campo) {
        if (texto == null || texto.isBlank()) return null;
        try {
            Instant.parse(texto.strip());
            return null;
        } catch (DateTimeParseException exception) {
            return campo + " no es válida. Usá el formato 2026-12-01T15:00:00Z";
        }
    }

    static String errorFecha(String texto, String campo) {
        if (texto == null || texto.isBlank()) return "Completá " + campo;
        try {
            LocalDate.parse(texto.strip());
            return null;
        } catch (DateTimeParseException exception) {
            return campo + " no es válida. Usá el formato AAAA-MM-DD, por ejemplo 2026-12-01";
        }
    }

    /** Dos letras del país y tres del nombre, como ARROS; si ese ya existe, le suma un número. */
    static String codigoUbicacion(String pais, String nombre, Set<String> existentes) {
        String base = letras(pais, 2) + letras(nombre, 3);
        String codigo = base;
        for (int i = 2; existentes.contains(codigo); i++) codigo = base + i;
        return codigo;
    }

    private static String letras(String texto, int cantidad) {
        String limpio = Normalizer.normalize(texto, Normalizer.Form.NFD).toUpperCase().replaceAll("[^A-Z0-9]", "");
        return limpio.substring(0, Math.min(cantidad, limpio.length()));
    }

    /** Agrega al lado del CUIT un botón que completa uno válido y la aclaración de sus requisitos. */
    static void cuit(GridPane grid, int fila, TextField campo) {
        Button generar = new Button("Generar");
        generar.setOnAction(event -> campo.setText(generarCuit()));
        conBoton(grid, fila, generar, "Opcional. 11 dígitos sin guiones, válido y no repetido");
    }

    /** Pone a la derecha de un campo un botón que lo completa, junto a la aclaración de sus requisitos. */
    static void conBoton(GridPane grid, int fila, Button boton, String texto) {
        Label ayuda = new Label(texto);
        ayuda.setStyle(GRIS);
        HBox contenido = new HBox(8, boton, ayuda);
        contenido.setAlignment(Pos.CENTER_LEFT);
        grid.add(contenido, 2, fila);
    }

    /** CUIT de empresa (prefijo 30) con el dígito verificador correcto. */
    static String generarCuit() {
        while (true) {
            String base = "30" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
            int digito = digitoVerificador(base);
            if (digito < 10) return base + digito;
        }
    }

    /** Mismo cálculo que tp_logistica.cuit_valido; da 10 cuando esos diez dígitos no admiten un CUIT válido. */
    static int digitoVerificador(String diezDigitos) {
        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < pesos.length; i++) suma += (diezDigitos.charAt(i) - '0') * pesos[i];
        int digito = 11 - suma % 11;
        return digito == 11 ? 0 : digito;
    }

    /** Marca los campos obligatorios con un asterisco aparte, sin cambiar el texto de la etiqueta. */
    static void obligatorios(GridPane grid, String... etiquetas) {
        Set<String> nombres = Set.of(etiquetas);
        for (Node nodo : grid.getChildren()) {
            if (nodo instanceof Label etiqueta && nombres.contains(etiqueta.getText())) {
                Label asterisco = new Label("*");
                asterisco.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                etiqueta.setGraphic(asterisco);
                etiqueta.setContentDisplay(ContentDisplay.RIGHT);
            }
        }
        Label nota = new Label("* Campo obligatorio");
        nota.setStyle(GRIS);
        grid.add(nota, 0, grid.getRowCount(), 3, 1);
    }
}
