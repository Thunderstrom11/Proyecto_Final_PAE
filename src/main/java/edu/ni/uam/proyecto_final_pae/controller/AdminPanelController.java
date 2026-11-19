package edu.ni.uam.proyecto_final_pae.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ni.uam.proyecto_final_pae.config.DivisionesMundo;
import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.AlertaOperadorDAO;
import edu.ni.uam.proyecto_final_pae.dao.OperadorDAO;
import edu.ni.uam.proyecto_final_pae.dao.PaisDAO;
import edu.ni.uam.proyecto_final_pae.dao.RutaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UbicacionDAO;
import edu.ni.uam.proyecto_final_pae.dao.UsuarioDAO;
import edu.ni.uam.proyecto_final_pae.model.AlertaOperador;
import edu.ni.uam.proyecto_final_pae.model.Operador;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Ruta;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import edu.ni.uam.proyecto_final_pae.util.Listas;
import edu.ni.uam.proyecto_final_pae.util.ComboBuscador;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Window;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Panel administrativo exclusivo del administrador principal (N.º 5).
 *
 * <p>Gestiona cuentas de administradores y operadores, el permiso de reportes financieros,
 * las ubicaciones de origen/destino con sus coordenadas y las rutas con su tiempo estimado.</p>
 */
public class AdminPanelController {

    private static final String USER_AGENT =
            "FleetRouteProAcademic/1.0 (contacto: estudiante-uam)";

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final OperadorDAO operadorDAO = new OperadorDAO();
    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();
    private final RutaDAO rutaDAO = new RutaDAO();
    private final PaisDAO paisDAO = new PaisDAO();
    private final AlertaOperadorDAO alertaOperadorDAO = new AlertaOperadorDAO();
    private static final DateTimeFormatter FORMATO_ALERTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ObjectMapper json = new ObjectMapper();

    // cuentas
    @FXML private TableView<Usuario> tablaCuentas;
    @FXML private TableColumn<Usuario, String> colCuentaUsuario;
    @FXML private TableColumn<Usuario, String> colCuentaRol;
    @FXML private TableColumn<Usuario, String> colCuentaOperador;
    @FXML private TableColumn<Usuario, String> colCuentaActivo;
    @FXML private TableColumn<Usuario, String> colCuentaReportes;
    @FXML private TableColumn<Usuario, String> colCuentaPrincipal;
    @FXML private TextField txtBuscarCuenta;
    @FXML private Button btnLimpiarBusquedaCuenta;
    @FXML private ToggleButton btnCuentaTodos;
    @FXML private ToggleButton btnCuentaAdmins;
    @FXML private ToggleButton btnCuentaOperadores;
    @FXML private ToggleButton btnCuentaEstadoTodos;
    @FXML private ToggleButton btnCuentaActivos;
    @FXML private ToggleButton btnCuentaInactivos;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<RolUsuario> cmbRol;
    @FXML private VBox panelOperadorAsociado;
    @FXML private ComboBox<Operador> cmbOperadorCuenta;
    @FXML private CheckBox chkCuentaActiva;
    @FXML private CheckBox chkCuentaReportes;
    @FXML private Button btnGuardarCuenta;
    @FXML private Button btnActualizarCuenta;
    @FXML private Button btnRestablecerCuenta;

    // ubicaciones
    @FXML private TableView<Ubicacion> tablaUbicaciones;
    @FXML private TableColumn<Ubicacion, String> colUbicacionNombre;
    @FXML private TableColumn<Ubicacion, String> colUbicacionDepartamento;
    @FXML private TableColumn<Ubicacion, String> colUbicacionPais;
    @FXML private TableColumn<Ubicacion, String> colUbicacionLatitud;
    @FXML private TableColumn<Ubicacion, String> colUbicacionLongitud;
    @FXML private TextField txtNombreUbicacion;
    @FXML private TextField txtDepartamentoUbicacion;
    @FXML private ComboBox<String> cmbPaisUbicacion;
    @FXML private TextField txtLatitud;
    @FXML private TextField txtLongitud;
    @FXML private StackPane contenedorMapaAdmin;
    @FXML private Button btnPaises;
    @FXML private Button btnGuardarUbicacion;
    @FXML private Button btnActualizarUbicacion;
    @FXML private Button btnEliminarUbicacion;
    private WebView mapa;
    private boolean mapaListo;

    // rutas
    @FXML private TableView<Ruta> tablaRutas;
    @FXML private TextField txtBuscarUbicacion;
    @FXML private TextField txtBuscarRutaAdmin;
    @FXML private TableColumn<Ruta, String> colRutaOrigen;
    @FXML private TableColumn<Ruta, String> colRutaDestino;
    @FXML private TableColumn<Ruta, String> colRutaHoras;
    @FXML private TableColumn<Ruta, String> colRutaPrecio;
    @FXML private ComboBox<Ubicacion> cmbOrigenRuta;
    @FXML private ComboBox<Ubicacion> cmbDestinoRuta;
    @FXML private TextField txtHorasRuta;
    @FXML private TextField txtPrecioRuta;
    @FXML private Button btnGuardarRuta;
    @FXML private Button btnActualizarRuta;
    @FXML private Button btnEliminarRuta;

    // alertas
    @FXML private TableView<AlertaOperador> tablaAlertas;
    @FXML private TableColumn<AlertaOperador, String> colAlertaFecha;
    @FXML private TableColumn<AlertaOperador, String> colAlertaOperador;
    @FXML private TableColumn<AlertaOperador, String> colAlertaViaje;
    @FXML private TableColumn<AlertaOperador, String> colAlertaUnidad;
    @FXML private TableColumn<AlertaOperador, String> colAlertaUbicacion;
    @FXML private TableColumn<AlertaOperador, String> colAlertaMensaje;
    @FXML private TableColumn<AlertaOperador, String> colAlertaEstado;
    @FXML private Button btnMarcarAlertaAtendida;

    @FXML private Label lblEstado;
    @FXML private Label lblAdministrador;

    private List<Usuario> todasLasCuentas = List.of();
    private List<Ubicacion> ubicaciones = List.of();
    private List<Ruta> todasLasRutas = List.of();
    private String filtroRolCuenta = "Todos";
    private String filtroEstadoCuenta = "Todos";
    private List<String> paisesMundo = List.of();
    private List<String> paisesActivos = List.of();

