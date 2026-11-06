package ar.edu.uade.logistica.integration.neo4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

/** Lee la red de rutas: una sentencia Cypher por línea, sin contar comentarios ni líneas vacías. */
final class RedRutas {
    private static final String ARCHIVO = "neo4j/red_rutas.cypher";

    private RedRutas() {
    }

    static List<String> sentencias() {
        try {
            return sentencias(new ClassPathResource(ARCHIVO).getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudo leer la red de rutas", exception);
        }
    }

    static List<String> sentencias(String contenido) {
        return contenido.lines().map(String::strip)
                .filter(linea -> !linea.isEmpty() && !linea.startsWith("//"))
                .map(linea -> linea.endsWith(";") ? linea.substring(0, linea.length() - 1) : linea)
                .toList();
    }
}
