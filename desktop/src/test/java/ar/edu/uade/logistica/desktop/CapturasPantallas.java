package ar.edu.uade.logistica.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Prueba de pantallas: maneja la aplicación JavaFX real como lo haría un usuario, sin atajos a la API.
 * Entra, recorre los menús, aplica filtros y guarda capturas en evidencias/RXX. Falla si aparece una
 * ventana de error o si una tabla que debería tener datos queda vacía. Se corre con scripts/pruebas/capturas.ps1.
 */
public class CapturasPantallas extends Application {
    private static final Path EVIDENCIAS = Path.of("evidencias");
    private static final UUID SEN_001 = UUID.fromString("101231a8-af8f-53ec-bfb0-f4d579ba7455");
    private static final UUID SENSOR_DEMO = UUID.fromString("cb6a673c-14a5-5469-a365-30fe9700c80b");
    private static final UUID CONTENEDOR = UUID.fromString("42d867d9-d7a4-5248-9681-33e2c0cdb123");
    private static final String ENVIO_E002 = "ec22d634-96cd-5267-b7bd-accdce959d92";
    private static final String CLIENTE_C02 = "270823a9-7978-5eda-80f0-9ae673331b0b";

    private static PrintWriter log;
    private static int oks;
    private static int fallas;
    private Stage principal;
    private String clienteNuevo;