    @FXML
    private void initialize() {
        Usuario admin;
        try {
            admin = SesionActual.requerir(RolUsuario.ADMIN);
        } catch (SecurityException e) {
            lblEstado.setText(e.getMessage());
            deshabilitarPanel();
            return;
        }
        if (!Boolean.TRUE.equals(admin.getEsPrincipal())) {
            lblEstado.setText("Solo el administrador principal puede usar este panel.");
            deshabilitarPanel();
            return;
        }
        lblAdministrador.setText("Sesión: " + admin.getUsername());

        configurarCuentas();
        configurarUbicaciones();
        configurarRutas();
        configurarAlertas();
        txtBuscarUbicacion.textProperty().addListener((o, a, b) -> aplicarFiltroUbicaciones());
        txtBuscarRutaAdmin.textProperty().addListener((o, a, b) -> aplicarFiltroRutas());
        crearMapa();

        try {
            ComboBuscador.configurar(cmbOperadorCuenta, operadorDAO.listar());
            paisesMundo = Listas.paises();
            cargarPaisesActivos();
            ComboBuscador.configurar(cmbPaisUbicacion, paisesActivos);
            cargarCuentas();
            cargarUbicaciones();
            cargarRutas();
            cargarAlertas();
        } catch (PersistenciaException e) {
            error("No se pudo conectar con la base de datos", e);
        }
    }

    private void deshabilitarPanel() {
        Platform.runLater(() -> {
            if (lblEstado.getScene() != null) {
                lblEstado.getScene().getRoot().setDisable(true);
            }
        });
    }

    // ------------------------------------------------------------------ cuentas

