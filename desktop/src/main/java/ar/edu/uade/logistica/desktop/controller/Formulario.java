package ar.edu.uade.logistica.desktop.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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

    /** Agrega al lado del CUIT un botón que completa uno válido y la aclaración de sus requisitos. */
    static void cuit(GridPane grid, int fila, TextField campo) {
        Button generar = new Button("Generar");
        generar.setOnAction(event -> campo.setText(generarCuit()));
        Label ayuda = new Label("Opcional. 11 dígitos sin guiones, válido y no repetido");
        ayuda.setStyle(GRIS);
        HBox contenido = new HBox(8, generar, ayuda);
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