    public static final class Lanzador {
        public static void main(String[] args) throws IOException {
            log = new PrintWriter(Files.newBufferedWriter(EVIDENCIAS.resolve("pantallas-javafx.txt"), StandardCharsets.UTF_8));
            Application.launch(CapturasPantallas.class, args);
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        principal = stage;
        new DesktopApplication().start(stage);
        Thread hilo = new Thread(() -> {
            try {
                recorrerComoAdmin();
                recorrerComoCliente();
            } catch (Throwable error) {
                falla("recorrido interrumpido: " + error);
            }
            linea("RESUMEN pantallas: " + oks + " OK, " + fallas + " FALLA");
            log.close();
            System.exit(fallas == 0 ? 0 : 1);
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    // ---- Recorridos ---------------------------------------------------------------------------

    private void recorrerComoAdmin() throws Exception {
        titulo("Administrador");
        entrar(System.getenv("CAPTURA_ADMIN_EMAIL"), System.getenv("CAPTURA_ADMIN_PASSWORD"));
        altaClienteDesdeLaPantalla();
        conDatos(principal, "R01 listado de clientes");
        captura(principal, "clientes", "R01");

        Window admin = abrir("Usuarios y roles");
        conDatos(admin, "R02 usuarios");
        captura(admin, "usuarios", "R02");
        pestania(admin, "Roles y permisos");
        conDatos(admin, "R02 roles");
        captura(admin, "roles", "R02");
        cerrar(admin);

        Window sesiones = abrir("Sesiones activas");
        captura(sesiones, "sesiones", "R03");
        cerrar(sesiones);

        Window proveedores = abrir("Proveedores");
        conDatos(proveedores, "R04 proveedores");
        captura(proveedores, "proveedores", "R04");
        cerrar(proveedores);
        Window operadores = abrir("Operadores");
        conDatos(operadores, "R05 operadores");
        captura(operadores, "operadores", "R05");
        cerrar(operadores);
        Window vehiculos = abrir("Vehículos");
        conDatos(vehiculos, "R05 vehiculos");
        captura(vehiculos, "vehiculos", "R05");
        cerrar(vehiculos);

        recorrerEnvios();
        recorrerTrayectos();
        recorrerFacturacion();
        recorrerReportes(true);
        recorrerMonitoreo();
        recorrerAlertas();
        recorrerAnalitica();
        click(boton(principal, "Cerrar sesión"));
        esperarLogin();
    }

    private void altaClienteDesdeLaPantalla() throws Exception {
        String nombre = "Alta desde pantalla " + UUID.randomUUID().toString().substring(0, 6);
        clienteNuevo = nombre;
        Window dialogo = abrirCon(() -> click(boton(principal, "Nuevo cliente")));
        List<TextField> campos = nodos(dialogo, TextField.class);
        fx(() -> {
            campos.get(0).setText(nombre);
            campos.get(1).setText(cuitValido());
            return null;
        });
        captura(dialogo, "alta-cliente", "R01");
        click(boton(dialogo, "Guardar"));
        esperarCierre(dialogo);
        espera(1500);
        fx(() -> {
            visibleEnFx(principal, TextField.class).getFirst().setText(nombre);
            return null;
        });
        click(boton(principal, "Buscar"));
        espera(1200);
        verificar("R01 el cliente nuevo aparece en la tabla", filas(principal, 0) == 1);
        captura(principal, "cliente-creado", "R01");
        fx(() -> {
            visibleEnFx(principal, TextField.class).getFirst().clear();
            return null;
        });
        click(boton(principal, "Buscar"));
        espera(1200);
    }

    private void recorrerEnvios() throws Exception {
        Window envios = abrir("Envíos");
        conDatos(envios, "R06 envios");
        captura(envios, "envios", "R06");
        seleccionar(tabla(envios, 0), "Valencia");
        Window detalle = abrirCon(() -> click(boton(envios, "Detalle y trazabilidad")));
        conDatos(detalle, "R06 detalle del envio");
        captura(detalle, "detalle-envio", "R06", "R07", "R08");
        cerrar(detalle);
        Window catalogos = abrirCon(() -> click(boton(envios, "Catálogos contenedores/sensores")));
        conDatos(catalogos, "R07 contenedores");
        captura(catalogos, "contenedores", "R07");
        seleccionar(tabla(catalogos, 0), "");
        Window historial = abrirCon(() -> click(boton(catalogos, "Historial de movimientos")));
        conDatos(historial, "R07 historial del contenedor");
        captura(historial, "historial-contenedor", "R07");
        cerrar(historial);
        pestania(catalogos, "Sensores");
        conDatos(catalogos, "R08 sensores");
        captura(catalogos, "sensores", "R08");
        cerrar(catalogos);
        cerrar(envios);
    }

    private void recorrerTrayectos() throws Exception {
        Window trayectos = abrir("Trayectos");
        ComboBox<Object> envio = combo(trayectos, 0);
        fx(() -> {
            for (Object item : envio.getItems()) {
                if (item.toString().contains("Valencia") && item.toString().contains("Transportes del Sur")) {
                    envio.setValue(item);
                }
            }
            return null;
        });
        espera(1500);
        conDatos(trayectos, "R24 tramos del envio");
        captura(trayectos, "tramos", "R24", "R04", "R05");
        seleccionar(tabla(trayectos, 0), "");
        Window edicion = abrirCon(() -> click(boton(trayectos, "Editar")));
        captura(edicion, "edicion-tramo", "R24", "R04");
        cerrar(edicion);
        cerrar(trayectos);
    }

    private void recorrerFacturacion() throws Exception {
        Window finanzas = abrir("Facturación");
        conDatos(finanzas, "R14 cuentas corrientes");
        captura(finanzas, "cuentas", "R14");
        seleccionar(tabla(finanzas, 0), "Transportes del Sur");
        espera(1200);
        captura(finanzas, "facturas-cliente", "R10", "R12");
        seleccionar(tabla(finanzas, 0), "Aduana y Servicios");
        espera(1200);
        verificar("R16 la cuenta del proveedor tiene facturas", filas(finanzas, 1) > 0);
        captura(finanzas, "cuenta-proveedor", "R16", "R04");
        seleccionar(tabla(finanzas, 0), "Rutas Globales");
        espera(1200);
        verificar("R16 la cuenta del operador tiene facturas", filas(finanzas, 1) > 0);
        captura(finanzas, "cuenta-operador", "R16", "R05");

        // Flujo completo con el cliente creado desde la pantalla: factura, emisión, cobros y cierre.
        seleccionar(tabla(finanzas, 0), clienteNuevo);
        espera(800);
        Window factura = abrirCon(() -> click(boton(finanzas, "Nueva factura")));
        List<TextField> campos = visible(factura, TextField.class);
        fx(() -> {
            campos.get(0).setText("Servicio desde la pantalla");
            campos.get(2).setText("1000");
            return null;
        });
        click(boton(factura, "Guardar"));
        esperarCierre(factura);
        espera(1500);
        revisarErrores();
        seleccionar(tabla(finanzas, 1), "state=BORRADOR");
        click(boton(finanzas, "Emitir"));
        espera(1500);
        revisarErrores();
        seleccionar(tabla(finanzas, 1), "state=EMITIDA");
        pagar(finanzas, "400");
        seleccionar(tabla(finanzas, 0), clienteNuevo);
        espera(1000);
        verificar("R10 el cobro parcial cambia el estado", fx(() -> tabla0(finanzas, 1).toString().contains("state=PARCIAL")));
        captura(finanzas, "cobro-parcial", "R10", "R13");
        seleccionar(tabla(finanzas, 1), "state=PARCIAL");
        pagar(finanzas, "600");
        seleccionar(tabla(finanzas, 0), clienteNuevo);
        espera(1000);
        verificar("R10 el cobro total deja la factura pagada", fx(() -> tabla0(finanzas, 1).toString().contains("state=PAGADA")));
        captura(finanzas, "cobro-total", "R10", "R13", "R14");
        Window periodo = abrirCon(() -> click(boton(finanzas, "Cierre mensual")));
        fx(() -> {
            visibleEnFx(periodo, TextField.class).getFirst().setText(LocalDate.now().withDayOfMonth(1).toString());
            return null;
        });
        Window resultado = abrirCon(() -> Platform.runLater(() -> {
            Button ok = (Button) ((DialogPane) periodo.getScene().getRoot()).lookupButton(ButtonType.OK);
            ok.fire();
        }));
        verificar("R15 el cierre informa el saldo", fx(() -> ((DialogPane) resultado.getScene().getRoot()).getContentText().contains("Saldo final")));
        captura(resultado, "cierre-mensual", "R15");
        cerrar(resultado);
        cerrar(finanzas);
    }

    private void pagar(Window finanzas, String monto) throws Exception {
        Window pago = abrirCon(() -> click(boton(finanzas, "Registrar pago")));
        fx(() -> {
            visibleEnFx(pago, TextField.class).getFirst().setText(monto);
            return null;
        });
        click(boton(pago, "Guardar"));
        esperarCierre(pago);
        espera(1500);
        revisarErrores();
    }

    private static Object tabla0(Window ventana, int indice) {
        return visibleEnFx(ventana, TableView.class).get(indice).getItems().getFirst();
    }

    private void recorrerReportes(boolean admin) throws Exception {
        Window reportes = abrir("Reportes");
        conDatos(reportes, "R35 reporte de envios");
        if (admin) {
            ComboBox<Object> cliente = combo(reportes, 0);
            fx(() -> {
                for (Object item : cliente.getItems()) {
                    if (item.toString().contains("Transportes del Sur")) cliente.setValue(item);
                }
                return null;
            });
            int todos = filas(reportes, 0);
            click(boton(reportes, "Aplicar filtros"));
            espera(1200);
            verificar("R35 el filtro por cliente reduce la tabla", filas(reportes, 0) > 0 && filas(reportes, 0) < todos);
            captura(reportes, "filtro-cliente", "R35");
        } else {
            verificar("R35 el CLIENTE no ve el combo de clientes", nodos(reportes, ComboBox.class).isEmpty());
            captura(reportes, "vista-cliente", "R35");
            cerrar(reportes);
            return;
        }
        String[][] pestanias = {{"R36", "R36 Máx./mín."}, {"R37", "R37 Incidentes"}, {"R38", "R38 Promedio"}, {"R39", "R39 Costos"}};
        for (String[] p : pestanias) {
            pestania(reportes, p[1]);
            espera(600);
            if (!"R38".equals(p[0])) conDatos(reportes, p[0] + " reporte");
            captura(reportes, "reporte", p[0]);
        }
        cerrar(reportes);
    }

    private void recorrerMonitoreo() throws Exception {
        Window iot = abrir("Monitoreo IoT");
        espera(2000);
        conDatos(iot, "R22 estado de sensores");
        captura(iot, "estado-sensores", "R22");
        pestania(iot, "Histórico de lecturas");
        List<DatePicker> fechas = nodos(iot, DatePicker.class);
        ComboBox<Object> id = combo(iot, 1);
        fx(() -> {
            id.setValue(SEN_001);
            fechas.get(0).setValue(LocalDate.of(2026, 10, 20));
            fechas.get(1).setValue(LocalDate.of(2026, 10, 21));
            return null;
        });
        click(boton(iot, "Consultar"));
        espera(1800);
        conDatos(iot, "R19 historico de dos dias");
        captura(iot, "historico-dos-dias", "R19");
        fx(() -> {
            id.setValue(SENSOR_DEMO);
            fechas.get(0).setValue(LocalDate.now().minusDays(1));
            fechas.get(1).setValue(LocalDate.now().plusDays(1));
            return null;
        });
        click(boton(iot, "Consultar"));
        espera(1800);
        conDatos(iot, "R18 lecturas registradas");
        captura(iot, "lecturas-registradas", "R18");
        pestania(iot, "Recorrido y mapa");
        ComboBox<Object> contenedor = combo(iot, 0);
        List<DatePicker> fechasMapa = nodos(iot, DatePicker.class);
        fx(() -> {
            contenedor.setValue(CONTENEDOR);
            fechasMapa.get(0).setValue(LocalDate.of(2026, 10, 20));
            fechasMapa.get(1).setValue(LocalDate.of(2026, 10, 22));
            return null;
        });
        click(boton(iot, "Ver recorrido"));
        espera(20000);
        captura(iot, "mapa-recorrido", "R20", "R21");
        cerrar(iot);
    }

    private void recorrerAlertas() throws Exception {
        Window alertas = abrir("Alertas");
        espera(1800);
        conDatos(alertas, "R26 alertas");
        captura(alertas, "alertas", "R26");
        seleccionar(tabla(alertas, 0), "tipoRegla=TEMPERATURA");
        captura(alertas, "alerta-temperatura", "R09", "R25");
        seleccionar(tabla(alertas, 0), "tipoRegla=ANOMALIA_TEMPERATURA");
        captura(alertas, "alerta-anomalia", "R32");
        seleccionar(tabla(alertas, 0), "tipoRegla=INACTIVIDAD");
        captura(alertas, "alerta-inactividad", "R23");
        seleccionar(tabla(alertas, 0), "estado=ABIERTA");
        String idAlerta = fx(() -> visibleEnFx(alertas, TableView.class).getFirst().getSelectionModel().getSelectedItem().toString())
                .replaceAll(".*?\\bid=([^,]+),.*", "$1");
        click(boton(alertas, "Reconocer"));
        espera(1500);
        seleccionar(tabla(alertas, 0), "id=" + idAlerta + ",");
        verificar("R26 la alerta pasa a RECONOCIDA", fx(() -> visibleEnFx(alertas, TableView.class).getFirst()
                .getSelectionModel().getSelectedItem().toString().contains("estado=RECONOCIDA")));
        captura(alertas, "alerta-reconocida", "R26");
        click(boton(alertas, "Cerrar"));
        espera(1500);
        seleccionar(tabla(alertas, 0), "id=" + idAlerta + ",");
        verificar("R26 la alerta pasa a CERRADA", fx(() -> visibleEnFx(alertas, TableView.class).getFirst()
                .getSelectionModel().getSelectedItem().toString().contains("estado=CERRADA")));
        captura(alertas, "alerta-cerrada", "R26");
        pestania(alertas, "Reglas");
        espera(1200);
        conDatos(alertas, "R09 reglas");
        int reglas = filas(alertas, 0);
        List<TextField> camposRegla = visible(alertas, TextField.class);
        fx(() -> {
            camposRegla.get(0).setText("Regla desde la pantalla");
            camposRegla.get(1).setText("45");
            return null;
        });
        click(boton(alertas, "Crear regla"));
        espera(1500);
        verificar("R09 la regla nueva aparece en la tabla", filas(alertas, 0) == reglas + 1);
        seleccionar(tabla(alertas, 0), "Regla desde la pantalla");
        captura(alertas, "regla-creada", "R09");
        click(boton(alertas, "Desactivar"));
        espera(1500);
        pestania(alertas, "Monitoreo de envío");
        TextField envio = visible(alertas, TextField.class).getFirst();
        fx(() -> {
            envio.setText(ENVIO_E002);
            return null;
        });
        click(boton(alertas, "Consultar"));
        espera(2500);
        conDatos(alertas, "R17 monitoreo del envio");
        captura(alertas, "monitoreo-envio", "R17");
        pestania(alertas, "Mensajería");
        espera(1500);
        conDatos(alertas, "R40 conversaciones internas");
        seleccionar(tabla(alertas, 0), "GRUPAL");
        espera(1500);
        verificar("R40 la conversacion grupal muestra mensajes", filas(alertas, 1) > 0);
        captura(alertas, "mensajeria-grupal", "R40");
        cerrar(alertas);
    }

    private void recorrerAnalitica() throws Exception {
        Window analitica = abrir("Analítica");
        espera(1500);
        ComboBox<Object> consulta = combo(analitica, 0);
        int[] orden = {0, 1, 2};
        String[] requisitos = new String[3];
        for (int i : orden) {
            fx(() -> {
                consulta.getSelectionModel().select(i);
                return null;
            });
            espera(1500);
            String texto = String.valueOf(consulta.getValue()).toLowerCase();
            String r = texto.contains("rutas") ? "R27" : texto.contains("cliente") ? "R28" : "R31";
            requisitos[i] = r;
            conDatos(analitica, r + " analitica");
            captura(analitica, "resultado", r);
        }
        pestania(analitica, "Predicción de atrasos");
        espera(1500);
        conDatos(analitica, "R29 predicciones");
        captura(analitica, "predicciones", "R29");
        pestania(analitica, "Reportes");
        espera(1200);
        conDatos(analitica, "R11 solicitudes de reporte");
        seleccionar(tabla(analitica, 0), "PREDICTIVO");
        espera(1500);
        captura(analitica, "reporte-predictivo", "R34", "R11");
        seleccionar(tabla(analitica, 0), "FACTURADO");
        espera(1500);
        captura(analitica, "reporte-facturado", "R33");
        pestania(analitica, "Red del cliente");
        TextField cliente = visible(analitica, TextField.class).getFirst();
        fx(() -> {
            cliente.setText(CLIENTE_C02);
            return null;
        });
        click(boton(analitica, "Consultar"));
        espera(2000);
        conDatos(analitica, "R30 red del cliente");
        captura(analitica, "red-cliente", "R30");
        cerrar(analitica);
    }

    private void recorrerComoCliente() throws Exception {
        titulo("Usuario CLIENTE");
        entrar(System.getenv("CAPTURA_CLIENTE_EMAIL"), System.getenv("CAPTURA_CLIENTE_PASSWORD"));
        verificar("R02 el CLIENTE ve solo su organizacion", filas(principal, 0) == 1);
        captura(principal, "vista-cliente", "R02");
        recorrerReportes(false);
        Window alertas = abrir("Alertas");
        espera(1500);
        pestania(alertas, "Mensajería");
        espera(1500);
        conDatos(alertas, "R40 conversaciones del cliente");
        seleccionar(tabla(alertas, 0), "PRIVADA");
        espera(1500);
        int mensajes = filas(alertas, 1);
        TextField texto = visible(alertas, TextField.class).getFirst();
        fx(() -> {
            texto.setText("Mensaje enviado desde la pantalla");
            return null;
        });
        click(boton(alertas, "Enviar"));
        espera(1800);
        verificar("R40 el mensaje nuevo aparece en la conversacion", filas(alertas, 1) == mensajes + 1);
        captura(alertas, "mensajeria-cliente", "R40");
        cerrar(alertas);
    }

    // ---- Acciones de usuario ------------------------------------------------------------------

    private void entrar(String email, String clave) throws Exception {
        espera(800);
        fx(() -> {
            ((TextField) principal.getScene().lookup("#emailField")).setText(email);
            ((PasswordField) principal.getScene().lookup("#passwordField")).setText(clave);
            return null;
        });
        click((Button) fx(() -> principal.getScene().lookup("#loginButton")));
        for (int i = 0; i < 40; i++) {
            espera(250);
            if (fx(() -> principal.getScene().lookup("#searchField") != null || principal.getScene().lookup("#clientsTable") != null)) {
                espera(1500);
                verificar("entra a la pantalla principal", true);
                return;
            }
        }
        throw new IllegalStateException("No pasó del login");
    }

    private void esperarLogin() throws Exception {
        for (int i = 0; i < 40; i++) {
            espera(250);
            if (fx(() -> principal.getScene().lookup("#emailField") != null)) return;
        }
        throw new IllegalStateException("No volvió al login");
    }

    private Window abrir(String textoBoton) throws Exception {
        return abrirCon(() -> click(boton(principal, textoBoton)));
    }

    private Window abrirCon(Accion accion) throws Exception {
        Set<Window> antes = new HashSet<>(ventanas());
        accion.correr();
        for (int i = 0; i < 60; i++) {
            espera(250);
            for (Window w : ventanas()) {
                if (!antes.contains(w)) {
                    espera(1200);
                    revisarErrores();
                    return w;
                }
            }
        }
        throw new IllegalStateException("No se abrió la ventana");
    }

    private void cerrar(Window ventana) throws Exception {
        revisarErrores();
        Platform.runLater(ventana::hide);
        esperarCierre(ventana);
    }

    private void esperarCierre(Window ventana) throws Exception {
        for (int i = 0; i < 40; i++) {
            espera(150);
            if (!fx(ventana::isShowing)) return;
        }
        throw new IllegalStateException("La ventana no se cerró");
    }

    private void click(Button boton) {
        Platform.runLater(boton::fire);
    }

    private void pestania(Window ventana, String texto) throws Exception {
        boolean seleccionada = fx(() -> {
            for (TabPane panel : visibleEnFx(ventana, TabPane.class)) {
                for (Tab tab : panel.getTabs()) {
                    if (tab.getText().equals(texto)) {
                        panel.getSelectionModel().select(tab);
                        return true;
                    }
                }
            }
            return false;
        });
        if (!seleccionada) throw new IllegalStateException("No existe la pestaña " + texto);
        espera(500);
    }

    private void seleccionar(TableView<?> tabla, String contiene) throws Exception {
        boolean encontrada = fx(() -> {
            ObservableList<?> items = tabla.getItems();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).toString().contains(contiene)) {
                    tabla.getSelectionModel().clearAndSelect(i);
                    tabla.scrollTo(i);
                    return true;
                }
            }
            return false;
        });
        verificar("hay una fila con [" + contiene + "]", encontrada);
        espera(400);
    }

