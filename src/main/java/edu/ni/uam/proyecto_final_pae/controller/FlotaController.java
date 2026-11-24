package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.CatalogoDAO;
import edu.ni.uam.proyecto_final_pae.dao.ClienteDAO;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.FlotaDAO;
import edu.ni.uam.proyecto_final_pae.dao.OperadorDAO;
import edu.ni.uam.proyecto_final_pae.dao.UnidadDAO;
import edu.ni.uam.proyecto_final_pae.dao.UsuarioDAO;
import edu.ni.uam.proyecto_final_pae.model.Cliente;
import edu.ni.uam.proyecto_final_pae.model.EstadoOperador;
import edu.ni.uam.proyecto_final_pae.model.EstadoUnidad;
import edu.ni.uam.proyecto_final_pae.model.Operador;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Unidad;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import edu.ni.uam.proyecto_final_pae.util.Tooltips;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** CRUD de unidades, operadores y clientes conectado a Hibernate (RF-01, RF-02 y login). */
public class FlotaController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UnidadDAO unidadDAO = new UnidadDAO();
    private final OperadorDAO operadorDAO = new OperadorDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();
    private final FlotaDAO flotaDAO = new FlotaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private Timeline reloj;
    private Timeline relojTunel;
    private Process procesoTunel;
    private volatile Process procesoApi;
    private final Object bloqueoApi = new Object();
    private Path logApi;
    private String enlaceTunel;
    private String estadoTunel = "Sin conexión";
    private String errorConexion;
    private volatile boolean apiIniciadaPorApp;
    private volatile boolean iniciandoConexion;
    private volatile long revisionConexion;
    private final HttpClient httpCliente = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(2))
            .build();
    private PauseTransition pausaTooltipTunel;
    private static final Pattern URL_TUNEL = Pattern.compile("https://[a-zA-Z0-9.-]+\\.trycloudflare\\.com");

    private List<Unidad> todasLasUnidades = List.of();
    private List<Operador> todosLosOperadores = List.of();
    private List<Cliente> todosLosClientes = List.of();
    private boolean cargandoFlota;

    /** Copia de las tres listas leidas en el hilo de fondo. */
    private record DatosFlota(List<Unidad> unidades, List<Operador> operadores, List<Cliente> clientes) {
    }

    @FXML private TableView<Unidad> tablaUnidades;
    @FXML private TableColumn<Unidad, String> colNumeroUnidad;
    @FXML private TableColumn<Unidad, String> colPlacas;
    @FXML private TableColumn<Unidad, String> colModelo;
    @FXML private TableColumn<Unidad, String> colCapacidad;
    @FXML private TableColumn<Unidad, String> colEstadoUnidad;

    @FXML private TextField txtNumeroUnidad;
    @FXML private TextField txtPlacas;
    @FXML private TextField txtModelo;
    @FXML private TextField txtCapacidad;
    @FXML private ComboBox<EstadoUnidad> cmbEstadoUnidad;

    @FXML private TableView<Operador> tablaOperadores;
    @FXML private TableColumn<Operador, String> colCedula;
    @FXML private TableColumn<Operador, String> colNombreOperador;
    @FXML private TableColumn<Operador, String> colTelefono;
    @FXML private TableColumn<Operador, String> colHorasMax;
    @FXML private TableColumn<Operador, String> colHorasMin;
    @FXML private TableColumn<Operador, String> colEstadoOperador;

    @FXML private TextField txtCedula;
    @FXML private TextField txtNombreOperador;
    @FXML private TextField txtTelefonoOperador;
    @FXML private TextField txtHorasMax;
    @FXML private TextField txtHorasMin;
    @FXML private ComboBox<EstadoOperador> cmbEstadoOperador;

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombreCliente;
    @FXML private TableColumn<Cliente, String> colContactoCliente;
    @FXML private TableColumn<Cliente, String> colTelefonoCliente;
    @FXML private TableColumn<Cliente, String> colEmailCliente;
    @FXML private TableColumn<Cliente, String> colDireccionCliente;
    @FXML private TableColumn<Cliente, String> colActivoCliente;
    @FXML private TableColumn<Cliente, String> colRegistroCliente;

    @FXML private TextField txtNombreCliente;
    @FXML private TextField txtContactoCliente;
    @FXML private TextField txtTelefonoCliente;
    @FXML private TextField txtEmailCliente;
    @FXML private TextField txtDireccionCliente;
    @FXML private CheckBox chkActivoCliente;

    @FXML private Label lblEmpresa;

    @FXML private TextField txtFiltroUnidad;
    @FXML private TextField txtFiltroOperador;
    @FXML private TextField txtFiltroCliente;
    @FXML private Button btnLimpiarBusquedaUnidad;
    @FXML private Button btnLimpiarBusquedaOperador;
    @FXML private Button btnLimpiarBusquedaCliente;

    @FXML private ToggleButton btnUnidadTodos;
    @FXML private ToggleButton btnUnidadDisponible;
    @FXML private ToggleButton btnUnidadRuta;
    @FXML private ToggleButton btnUnidadMantenimiento;
    @FXML private ToggleButton btnOperadorTodos;
    @FXML private ToggleButton btnOperadorDisponible;
    @FXML private ToggleButton btnOperadorRuta;
    @FXML private ToggleButton btnOperadorDescanso;
    @FXML private ToggleButton btnOperadorInactivo;
    @FXML private ToggleButton btnClienteTodos;
    @FXML private ToggleButton btnClienteActivos;
    @FXML private ToggleButton btnClienteInactivos;

    @FXML private Label lblEstadoUnidades;
    @FXML private Label lblEstadoOperadores;
    @FXML private Label lblEstadoClientes;
    @FXML private TabPane tabs;

    @FXML private Button btnRefrescarFlota;
    @FXML private Button btnPanelAdmin;
    @FXML private Button btnReporte;

    @FXML private Button btnGuardarUnidad;
    @FXML private Button btnActualizarUnidad;
    @FXML private Button btnEliminarUnidad;
    @FXML private Button btnLimpiarUnidad;

    @FXML private Button btnCargas;
    @FXML private Button btnViajes;
    @FXML private Button btnMonitor;
    @FXML private Button btnSalir;
    @FXML private Button btnConexion;
    @FXML private Circle circuloConexion;
    @FXML private Button btnEnlaceTunel;
    @FXML private FontIcon iconoEnlaceTunel;

    @FXML private Button btnGuardarOperador;
    @FXML private Button btnActualizarOperador;
    @FXML private Button btnEliminarOperador;
    @FXML private Button btnLimpiarOperador;

    @FXML private Button btnGuardarCliente;
    @FXML private Button btnActualizarCliente;
    @FXML private Button btnEliminarCliente;
    @FXML private Button btnLimpiarCliente;

    private String filtroEstadoUnidad = "Todos";
    private String filtroEstadoOperador = "Todos";
    private String filtroEstadoCliente = "Todos";

    @FXML
    private void initialize() {
        if (!sesionAdmin()) {
            return;
        }
        Usuario usuarioActual = SesionActual.getUsuario();
        boolean principal = Boolean.TRUE.equals(usuarioActual.getEsPrincipal());
        boolean conReportes = principal || Boolean.TRUE.equals(usuarioActual.getPuedeVerReportes());
        btnPanelAdmin.setVisible(principal);
        btnPanelAdmin.setManaged(principal);
        btnReporte.setVisible(conReportes);
        btnReporte.setManaged(conReportes);

        configurarConexionTunel();
        configurarFiltros();
        txtFiltroUnidad.textProperty().addListener((o, a, b) -> aplicarFiltroUnidades());
        txtFiltroOperador.textProperty().addListener((o, a, b) -> aplicarFiltroOperadores());
        txtFiltroCliente.textProperty().addListener((o, a, b) -> aplicarFiltroClientes());

        colNumeroUnidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroUnidad()));
        colPlacas.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPlacas()));
        colModelo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModelo()));
        colCapacidad.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCapacidadToneladas() == null ? "—" : c.getValue().getCapacidadToneladas().toPlainString()));
        colEstadoUnidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getNombre()));

        colCedula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCedula()));
        colNombreOperador.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() == null ? "—" : c.getValue().getTelefono()));
        colHorasMax.setCellValueFactory(c -> new SimpleStringProperty(
                horas(c.getValue().getHorasMaxConduccion())));
        colHorasMin.setCellValueFactory(c -> new SimpleStringProperty(
                horas(c.getValue().getHorasMinDescanso())));
        colEstadoOperador.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getNombre()));

        colNombreCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreEmpresa()));
        colContactoCliente.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getContacto())));
        colTelefonoCliente.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getTelefono())));
        colEmailCliente.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getEmail())));
        colDireccionCliente.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getDireccion())));
        colActivoCliente.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getActivo()) ? "Sí" : "No"));
        colRegistroCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaRegistro() == null ? "—" : c.getValue().getFechaRegistro().format(FECHA)));

        tablaUnidades.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual != null) {
                llenarUnidad(actual);
            }
        });
        tablaOperadores.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual != null) {
                llenarOperador(actual);
            }
        });
        tablaClientes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual != null) {
                llenarCliente(actual);
            }
        });

        BooleanBinding unidadIncompleta = Bindings.createBooleanBinding(
                () -> vacio(txtNumeroUnidad) || vacio(txtPlacas) || vacio(txtModelo)
                        || cmbEstadoUnidad.getValue() == null,
                txtNumeroUnidad.textProperty(), txtPlacas.textProperty(),
                txtModelo.textProperty(), cmbEstadoUnidad.valueProperty());

        BooleanBinding operadorIncompleto = Bindings.createBooleanBinding(
                () -> vacio(txtCedula) || vacio(txtNombreOperador) || vacio(txtHorasMax)
                        || vacio(txtHorasMin) || cmbEstadoOperador.getValue() == null,
                txtCedula.textProperty(), txtNombreOperador.textProperty(),
                txtHorasMax.textProperty(), txtHorasMin.textProperty(),
                cmbEstadoOperador.valueProperty());

        BooleanBinding sinUnidadSeleccionada = tablaUnidades.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding sinOperadorSeleccionado = tablaOperadores.getSelectionModel().selectedItemProperty().isNull();
        BooleanBinding clienteIncompleto = Bindings.createBooleanBinding(
                () -> vacio(txtNombreCliente), txtNombreCliente.textProperty());
        BooleanBinding sinClienteSeleccionado = tablaClientes.getSelectionModel().selectedItemProperty().isNull();

        btnGuardarUnidad.disableProperty().bind(unidadIncompleta);
        btnActualizarUnidad.disableProperty().bind(sinUnidadSeleccionada.or(unidadIncompleta));
        btnEliminarUnidad.disableProperty().bind(sinUnidadSeleccionada);

        btnGuardarOperador.disableProperty().bind(operadorIncompleto);
        btnActualizarOperador.disableProperty().bind(sinOperadorSeleccionado.or(operadorIncompleto));
        btnEliminarOperador.disableProperty().bind(sinOperadorSeleccionado);

        btnGuardarCliente.disableProperty().bind(clienteIncompleto);
        btnActualizarCliente.disableProperty().bind(sinClienteSeleccionado.or(clienteIncompleto));
        btnEliminarCliente.disableProperty().bind(sinClienteSeleccionado);

        try {
            empresaDAO.obtener().ifPresent(empresa -> lblEmpresa.setText(empresa.getNombre()));
            cmbEstadoUnidad.setItems(FXCollections.observableArrayList(catalogoDAO.listar(EstadoUnidad.class)));
            cmbEstadoOperador.setItems(FXCollections.observableArrayList(catalogoDAO.listar(EstadoOperador.class)));
            verUnidades();
            verOperadores();
            verClientes();
        } catch (PersistenciaException e) {
            error("No se pudo conectar con la base de datos", e);
        }

        reloj = new Timeline(new KeyFrame(javafx.util.Duration.seconds(30), evento -> {
            if (!Navegacion.vistaVisible(lblEstadoUnidades)) {
                reloj.stop();
                return;
            }
            verificarSesion();
            if (SesionActual.getUsuario() != null) {
                actualizarDatosAutomaticamente();
            }
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }

    private void configurarFiltros() {
        btnUnidadTodos.setUserData("Todos");
        btnUnidadDisponible.setUserData("Disponible");
        btnUnidadRuta.setUserData("En ruta");
        btnUnidadMantenimiento.setUserData("Mantenimiento");
        btnUnidadTodos.getToggleGroup().selectedToggleProperty().addListener((o, anterior, actual) -> {
            filtroEstadoUnidad = actual == null ? "Todos" : String.valueOf(actual.getUserData());
            aplicarFiltroUnidades();
        });

        btnOperadorTodos.setUserData("Todos");
        btnOperadorDisponible.setUserData("Disponible");
        btnOperadorRuta.setUserData("En ruta");
        btnOperadorDescanso.setUserData("Descanso");
        btnOperadorInactivo.setUserData("Inactivo");
        btnOperadorTodos.getToggleGroup().selectedToggleProperty().addListener((o, anterior, actual) -> {
            filtroEstadoOperador = actual == null ? "Todos" : String.valueOf(actual.getUserData());
            aplicarFiltroOperadores();
        });

        btnClienteTodos.setUserData("Todos");
        btnClienteActivos.setUserData("Activos");
        btnClienteInactivos.setUserData("Inactivos");
        btnClienteTodos.getToggleGroup().selectedToggleProperty().addListener((o, anterior, actual) -> {
            filtroEstadoCliente = actual == null ? "Todos" : String.valueOf(actual.getUserData());
            aplicarFiltroClientes();
        });
    }

    @FXML
    private void limpiarBusquedaUnidad() {
        txtFiltroUnidad.clear();
    }

    @FXML
    private void limpiarBusquedaOperador() {
        txtFiltroOperador.clear();
    }

    @FXML
    private void limpiarBusquedaCliente() {
        txtFiltroCliente.clear();
    }

    @FXML
    private void refrescarFlota() {
        if (!sesionAdmin()) {
            return;
        }
        cargarFlotaEnSegundoPlano(true, true, true, true);
    }

    private void actualizarDatosAutomaticamente() {
        boolean unidadesLibres = tablaUnidades.getSelectionModel().getSelectedItem() == null;
        boolean operadoresLibres = tablaOperadores.getSelectionModel().getSelectedItem() == null;
        boolean clientesLibres = tablaClientes.getSelectionModel().getSelectedItem() == null;
        cargarFlotaEnSegundoPlano(false, unidadesLibres, operadoresLibres, clientesLibres);
    }

    /**
     * Consulta las tres listas en un hilo de fondo y publica el resultado en el hilo de JavaFX.
     * Las consultas que se hacen desde la UI no bloquean el refresco de la ventana.
     */
    private void cargarFlotaEnSegundoPlano(boolean manual, boolean aplicarUnidades,
                                            boolean aplicarOperadores, boolean aplicarClientes) {
        if (cargandoFlota) {
            return;
        }
        cargandoFlota = true;
        if (manual) {
            btnRefrescarFlota.setDisable(true);
        }

        Task<DatosFlota> tarea = new Task<>() {
            @Override
            protected DatosFlota call() {
                return new DatosFlota(unidadDAO.listar(), operadorDAO.listar(), clienteDAO.listar());
            }
        };

        tarea.setOnSucceeded(evento -> Platform.runLater(() -> {
            DatosFlota datos = tarea.getValue();
            if (aplicarUnidades) {
                todasLasUnidades = datos.unidades();
                aplicarFiltroUnidades();
            }
            if (aplicarOperadores) {
                todosLosOperadores = datos.operadores();
                aplicarFiltroOperadores();
            }
            if (aplicarClientes) {
                todosLosClientes = datos.clientes();
                aplicarFiltroClientes();
            }
            mostrarBotonRefrescar(false);
            if (manual) {
                btnRefrescarFlota.setDisable(false);
                mensaje("Datos actualizados.");
            }
            cargandoFlota = false;
        }));

        tarea.setOnFailed(evento -> Platform.runLater(() -> {
            mostrarBotonRefrescar(true);
            if (manual) {
                btnRefrescarFlota.setDisable(false);
                Throwable causa = tarea.getException();
                PersistenciaException error = causa instanceof PersistenciaException persistencia
                        ? persistencia
                        : new PersistenciaException("No se pudieron actualizar los datos", causa);
                error("No se pudieron actualizar los datos", error);
            } else {
                mensaje("No se pudieron actualizar los datos. Use el botón para reintentar.");
            }
            cargandoFlota = false;
        }));

        Thread hilo = new Thread(tarea, "flota-carga");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarBotonRefrescar(boolean mostrar) {
        btnRefrescarFlota.setVisible(mostrar);
        btnRefrescarFlota.setManaged(mostrar);
    }

    private void mensaje(String texto) {
        int indice = tabs == null ? 0 : tabs.getSelectionModel().getSelectedIndex();
        if (indice == 1 && lblEstadoOperadores != null) {
            lblEstadoOperadores.setText(texto);
        } else if (indice == 2 && lblEstadoClientes != null) {
            lblEstadoClientes.setText(texto);
        } else if (lblEstadoUnidades != null) {
            lblEstadoUnidades.setText(texto);
        }
    }

    @FXML
    private void salir() {
        detenerServicios();
        Navegacion.cerrarSesion(Navegacion.stageDe(btnSalir));
    }

    // ------------------------------------------------------------------ túnel de administración

    private void configurarConexionTunel() {
        pintarConexion(Color.web("#B42318"), "Sin conexión", "Iniciar Conexión");
        relojTunel = new Timeline(new KeyFrame(javafx.util.Duration.seconds(5), evento -> actualizarEstadoTunel()));
        relojTunel.setCycleCount(Timeline.INDEFINITE);
        relojTunel.play();
        Platform.runLater(() -> {
            if (btnSalir.getScene() != null) {
                ((Stage) btnSalir.getScene().getWindow()).setOnCloseRequest(evento -> detenerServicios());
            }
        });
    }

    @FXML
    private void alternarConexion() {
        if (iniciandoConexion) {
            detenerServicios();
        } else if (procesoTunel != null && procesoTunel.isAlive()) {
            detenerServicios();
        } else {
            iniciarTunel();
        }
    }

    private void iniciarTunel() {
        if (iniciandoConexion || (procesoTunel != null && procesoTunel.isAlive())) {
            return;
        }
        String ejecutable = buscarCloudflared();
        if (ejecutable == null) {
            pintarConexion(Color.web("#B8860B"), "cloudflared no está instalado", "Iniciar Conexión");
            return;
        }
        iniciandoConexion = true;
        long solicitud = ++revisionConexion;
        errorConexion = null;
        if (relojTunel != null) {
            relojTunel.play();
        }
        pintarConexion(Color.web("#B8860B"), "Iniciando API…", "Detener Conexión");
        Thread preparacion = new Thread(() -> prepararConexion(ejecutable, solicitud), "fleetroute-api-tunel");
        preparacion.setDaemon(true);
        preparacion.start();
    }

    private boolean conexionActual(long solicitud) {
        return iniciandoConexion && revisionConexion == solicitud;
    }

    private void prepararConexion(String ejecutable, long solicitud) {
        String error = asegurarApi(solicitud);
        if (!conexionActual(solicitud)) {
            return;
        }
        if (error != null) {
            Platform.runLater(() -> {
                if (!conexionActual(solicitud)) return;
                iniciandoConexion = false;
                errorConexion = error;
                detenerApiPropia();
                pintarConexion(Color.web("#B8860B"), error, "Iniciar Conexión");
            });
            return;
        }
        Platform.runLater(() -> {
            if (!conexionActual(solicitud)) {
                return;
            }
            iniciandoConexion = false;
            lanzarTunel(ejecutable);
        });
    }

    /** Comprueba la API y la arranca desde api-operador/target si es necesario. */
    private String asegurarApi(long solicitud) {
        if (apiSaludable()) {
            return null;
        }
        if (Runtime.version().feature() < 21) {
            return "La API requiere Java 21 o superior. Ejecute FleetRoute con JDK 21.";
        }
        Path jar = buscarJarApi();
        if (jar == null) {
            return "Falta api-operador/target/api-operador-0.0.1-SNAPSHOT.jar. Compile la API.";
        }
        synchronized (bloqueoApi) {
            if (!conexionActual(solicitud)) return null;
            if (procesoApi == null || !procesoApi.isAlive()) {
                try {
                    Path apiDir = jar.getParent().getParent();
                    Path javaExe = Path.of(System.getProperty("java.home"), "bin", "java.exe");
                    if (!Files.isRegularFile(javaExe)) javaExe = Path.of(System.getProperty("java.home"), "bin", "java");
                    ProcessBuilder builder = new ProcessBuilder(
                            javaExe.toString(), "-jar", jar.toAbsolutePath().toString());
                    builder.directory(apiDir.toFile());
                    logApi = Files.createTempFile("fleetroute-api-", ".log");
                    builder.redirectErrorStream(true);
                    builder.redirectOutput(logApi.toFile());
                    procesoApi = builder.start();
                    apiIniciadaPorApp = true;
                } catch (IOException e) {
                    procesoApi = null;
                    apiIniciadaPorApp = false;
                    return "No se pudo iniciar la API: " + e.getMessage();
                }
            }
        }
        for (int intento = 0; intento < 30 && conexionActual(solicitud); intento++) {
            if (apiSaludable()) {
                return null;
            }
            if (procesoApi == null || !procesoApi.isAlive()) {
                return "La API se cerró durante el arranque. Revise el registro: " + logApi;
            }
            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "El arranque de la conexión fue interrumpido.";
            }
        }
        return conexionActual(solicitud)
                ? "La API no respondió a tiempo. Revise el registro: " + logApi
                : null;
    }

    private boolean apiSaludable() {
        try {
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/salud"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .GET()
                    .build();
            return httpCliente.send(solicitud, HttpResponse.BodyHandlers.discarding())
                    .statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private Path buscarJarApi() {
        Path directorio = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int nivel = 0; nivel < 5 && directorio != null; nivel++, directorio = directorio.getParent()) {
            Path jar = directorio.resolve("api-operador")
                    .resolve("target")
                    .resolve("api-operador-0.0.1-SNAPSHOT.jar");
            if (Files.isRegularFile(jar)) {
                return jar;
            }
            if ("api-operador".equals(String.valueOf(directorio.getFileName()))) {
                jar = directorio.resolve("target").resolve("api-operador-0.0.1-SNAPSHOT.jar");
                if (Files.isRegularFile(jar)) {
                    return jar;
                }
            }
        }
        return null;
    }

    private void lanzarTunel(String ejecutable) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    ejecutable,
                    "tunnel",
                    "--url", "http://localhost:8080",
                    "--protocol", "http2",
                    "--edge-ip-version", "4");
            builder.redirectErrorStream(true);
            procesoTunel = builder.start();
            Process proceso = procesoTunel;
            enlaceTunel = null;
            if (relojTunel != null) {
                relojTunel.play();
            }
            pintarConexion(Color.web("#B8860B"), "Conectando…", "Detener Conexión");
            Thread lector = new Thread(() -> leerSalidaTunel(proceso), "cloudflared-output");
            lector.setDaemon(true);
            lector.start();
        } catch (IOException e) {
            procesoTunel = null;
            errorConexion = "No se pudo iniciar cloudflared";
            pintarConexion(Color.web("#B8860B"), errorConexion, "Iniciar Conexión");
        }
    }

    private String buscarCloudflared() {
        List<String> candidatos = new ArrayList<>();
        String local = System.getenv("LOCALAPPDATA");
        if (local != null) {
            candidatos.add(local + "\\Microsoft\\WinGet\\Links\\cloudflared.exe");
        }
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            candidatos.add(programFiles + "\\cloudflared\\cloudflared.exe");
        }
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        if (programFilesX86 != null) {
            candidatos.add(programFilesX86 + "\\cloudflared\\cloudflared.exe");
        }
        for (String candidato : candidatos) {
            if (Files.isExecutable(Path.of(candidato))) {
                return candidato;
            }
        }
        return "cloudflared";
    }

    private void leerSalidaTunel(Process proceso) {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                Matcher matcher = URL_TUNEL.matcher(linea);
                if (matcher.find()) {
                    String url = matcher.group();
                    Platform.runLater(() -> {
                        if (procesoTunel == proceso) {
                            enlaceTunel = url;
                            errorConexion = null;
                            pintarConexion(Color.web("#1B7F3B"), url, "Detener Conexión");
                        }
                    });
                }
            }
        } catch (IOException ignored) {
            // El monitor de estado detecta la caída del proceso.
        }
        Platform.runLater(() -> {
            if (procesoTunel == proceso) {
                procesoTunel = null;
                enlaceTunel = null;
                pintarConexion(Color.web("#B42318"), "Sin conexión", "Iniciar Conexión");
            }
        });
    }

    private void actualizarEstadoTunel() {
        if (iniciandoConexion) {
            pintarConexion(Color.web("#B8860B"), "Iniciando API…", "Detener Conexión");
            return;
        }
        if (procesoTunel == null || !procesoTunel.isAlive()) {
            if (procesoTunel != null) {
                procesoTunel = null;
                enlaceTunel = null;
            }
            if (errorConexion != null) {
                pintarConexion(Color.web("#B8860B"), errorConexion, "Iniciar Conexión");
            } else {
                pintarConexion(Color.web("#B42318"), "Sin conexión", "Iniciar Conexión");
            }
            return;
        }
        if (enlaceTunel == null || enlaceTunel.isBlank()) {
            pintarConexion(Color.web("#B8860B"), "Conectando…", "Detener Conexión");
            return;
        }
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/salud"))
                .timeout(java.time.Duration.ofSeconds(2))
                .GET()
                .build();
        httpCliente.sendAsync(solicitud, HttpResponse.BodyHandlers.discarding())
                .thenAccept(respuesta -> Platform.runLater(() -> {
                    if (enlaceTunel != null) {
                        pintarConexion(respuesta.statusCode() == 200
                                        ? Color.web("#1B7F3B") : Color.web("#B8860B"),
                                enlaceTunel, "Detener Conexión");
                    }
                }))
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        if (enlaceTunel != null) {
                            pintarConexion(Color.web("#B8860B"), enlaceTunel, "Detener Conexión");
                        }
                    });
                    return null;
                });
    }

    private void detenerTunel() {
        iniciandoConexion = false;
        revisionConexion++;
        errorConexion = null;
        if (relojTunel != null) {
            relojTunel.stop();
        }
        if (procesoTunel != null) {
            procesoTunel.destroy();
            procesoTunel = null;
        }
        enlaceTunel = null;
        if (btnConexion != null) {
            pintarConexion(Color.web("#B42318"), "Sin conexión", "Iniciar Conexión");
        }
    }

    private void detenerServicios() {
        detenerTunel();
        detenerApiPropia();
    }

    private void detenerApiPropia() {
        synchronized (bloqueoApi) {
            if (apiIniciadaPorApp && procesoApi != null) {
                procesoApi.destroy();
            }
            procesoApi = null;
            apiIniciadaPorApp = false;
        }
    }

    private void pintarConexion(Color color, String texto, String textoBoton) {
        if (circuloConexion != null) {
            circuloConexion.setFill(color);
        }
        estadoTunel = texto;
        boolean activo = enlaceTunel != null && !enlaceTunel.isBlank();
        if (iconoEnlaceTunel != null) {
            iconoEnlaceTunel.setIconLiteral(activo ? "fas-link" : "fas-unlink");
            iconoEnlaceTunel.setIconColor(Color.web(activo ? "#0E7490" : "#8A97AA"));
        }
        if (btnEnlaceTunel != null) {
            actualizarTooltipTunel();
        }
        if (btnConexion != null) {
            btnConexion.setText(textoBoton);
        }
    }

    @FXML
    private void copiarEnlaceTunel() {
        if (enlaceTunel == null || enlaceTunel.isBlank()) {
            mostrarTooltipTunel("Todavía no hay un enlace activo.");
            return;
        }
        ClipboardContent contenido = new ClipboardContent();
        contenido.putString(enlaceTunel);
        Clipboard.getSystemClipboard().setContent(contenido);
        mostrarTooltipTunel("Enlace copiado al portapapeles.");
    }

    private void actualizarTooltipTunel() {
        if (btnEnlaceTunel == null || btnEnlaceTunel.getTooltip() == null) {
            return;
        }
        btnEnlaceTunel.getTooltip().setText(enlaceTunel != null && !enlaceTunel.isBlank()
                ? "Copiar enlace del túnel."
                : estadoTunel);
    }

    private void mostrarTooltipTunel(String mensaje) {
        if (btnEnlaceTunel == null || btnEnlaceTunel.getScene() == null
                || btnEnlaceTunel.getTooltip() == null) {
            return;
        }
        if (pausaTooltipTunel != null) {
            pausaTooltipTunel.stop();
        }
        Tooltip tooltip = btnEnlaceTunel.getTooltip();
        tooltip.setText(mensaje);
        Bounds bordes = btnEnlaceTunel.localToScreen(btnEnlaceTunel.getBoundsInLocal());
        if (bordes != null) {
            tooltip.show(btnEnlaceTunel, bordes.getMinX(), bordes.getMaxY() + 4);
        }
        pausaTooltipTunel = new PauseTransition(Duration.seconds(2));
        pausaTooltipTunel.setOnFinished(evento -> {
            tooltip.hide();
            actualizarTooltipTunel();
        });
        pausaTooltipTunel.play();
    }

    private boolean sesionAdmin() {
        try {
            SesionActual.requerir(RolUsuario.ADMIN);
            return true;
        } catch (SecurityException e) {
            mensaje(e.getMessage());
            return false;
        }
    }

    private void verificarSesion() {
        if (SesionActual.getUsuario() != null && !SesionActual.activa()) {
            Navegacion.sesionExpirada(Navegacion.stageDe(lblEstadoUnidades));
        }
    }

    // ------------------------------------------------------------------ unidades

    @FXML
    private void verUnidades() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            todasLasUnidades = unidadDAO.listar();
            txtFiltroUnidad.clear();
            btnUnidadTodos.setSelected(true);
            aplicarFiltroUnidades();
        } catch (PersistenciaException e) {
            error("No se pudieron consultar las unidades", e);
        }
    }

    private void recargarUnidades() {
        try {
            todasLasUnidades = unidadDAO.listar();
            aplicarFiltroUnidades();
        } catch (PersistenciaException e) {
            error("No se pudieron consultar las unidades", e);
        }
    }

    private void aplicarFiltroUnidades() {
        if (txtFiltroUnidad == null) {
            return;
        }
        String texto = txtFiltroUnidad.getText();
        List<Unidad> visibles = todasLasUnidades.stream()
                .filter(unidad -> coincideUnidad(unidad, texto))
                .filter(unidad -> estadoUnidad(unidad, filtroEstadoUnidad))
                .toList();
        tablaUnidades.setItems(FXCollections.observableArrayList(visibles));
        tablaUnidades.refresh();
        if (lblEstadoUnidades != null) {
            lblEstadoUnidades.setText("Mostrando " + visibles.size() + " de "
                    + todasLasUnidades.size() + " unidad(es).");
        }
    }

    private boolean coincideUnidad(Unidad unidad, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String buscado = texto.trim().toLowerCase(Locale.ROOT);
        String capacidad = unidad.getCapacidadToneladas() == null
                ? "" : unidad.getCapacidadToneladas().toPlainString();
        return contiene(unidad.getNumeroUnidad(), buscado)
                || contiene(unidad.getPlacas(), buscado)
                || contiene(unidad.getModelo(), buscado)
                || capacidad.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private boolean estadoUnidad(Unidad unidad, String filtro) {
        if (filtro == null || "Todos".equals(filtro)) {
            return true;
        }
        String estado = unidad.getEstado() == null ? "" : unidad.getEstado().getNombre();
        return switch (filtro) {
            case "Disponible" -> "DISPONIBLE".equals(estado);
            case "En ruta" -> "EN_RUTA".equals(estado);
            case "Mantenimiento" -> "MANTENIMIENTO".equals(estado);
            default -> true;
        };
    }

    @FXML
    private void guardarUnidad() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            Unidad unidad = construirUnidad(new Unidad());
            unidadDAO.guardar(unidad);
            limpiarUnidad();
            recargarUnidades();
            mensaje("Unidad " + unidad.getNumeroUnidad() + " guardada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo guardar la unidad", e);
        }
    }

    @FXML
    private void actualizarUnidad() {
        if (!sesionAdmin()) {
            return;
        }
        Unidad seleccionada = tablaUnidades.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una unidad de la tabla para actualizarla.");
            return;
        }
        try {
            String estadoAnterior = seleccionada.getEstado().getNombre();
            Unidad unidad = construirUnidad(new Unidad());
            unidad.setId(seleccionada.getId());
            if (flotaDAO.unidadTieneViajeActivo(unidad.getId())
                    && !estadoAnterior.equals(unidad.getEstado().getNombre())) {
                throw new IllegalArgumentException(
                        "No se puede cambiar el estado de una unidad con viajes activos. "
                                + "Finalice o cancele esos viajes primero.");
            }
            unidadDAO.actualizar(unidad);
            limpiarUnidad();
            recargarUnidades();
            mensaje("Unidad " + unidad.getNumeroUnidad() + " actualizada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo actualizar la unidad", e);
        }
    }

    @FXML
    private void eliminarUnidad() {
        if (!sesionAdmin()) {
            return;
        }
        Unidad seleccionada = tablaUnidades.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una unidad de la tabla para eliminarla.");
            return;
        }
        if (!confirmar("¿Eliminar la unidad " + seleccionada.getNumeroUnidad() + "?")) {
            return;
        }
        try {
            unidadDAO.eliminar(seleccionada.getId());
            limpiarUnidad();
            recargarUnidades();
            mensaje("Unidad eliminada.");
        } catch (PersistenciaException e) {
            error("No se pudo eliminar la unidad (puede estar asignada a un viaje)", e);
        }
    }

    @FXML
    private void limpiarUnidad() {
        tablaUnidades.getSelectionModel().clearSelection();
        txtNumeroUnidad.clear();
        txtPlacas.clear();
        txtModelo.clear();
        txtCapacidad.clear();
        cmbEstadoUnidad.getSelectionModel().clearSelection();
    }

    private Unidad construirUnidad(Unidad unidad) {
        String numero = requerido(txtNumeroUnidad, "El número de unidad", 20);
        String placas = requerido(txtPlacas, "Las placas", 15);
        String modelo = requerido(txtModelo, "El modelo", 80);

        BigDecimal capacidad = null;
        String textoCapacidad = txtCapacidad.getText() == null ? "" : txtCapacidad.getText().trim();
        if (!textoCapacidad.isEmpty()) {
            capacidad = decimal(textoCapacidad, "La capacidad");
            if (capacidad.signum() <= 0) {
                throw new IllegalArgumentException("La capacidad debe ser mayor que 0.");
            }
        }
        if (cmbEstadoUnidad.getValue() == null) {
            throw new IllegalArgumentException("Seleccione el estado de la unidad.");
        }

        unidad.setNumeroUnidad(numero);
        unidad.setPlacas(placas);
        unidad.setModelo(modelo);
        unidad.setCapacidadToneladas(capacidad);
        unidad.setEstado(cmbEstadoUnidad.getValue());
        return unidad;
    }

    private void llenarUnidad(Unidad unidad) {
        txtNumeroUnidad.setText(unidad.getNumeroUnidad());
        txtPlacas.setText(unidad.getPlacas());
        txtModelo.setText(unidad.getModelo());
        txtCapacidad.setText(unidad.getCapacidadToneladas() == null ? "" : unidad.getCapacidadToneladas().toPlainString());
        cmbEstadoUnidad.setValue(unidad.getEstado());
    }

    // ------------------------------------------------------------------ operadores

    @FXML
    private void verOperadores() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            todosLosOperadores = operadorDAO.listar();
            txtFiltroOperador.clear();
            btnOperadorTodos.setSelected(true);
            aplicarFiltroOperadores();
        } catch (PersistenciaException e) {
            error("No se pudieron consultar los operadores", e);
        }
    }

    private void recargarOperadores() {
        try {
            todosLosOperadores = operadorDAO.listar();
            aplicarFiltroOperadores();
        } catch (PersistenciaException e) {
            error("No se pudieron consultar los operadores", e);
        }
    }

    private void aplicarFiltroOperadores() {
        if (txtFiltroOperador == null) {
            return;
        }
        String texto = txtFiltroOperador.getText();
        List<Operador> visibles = todosLosOperadores.stream()
                .filter(operador -> coincideOperador(operador, texto))
                .filter(operador -> estadoOperador(operador, filtroEstadoOperador))
                .toList();
        tablaOperadores.setItems(FXCollections.observableArrayList(visibles));
        tablaOperadores.refresh();
        if (lblEstadoOperadores != null) {
            lblEstadoOperadores.setText("Mostrando " + visibles.size() + " de "
                    + todosLosOperadores.size() + " operador(es).");
        }
    }

    private boolean coincideOperador(Operador operador, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String buscado = texto.trim().toLowerCase(Locale.ROOT);
        return contiene(operador.getNombre(), buscado)
                || contiene(operador.getCedula(), buscado)
                || contiene(operador.getTelefono(), buscado);
    }

    private boolean estadoOperador(Operador operador, String filtro) {
        if (filtro == null || "Todos".equals(filtro)) {
            return true;
        }
        String estado = operador.getEstado() == null ? "" : operador.getEstado().getNombre();
        return switch (filtro) {
            case "Disponible" -> "DISPONIBLE".equals(estado);
            case "En ruta" -> "EN_RUTA".equals(estado);
            case "Descanso" -> "DESCANSO".equals(estado);
            case "Inactivo" -> "INACTIVO".equals(estado);
            default -> true;
        };
    }

    private void mostrarCredencialesOperador(String usuario, String temporal) {
        TextArea contenido = new TextArea("Usuario: " + usuario
                + "\nContraseña temporal: " + temporal
                + "\nEl administrador principal puede restablecerla desde el panel administrativo.");
        contenido.setEditable(false);
        contenido.setWrapText(true);
        contenido.setPrefRowCount(4);
        ButtonType copiar = new ButtonType("Copiar", ButtonBar.ButtonData.OK_DONE);
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, "", copiar, ButtonType.CLOSE);
        alerta.setTitle("Cuenta del operador creada");
        alerta.setHeaderText("Cuenta del operador creada");
        alerta.getDialogPane().setContent(contenido);
        alerta.showAndWait().ifPresent(respuesta -> {
            if (respuesta == copiar) {
                ClipboardContent portapapeles = new ClipboardContent();
                portapapeles.putString("Usuario: " + usuario + "\nContraseña temporal: " + temporal);
                Clipboard.getSystemClipboard().setContent(portapapeles);
            }
        });
    }

    @FXML
    private void guardarOperador() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            Operador operador = construirOperador(new Operador());
            if (operadorDAO.existeCedula(operador.getCedula(), null)) {
                throw new IllegalArgumentException("Ya existe un operador con la cédula " + operador.getCedula() + ".");
            }
            operadorDAO.guardar(operador);
            String temporal = null;
            try {
                temporal = usuarioDAO.crearCuentaOperador(operador);
            } catch (RuntimeException e) {
                advertencia("El operador se guardó, pero no se pudo crear su cuenta: " + e.getMessage()
                        + "\nPuede crearla desde el panel administrativo.");
            }
            limpiarOperador();
            recargarOperadores();
            mensaje("Operador " + operador.getNombre() + " guardado.");
            if (temporal != null) {
                mostrarCredencialesOperador(operador.getCedula(), temporal);
            }
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo guardar el operador", e);
        }
    }

    @FXML
    private void actualizarOperador() {
        if (!sesionAdmin()) {
            return;
        }
        Operador seleccionado = tablaOperadores.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            advertencia("Seleccione un operador de la tabla para actualizarlo.");
            return;
        }
        try {
            String estadoAnterior = seleccionado.getEstado().getNombre();
            Operador operador = construirOperador(new Operador());
            operador.setId(seleccionado.getId());
            if (operadorDAO.existeCedula(operador.getCedula(), operador.getId())) {
                throw new IllegalArgumentException("Ya existe otro operador con la cédula "
                        + operador.getCedula() + ".");
            }
            if (flotaDAO.operadorTieneViajeActivo(operador.getId())
                    && !estadoAnterior.equals(operador.getEstado().getNombre())) {
                throw new IllegalArgumentException(
                        "No se puede cambiar el estado de un operador con viajes activos. "
                                + "Finalice o cancele esos viajes primero.");
            }
            Usuario cuenta = usuarioDAO.buscarPorOperador(operador.getId());
            if (cuenta != null && !operador.getCedula().equals(cuenta.getUsername())
                    && usuarioDAO.existeUsernameExcepto(operador.getCedula(), cuenta.getId())) {
                throw new IllegalArgumentException("Ya existe una cuenta con el usuario "
                        + operador.getCedula() + ".");
            }
            operadorDAO.actualizar(operador);
            if (cuenta != null && !operador.getCedula().equals(cuenta.getUsername())) {
                cuenta.setUsername(operador.getCedula());
                usuarioDAO.actualizar(cuenta);
            }
            limpiarOperador();
            recargarOperadores();
            mensaje("Operador " + operador.getNombre() + " actualizado.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo actualizar el operador", e);
        }
    }

    @FXML
    private void eliminarOperador() {
        if (!sesionAdmin()) {
            return;
        }
        Operador seleccionado = tablaOperadores.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            advertencia("Seleccione un operador de la tabla para eliminarlo.");
            return;
        }
        if (!confirmar("¿Eliminar al operador " + seleccionado.getNombre() + "?"
                + (usuarioDAO.existeCuentaOperador(seleccionado.getId())
                ? " También se elimina su cuenta de acceso." : ""))) {
            return;
        }
        try {
            if (flotaDAO.operadorTieneViajes(seleccionado.getId())) {
                informacion("Operación no permitida",
                        "El operador tiene viajes registrados y no se puede eliminar.");
                return;
            }
            operadorDAO.eliminarConCuenta(seleccionado.getId());
            limpiarOperador();
            recargarOperadores();
            mensaje("Operador eliminado.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo eliminar el operador", e);
        }
    }

    @FXML
    private void limpiarOperador() {
        tablaOperadores.getSelectionModel().clearSelection();
        txtCedula.clear();
        txtNombreOperador.clear();
        txtTelefonoOperador.clear();
        txtHorasMax.clear();
        txtHorasMin.clear();
        cmbEstadoOperador.getSelectionModel().clearSelection();
    }

    private Operador construirOperador(Operador operador) {
        String cedula = requerido(txtCedula, "La cédula", 20);
        if (cedula.contains(" ")) {
            throw new IllegalArgumentException("La cédula no puede contener espacios.");
        }
        String nombre = requerido(txtNombreOperador, "El nombre", 150);
        BigDecimal horasMax = decimal(requerido(txtHorasMax, "Las horas máximas de conducción", 5), "Las horas máximas");
        BigDecimal horasMin = decimal(requerido(txtHorasMin, "Las horas mínimas de descanso", 5), "Las horas mínimas");

        if (horasMax.signum() <= 0 || horasMax.compareTo(new BigDecimal("24")) > 0) {
            throw new IllegalArgumentException("Las horas máximas deben ser mayores a 0 y máximo 24.");
        }
        if (horasMin.signum() <= 0 || horasMin.compareTo(new BigDecimal("24")) > 0) {
            throw new IllegalArgumentException("Las horas mínimas deben ser mayores a 0 y máximo 24.");
        }
        if (cmbEstadoOperador.getValue() == null) {
            throw new IllegalArgumentException("Seleccione el estado del operador.");
        }

        String telefono = txtTelefonoOperador.getText() == null ? "" : txtTelefonoOperador.getText().trim();
        operador.setCedula(cedula);
        operador.setNombre(nombre);
        operador.setTelefono(telefono.isEmpty() ? null : telefono);
        operador.setHorasMaxConduccion(horasMax);
        operador.setHorasMinDescanso(horasMin);
        operador.setEstado(cmbEstadoOperador.getValue());
        return operador;
    }

    private void llenarOperador(Operador operador) {
        txtCedula.setText(operador.getCedula());
        txtNombreOperador.setText(operador.getNombre());
        txtTelefonoOperador.setText(operador.getTelefono() == null ? "" : operador.getTelefono());
        txtHorasMax.setText(operador.getHorasMaxConduccion().toPlainString());
        txtHorasMin.setText(operador.getHorasMinDescanso().toPlainString());
        cmbEstadoOperador.setValue(operador.getEstado());
    }

    // ------------------------------------------------------------------ clientes

    @FXML
    private void verClientes() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            todosLosClientes = clienteDAO.listar();
            txtFiltroCliente.clear();
            btnClienteTodos.setSelected(true);
            aplicarFiltroClientes();
        } catch (PersistenciaException e) {
            error("No se pudieron consultar los clientes", e);
        }
    }

    private void recargarClientes() {
        try {
            todosLosClientes = clienteDAO.listar();
            aplicarFiltroClientes();
        } catch (PersistenciaException e) {
            error("No se pudieron consultar los clientes", e);
        }
    }

    private void aplicarFiltroClientes() {
        if (txtFiltroCliente == null) {
            return;
        }
        String texto = txtFiltroCliente.getText();
        List<Cliente> visibles = todosLosClientes.stream()
                .filter(cliente -> coincideCliente(cliente, texto))
                .filter(cliente -> estadoCliente(cliente, filtroEstadoCliente))
                .toList();
        tablaClientes.setItems(FXCollections.observableArrayList(visibles));
        tablaClientes.refresh();
        if (lblEstadoClientes != null) {
            lblEstadoClientes.setText("Mostrando " + visibles.size() + " de "
                    + todosLosClientes.size() + " cliente(s).");
        }
    }

    private boolean coincideCliente(Cliente cliente, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String buscado = texto.trim().toLowerCase(Locale.ROOT);
        return contiene(cliente.getNombreEmpresa(), buscado)
                || contiene(cliente.getContacto(), buscado)
                || contiene(cliente.getTelefono(), buscado)
                || contiene(cliente.getEmail(), buscado);
    }

    private boolean estadoCliente(Cliente cliente, String filtro) {
        if (filtro == null || "Todos".equals(filtro)) {
            return true;
        }
        boolean activo = Boolean.TRUE.equals(cliente.getActivo());
        return "Activos".equals(filtro) == activo;
    }

    @FXML
    private void guardarCliente() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            Cliente cliente = construirCliente(new Cliente());
            clienteDAO.guardar(cliente);
            limpiarCliente();
            recargarClientes();
            mensaje("Cliente " + cliente.getNombreEmpresa() + " guardado.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo guardar el cliente (¿nombre repetido?)", e);
        }
    }

    @FXML
    private void actualizarCliente() {
        if (!sesionAdmin()) {
            return;
        }
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            advertencia("Seleccione un cliente de la tabla para actualizarlo.");
            return;
        }
        try {
            Cliente cliente = construirCliente(new Cliente());
            cliente.setId(seleccionado.getId());
            clienteDAO.actualizar(cliente);
            limpiarCliente();
            recargarClientes();
            mensaje("Cliente " + cliente.getNombreEmpresa() + " actualizado.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo actualizar el cliente", e);
        }
    }

    @FXML
    private void eliminarCliente() {
        if (!sesionAdmin()) {
            return;
        }
        Cliente seleccionado = tablaClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            advertencia("Seleccione un cliente de la tabla para eliminarlo.");
            return;
        }
        if (!confirmar("¿Eliminar al cliente " + seleccionado.getNombreEmpresa() + "?")) {
            return;
        }
        try {
            clienteDAO.eliminar(seleccionado.getId());
            limpiarCliente();
            recargarClientes();
            mensaje("Cliente eliminado.");
        } catch (PersistenciaException e) {
            error("No se pudo eliminar el cliente (puede tener cargas asociadas)", e);
        }
    }

    @FXML
    private void limpiarCliente() {
        tablaClientes.getSelectionModel().clearSelection();
        txtNombreCliente.clear();
        txtContactoCliente.clear();
        txtTelefonoCliente.clear();
        txtEmailCliente.clear();
        txtDireccionCliente.clear();
        chkActivoCliente.setSelected(true);
    }

    private Cliente construirCliente(Cliente cliente) {
        cliente.setNombreEmpresa(requerido(txtNombreCliente, "El nombre de la empresa", 150));
        cliente.setContacto(opcional(txtContactoCliente, "El contacto", 120));
        cliente.setTelefono(opcional(txtTelefonoCliente, "El teléfono", 30));
        String email = opcional(txtEmailCliente, "El email", 120);
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("El email no tiene un formato válido.");
        }
        cliente.setEmail(email);
        cliente.setDireccion(opcional(txtDireccionCliente, "La dirección", 200));
        cliente.setActivo(chkActivoCliente.isSelected());
        return cliente;
    }

    private void llenarCliente(Cliente cliente) {
        txtNombreCliente.setText(cliente.getNombreEmpresa());
        txtContactoCliente.setText(cliente.getContacto() == null ? "" : cliente.getContacto());
        txtTelefonoCliente.setText(cliente.getTelefono() == null ? "" : cliente.getTelefono());
        txtEmailCliente.setText(cliente.getEmail() == null ? "" : cliente.getEmail());
        txtDireccionCliente.setText(cliente.getDireccion() == null ? "" : cliente.getDireccion());
        chkActivoCliente.setSelected(Boolean.TRUE.equals(cliente.getActivo()));
    }

    // ------------------------------------------------------------------ apoyo

    @FXML
    private void abrirPanelAdmin() {
        if (!sesionAdmin()) {
            return;
        }
        if (!Boolean.TRUE.equals(SesionActual.getUsuario().getEsPrincipal())) {
            advertencia("Solo el administrador principal puede abrir el panel administrativo.");
            return;
        }
        abrirVista("admin-view.fxml", "Panel administrativo", 1040, 740);
    }

    @FXML
    private void abrirReporte() {
        if (!sesionAdmin()) {
            return;
        }
        Usuario usuario = SesionActual.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getEsPrincipal())
                && !Boolean.TRUE.equals(usuario.getPuedeVerReportes())) {
            advertencia("Su cuenta no tiene permiso para ver el reporte financiero.");
            return;
        }
        abrirVista("reporte-view.fxml", "Reportes financieros", 1120, 820);
    }

    @FXML
    private void abrirCargas() {
        if (!sesionAdmin()) {
            return;
        }
        abrirVista("cargas-view.fxml", "Cargas");
    }

    @FXML
    private void abrirViajes() {
        if (!sesionAdmin()) {
            return;
        }
        abrirVista("viajes-view.fxml", "Viajes");
    }

    @FXML
    private void abrirMonitor() {
        if (!sesionAdmin()) {
            return;
        }
        abrirVista("monitor-view.fxml", "Monitor de flota");
    }

    private void abrirVista(String archivo, String titulo) {
        abrirVista(archivo, titulo, 980, 700);
    }

    private void abrirVista(String archivo, String titulo, double ancho, double alto) {
        if (Navegacion.enfocarVentana("FleetRoute Pro — " + titulo)) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edu/ni/uam/proyecto_final_pae/" + archivo));
            Parent root = loader.load();
            Tooltips.instalar(root);
            Stage stage = new Stage();
            stage.setTitle("FleetRoute Pro — " + titulo);
            stage.setScene(new Scene(root, ancho, alto));
            stage.setMinWidth(Math.min(ancho, 900));
            stage.setMinHeight(Math.min(alto, 620));
            Navegacion.registrar(stage);
            stage.show();
        } catch (Exception e) {
            advertencia("No se pudo abrir " + titulo + ": " + e.getMessage());
        }
    }

    private String requerido(TextField campo, String nombre, int maximo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Complete " + nombre + ".");
        }
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(nombre + " no puede pasar de " + maximo + " caracteres.");
        }
        return valor;
    }

    private boolean vacio(TextField campo) {
        return campo.getText() == null || campo.getText().isBlank();
    }

    private String opcional(TextField campo, String nombre, int maximo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(nombre + " no puede pasar de " + maximo + " caracteres.");
        }
        return valor.isEmpty() ? null : valor;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private static String horas(BigDecimal valor) {
        return valor == null ? "—" : valor.stripTrailingZeros().toPlainString() + " h";
    }

    private BigDecimal decimal(String valor, String nombre) {
        try {
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nombre + " debe ser un número, por ejemplo 8.50.");
        }
    }

    private boolean confirmar(String mensaje) {
        ButtonType eliminar = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, eliminar, cancelar);
        alerta.setHeaderText(null);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == eliminar;
    }

    private void advertencia(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK);
        alerta.setHeaderText("Datos incompletos o inválidos");
        alerta.showAndWait();
    }

    private void informacion(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }

    private static boolean contiene(String valor, String buscado) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private void error(String titulo, PersistenciaException e) {
        mensaje(titulo);
        Alert alerta = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}
