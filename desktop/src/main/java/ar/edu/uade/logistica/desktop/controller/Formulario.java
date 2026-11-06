package ar.edu.uade.logistica.desktop.controller;

import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.Set;

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