    // ---- Verificaciones y evidencia -----------------------------------------------------------

    private void conDatos(Window ventana, String que) throws Exception {
        revisarErrores();
        int total = fx(() -> visibleEnFx(ventana, TableView.class).stream().mapToInt(t -> t.getItems().size()).sum());
        verificar(que + " (" + total + " filas)", total > 0);
    }

    private int filas(Window ventana, int indice) throws Exception {
        return fx(() -> visibleEnFx(ventana, TableView.class).get(indice).getItems().size());
    }

    private void revisarErrores() throws Exception {
        for (Window w : ventanas()) {
            DialogPane pane = fx(() -> w.getScene().getRoot() instanceof DialogPane d ? d : null);
            if (pane != null && fx(() -> pane.getStyleClass().contains("error"))) {
                falla("ventana de error: " + fx(pane::getContentText));
                Platform.runLater(w::hide);
                esperarCierre(w);
            }
        }
    }

    private void captura(Window ventana, String nombre, String... requisitos) throws Exception {
        revisarErrores();
        WritableImage imagen = fx(() -> ventana.getScene().snapshot(null));
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixeles = imagen.getPixelReader();
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) salida.setRGB(x, y, pixeles.getArgb(x, y));
        }
        for (String requisito : requisitos) {
            Path carpeta = EVIDENCIAS.resolve(requisito);
            Files.createDirectories(carpeta);
            long n;
            try (var archivos = Files.list(carpeta)) {
                n = archivos.filter(p -> p.getFileName().toString().startsWith("captura-")).count();
            }
            String archivo = String.format("captura-%02d-%s.png", n + 1, nombre);
            ImageIO.write(salida, "png", carpeta.resolve(archivo).toFile());
            linea("     " + requisito + "/" + archivo);
        }
    }

    private void verificar(String que, boolean condicion) {
        if (condicion) {
            oks++;
            linea("OK    " + que);
        } else {
            falla(que);
        }
    }

    private void falla(String que) {
        fallas++;
        linea("FALLA " + que);
    }

    private void titulo(String texto) {
        linea("=== " + texto);
    }

    private static void linea(String texto) {
        System.out.println(texto);
        log.println(texto);
        log.flush();
    }

    // ---- Búsqueda de nodos --------------------------------------------------------------------

    private List<Window> ventanas() throws Exception {
        return fx(() -> Window.getWindows().stream().filter(Window::isShowing).toList());
    }

    private Button boton(Window ventana, String texto) throws Exception {
        return fx(() -> {
            for (Button b : visibleEnFx(ventana, Button.class)) {
                if (texto.equals(b.getText())) return b;
            }
            throw new IllegalStateException("No existe el botón " + texto);
        });
    }

    private TableView<?> tabla(Window ventana, int indice) throws Exception {
        return fx(() -> visibleEnFx(ventana, TableView.class).get(indice));
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Object> combo(Window ventana, int indice) throws Exception {
        return fx(() -> (ComboBox<Object>) visibleEnFx(ventana, ComboBox.class).get(indice));
    }

    private <T extends Node> List<T> nodos(Window ventana, Class<T> tipo) throws Exception {
        return visible(ventana, tipo);
    }

    private <T extends Node> List<T> visible(Window ventana, Class<T> tipo) throws Exception {
        return fx(() -> visibleEnFx(ventana, tipo));
    }

    private static <T extends Node> List<T> visibleEnFx(Window ventana, Class<T> tipo) {
        List<T> encontrados = new ArrayList<>();
        Scene escena = ventana.getScene();
        recorrer(escena.getRoot(), tipo, encontrados);
        return encontrados;
    }

    private static <T extends Node> void recorrer(Node nodo, Class<T> tipo, List<T> encontrados) {
        if (!nodo.isVisible()) return;
        if (tipo.isInstance(nodo)) {
            encontrados.add(tipo.cast(nodo));
            // Un DatePicker contiene su propio TextField: no se entra en los controles compuestos.
            if (nodo instanceof ComboBox || nodo instanceof DatePicker) return;
        }
        if (nodo instanceof ComboBox || nodo instanceof DatePicker) return;
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) recorrer(hijo, tipo, encontrados);
        }
    }

    private static <T> T fx(Callable<T> tarea) throws Exception {
        CompletableFuture<T> resultado = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                resultado.complete(tarea.call());
            } catch (Throwable error) {
                resultado.completeExceptionally(error);
            }
        });
        return resultado.get(20, TimeUnit.SECONDS);
    }

    private static void espera(long milisegundos) throws InterruptedException {
        Thread.sleep(milisegundos);
    }

    private static String cuitValido() {
        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        Random azar = new Random();
        while (true) {
            String numero = "30" + (10000000 + azar.nextInt(89999999));
            int suma = 0;
            for (int i = 0; i < 10; i++) suma += (numero.charAt(i) - '0') * pesos[i];
            int digito = 11 - suma % 11;
            if (digito == 11) digito = 0;
            if (digito < 10) return numero + digito;
        }
    }

    @FunctionalInterface
    private interface Accion {
        void correr() throws Exception;
    }
}