    private void configurarCuentas() {
        colCuentaUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colCuentaRol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRol() == RolUsuario.ADMIN ? "Administrador" : "Operador"));
        colCuentaOperador.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOperador() == null ? "—" : c.getValue().getOperador().getNombre()));
        colCuentaActivo.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getActivo()) ? "Sí" : "No"));
        colCuentaReportes.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getPuedeVerReportes()) ? "Sí" : "No"));
        colCuentaPrincipal.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getEsPrincipal()) ? "Principal" : "—"));

        cmbRol.setItems(FXCollections.observableArrayList(RolUsuario.ADMIN, RolUsuario.OPERADOR));
        cmbRol.setValue(RolUsuario.ADMIN);
        panelOperadorAsociado.visibleProperty().bind(cmbRol.valueProperty().isEqualTo(RolUsuario.OPERADOR));
        panelOperadorAsociado.managedProperty().bind(panelOperadorAsociado.visibleProperty());
        chkCuentaReportes.disableProperty().bind(cmbRol.valueProperty().isNotEqualTo(RolUsuario.ADMIN));

        btnCuentaTodos.setUserData("Todos");
        btnCuentaAdmins.setUserData("Administrador");
        btnCuentaOperadores.setUserData("Operador");
        btnCuentaTodos.getToggleGroup().selectedToggleProperty().addListener((o, anterior, actual) -> {
            filtroRolCuenta = actual == null ? "Todos" : String.valueOf(actual.getUserData());
            aplicarFiltroCuentas();
        });
        btnCuentaEstadoTodos.setUserData("Todos");
        btnCuentaActivos.setUserData("Activos");
        btnCuentaInactivos.setUserData("Inactivos");
        btnCuentaEstadoTodos.getToggleGroup().selectedToggleProperty().addListener((o, anterior, actual) -> {
            filtroEstadoCuenta = actual == null ? "Todos" : String.valueOf(actual.getUserData());
            aplicarFiltroCuentas();
        });

        txtBuscarCuenta.textProperty().addListener((o, a, b) -> aplicarFiltroCuentas());

        tablaCuentas.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                llenarCuenta(b);
            }
        });

        BooleanBinding incompleta = Bindings.createBooleanBinding(
                () -> vacio(txtUsername) || vacio(txtPassword)
                        || cmbRol.getValue() == null
                        || (cmbRol.getValue() == RolUsuario.OPERADOR && cmbOperadorCuenta.getValue() == null),
                txtUsername.textProperty(), txtPassword.textProperty(),
                cmbRol.valueProperty(), cmbOperadorCuenta.valueProperty());
        BooleanBinding sinSeleccion = tablaCuentas.getSelectionModel().selectedItemProperty().isNull();

        btnGuardarCuenta.disableProperty().bind(incompleta);
        btnActualizarCuenta.disableProperty().bind(sinSeleccion.or(
                Bindings.createBooleanBinding(() -> vacio(txtUsername),
                        txtUsername.textProperty())));
        btnRestablecerCuenta.disableProperty().bind(sinSeleccion.or(
                Bindings.createBooleanBinding(() -> vacio(txtPassword),
                        txtPassword.textProperty())));
    }

    private void llenarCuenta(Usuario usuario) {
        txtUsername.setText(usuario.getUsername());
        txtPassword.clear();
        cmbRol.setValue(usuario.getRol());
        cmbOperadorCuenta.setValue(usuario.getOperador());
        chkCuentaActiva.setSelected(Boolean.TRUE.equals(usuario.getActivo()));
        chkCuentaReportes.setSelected(Boolean.TRUE.equals(usuario.getPuedeVerReportes()));
    }

    @FXML
    private void guardarCuenta() {
        if (!sesionPrincipal()) {
            return;
        }
        try {
            String username = requerido(txtUsername, "El nombre de usuario", 50);
            String password = requerido(txtPassword, "La contraseña", 100);
            if (password.length() < 8) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
            }
            if (usuarioDAO.existeUsername(username)) {
                throw new IllegalArgumentException("Ya existe una cuenta con el usuario " + username + ".");
            }
            RolUsuario rol = cmbRol.getValue();
            if (rol == null) {
                throw new IllegalArgumentException("Seleccione el rol de la cuenta.");
            }
            Operador operador = null;
            if (rol == RolUsuario.OPERADOR) {
                operador = cmbOperadorCuenta.getValue();
                if (operador == null) {
                    throw new IllegalArgumentException("Seleccione el operador de la cuenta.");
                }
                if (usuarioDAO.existeCuentaOperador(operador.getId())) {
                    throw new IllegalArgumentException("Ese operador ya tiene una cuenta de acceso.");
                }
            }
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt()));
            usuario.setRol(rol);
            usuario.setOperador(operador);
            usuario.setActivo(chkCuentaActiva.isSelected());
            usuario.setEsPrincipal(false);
            usuario.setPuedeVerReportes(rol == RolUsuario.ADMIN && chkCuentaReportes.isSelected());
            usuarioDAO.guardar(usuario);
            limpiarCuenta();
            cargarCuentas();
            lblEstado.setText("Cuenta " + username + " creada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo crear la cuenta", e);
        }
    }

    @FXML
    private void actualizarCuenta() {
        if (!sesionPrincipal()) {
            return;
        }
        Usuario seleccionada = tablaCuentas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una cuenta de la tabla.");
            return;
        }
        try {
            String username = requerido(txtUsername, "El nombre de usuario", 50);
            if (usuarioDAO.existeUsernameExcepto(username, seleccionada.getId())) {
                throw new IllegalArgumentException("Ya existe otra cuenta con el usuario " + username + ".");
            }
            RolUsuario rol = cmbRol.getValue();
            if (rol == null) {
                throw new IllegalArgumentException("Seleccione el rol de la cuenta.");
            }
            boolean principal = Boolean.TRUE.equals(seleccionada.getEsPrincipal());
            if (principal && rol != RolUsuario.ADMIN) {
                throw new IllegalArgumentException("No se puede quitar el rol de administrador a la cuenta principal.");
            }
            if (principal && !chkCuentaActiva.isSelected()) {
                throw new IllegalArgumentException("No se puede desactivar la cuenta principal.");
            }
            Operador operador = null;
            if (rol == RolUsuario.OPERADOR) {
                operador = cmbOperadorCuenta.getValue();
                if (operador == null) {
                    throw new IllegalArgumentException("Seleccione el operador de la cuenta.");
                }
                Usuario duena = usuarioDAO.buscarPorOperador(operador.getId());
                if (duena != null && !duena.getId().equals(seleccionada.getId())) {
                    throw new IllegalArgumentException("Ese operador ya tiene una cuenta de acceso.");
                }
            }
            seleccionada.setUsername(username);
            seleccionada.setRol(rol);
            seleccionada.setOperador(operador);
            seleccionada.setActivo(chkCuentaActiva.isSelected());
            seleccionada.setPuedeVerReportes(rol == RolUsuario.ADMIN && chkCuentaReportes.isSelected());
            usuarioDAO.actualizar(seleccionada);
            limpiarCuenta();
            cargarCuentas();
            lblEstado.setText("Cuenta " + username + " actualizada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo actualizar la cuenta", e);
        }
    }

    @FXML
    private void restablecerCuenta() {
        if (!sesionPrincipal()) {
            return;
        }
        Usuario seleccionada = tablaCuentas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una cuenta de la tabla.");
            return;
        }
        String password = txtPassword.getText();
        if (password == null || password.length() < 8) {
            advertencia("Escriba la nueva contraseña (mínimo 8 caracteres) en el campo de contraseña.");
            return;
        }
        if (!confirmar("¿Restablecer la contraseña de " + seleccionada.getUsername() + "?")) {
            return;
        }
        try {
            usuarioDAO.restablecerPassword(seleccionada, password);
            txtPassword.clear();
            lblEstado.setText("Contraseña de " + seleccionada.getUsername() + " restablecida.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo restablecer la contraseña", e);
        }
    }

    @FXML
    private void limpiarCuenta() {
        tablaCuentas.getSelectionModel().clearSelection();
        txtUsername.clear();
        txtPassword.clear();
        cmbRol.setValue(RolUsuario.ADMIN);
        cmbOperadorCuenta.setValue(null);
        chkCuentaActiva.setSelected(true);
        chkCuentaReportes.setSelected(false);
    }

    private void cargarCuentas() {
        todasLasCuentas = usuarioDAO.listar();
        aplicarFiltroCuentas();
    }

    private void aplicarFiltroCuentas() {
        if (txtBuscarCuenta == null) {
            return;
        }
        String texto = txtBuscarCuenta.getText();
        List<Usuario> visibles = todasLasCuentas.stream()
                .filter(cuenta -> coincideCuenta(cuenta, texto))
                .filter(cuenta -> filtroRol(cuenta, filtroRolCuenta))
                .filter(cuenta -> filtroEstadoCuenta(cuenta, filtroEstadoCuenta))
                .toList();
        tablaCuentas.setItems(FXCollections.observableArrayList(visibles));
        tablaCuentas.refresh();
    }

    @FXML
    private void limpiarBusquedaCuenta() {
        txtBuscarCuenta.clear();
    }

    private boolean coincideCuenta(Usuario cuenta, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String buscado = texto.trim().toLowerCase(Locale.ROOT);
        String operador = cuenta.getOperador() == null || cuenta.getOperador().getNombre() == null
                ? "" : cuenta.getOperador().getNombre();
        return contiene(cuenta.getUsername(), buscado) || operador.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private boolean filtroRol(Usuario cuenta, String filtro) {
        if (filtro == null || "Todos".equals(filtro)) {
            return true;
        }
        return "Administrador".equals(filtro) ? cuenta.getRol() == RolUsuario.ADMIN
                : cuenta.getRol() == RolUsuario.OPERADOR;
    }

    private boolean filtroEstadoCuenta(Usuario cuenta, String filtro) {
        if (filtro == null || "Todos".equals(filtro)) {
            return true;
        }
        boolean activo = Boolean.TRUE.equals(cuenta.getActivo());
        return "Activos".equals(filtro) == activo;
    }

    // ------------------------------------------------------------------ ubicaciones

    private void configurarUbicaciones() {
        colUbicacionNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colUbicacionDepartamento.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartamento()));
        colUbicacionPais.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPais()));
        colUbicacionLatitud.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLatitud() == null ? "—" : String.format(Locale.ROOT, "%.4f", c.getValue().getLatitud())));
        colUbicacionLongitud.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLongitud() == null ? "—" : String.format(Locale.ROOT, "%.4f", c.getValue().getLongitud())));

        tablaUbicaciones.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                llenarUbicacion(b);
                centrarMapa(b);
            }
        });

        BooleanBinding incompleta = Bindings.createBooleanBinding(
                () -> vacio(txtNombreUbicacion) || vacio(txtDepartamentoUbicacion)
                        || resolverPais() == null,
                txtNombreUbicacion.textProperty(), txtDepartamentoUbicacion.textProperty(),
                cmbPaisUbicacion.valueProperty(), cmbPaisUbicacion.getEditor().textProperty());
        BooleanBinding sinSeleccion = tablaUbicaciones.getSelectionModel().selectedItemProperty().isNull();

        btnGuardarUbicacion.disableProperty().bind(incompleta);
        btnActualizarUbicacion.disableProperty().bind(sinSeleccion.or(incompleta));
        btnEliminarUbicacion.disableProperty().bind(sinSeleccion);
    }

    private void configurarBuscadorPais() {
        ComboBuscador.configurar(cmbPaisUbicacion, paisesActivos);
    }

    private void seleccionarPais(String pais) {
        if (pais == null || pais.isBlank()) {
            cmbPaisUbicacion.setValue(null);
            cmbPaisUbicacion.getEditor().clear();
            return;
        }
        ComboBuscador.seleccionar(cmbPaisUbicacion, pais);
    }

    private void cargarPaisesActivos() {
        paisesActivos = paisDAO.listarActivos();
        if (paisesActivos.isEmpty()) {
            paisesActivos = paisesMundo;
        }
    }

    @FXML
    private void abrirPaises() {
        if (!sesionPrincipal()) {
            return;
        }
        List<String> activos;
        try {
            activos = paisDAO.listarActivos();
        } catch (PersistenciaException e) {
            error("No se pudieron consultar los países activos", e);
            return;
        }
        Window duenio = lblEstado.getScene() == null ? null : lblEstado.getScene().getWindow();
        Optional<List<String>> eleccion = DialogoPaises.elegir(duenio, activos, "Países activos",
                "Activar o desactivar países",
                "Marque los países donde la empresa tiene ubicaciones. Al guardar se agregan sus "
                        + "provincias y ciudades al sistema y al mapa. Los lugares que ya existen no se tocan.");
        if (eleccion.isEmpty()) {
            return;
        }
        guardarPaises(eleccion.get());
    }

    /** Guarda los países marcados y agrega las ubicaciones que falten, sin congelar la ventana. */
    private void guardarPaises(List<String> seleccion) {
        String paisPrevio = resolverPais();
        lblEstado.setText("Agregando las provincias y ciudades de los países seleccionados...");
        btnPaises.setDisable(true);
        Task<DivisionesMundo.Resumen> tarea = new Task<>() {
            @Override
            protected DivisionesMundo.Resumen call() {
                return DivisionesMundo.activar(seleccion);
            }
        };
        tarea.setOnSucceeded(evento -> {
            btnPaises.setDisable(false);
            DivisionesMundo.Resumen resumen = tarea.getValue();
            cargarPaisesActivos();
            ComboBuscador.configurar(cmbPaisUbicacion, paisesActivos);
            seleccionarPais(paisPrevio);
            cargarUbicaciones();
            lblEstado.setText("Países activos: " + resumen.paises()
                    + " · ubicaciones nuevas: " + resumen.ubicaciones() + ".");
        });
        tarea.setOnFailed(evento -> {
            btnPaises.setDisable(false);
            Throwable causa = tarea.getException();
            String detalle = causa == null ? "" : causa.getMessage();
            lblEstado.setText("No se pudieron guardar los países activos.");
            Alert alerta = new Alert(Alert.AlertType.ERROR, detalle, ButtonType.OK);
            alerta.setHeaderText("No se pudieron guardar los países activos");
            alerta.showAndWait();
        });
        Thread hilo = new Thread(tarea, "paises-activos");
        hilo.setDaemon(true);
        hilo.start();
    }

    private String resolverPais() {
        String elegido = cmbPaisUbicacion.getValue();
        if (elegido != null && !elegido.isBlank() && paisesMundo.contains(elegido)) {
            return elegido;
        }
        String buscado = normalizar(cmbPaisUbicacion.getEditor().getText());
        if (buscado.isBlank()) {
            return null;
        }
        return paisesMundo.stream()
                .filter(pais -> normalizar(pais).equals(buscado))
                .findFirst()
                .orElse(null);
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private void llenarUbicacion(Ubicacion ubicacion) {
        txtNombreUbicacion.setText(ubicacion.getNombre());
        txtDepartamentoUbicacion.setText(ubicacion.getDepartamento());
        seleccionarPais(ubicacion.getPais());
        txtLatitud.setText(ubicacion.getLatitud() == null ? ""
                : String.format(Locale.ROOT, "%.4f", ubicacion.getLatitud()));
        txtLongitud.setText(ubicacion.getLongitud() == null ? ""
                : String.format(Locale.ROOT, "%.4f", ubicacion.getLongitud()));
    }

    @FXML
    private void guardarUbicacion() {
        if (!sesionPrincipal()) {
            return;
        }
        try {
            ubicacionDAO.guardar(construirUbicacion(new Ubicacion()));
            limpiarUbicacion();
            cargarUbicaciones();
            lblEstado.setText("Ubicación guardada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo guardar la ubicación (¿nombre y departamento repetidos?)", e);
        }
    }

    @FXML
    private void actualizarUbicacion() {
        if (!sesionPrincipal()) {
            return;
        }
        Ubicacion seleccionada = tablaUbicaciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una ubicación de la tabla.");
            return;
        }
        try {
            Ubicacion ubicacion = construirUbicacion(seleccionada);
            ubicacionDAO.actualizar(ubicacion);
            limpiarUbicacion();
            cargarUbicaciones();
            lblEstado.setText("Ubicación " + ubicacion.getNombre() + " actualizada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo actualizar la ubicación", e);
        }
    }

    @FXML
    private void eliminarUbicacion() {
        if (!sesionPrincipal()) {
            return;
        }
        Ubicacion seleccionada = tablaUbicaciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una ubicación de la tabla.");
            return;
        }
        if (!confirmar("¿Eliminar la ubicación " + seleccionada.getNombre() + "?")) {
            return;
        }
        try {
            ubicacionDAO.eliminar(seleccionada.getId());
            limpiarUbicacion();
            cargarUbicaciones();
            cargarRutas();
            lblEstado.setText("Ubicación eliminada.");
        } catch (PersistenciaException e) {
            error("No se pudo eliminar (puede estar usada en viajes o rutas)", e);
        }
    }

    @FXML
    private void limpiarUbicacion() {
        tablaUbicaciones.getSelectionModel().clearSelection();
        txtNombreUbicacion.clear();
        txtDepartamentoUbicacion.clear();
        seleccionarPais(null);
        txtLatitud.clear();
        txtLongitud.clear();
    }

    private Ubicacion construirUbicacion(Ubicacion ubicacion) {
        String nombre = requerido(txtNombreUbicacion, "El nombre", 120);
        String departamento = requerido(txtDepartamentoUbicacion, "El departamento", 80);
        ubicacion.setNombre(nombre);
        ubicacion.setDepartamento(departamento);
        ubicacion.setPais(resolverPais());
        if (ubicacion.getPais() == null) {
            throw new IllegalArgumentException("Seleccione un país de la lista.");
        }
        Double latitud = coordenada(txtLatitud, "La latitud", -90, 90);
        Double longitud = coordenada(txtLongitud, "La longitud", -180, 180);
        if ((latitud == null) != (longitud == null)) {
            throw new IllegalArgumentException(
                    "Ingrese latitud y longitud juntas, o deje ambas coordenadas vacías.");
        }
        ubicacion.setLatitud(latitud);
        ubicacion.setLongitud(longitud);
        return ubicacion;
    }

    private Double coordenada(TextField campo, String nombre, double minimo, double maximo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        if (valor.isEmpty()) {
            return null;
        }
        try {
            double numero = Double.parseDouble(valor.replace(',', '.'));
            if (numero < minimo || numero > maximo) {
                throw new IllegalArgumentException(nombre + " está fuera del rango permitido ("
                        + minimo + " a " + maximo + ").");
            }
            return numero;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nombre + " debe ser un número decimal, por ejemplo 12.1364.");
        }
    }

    private void cargarUbicaciones() {
        ubicaciones = ubicacionDAO.listar();
        aplicarFiltroUbicaciones();
        ComboBuscador.configurar(cmbOrigenRuta, ubicaciones);
        ComboBuscador.configurar(cmbDestinoRuta, ubicaciones);
    }

    private void aplicarFiltroUbicaciones() {
        String filtro = normalizar(txtBuscarUbicacion == null ? "" : txtBuscarUbicacion.getText());
        List<Ubicacion> visibles = ubicaciones.stream()
                .filter(u -> filtro.isBlank()
                        || normalizar(u.getNombre()).contains(filtro)
                        || normalizar(u.getDepartamento()).contains(filtro)
                        || normalizar(u.getPais()).contains(filtro))
                .toList();
        tablaUbicaciones.setItems(FXCollections.observableArrayList(visibles));
        tablaUbicaciones.refresh();
        actualizarPuntosMapa(visibles);
    }

    @FXML
    private void limpiarBusquedaUbicacion() {
        txtBuscarUbicacion.clear();
    }

    private void crearMapa() {
        try {
            Path html = prepararMapaLocal();
            WebView web = new WebView();
            this.mapa = web;
            web.setContextMenuEnabled(false);
            contenedorMapaAdmin.getChildren().add(web);
            WebEngine motor = web.getEngine();
            motor.setUserAgent(USER_AGENT);
            motor.getLoadWorker().stateProperty().addListener((o, anterior, estado) -> {
                if (estado == Worker.State.SUCCEEDED) {
                    mapaListo = true;
                    JSObject ventanaJs = (JSObject) motor.executeScript("window");
                    ventanaJs.setMember("javaApp", this);
                    ajustarLimitesMapa();
                    actualizarPuntosMapa(tablaUbicaciones.getItems());
                    centrarMapa(tablaUbicaciones.getSelectionModel().getSelectedItem());
                }
            });
            motor.load(html.toUri().toString());
        } catch (IOException e) {
            lblEstado.setText("No se pudo preparar el mapa: " + e.getMessage());
        }
    }

    private void ajustarLimitesMapa() {
        if (!mapaListo || mapa == null) {
            return;
        }
        ejecutarMapa("limitarMundo()");
    }

    private void ejecutarMapa(String script) {
        try {
            mapa.getEngine().executeScript(script);
        } catch (Exception e) {
            lblEstado.setText("No se pudo ajustar el mapa.");
        }
    }

    private void actualizarPuntosMapa(List<Ubicacion> visibles) {
        if (!mapaListo || mapa == null) {
            return;
        }
        List<Map<String, Object>> puntos = new ArrayList<>();
        for (Ubicacion ubicacion : visibles) {
            if (ubicacion.getLatitud() == null || ubicacion.getLongitud() == null) {
                continue;
            }
            puntos.add(Map.of(
                    "id", ubicacion.getId(),
                    "lat", ubicacion.getLatitud(),
                    "lng", ubicacion.getLongitud(),
                    "nombre", ubicacion.getNombre(),
                    "detalle", ubicacion.getDepartamento() + " · " + ubicacion.getPais(),
                    "color", colorSeguro(ubicacion.getColor())));
        }
        try {
            mapa.getEngine().executeScript(
                    "cargarPuntos(" + json.writeValueAsString(puntos) + ")");
        } catch (Exception e) {
            lblEstado.setText("No se pudieron actualizar los puntos del mapa.");
        }
    }

    private void centrarMapa(Ubicacion ubicacion) {
        if (!mapaListo || mapa == null || ubicacion == null
                || ubicacion.getLatitud() == null || ubicacion.getLongitud() == null) {
            return;
        }
        mapa.getEngine().executeScript("centrar(" + ubicacion.getLatitud() + ","
                + ubicacion.getLongitud() + ",14)");
    }

    /** Invocado desde JavaScript al pulsar un marcador; solo selecciona, no crea puntos. */
    public void seleccionarUbicacionMapa(int id) {
        Platform.runLater(() -> tablaUbicaciones.getItems().stream()
                .filter(ubicacion -> Objects.equals(ubicacion.getId(), id))
                .findFirst()
                .ifPresent(ubicacion -> {
                    tablaUbicaciones.getSelectionModel().select(ubicacion);
                    tablaUbicaciones.scrollTo(ubicacion);
                }));
    }

    private String colorSeguro(String color) {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$")
                ? color : "#0891B2";
    }

    private Path prepararMapaLocal() throws IOException {
        Path raiz = Files.createTempDirectory("fleetroute-admin-mapa-");
        Path leaflet = raiz.resolve("leaflet");
        Path imagenes = leaflet.resolve("images");
        Files.createDirectories(imagenes);
        raiz.toFile().deleteOnExit();
        leaflet.toFile().deleteOnExit();
        imagenes.toFile().deleteOnExit();
        copiarRecurso("leaflet.css", leaflet.resolve("leaflet.css"));
        copiarRecurso("leaflet.js", leaflet.resolve("leaflet.js"));
        for (String imagen : List.of("marker-icon.png", "marker-icon-2x.png", "marker-shadow.png",
                "layers.png", "layers-2x.png")) {
            copiarRecurso("images/" + imagen, imagenes.resolve(imagen));
        }
        Path html = raiz.resolve("mapa.html");
        Files.writeString(html, HTML_MAPA_ADMIN, StandardCharsets.UTF_8);
        html.toFile().deleteOnExit();
        return html;
    }

    private void copiarRecurso(String nombre, Path destino) throws IOException {
        String ruta = "/edu/ni/uam/proyecto_final_pae/web/leaflet/" + nombre;
        try (InputStream entrada = getClass().getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IOException("Falta el recurso " + ruta);
            }
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            destino.toFile().deleteOnExit();
        }
    }

    private static final String HTML_MAPA_ADMIN = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <link rel="stylesheet" href="leaflet/leaflet.css"/>
              <style>
                html, body { margin: 0; height: 100%; font-family: Segoe UI, sans-serif; }
                #mapa { width: 100%; height: 100%; background: #D6EAF8; }
                .leaflet-control-attribution { font-size: 9px; }
                .leaflet-popup-content { font-size: 12px; }
              </style>
            </head>
            <body>
              <div id="mapa"></div>
              <script>window.L_DISABLE_3D = true; /* El WebView de JavaFX no pinta bien translate3d. */</script>
              <script src="leaflet/leaflet.js"></script>
              <script>
                L.Browser.any3d = false;
                var MAX_SATELITE = 17;
                var MAX_CALLE = 19;
                var mapa = L.map('mapa', {
                  zoomControl: true,
                  zoomAnimation: false,
                  fadeAnimation: false,
                  markerZoomAnimation: false,
                  minZoom: 3,
                  maxZoom: MAX_SATELITE
                }).setView([12.8, -85.5], 6);
                var capaCalles = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
                  maxZoom: MAX_CALLE,
                  keepBuffer: 4,
                  noWrap: true,
                  attribution: 'Tiles &copy; Esri · Geocoding &copy; OpenStreetMap/Nominatim'
                });
                var capaSatelite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                  maxZoom: MAX_CALLE,
                  maxNativeZoom: MAX_SATELITE,
                  keepBuffer: 4,
                  noWrap: true,
                  attribution: 'Tiles &copy; Esri · Geocoding &copy; OpenStreetMap/Nominatim'
                }).addTo(mapa);
                L.control.layers(
                  { 'Calles': capaCalles, 'Satélite': capaSatelite },
                  null,
                  { position: 'topright', collapsed: true }
                ).addTo(mapa);
                mapa.on('baselayerchange', function (e) {
                  if (e.name === 'Satélite') {
                    mapa.setMaxZoom(MAX_SATELITE);
                    if (mapa.getZoom() > MAX_SATELITE) { mapa.setZoom(MAX_SATELITE); }
                  } else {
                    mapa.setMaxZoom(MAX_CALLE);
                  }
                });
                var capaPuntos = L.layerGroup().addTo(mapa);
                var limitesPuntos = [];

                function escapar(texto) {
                  return String(texto == null ? '' : texto)
                    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
                }

                function colorSeguro(color) {
                  return /^#[0-9A-Fa-f]{6}$/.test(color || '') ? color : '#0891B2';
                }

                function cargarPuntos(puntos) {
                  capaPuntos.clearLayers();
                  var limites = [];
                  for (var i = 0; i < puntos.length; i += 1) {
                    (function (p) {
                      var marcador = L.circleMarker([p.lat, p.lng], {
                        radius: 7,
                        color: '#ffffff',
                        weight: 2,
                        fillColor: colorSeguro(p.color),
                        fillOpacity: 0.95
                      });
                      marcador.bindPopup('<b>' + escapar(p.nombre) + '</b>' +
                        (p.detalle ? '<br/>' + escapar(p.detalle) : ''));
                      marcador.on('click', function () {
                        if (window.javaApp) { window.javaApp.seleccionarUbicacionMapa(p.id); }
                      });
                      capaPuntos.addLayer(marcador);
                      limites.push([p.lat, p.lng]);
                    })(puntos[i]);
                  }
                  limitesPuntos = limites;
                  setTimeout(ajustarVista, 180);
                }

                function ajustarVista() {
                  mapa.invalidateSize();
                  if (limitesPuntos.length === 1) {
                    mapa.setView(limitesPuntos[0], 14);
                  } else if (limitesPuntos.length > 1) {
                    mapa.fitBounds(limitesPuntos, { padding: [28, 28], maxZoom: 10 });
                  } else {
                    mapa.setView([12.8, -85.5], 6);
                  }
                }

                function limitarMundo() {
                  var mundo = L.latLngBounds([-85, -180], [85, 180]);
                  mapa.setMaxBounds(mundo);
                  mapa.options.maxBoundsViscosity = 1.0;
                  var zoomMinimo = mapa.getBoundsZoom(mundo, true);
                  if (!isFinite(zoomMinimo) || zoomMinimo < 2) {
                    zoomMinimo = 2;
                  }
                  zoomMinimo = Math.ceil(zoomMinimo);
                  mapa.setMinZoom(zoomMinimo);
                  if (mapa.getZoom() < zoomMinimo) {
                    mapa.setZoom(zoomMinimo);
                  }
                }

                function centrar(lat, lng, zoom) {
                  mapa.setView([lat, lng], zoom || 14);
                  setTimeout(function () { mapa.invalidateSize(); }, 150);
                }

                function reajustar() { mapa.invalidateSize(); }
                window.addEventListener('resize', reajustar);
                setTimeout(reajustar, 300);
                setTimeout(reajustar, 900);
              </script>
            </body>
            </html>
            """;

    // ------------------------------------------------------------------ rutas

    private void configurarRutas() {
        colRutaOrigen.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrigen().getNombre()));
        colRutaDestino.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDestino().getNombre()));
        colRutaHoras.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTiempoEstimadoHoras() == null ? "—"
                        : c.getValue().getTiempoEstimadoHoras().stripTrailingZeros().toPlainString() + " h"));
        colRutaPrecio.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPrecio() == null ? "—"
                        : c.getValue().getPrecio().stripTrailingZeros().toPlainString()));

        tablaRutas.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                cmbOrigenRuta.setValue(b.getOrigen());
                cmbDestinoRuta.setValue(b.getDestino());
                txtHorasRuta.setText(b.getTiempoEstimadoHoras() == null ? ""
                        : b.getTiempoEstimadoHoras().stripTrailingZeros().toPlainString());
                txtPrecioRuta.setText(b.getPrecio() == null ? ""
                        : b.getPrecio().stripTrailingZeros().toPlainString());
            }
        });

        BooleanBinding incompleta = Bindings.createBooleanBinding(
                () -> cmbOrigenRuta.getValue() == null || cmbDestinoRuta.getValue() == null
                        || vacio(txtHorasRuta),
                cmbOrigenRuta.valueProperty(), cmbDestinoRuta.valueProperty(),
                txtHorasRuta.textProperty());
        BooleanBinding sinSeleccion = tablaRutas.getSelectionModel().selectedItemProperty().isNull();

        btnGuardarRuta.disableProperty().bind(incompleta);
        btnActualizarRuta.disableProperty().bind(sinSeleccion.or(incompleta));
        btnEliminarRuta.disableProperty().bind(sinSeleccion);
    }

    @FXML
    private void guardarRuta() {
        if (!sesionPrincipal()) {
            return;
        }
        try {
            Ruta ruta = construirRuta();
            if (rutaDAO.buscar(ruta.getOrigen(), ruta.getDestino()).isPresent()) {
                throw new IllegalArgumentException("Ya existe una ruta configurada para ese origen y destino.");
            }
            rutaDAO.guardar(ruta);
            limpiarRuta();
            cargarRutas();
            lblEstado.setText("Ruta guardada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo guardar la ruta", e);
        }
    }

    @FXML
    private void actualizarRuta() {
        if (!sesionPrincipal()) {
            return;
        }
        Ruta seleccionada = tablaRutas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una ruta de la tabla.");
            return;
        }
        try {
            Ruta ruta = construirRuta();
            Optional<Ruta> existente = rutaDAO.buscar(ruta.getOrigen(), ruta.getDestino());
            if (existente.isPresent() && !existente.get().getId().equals(seleccionada.getId())) {
                throw new IllegalArgumentException("Ya existe otra ruta configurada para ese origen y destino.");
            }
            ruta.setId(seleccionada.getId());
            rutaDAO.actualizar(ruta);
            limpiarRuta();
            cargarRutas();
            lblEstado.setText("Ruta actualizada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo actualizar la ruta", e);
        }
    }

    @FXML
    private void eliminarRuta() {
        if (!sesionPrincipal()) {
            return;
        }
        Ruta seleccionada = tablaRutas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una ruta de la tabla.");
            return;
        }
        if (!confirmar("¿Eliminar la ruta " + seleccionada + "?")) {
            return;
        }
        try {
            rutaDAO.eliminar(seleccionada.getId());
            limpiarRuta();
            cargarRutas();
            lblEstado.setText("Ruta eliminada.");
        } catch (PersistenciaException e) {
            error("No se pudo eliminar la ruta", e);
        }
    }

    @FXML
    private void limpiarRuta() {
        tablaRutas.getSelectionModel().clearSelection();
        cmbOrigenRuta.setValue(null);
        cmbDestinoRuta.setValue(null);
        txtHorasRuta.clear();
        txtPrecioRuta.clear();
    }

    private Ruta construirRuta() {
        Ubicacion origen = cmbOrigenRuta.getValue();
        Ubicacion destino = cmbDestinoRuta.getValue();
        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Seleccione origen y destino.");
        }
        if (Objects.equals(origen.getId(), destino.getId())) {
            throw new IllegalArgumentException("El origen y el destino deben ser distintos.");
        }
        BigDecimal horas;
        try {
            horas = new BigDecimal(txtHorasRuta.getText().trim().replace(',', '.'));
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Las horas deben ser un número, por ejemplo 6.50.");
        }
        if (horas.signum() <= 0 || horas.compareTo(new BigDecimal("999")) > 0) {
            throw new IllegalArgumentException("Las horas deben ser mayores a 0 y menores a 999.");
        }
        BigDecimal precio = null;
        String textoPrecio = txtPrecioRuta.getText() == null ? "" : txtPrecioRuta.getText().trim();
        if (!textoPrecio.isEmpty()) {
            try {
                precio = new BigDecimal(textoPrecio.replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El precio debe ser un número, por ejemplo 1800.00.");
            }
            if (precio.signum() <= 0) {
                throw new IllegalArgumentException("El precio debe ser mayor que 0.");
            }
        }
        return new Ruta(origen, destino, horas, precio);
    }

    private void cargarRutas() {
        todasLasRutas = rutaDAO.listar();
        aplicarFiltroRutas();
    }

    private void aplicarFiltroRutas() {
        String filtro = normalizar(txtBuscarRutaAdmin == null ? "" : txtBuscarRutaAdmin.getText());
        List<Ruta> visibles = todasLasRutas.stream()
                .filter(r -> filtro.isBlank()
                        || normalizar(r.getOrigen().getNombre()).contains(filtro)
                        || normalizar(r.getDestino().getNombre()).contains(filtro))
                .toList();
        tablaRutas.setItems(FXCollections.observableArrayList(visibles));
        tablaRutas.refresh();
    }

    @FXML
    private void limpiarBusquedaRuta() {
        txtBuscarRutaAdmin.clear();
    }

    // ------------------------------------------------------------------ borrados masivos

    @FXML
    private void eliminarCuentasFiltradas() {
        if (!sesionPrincipal()) {
            return;
        }
        List<Usuario> eliminables = tablaCuentas.getItems().stream()
                .filter(cuenta -> cuenta.getRol() == RolUsuario.OPERADOR)
                .filter(cuenta -> !Boolean.TRUE.equals(cuenta.getEsPrincipal()))
                .toList();
        if (eliminables.isEmpty()) {
            advertencia("No hay cuentas de operador en el filtro actual.");
            return;
        }
        if (!confirmarConPassword("Eliminar cuentas filtradas",
                "Se eliminarán " + eliminables.size()
                        + " cuenta(s) de operador del filtro actual. "
                        + "Las cuentas de administrador se conservan.",
                "Eliminar cuentas")) {
            return;
        }
        try {
            int total = usuarioDAO.eliminarCuentasOperador(
                    eliminables.stream().map(Usuario::getId).toList());
            limpiarCuenta();
            cargarCuentas();
            lblEstado.setText(total + " cuenta(s) eliminada(s).");
        } catch (PersistenciaException e) {
            error("No se pudieron eliminar las cuentas", e);
        }
    }

    @FXML
    private void eliminarTodasUbicaciones() {
        if (!sesionPrincipal()) {
            return;
        }
        if (ubicaciones.isEmpty()) {
            advertencia("No hay ubicaciones registradas para eliminar.");
            return;
        }
        if (!confirmarConPassword("Eliminar todas las ubicaciones",
                "Se eliminarán TODAS las ubicaciones y las rutas configuradas. "
                        + "Si existen viajes, la operación se cancelará para proteger el historial.",
                "Eliminar ubicaciones")) {
            return;
        }
        try {
            int[] resultado = ubicacionDAO.eliminarTodas();
            limpiarUbicacion();
            limpiarRuta();
            cargarUbicaciones();
            cargarRutas();
            lblEstado.setText("Eliminadas " + resultado[1] + " ubicación(es) y "
                    + resultado[0] + " ruta(s).");
        } catch (PersistenciaException e) {
            error("No se pudieron eliminar las ubicaciones", e);
        }
    }

    @FXML
    private void eliminarTodasRutas() {
        if (!sesionPrincipal()) {
            return;
        }
        if (todasLasRutas.isEmpty()) {
            advertencia("No hay rutas configuradas para eliminar.");
            return;
        }
        if (!confirmarConPassword("Eliminar todas las rutas",
                "Se eliminarán TODAS las rutas configuradas. No se puede deshacer.",
                "Eliminar rutas")) {
            return;
        }
        try {
            int total = rutaDAO.eliminarTodas();
            limpiarRuta();
            cargarRutas();
            lblEstado.setText(total + " ruta(s) eliminada(s).");
        } catch (PersistenciaException e) {
            error("No se pudieron eliminar las rutas", e);
        }
    }

    private boolean confirmarConPassword(String titulo, String mensaje, String accion) {
        PasswordField clave = new PasswordField();
        clave.setPromptText("Contraseña del administrador");
        Label aviso = new Label(mensaje);
        aviso.setWrapText(true);
        Label instruccion = new Label("Escriba su contraseña para confirmar:");
        VBox contenido = new VBox(8, aviso, instruccion, clave);
        contenido.setPrefWidth(420);

        ButtonType ejecutar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, "", ejecutar, cancelar);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.getDialogPane().setContent(contenido);
        if (lblEstado.getScene() != null) {
            alerta.initOwner(lblEstado.getScene().getWindow());
            alerta.initModality(Modality.WINDOW_MODAL);
        }
        Button botonEjecutar = (Button) alerta.getDialogPane().lookupButton(ejecutar);
        botonEjecutar.disableProperty().bind(clave.textProperty().isEmpty());

        while (true) {
            Optional<ButtonType> respuesta = alerta.showAndWait();
            if (respuesta.isEmpty() || respuesta.get() != ejecutar) {
                return false;
            }
            Usuario admin = SesionActual.getUsuario();
            if (admin == null) {
                return false;
            }
            String password = clave.getText() == null ? "" : clave.getText();
            try {
                Usuario validado = usuarioDAO.autenticar(admin.getUsername(), password);
                if (validado != null && Objects.equals(validado.getId(), admin.getId())
                        && validado.getRol() == RolUsuario.ADMIN) {
                    return true;
                }
            } catch (PersistenciaException e) {
                error("No se pudo validar la contraseña", e);
                return false;
            }
            advertencia("Contraseña incorrecta.");
            clave.clear();
        }
    }

    // ------------------------------------------------------------------ apoyo

    private boolean sesionPrincipal() {
        try {
            Usuario usuario = SesionActual.requerir(RolUsuario.ADMIN);
            if (!Boolean.TRUE.equals(usuario.getEsPrincipal())) {
                advertencia("Solo el administrador principal puede modificar esta configuración.");
                return false;
            }
            return true;
        } catch (SecurityException e) {
            lblEstado.setText(e.getMessage());
            return false;
        }
    }

    private String requerido(TextField campo, String nombre, int maximo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException(nombre + " es obligatorio.");
        }
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(nombre + " no puede pasar de " + maximo + " caracteres.");
        }
        return valor;
    }

    private boolean vacio(TextField campo) {
        return campo.getText() == null || campo.getText().isBlank();
    }

    private static boolean contiene(String valor, String buscado) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private boolean confirmar(String mensaje) {
        ButtonType continuar = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, continuar, cancelar);
        alerta.setHeaderText(null);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == continuar;
    }

    private void advertencia(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK);
        alerta.setHeaderText("Datos incompletos o inválidos");
        alerta.showAndWait();
    }

    // ------------------------------------------------------------------ alertas

    private void configurarAlertas() {
        colAlertaFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaHora() == null ? "—" : c.getValue().getFechaHora().format(FORMATO_ALERTA)));
        colAlertaOperador.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOperador() == null ? "—" : c.getValue().getOperador().getNombre()));
        colAlertaViaje.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getViaje() == null ? "—"
                        : "#" + c.getValue().getViaje().getId() + " · "
                        + c.getValue().getViaje().getCarga().getNumeroCarga()));
        colAlertaUnidad.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUnidad() == null ? "—" : c.getValue().getUnidad().getNumeroUnidad()));
        colAlertaUbicacion.setCellValueFactory(c -> new SimpleStringProperty(
                ubicacionAlerta(c.getValue())));
        colAlertaMensaje.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMensaje() == null ? "—" : c.getValue().getMensaje()));
        colAlertaEstado.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getAtendida()) ? "Atendida" : "Pendiente"));
        btnMarcarAlertaAtendida.disableProperty().bind(
                tablaAlertas.getSelectionModel().selectedItemProperty().isNull());
    }

    @FXML
    private void cargarAlertas() {
        try {
            tablaAlertas.setItems(FXCollections.observableArrayList(alertaOperadorDAO.listarTodas()));
            tablaAlertas.refresh();
        } catch (PersistenciaException e) {
            error("No se pudieron cargar las alertas", e);
        }
    }

    @FXML
    private void marcarAlertaAtendida() {
        AlertaOperador seleccionada = tablaAlertas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una alerta de la tabla.");
            return;
        }
        try {
            alertaOperadorDAO.marcarAtendida(seleccionada.getId());
            cargarAlertas();
        } catch (PersistenciaException e) {
            error("No se pudo marcar la alerta", e);
        }
    }

    private String ubicacionAlerta(AlertaOperador alerta) {
        String ubicacion = alerta.getUbicacion() == null ? "—" : alerta.getUbicacion();
        if (alerta.getLatitud() != null && alerta.getLongitud() != null) {
            ubicacion += " (" + String.format(Locale.ROOT, "%.5f", alerta.getLatitud())
                    + ", " + String.format(Locale.ROOT, "%.5f", alerta.getLongitud()) + ")";
        }
        return ubicacion;
    }

    private void error(String titulo, PersistenciaException e) {
        lblEstado.setText(titulo);
        Alert alerta = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}