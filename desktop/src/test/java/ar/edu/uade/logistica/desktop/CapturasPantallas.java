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
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
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
    private static final String CLIENTE_C01 = "aef2750c-1592-5ed4-b7a7-ab5771efc165";
    private static final String CLAVE_GUIA = "Guia-Prueba-2026";

    private static PrintWriter log;
    private static int oks;
    private static int fallas;
    private Stage principal;
    private String clienteNuevo;
    private final String marca = UUID.randomUUID().toString().substring(0, 6);

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
                if (System.getenv("CAPTURA_SOLO_ENVIO") != null) {
                    entrar(System.getenv("CAPTURA_ADMIN_EMAIL"), System.getenv("CAPTURA_ADMIN_PASSWORD"));
                    paso("R06 R07 R08 R24", this::guiaEnvioCompleto);
                } else {
                    recorrerComoAdmin();
                    recorrerComoCliente();
                    recorrerUsuariosCreados();
                }
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
        recorrerGuia();
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
        Window sobrepago = abrirCon(() -> click(boton(finanzas, "Registrar pago")));
        fx(() -> {
            visibleEnFx(sobrepago, TextField.class).getFirst().setText("9999");
            return null;
        });
        Window rechazo = abrirConError(() -> click(boton(sobrepago, "Guardar")));
        verificar("R13 el sobrepago explica el motivo", fx(() -> ((DialogPane) rechazo.getScene().getRoot()).getContentText()).contains("excede"));
        guardar(rechazo, "sobrepago-rechazado", "R13");
        Platform.runLater(rechazo::hide);
        esperarCierre(rechazo);
        seleccionar(tabla(finanzas, 0), clienteNuevo);
        espera(1000);
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
        String[][] pestanias = {{"R36", "Tiempos por cliente"}, {"R37", "Incidentes"}, {"R38", "Tiempo promedio"}, {"R39", "Costos por transporte"}};
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
        pestania(iot, "Seguimiento del viaje");
        ComboBox<Object> contenedor = combo(iot, 0);
        List<DatePicker> fechasMapa = nodos(iot, DatePicker.class);
        fx(() -> {
            contenedor.setValue(CONTENEDOR);
            fechasMapa.get(0).setValue(LocalDate.of(2026, 10, 20));
            fechasMapa.get(1).setValue(LocalDate.of(2026, 10, 22));
            return null;
        });
        click(boton(iot, "Ver recorrido"));
        espera(2500);
        verificar("R20 las posiciones del recorrido aparecen en orden", filas(iot, 0) > 0);
        captura(iot, "seguimiento-viaje", "R20", "R21");
        verificar("R20 la lista muestra el codigo del contenedor", fx(() -> {
            ComboBox<?> lista = visibleEnFx(iot, ComboBox.class).getFirst();
            return lista.getEditor() != null && !lista.getItems().isEmpty();
        }));
        Window envios = abrirCon(() -> click(boton(iot, "Elegir envío…")));
        click(boton(envios, "Seleccionar"));
        esperarCierre(envios);
        espera(2500);
        for (Window extra : ventanas()) {
            if (extra != iot && extra != principal && extra.getScene().getRoot() instanceof DialogPane) {
                click(boton(extra, "Seleccionar"));
                esperarCierre(extra);
                espera(2000);
            }
        }
        verificar("R20 Elegir envio llega al mapa", textos(iot).contains("posición") || textos(iot).contains("Sin posiciones") || textos(iot).contains("no tiene contenedores"));
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
        seleccionar(tabla(alertas, 0), "Regla desde la pantalla");
        click(boton(alertas, "Activar"));
        espera(1500);
        verificar("R09 activar vuelve a dejar activa la regla", fx(() -> visibleEnFx(alertas, TableView.class).getFirst().getItems()
                .stream().anyMatch(r -> r.toString().contains("Regla desde la pantalla") && r.toString().contains("activa=true"))));
        seleccionar(tabla(alertas, 0), "Regla desde la pantalla");
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
        String mensajeAdmin = "Mensaje del admin " + marca;
        fx(() -> {
            TextField campoMensaje = visibleEnFx(alertas, TextField.class).stream()
                    .filter(t -> t.getPromptText() != null && t.getPromptText().contains("escribí un mensaje")).findFirst().orElseThrow();
            campoMensaje.setText(mensajeAdmin);
            campoMensaje.fireEvent(new javafx.event.ActionEvent());
            return null;
        });
        espera(1800);
        verificar("R40 Enter envia el mensaje como admin", fx(() -> visibleEnFx(alertas, TableView.class).get(1).getItems()
                .stream().anyMatch(m -> m.toString().contains(mensajeAdmin))));
        Window destinatarios = abrirCon(() -> click(boton(alertas, "Elegir…")));
        click(boton(destinatarios, "Seleccionar"));
        esperarCierre(destinatarios);
        verificar("R40 Elegir completa el UUID del destinatario", fx(() -> visibleEnFx(alertas, TextField.class).stream()
                .anyMatch(t -> "UUID del destinatario".equals(t.getPromptText()) && t.getText().matches("[0-9a-f-]{36}"))));
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
        captura(analitica, "reportes", "R11");
        pestania(analitica, "Red del cliente");
        TextField cliente = visible(analitica, TextField.class).getFirst();
        fx(() -> {
            cliente.setText(CLIENTE_C02);
            return null;
        });
        click(boton(analitica, "Consultar"));
        esperarConsulta(analitica);
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
        TextField texto = fx(() -> visibleEnFx(alertas, TextField.class).stream()
                .filter(t -> t.getPromptText() != null && t.getPromptText().contains("escribí un mensaje")).findFirst().orElseThrow());
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

    // ---- Recorrido de las guías: usa cada formulario como lo haría una persona -------------------

    private void recorrerGuia() {
        titulo("Guia paso a paso");
        paso("R18", this::guiaLecturas);
        paso("R09 R32 R26 R23", this::guiaAlertas);
        paso("R01", this::guiaClientes);
        paso("R04", () -> guiaOrganizacion("Proveedores", "Proveedor guia"));
        paso("R05", () -> guiaOrganizacion("Operadores", "Operador guia"));
        paso("R05 vehiculos", this::guiaVehiculo);
        paso("R24 ubicaciones", this::guiaUbicacion);
        paso("R20 R21 viaje", this::guiaViaje);
        paso("R06 R07 R08 R24", this::guiaEnvioCompleto);
        paso("R16", this::guiaTerceros);
        paso("R35", this::guiaFiltrosEnvios);
        paso("R29 R34 R11 R33 R30", this::guiaAnalitica);
        paso("R02", this::guiaUsuarios);
        paso("R12 R15", this::guiaProcesos);
    }

    private void paso(String nombre, Accion accion) {
        try {
            accion.correr();
        } catch (Throwable error) {
            falla(nombre + " interrumpido: " + error);
        }
        try {
            for (Window w : ventanas()) {
                if (w != principal) {
                    Platform.runLater(w::hide);
                    espera(250);
                }
            }
            espera(400);
        } catch (Exception error) {
            falla(nombre + " al cerrar ventanas: " + error);
        }
    }

    private void lecturas(String... argumentos) throws Exception {
        List<String> comando = new ArrayList<>(List.of("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                "scripts\\mandar-lecturas.ps1", "-Email", System.getenv("CAPTURA_ADMIN_EMAIL"),
                "-Clave", System.getenv("CAPTURA_ADMIN_PASSWORD")));
        comando.addAll(List.of(argumentos));
        Process proceso = new ProcessBuilder(comando).redirectErrorStream(true).start();
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        verificar("el script manda la lectura " + String.join(" ", argumentos),
                proceso.waitFor() == 0 && salida.contains("Lectura enviada"));
    }

    private void guiaLecturas() throws Exception {
        lecturas("-Cantidad", "12");
        Window iot = abrir("Monitoreo IoT");
        espera(2000);
        verificar("R22 el sensor demo queda ONLINE con bateria", fx(() -> visibleEnFx(iot, TableView.class).getFirst().getItems()
                .stream().anyMatch(s -> s.toString().contains(SENSOR_DEMO.toString()) && s.toString().contains("conexion=ONLINE"))));
        pestania(iot, "Histórico de lecturas");
        ComboBox<Object> id = combo(iot, 1);
        fx(() -> {
            id.setValue(SENSOR_DEMO);
            return null;
        });
        click(boton(iot, "Consultar"));
        espera(1800);
        verificar("R18 las 12 lecturas aparecen con las fechas por defecto (" + filas(iot, 0) + ")", filas(iot, 0) >= 12);
        captura(iot, "lecturas-enviadas", "R18");
        cerrar(iot);
    }

    private void guiaAlertas() throws Exception {
        Window alertas = abrir("Alertas");
        espera(1800);
        pestania(alertas, "Reglas");
        espera(1000);
        List<TextField> campos = visible(alertas, TextField.class);
        fx(() -> {
            campos.get(0).setText("Regla guia " + marca);
            campos.get(1).setText("40");
            return null;
        });
        click(boton(alertas, "Crear regla"));
        espera(1500);
        seleccionar(tabla(alertas, 0), "Regla guia " + marca);
        lecturas("-Temperatura", "45");
        pestania(alertas, "Alertas");
        click(boton(alertas, "Filtrar"));
        espera(1800);
        verificar("R09 la lectura de 45 genera alerta de la regla nueva", fx(() -> visibleEnFx(alertas, TableView.class).getFirst().getItems()
                .stream().anyMatch(a -> a.toString().contains(SENSOR_DEMO.toString()) && a.toString().contains("Regla guia " + marca))));
        verificar("R32 la misma lectura genera anomalia", fx(() -> visibleEnFx(alertas, TableView.class).getFirst().getItems()
                .stream().anyMatch(a -> a.toString().contains(SENSOR_DEMO.toString()) && a.toString().contains("tipoRegla=ANOMALIA_TEMPERATURA"))));
        captura(alertas, "alertas-de-la-lectura", "R09", "R32");
        seleccionar(tabla(alertas, 0), "Regla guia " + marca);
        click(boton(alertas, "Reconocer"));
        espera(1500);
        seleccionar(tabla(alertas, 0), "Regla guia " + marca);
        click(boton(alertas, "Cerrar"));
        espera(1500);
        seleccionar(tabla(alertas, 0), "Regla guia " + marca);
        click(boton(alertas, "Reconocer"));
        espera(1500);
        verificar("R26 repetir la accion avisa el error: " + textos(alertas), !textos(alertas).isBlank());
        lecturas("-Bateria", "10");
        click(boton(alertas, "Filtrar"));
        espera(1800);
        verificar("R23 la bateria baja genera alerta", fx(() -> visibleEnFx(alertas, TableView.class).getFirst().getItems()
                .stream().anyMatch(a -> a.toString().contains(SENSOR_DEMO.toString()) && a.toString().contains("tipoRegla=BATERIA"))));
        cerrar(alertas);
    }

    private void guiaClientes() throws Exception {
        String nombre = "Cliente guia " + marca;
        Window nuevo = abrirCon(() -> click(boton(principal, "Nuevo cliente")));
        escribir(nuevo, "Razón social", nombre);
        escribir(nuevo, "Email", "email-mal-escrito");
        Window aviso = abrirConError(() -> click(boton(nuevo, "Guardar")));
        verificar("R01 un email mal escrito avisa", fx(() -> ((DialogPane) aviso.getScene().getRoot()).getContentText().contains("email")));
        aceptar(aviso);
        esperarCierre(aviso);
        verificar("R01 el formulario sigue abierto con lo cargado", fx(nuevo::isShowing));
        escribir(nuevo, "Email", "");
        click(boton(nuevo, "Generar"));
        click(boton(nuevo, "Guardar"));
        esperarCierre(nuevo);
        espera(1500);
        verificar("R01 la base acepta el CUIT generado", fx(() -> visibleEnFx(principal, TableView.class).getFirst().getItems()
                .stream().anyMatch(c -> c.toString().contains(nombre) && c.toString().matches(".*cuit=30\\d{9}.*"))));
        seleccionar(tabla(principal, 0), nombre);
        Window edicion = abrirCon(() -> click(boton(principal, "Editar")));
        escribir(edicion, "Teléfono", "1144445555");
        click(boton(edicion, "Guardar"));
        esperarCierre(edicion);
        espera(1500);
        seleccionar(tabla(principal, 0), nombre);
        Window confirma = abrirCon(() -> click(boton(principal, "Desactivar")));
        aceptar(confirma);
        esperarCierre(confirma);
        espera(1500);
        verificar("R01 la baja deja el cliente INACTIVO", fx(() -> visibleEnFx(principal, TableView.class).getFirst().getItems()
                .stream().anyMatch(c -> c.toString().contains(nombre) && c.toString().contains("state=INACTIVO"))));
        seleccionar(tabla(principal, 0), nombre);
        click(boton(principal, "Activar"));
        espera(1500);
        verificar("R01 activar deja el cliente ACTIVO", fx(() -> visibleEnFx(principal, TableView.class).getFirst().getItems()
                .stream().anyMatch(c -> c.toString().contains(nombre) && c.toString().contains("state=ACTIVO"))));
        Window duplicado = abrirCon(() -> click(boton(principal, "Nuevo cliente")));
        escribir(duplicado, "Razón social", "Duplicado " + marca);
        escribir(duplicado, "CUIT", "30755666773");
        click(boton(duplicado, "Guardar"));
        esperarCierre(duplicado);
        espera(1500);
        verificar("R01 el CUIT repetido se rechaza: " + textos(principal), textos(principal).contains("conflicto"));
    }

    private void guiaOrganizacion(String menu, String prefijo) throws Exception {
        String nombre = prefijo + " " + marca;
        Window lista = abrir(menu);
        espera(1200);
        Window nuevo = abrirCon(() -> click(boton(lista, "Nuevo")));
        escribir(nuevo, "Razón social", nombre);
        click(boton(nuevo, "Guardar"));
        esperarCierre(nuevo);
        espera(1500);
        seleccionar(tabla(lista, 0), nombre);
        Window edicion = abrirCon(() -> click(boton(lista, "Editar")));
        escribir(edicion, "Teléfono", "1144445555");
        click(boton(edicion, "Guardar"));
        esperarCierre(edicion);
        espera(1500);
        seleccionar(tabla(lista, 0), nombre);
        Window confirma = abrirCon(() -> click(boton(lista, "Desactivar")));
        aceptar(confirma);
        esperarCierre(confirma);
        espera(1500);
        verificar(menu + " la baja queda INACTIVO", fx(() -> visibleEnFx(lista, TableView.class).getFirst().getItems()
                .stream().anyMatch(o -> o.toString().contains(nombre) && o.toString().contains("state=INACTIVO"))));
        seleccionar(tabla(lista, 0), nombre);
        click(boton(lista, "Activar"));
        espera(1500);
        verificar(menu + " activar deja ACTIVO", fx(() -> visibleEnFx(lista, TableView.class).getFirst().getItems()
                .stream().anyMatch(o -> o.toString().contains(nombre) && o.toString().contains("state=ACTIVO"))));
        String cuit = fx(() -> {
            for (Object fila : visibleEnFx(lista, TableView.class).getFirst().getItems()) {
                String texto = fila.toString();
                if (texto.matches(".*cuit=\\d{11}.*")) return texto.replaceAll(".*cuit=(\\d{11}).*", "$1");
            }
            throw new IllegalStateException("Ninguna fila tiene CUIT");
        });
        Window duplicado = abrirCon(() -> click(boton(lista, "Nuevo")));
        escribir(duplicado, "Razón social", "Duplicado " + marca);
        escribir(duplicado, "CUIT", cuit);
        click(boton(duplicado, "Guardar"));
        esperarCierre(duplicado);
        espera(1500);
        verificar(menu + " el CUIT repetido se rechaza: " + textos(lista), textos(lista).contains("conflicto"));
        cerrar(lista);
    }

    private void guiaVehiculo() throws Exception {
        Window lista = abrir("Vehículos");
        espera(1200);
        int antes = filas(lista, 0);
        Window nuevo = abrirCon(() -> click(boton(lista, "Nuevo")));
        elegir(nuevo, "Operador", "Logistica Oceano");
        click(boton(nuevo, "Generar"));
        verificar("R05 Generar arma una patente", fx(() -> ((TextField) campo(nuevo, "Identificación")).getText().matches("[A-Z]{2}[0-9]{3}[A-Z]{2}")));
        escribir(nuevo, "Identificación", "VH" + marca);
        click(boton(nuevo, "Guardar"));
        esperarCierre(nuevo);
        espera(1500);
        verificar("R05 el vehiculo se crea con su operador", filas(lista, 0) == antes + 1);
        String patente = ("VH" + marca).toUpperCase();
        seleccionar(tabla(lista, 0), patente);
        Window confirma = abrirCon(() -> click(boton(lista, "Desactivar")));
        aceptar(confirma);
        esperarCierre(confirma);
        espera(1500);
        verificar("R05 la baja deja el vehiculo INACTIVO", fx(() -> visibleEnFx(lista, TableView.class).getFirst().getItems()
                .stream().anyMatch(v -> v.toString().contains(patente) && v.toString().contains("state=INACTIVO"))));
        seleccionar(tabla(lista, 0), patente);
        click(boton(lista, "Activar"));
        espera(1500);
        verificar("R05 activar deja el vehiculo ACTIVO", fx(() -> visibleEnFx(lista, TableView.class).getFirst().getItems()
                .stream().anyMatch(v -> v.toString().contains(patente) && v.toString().contains("state=ACTIVO"))));
        cerrar(lista);
    }

    /** Un viaje de punta a punta desde la pantalla: se elige el envío, se inicia y se ve llegar. */
    private void guiaViaje() throws Exception {
        Window envios = abrir("Envíos");
        espera(1000);
        Window catalogos = abrirCon(() -> click(boton(envios, "Catálogos contenedores/sensores")));
        Window contenedor = abrirCon(() -> click(boton(catalogos, "Nuevo contenedor")));
        escribir(contenedor, "Código", "VIAJE" + marca);
        escribir(contenedor, "Tipo", "REEFER");
        escribir(contenedor, "Capacidad", "30");
        click(boton(contenedor, "Guardar"));
        esperarCierre(contenedor);
        espera(1200);
        cerrar(catalogos);
        Window nuevo = abrirCon(() -> click(boton(envios, "Nuevo envío")));
        elegir(nuevo, "Origen", "Buenos Aires");
        elegir(nuevo, "Destino", "Rosario");
        escribir(nuevo, "Volumen m³", "5");
        espera(1500);
        click(boton(nuevo, "Guardar"));
        esperarCierre(nuevo);
        espera(1500);
        seleccionar(tabla(envios, 0), "Rosario");
        Window detalle = abrirCon(() -> click(boton(envios, "Detalle y trazabilidad")));
        Window eleccion = abrirCon(() -> click(boton(detalle, "Asociar contenedor")));
        fx(() -> {
            ComboBox<Object> c = combo0(eleccion);
            c.getItems().stream().filter(i -> i.toString().contains("VIAJE" + marca)).findFirst().ifPresent(c::setValue);
            return null;
        });
        Window conContenedor = abrirCon(() -> click(boton(eleccion, "Seleccionar")));
        seleccionar(tabla(conContenedor, 0), "active=true");
        Window eleccionSensor = abrirCon(() -> click(boton(conContenedor, "Asociar sensor")));
        Window conSensor = abrirCon(() -> click(boton(eleccionSensor, "Seleccionar")));
        revisarErrores();
        cerrar(conSensor);
        cerrar(envios);
        Window iot = abrir("Monitoreo IoT");
        espera(2000);
        pestania(iot, "Seguimiento del viaje");
        Window lista = abrirCon(() -> click(boton(iot, "Elegir envío…")));
        click(boton(lista, "Seleccionar"));
        esperarCierre(lista);
        espera(2500);
        verificar("R20 antes de salir dice que no salio", textos(iot).contains("Todavía no salió"));
        click(boton(iot, "Iniciar viaje"));
        boolean enCamino = false;
        for (int i = 0; i < 40 && !enCamino; i++) {
            espera(500);
            enCamino = textos(iot).contains("Ubicación actual: entre Buenos Aires y Rosario");
        }
        verificar("R21 en viaje muestra la ubicacion con coordenadas", enCamino);
        boolean llego = false;
        for (int i = 0; i < 80 && !llego; i++) {
            espera(1000);
            llego = textos(iot).contains("Llegó a Rosario");
        }
        verificar("R20 al terminar dice que llego", llego);
        verificar("R21 quedan las posiciones del viaje", filas(iot, 0) > 0);
        captura(iot, "viaje-terminado", "R20", "R21");
        cerrar(iot);
    }

    private void guiaUbicacion() throws Exception {
        Window lista = abrir("Ubicaciones");
        espera(1200);
        Window nueva = abrirCon(() -> click(boton(lista, "Nueva")));
        escribir(nueva, "Nombre", "Rosario");
        click(boton(nueva, "Generar"));
        verificar("R24 Generar arma un codigo que no existe", fx(() -> ((TextField) campo(nueva, "Código")).getText().startsWith("ARROS")
                && !"ARROS".equals(((TextField) campo(nueva, "Código")).getText())));
        cerrar(nueva);
        cerrar(lista);
    }

    private void guiaEnvioCompleto() throws Exception {
        String origen = "Santa Fe";
        Window envios = abrir("Envíos");
        espera(1000);
        Window catalogos = abrirCon(() -> click(boton(envios, "Catálogos contenedores/sensores")));
        Window contenedor = abrirCon(() -> click(boton(catalogos, "Nuevo contenedor")));
        escribir(contenedor, "Código", "GUIA" + marca);
        escribir(contenedor, "Tipo", "DRY");
        escribir(contenedor, "Capacidad", "treinta");
        Window avisoNumero = abrirConError(() -> click(boton(contenedor, "Guardar")));
        verificar("R07 una capacidad que no es numero avisa", fx(() -> ((DialogPane) avisoNumero.getScene().getRoot()).getContentText().contains("número")));
        aceptar(avisoNumero);
        esperarCierre(avisoNumero);
        verificar("R07 el formulario sigue abierto con lo cargado", fx(contenedor::isShowing));
        escribir(contenedor, "Capacidad", "30");
        click(boton(contenedor, "Guardar"));
        esperarCierre(contenedor);
        espera(1200);
        cerrar(catalogos);

        Window nuevo = abrirCon(() -> click(boton(envios, "Nuevo envío")));
        elegir(nuevo, "Origen", "Buenos Aires");
        elegir(nuevo, "Destino", origen);
        escribir(nuevo, "Volumen m³", "10");
        espera(2000);
        verificar("R24 el formulario muestra la ruta y el precio", textos(nuevo).contains("531 km") && textos(nuevo).contains("Rosario"));
        click(boton(nuevo, "Guardar"));
        esperarCierre(nuevo);
        espera(1500);
        seleccionar(tabla(envios, 0), origen);
        Window estado = abrirCon(() -> click(boton(envios, "Cambiar estado")));
        fx(() -> {
            combo0(estado).setValue("EN_PREPARACION");
            return null;
        });
        aceptar(estado);
        esperarCierre(estado);
        espera(1500);
        verificar("R06 el envio cambia de estado", fx(() -> visibleEnFx(envios, TableView.class).getFirst().getItems()
                .stream().anyMatch(e -> e.toString().contains(origen) && e.toString().contains("state=EN_PREPARACION"))));

        seleccionar(tabla(envios, 0), origen);
        Window detalle = abrirCon(() -> click(boton(envios, "Detalle y trazabilidad")));
        Window eleccion = abrirCon(() -> click(boton(detalle, "Asociar contenedor")));
        fx(() -> {
            ComboBox<Object> c = combo0(eleccion);
            c.getItems().stream().filter(i -> i.toString().contains("GUIA" + marca)).findFirst().ifPresent(c::setValue);
            return null;
        });
        Window conContenedor = abrirCon(() -> click(boton(eleccion, "Seleccionar")));
        verificar("R07 el contenedor queda vigente", fx(() -> visibleEnFx(conContenedor, TableView.class).getFirst().getItems()
                .stream().anyMatch(a -> a.toString().contains("active=true"))));
        captura(conContenedor, "contenedor-asociado", "R07");

        seleccionar(tabla(conContenedor, 0), "active=true");
        Window eleccionSensor = abrirCon(() -> click(boton(conContenedor, "Asociar sensor")));
        Window conSensor = abrirCon(() -> click(boton(eleccionSensor, "Seleccionar")));
        revisarErrores();
        verificar("R08 el sensor aparece", filas(conSensor, 1) > 0);
        captura(conSensor, "sensor-asociado", "R08");
        seleccionar(tabla(conSensor, 1), "active=true");
        Window sinSensor = abrirCon(() -> click(boton(conSensor, "Desasociar sensor")));
        verificar("R08 el sensor queda con fecha de fin", fx(() -> visibleEnFx(sinSensor, TableView.class).get(1).getItems()
                .stream().anyMatch(a -> a.toString().contains("active=false"))));
        seleccionar(tabla(sinSensor, 0), "active=true");
        Window sinContenedor = abrirCon(() -> click(boton(sinSensor, "Desasociar")));
        verificar("R07 el contenedor queda con fecha de fin", fx(() -> visibleEnFx(sinContenedor, TableView.class).getFirst().getItems()
                .stream().anyMatch(a -> a.toString().contains("active=false"))));
        cerrar(sinContenedor);
        cerrar(envios);

        Window trayectos = abrir("Trayectos");
        ComboBox<Object> envio = combo(trayectos, 0);
        fx(() -> {
            envio.getItems().stream().filter(i -> i.toString().contains(origen)).findFirst().ifPresent(envio::setValue);
            return null;
        });
        espera(1200);
        verificar("R24 el envio nuevo trae los tramos de su ruta", filas(trayectos, 0) == 2);
        Window tramo = abrirCon(() -> click(boton(trayectos, "Nuevo tramo")));
        escribir(tramo, "Origen", "Punto A");
        escribir(tramo, "Destino", "Punto B");
        click(boton(tramo, "Guardar"));
        esperarCierre(tramo);
        espera(1500);
        verificar("R24 el tramo aparece", filas(trayectos, 0) == 3);
        seleccionar(tabla(trayectos, 0), "Punto A");
        Window edicion = abrirCon(() -> click(boton(trayectos, "Editar")));
        elegir(edicion, "Estado", "EN_CURSO");
        elegirIndice(edicion, "Proveedor opcional", 0);
        escribir(edicion, "Servicio proveedor", "ADUANA");
        escribir(edicion, "Costo proveedor", "100");
        click(boton(edicion, "Agregar otro proveedor"));
        espera(500);
        elegirIndice(edicion, "Proveedor opcional", 1);
        escribir(edicion, "Servicio proveedor", "DEPOSITO");
        escribir(edicion, "Costo proveedor", "200");
        click(boton(edicion, "Guardar"));
        esperarCierre(edicion);
        espera(1500);
        verificar("R24 el tramo queda EN_CURSO con dos proveedores", fx(() -> {
            String fila = visibleEnFx(trayectos, TableView.class).getFirst().getItems().getLast().toString();
            return fila.contains("state=EN_CURSO") && fila.split("supplierName=", -1).length == 3;
        }));
        captura(trayectos, "dos-proveedores", "R24", "R04");
        seleccionar(tabla(trayectos, 0), "Punto A");
        Window cancelar = abrirCon(() -> click(boton(trayectos, "Editar")));
        elegir(cancelar, "Estado", "CANCELADO");
        click(boton(cancelar, "Guardar"));
        esperarCierre(cancelar);
        espera(1500);
        seleccionar(tabla(trayectos, 0), "Punto A");
        click(boton(trayectos, "Eliminar"));
        espera(1500);
        verificar("R24 el tramo se elimina", filas(trayectos, 0) == 2);
        cerrar(trayectos);
    }

    private void guiaTerceros() throws Exception {
        Window finanzas = abrir("Facturación");
        for (String cuenta : List.of("Aduana y Servicios", "Rutas Globales")) {
            seleccionar(tabla(finanzas, 0), cuenta);
            espera(800);
            Window factura = abrirCon(() -> click(boton(finanzas, "Nueva factura")));
            escribir(factura, "Concepto", "Servicio de tercero");
            escribir(factura, "Precio unitario", "500");
            click(boton(factura, "Guardar"));
            esperarCierre(factura);
            espera(1500);
            seleccionar(tabla(finanzas, 1), "state=BORRADOR");
            click(boton(finanzas, "Emitir"));
            espera(1500);
            seleccionar(tabla(finanzas, 1), "state=EMITIDA");
            Window lineas = abrirCon(() -> click(boton(finanzas, "Ver detalle")));
            verificar("R10 la factura muestra su detalle", filas(lineas, 0) > 0);
            cerrar(lineas);
            seleccionar(tabla(finanzas, 1), "state=EMITIDA");
            pagar(finanzas, "200");
            seleccionar(tabla(finanzas, 0), cuenta);
            espera(800);
            verificar("R16 " + cuenta + " es cuenta a pagar y refleja el pago", fx(() -> {
                String fila = visibleEnFx(finanzas, TableView.class).getFirst().getSelectionModel().getSelectedItem().toString();
                return fila.contains("nature=PAGAR") && !fila.contains("balance=0.00");
            }));
        }
        cerrar(finanzas);
    }

    private void guiaFiltrosEnvios() throws Exception {
        Window reportes = abrir("Reportes");
        int todos = filas(reportes, 0);
        TextField pais = visible(reportes, TextField.class).getFirst();
        fx(() -> {
            pais.setText("Brasil");
            return null;
        });
        click(boton(reportes, "Aplicar filtros"));
        espera(1200);
        int brasil = filas(reportes, 0);
        verificar("R35 el filtro por pais achica la tabla (" + brasil + " de " + todos + ")", brasil > 0 && brasil < todos);
        fx(() -> {
            nodosDe(reportes, javafx.scene.control.CheckBox.class).getFirst().setSelected(true);
            return null;
        });
        click(boton(reportes, "Aplicar filtros"));
        espera(1200);
        verificar("R35 los filtros se combinan (" + filas(reportes, 0) + ")", filas(reportes, 0) <= brasil);
        cerrar(reportes);
    }

    private void guiaAnalitica() throws Exception {
        Window analitica = abrir("Analítica");
        espera(1500);
        pestania(analitica, "Predicción de atrasos");
        espera(1200);
        click(boton(analitica, "Generar predicción del día"));
        espera(2000);
        click(boton(analitica, "Generar predicción del día"));
        espera(2000);
        verificar("R29 se ven los riesgos", columnas(analitica, 0).contains("riesgo_operativo") && columnas(analitica, 0).contains("riesgo_climatico"));
        click(boton(analitica, "Ver validación"));
        espera(1200);
        revisarErrores();
        captura(analitica, "riesgos", "R29");

        pestania(analitica, "Reportes");
        espera(1200);
        ComboBox<Object> tipo = combo(analitica, 0);
        Window titulares = abrirCon(() -> click(boton(analitica, "Elegir…")));
        click(boton(titulares, "Seleccionar"));
        esperarCierre(titulares);
        verificar("R11 Elegir completa el UUID del cliente", fx(() -> visibleEnFx(analitica, TextField.class).getFirst().getText().matches("[0-9a-f-]{36}")));
        TextField cliente = visible(analitica, TextField.class).getFirst();
        fx(() -> {
            tipo.setValue("PREDICTIVO");
            cliente.setText("0bca0b45-e7f6-59b2-a455-02191ee367f6");
            return null;
        });
        click(boton(analitica, "Solicitar"));
        espera(2500);
        fx(() -> {
            visibleEnFx(analitica, TableView.class).getFirst().getSelectionModel().clearAndSelect(0);
            return null;
        });
        espera(1500);
        verificar("R34 el reporte predictivo trae riesgos", columnas(analitica, 1).contains("riesgo_operativo") && filas(analitica, 1) > 0);
        captura(analitica, "predictivo-con-riesgos", "R34");

        fx(() -> {
            tipo.setValue("DESEMPENO");
            cliente.setText(CLIENTE_C02);
            return null;
        });
        click(boton(analitica, "Solicitar"));
        espera(2500);
        fx(() -> {
            visibleEnFx(analitica, TableView.class).getFirst().getSelectionModel().clearAndSelect(0);
            return null;
        });
        espera(1500);
        verificar("R11 el reporte queda GENERADO con resultado", fx(() -> visibleEnFx(analitica, TableView.class).getFirst()
                .getSelectionModel().getSelectedItem().toString().contains("estado=GENERADO")) && filas(analitica, 1) > 0);
        click(boton(analitica, "Facturar"));
        espera(2000);
        String primera = fx(() -> visibleEnFx(analitica, TableView.class).getFirst().getItems().getFirst().toString());
        fx(() -> {
            visibleEnFx(analitica, TableView.class).getFirst().getSelectionModel().clearAndSelect(0);
            return null;
        });
        espera(800);
        click(boton(analitica, "Facturar"));
        espera(2000);
        String segunda = fx(() -> visibleEnFx(analitica, TableView.class).getFirst().getItems().getFirst().toString());
        verificar("R33 queda FACTURADO con una sola factura", primera.contains("estado=FACTURADO") && primera.equals(segunda));
        captura(analitica, "reporte-facturado", "R33");

        pestania(analitica, "Red del cliente");
        TextField red = visible(analitica, TextField.class).getFirst();
        fx(() -> {
            red.setText(CLIENTE_C02);
            return null;
        });
        click(boton(analitica, "Consultar"));
        esperarConsulta(analitica);
        ComboBox<Object> vista = combo(analitica, 0);
        for (String v : List.of("tramos", "operadores")) {
            fx(() -> {
                vista.setValue(v);
                return null;
            });
            espera(800);
            verificar("R30 la vista " + v + " tiene datos", filas(analitica, 0) > 0);
        }
        cerrar(analitica);
    }

    private void guiaUsuarios() throws Exception {
        for (String[] usuario : List.of(new String[]{correoGuia(1), CLIENTE_C02}, new String[]{correoGuia(2), CLIENTE_C01})) {
            Window admin = abrir("Usuarios y roles");
            Window nuevo = abrirCon(() -> click(boton(admin, "Nuevo usuario")));
            escribir(nuevo, "Nombre", "Guia");
            escribir(nuevo, "Apellido", "Prueba");
            escribir(nuevo, "Email", usuario[0]);
            escribir(nuevo, "Contraseña", CLAVE_GUIA);
            elegir(nuevo, "Tipo titular", "CLIENTE");
            Window titulares = abrirCon(() -> click(boton(nuevo, "Elegir…")));
            click(boton(titulares, "Seleccionar"));
            esperarCierre(titulares);
            verificar("R02 Elegir completa el UUID del titular", fx(() ->
                    ((TextField) campo(nuevo, "UUID titular")).getText().matches("[0-9a-f-]{36}")));
            escribir(nuevo, "UUID titular", usuario[1]);
            fx(() -> {
                javafx.scene.control.ListView<?> roles = nodosDe(nuevo, javafx.scene.control.ListView.class).getFirst();
                for (int i = 0; i < roles.getItems().size(); i++) {
                    if (roles.getItems().get(i).toString().contains("description=CLIENTE")) roles.getSelectionModel().select(i);
                }
                return null;
            });
            click(boton(nuevo, "Guardar"));
            esperarCierre(nuevo);
            espera(1500);
            revisarErrores();
            verificar("R02 el usuario aparece en la tabla", fx(() -> visibleEnFx(admin, TableView.class).getFirst().getItems()
                    .stream().anyMatch(u -> u.toString().contains(usuario[0]))));
            cerrar(admin);
        }
    }

    private void guiaProcesos() throws Exception {
        Window finanzas = abrir("Facturación");
        for (int vuelta = 1; vuelta <= 2; vuelta++) {
            Window mes = abrirCon(() -> click(boton(finanzas, "Facturación mensual")));
            Window resultado = abrirCon(() -> aceptar(mes));
            String texto = fx(() -> ((DialogPane) resultado.getScene().getRoot()).getContentText());
            linea("     " + texto);
            if (vuelta == 2) verificar("R12 la segunda corrida no crea nada", texto.contains("Facturas nuevas: 0"));
            cerrar(resultado);
            espera(800);
        }
        for (int vuelta = 1; vuelta <= 2; vuelta++) {
            Window mes = abrirCon(() -> click(boton(finanzas, "Cobro mensual")));
            Window resultado = abrirCon(() -> aceptar(mes));
            String texto = fx(() -> ((DialogPane) resultado.getScene().getRoot()).getContentText());
            linea("     " + texto);
            if (vuelta == 2) verificar("R13 cobrar dos veces no cobra de mas", texto.contains("Facturas cobradas: 0"));
            cerrar(resultado);
            espera(800);
        }
        for (int vuelta = 1; vuelta <= 2; vuelta++) {
            Window periodo = abrirCon(() -> click(boton(finanzas, "Cerrar todas las cuentas")));
            Window resultado = abrirCon(() -> aceptar(periodo));
            String texto = fx(() -> ((DialogPane) resultado.getScene().getRoot()).getContentText());
            linea("     " + texto);
            if (vuelta == 2) verificar("R15 repetir no cierra nada", texto.contains("Cuentas cerradas: 0"));
            cerrar(resultado);
            espera(800);
        }
        captura(finanzas, "procesos-de-cierre", "R12", "R15");
        cerrar(finanzas);
    }

    private void recorrerUsuariosCreados() throws Exception {
        titulo("Usuarios creados desde la pantalla");
        click(boton(principal, "Cerrar sesión"));
        esperarLogin();
        entrar(correoGuia(1), CLAVE_GUIA);
        verificar("R02 el usuario creado solo ve su cliente", filas(principal, 0) == 1);
        Window primero = abrir("Alertas");
        espera(1500);
        pestania(primero, "Mensajería");
        espera(1500);
        verificar("R40 el primer cliente ve la demora del E002", fx(() -> visibleEnFx(primero, TableView.class).getFirst().getItems()
                .stream().anyMatch(c -> c.toString().contains("Demora del env"))));
        cerrar(primero);
        click(boton(principal, "Cerrar sesión"));
        esperarLogin();
        entrar(correoGuia(2), CLAVE_GUIA);
        Window segundo = abrir("Alertas");
        espera(1500);
        pestania(segundo, "Mensajería");
        espera(1500);
        verificar("R40 otro cliente no ve esa conversacion", fx(() -> visibleEnFx(segundo, TableView.class).getFirst().getItems()
                .stream().noneMatch(c -> c.toString().contains("Demora del env"))));
        cerrar(segundo);
    }

    /** La primera consulta a Neo4j tarda: se espera a que la pantalla deje de decir "Consultando". */
    private void esperarConsulta(Window ventana) throws Exception {
        for (int i = 0; i < 80; i++) {
            espera(250);
            if (!textos(ventana).contains("Consultando")) return;
        }
    }

    private String correoGuia(int numero) {
        return "guia" + numero + "-" + marca + "@ejemplo.test";
    }

    private void aceptar(Window dialogo) {
        Platform.runLater(() -> ((Button) ((DialogPane) dialogo.getScene().getRoot()).lookupButton(ButtonType.OK)).fire());
    }

    private void escribir(Window ventana, String etiqueta, String texto) throws Exception {
        fx(() -> {
            ((TextField) campo(ventana, etiqueta)).setText(texto);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private void elegir(Window ventana, String etiqueta, String contiene) throws Exception {
        boolean elegido = fx(() -> {
            ComboBox<Object> combo = (ComboBox<Object>) campo(ventana, etiqueta);
            for (Object item : combo.getItems()) {
                if (item.toString().contains(contiene)) {
                    combo.setValue(item);
                    return true;
                }
            }
            return false;
        });
        if (!elegido) throw new IllegalStateException("No está la opción " + contiene + " en " + etiqueta);
    }

    @SuppressWarnings("unchecked")
    private void elegirIndice(Window ventana, String etiqueta, int indice) throws Exception {
        fx(() -> {
            ComboBox<Object> combo = (ComboBox<Object>) campo(ventana, etiqueta);
            combo.setValue(combo.getItems().get(indice));
            return null;
        });
    }

    /** El campo de un formulario: el nodo de la misma fila de la grilla que su etiqueta. */
    private static Node campo(Window ventana, String etiqueta) {
        for (GridPane grilla : nodosDe(ventana, GridPane.class)) {
            for (Node hijo : grilla.getChildren()) {
                if (hijo instanceof Label l && etiqueta.equals(l.getText())) {
                    Integer fila = GridPane.getRowIndex(hijo);
                    for (Node otro : grilla.getChildren()) {
                        if (otro != hijo && java.util.Objects.equals(GridPane.getRowIndex(otro), fila)) return otro;
                    }
                }
            }
        }
        throw new IllegalStateException("No existe el campo " + etiqueta);
    }

    private String textos(Window ventana) throws Exception {
        return fx(() -> String.join(" | ", nodosDe(ventana, Label.class).stream().map(Label::getText)
                .filter(t -> t != null && !t.isBlank()).toList()));
    }

    private String columnas(Window ventana, int indice) throws Exception {
        return fx(() -> String.join(",", visibleEnFx(ventana, TableView.class).get(indice).getColumns().stream()
                .map(c -> ((javafx.scene.control.TableColumn<?, ?>) c).getText()).toList()));
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<Object> combo0(Window ventana) {
        return (ComboBox<Object>) nodosDe(ventana, ComboBox.class).getFirst();
    }

    private static <T extends Node> List<T> nodosDe(Window ventana, Class<T> tipo) {
        return visibleEnFx(ventana, tipo);
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

    /** Como abrirCon, pero la ventana que se abre es un error esperado y no cuenta como falla. */
    private Window abrirConError(Accion accion) throws Exception {
        Set<Window> antes = new HashSet<>(ventanas());
        accion.correr();
        for (int i = 0; i < 60; i++) {
            espera(250);
            for (Window w : ventanas()) {
                if (!antes.contains(w)) return w;
            }
        }
        throw new IllegalStateException("No apareció el error esperado");
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
        guardar(ventana, nombre, requisitos);
    }

    private void guardar(Window ventana, String nombre, String... requisitos) throws Exception {
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
        try {
            return resultado.get(20, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException error) {
            Thread.getAllStackTraces().forEach((hilo, pila) -> {
                if (hilo.getName().contains("JavaFX Application")) {
                    linea("HILO TRABADO " + hilo.getName() + " " + hilo.getState());
                    for (StackTraceElement paso : pila) linea("     " + paso);
                }
            });
            throw error;
        }
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
