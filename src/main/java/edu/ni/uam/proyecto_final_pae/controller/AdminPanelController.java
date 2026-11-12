package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.OperadorDAO;
import edu.ni.uam.proyecto_final_pae.dao.RutaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UbicacionDAO;
import edu.ni.uam.proyecto_final_pae.dao.UsuarioDAO;
import edu.ni.uam.proyecto_final_pae.model.Operador;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Ruta;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Panel administrativo exclusivo del administrador principal (N.º 5).
 *
 * <p>Gestiona cuentas de administradores y operadores, el permiso de reportes financieros,
 * las ubicaciones de origen/destino con sus coordenadas y las rutas con su tiempo estimado.</p>
 */
public class AdminPanelController {

    private static final double MAPA_LON_MIN = -103.0;
    private static final double MAPA_LON_MAX = -73.0;
    private static final double MAPA_LAT_MIN = 6.0;
    private static final double MAPA_LAT_MAX = 18.73;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final OperadorDAO operadorDAO = new OperadorDAO();
    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();
    private final RutaDAO rutaDAO = new RutaDAO();

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
    @FXML private Canvas mapa;
    private Image mapaImagen;
    @FXML private Button btnGuardarUbicacion;
    @FXML private Button btnActualizarUbicacion;
    @FXML private Button btnEliminarUbicacion;

    // rutas
    @FXML private TableView<Ruta> tablaRutas;
    @FXML private TableColumn<Ruta, String> colRutaOrigen;
    @FXML private TableColumn<Ruta, String> colRutaDestino;
    @FXML private TableColumn<Ruta, String> colRutaHoras;
    @FXML private ComboBox<Ubicacion> cmbOrigenRuta;
    @FXML private ComboBox<Ubicacion> cmbDestinoRuta;
    @FXML private TextField txtHorasRuta;
    @FXML private Button btnGuardarRuta;
    @FXML private Button btnActualizarRuta;
    @FXML private Button btnEliminarRuta;

    @FXML private Label lblEstado;
    @FXML private Label lblAdministrador;

    private List<Usuario> todasLasCuentas = List.of();
    private List<Ubicacion> ubicaciones = List.of();
    private String filtroRolCuenta = "Todos";
    private String filtroEstadoCuenta = "Todos";

    @FXML
    private void initialize() {
        Usuario admin;
        try {
            admin = SesionActual.requerir(RolUsuario.ADMIN);
        } catch (SecurityException e) {
            lblEstado.setText(e.getMessage());
            return;
        }
        if (!Boolean.TRUE.equals(admin.getEsPrincipal())) {
            lblEstado.setText("Solo el administrador principal puede usar este panel.");
            return;
        }
        lblAdministrador.setText("Sesión: " + admin.getUsername());

        configurarCuentas();
        configurarUbicaciones();
        configurarRutas();
        cargarMapa();

        try {
            cmbOperadorCuenta.setItems(FXCollections.observableArrayList(operadorDAO.listar()));
            cmbPaisUbicacion.setItems(FXCollections.observableArrayList(paisesDelMundo()));
            cmbPaisUbicacion.setValue("Nicaragua");
            cargarCuentas();
            cargarUbicaciones();
            cargarRutas();
        } catch (PersistenciaException e) {
            error("No se pudo conectar con la base de datos", e);
        }
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
            if (!vacio(txtPassword)) {
                String password = txtPassword.getText();
                if (password.length() < 8) {
                    throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
                }
                seleccionada.setPasswordHash(
                        org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt()));
            }
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
            }
            dibujarMapa();
        });

        BooleanBinding incompleta = Bindings.createBooleanBinding(
                () -> vacio(txtNombreUbicacion) || vacio(txtDepartamentoUbicacion)
                        || cmbPaisUbicacion.getValue() == null,
                txtNombreUbicacion.textProperty(), txtDepartamentoUbicacion.textProperty(),
                cmbPaisUbicacion.valueProperty());
        BooleanBinding sinSeleccion = tablaUbicaciones.getSelectionModel().selectedItemProperty().isNull();

        btnGuardarUbicacion.disableProperty().bind(incompleta);
        btnActualizarUbicacion.disableProperty().bind(sinSeleccion.or(incompleta));
        btnEliminarUbicacion.disableProperty().bind(sinSeleccion);
    }

    private void llenarUbicacion(Ubicacion ubicacion) {
        txtNombreUbicacion.setText(ubicacion.getNombre());
        txtDepartamentoUbicacion.setText(ubicacion.getDepartamento());
        cmbPaisUbicacion.setValue(ubicacion.getPais());
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
            Ubicacion ubicacion = construirUbicacion(new Ubicacion());
            ubicacion.setId(seleccionada.getId());
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
        cmbPaisUbicacion.setValue("Nicaragua");
        txtLatitud.clear();
        txtLongitud.clear();
        dibujarMapa();
    }

    private Ubicacion construirUbicacion(Ubicacion ubicacion) {
        String nombre = requerido(txtNombreUbicacion, "El nombre", 120);
        String departamento = requerido(txtDepartamentoUbicacion, "El departamento", 80);
        ubicacion.setNombre(nombre);
        ubicacion.setDepartamento(departamento);
        ubicacion.setPais(cmbPaisUbicacion.getValue());
        ubicacion.setLatitud(coordenada(txtLatitud, "La latitud", -90, 90));
        ubicacion.setLongitud(coordenada(txtLongitud, "La longitud", -180, 180));
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
        tablaUbicaciones.setItems(FXCollections.observableArrayList(ubicaciones));
        tablaUbicaciones.refresh();
        cmbOrigenRuta.setItems(FXCollections.observableArrayList(ubicaciones));
        cmbDestinoRuta.setItems(FXCollections.observableArrayList(ubicaciones));
        dibujarMapa();
    }

    private void cargarMapa() {
        try (var flujo = getClass().getResourceAsStream(
                "/edu/ni/uam/proyecto_final_pae/images/mapa-centroamerica.png")) {
            if (flujo != null) {
                mapaImagen = new Image(flujo);
            }
        } catch (Exception e) {
            lblEstado.setText("No se pudo cargar la imagen de referencia del mapa.");
        }
        dibujarMapa();
    }

    private void dibujarMapa() {
        if (mapa == null) {
            return;
        }
        GraphicsContext g = mapa.getGraphicsContext2D();
        double ancho = mapa.getWidth();
        double alto = mapa.getHeight();
        g.clearRect(0, 0, ancho, alto);
        if (mapaImagen != null) {
            g.drawImage(mapaImagen, 0, 0, ancho, alto);
        } else {
            g.setFill(Color.web("#D6EAF8"));
            g.fillRect(0, 0, ancho, alto);
        }

        Ubicacion seleccionada = tablaUbicaciones.getSelectionModel().getSelectedItem();
        double puntoEtiquetaX = Double.NaN;
        double puntoEtiquetaY = Double.NaN;
        String nombreSeleccionado = null;

        for (Ubicacion ubicacion : ubicaciones) {
            if (ubicacion.getLatitud() == null || ubicacion.getLongitud() == null) {
                continue;
            }
            double x = (ubicacion.getLongitud() - MAPA_LON_MIN) / (MAPA_LON_MAX - MAPA_LON_MIN) * ancho;
            double y = (MAPA_LAT_MAX - ubicacion.getLatitud()) / (MAPA_LAT_MAX - MAPA_LAT_MIN) * alto;
            if (x < 0 || x > ancho || y < 0 || y > alto) {
                continue;
            }
            boolean activa = seleccionada != null
                    && Objects.equals(seleccionada.getId(), ubicacion.getId());
            double radio = activa ? 6 : 4;
            g.setFill(Color.WHITE);
            g.fillOval(x - radio - 1.5, y - radio - 1.5, radio * 2 + 3, radio * 2 + 3);
            g.setFill(activa ? Color.web("#C2712C") : Color.web("#223A5E"));
            g.fillOval(x - radio, y - radio, radio * 2, radio * 2);
            if (activa) {
                puntoEtiquetaX = x;
                puntoEtiquetaY = y;
                nombreSeleccionado = ubicacion.getNombre();
            }
        }

        // La etiqueta se dibuja al final para que quede por encima de todos los puntos.
        if (nombreSeleccionado != null) {
            Font fuente = Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12);
            Text medida = new Text(nombreSeleccionado);
            medida.setFont(fuente);
            double anchoTexto = medida.getLayoutBounds().getWidth();
            double anchoEtiqueta = anchoTexto + 12;
            double altoEtiqueta = 19;
            double etiquetaX = puntoEtiquetaX + 10;
            if (etiquetaX + anchoEtiqueta > ancho - 4) {
                etiquetaX = puntoEtiquetaX - anchoEtiqueta - 10;
            }
            double etiquetaY = puntoEtiquetaY - altoEtiqueta - 5;
            if (etiquetaY < 4) {
                etiquetaY = puntoEtiquetaY + 6;
            }
            g.setFill(Color.rgb(255, 255, 255, 0.96));
            g.fillRoundRect(etiquetaX, etiquetaY, anchoEtiqueta, altoEtiqueta, 6, 6);
            g.setStroke(Color.web("#223A5E"));
            g.setLineWidth(0.9);
            g.strokeRoundRect(etiquetaX, etiquetaY, anchoEtiqueta, altoEtiqueta, 6, 6);
            g.setFill(Color.web("#223A5E"));
            g.setFont(fuente);
            g.fillText(nombreSeleccionado, etiquetaX + 6, etiquetaY + 14);
        }
    }

    @FXML
    private void clicMapa(MouseEvent evento) {
        if (mapa == null || mapa.getWidth() <= 0) {
            return;
        }
        double x = Math.max(0, Math.min(mapa.getWidth(), evento.getX()));
        double y = Math.max(0, Math.min(mapa.getHeight(), evento.getY()));
        double longitud = MAPA_LON_MIN + (x / mapa.getWidth()) * (MAPA_LON_MAX - MAPA_LON_MIN);
        double latitud = MAPA_LAT_MAX - (y / mapa.getHeight()) * (MAPA_LAT_MAX - MAPA_LAT_MIN);
        txtLatitud.setText(String.format(Locale.ROOT, "%.5f", latitud));
        txtLongitud.setText(String.format(Locale.ROOT, "%.5f", longitud));
        lblEstado.setText("Coordenadas de la imagen: " + txtLatitud.getText() + ", "
                + txtLongitud.getText() + ". Complete el formulario y guarde.");
    }

    // ------------------------------------------------------------------ rutas

    private void configurarRutas() {
        colRutaOrigen.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrigen().getNombre()));
        colRutaDestino.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDestino().getNombre()));
        colRutaHoras.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTiempoEstimadoHoras() == null ? "—"
                        : c.getValue().getTiempoEstimadoHoras().stripTrailingZeros().toPlainString() + " h"));

        tablaRutas.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                cmbOrigenRuta.setValue(b.getOrigen());
                cmbDestinoRuta.setValue(b.getDestino());
                txtHorasRuta.setText(b.getTiempoEstimadoHoras() == null ? ""
                        : b.getTiempoEstimadoHoras().stripTrailingZeros().toPlainString());
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
        return new Ruta(origen, destino, horas);
    }

    private void cargarRutas() {
        List<Ruta> rutas = rutaDAO.listar();
        tablaRutas.setItems(FXCollections.observableArrayList(rutas));
        tablaRutas.refresh();
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

    private List<String> paisesDelMundo() {
        Locale espanol = Locale.forLanguageTag("es");
        return java.util.Arrays.stream(Locale.getISOCountries())
                .map(codigo -> new Locale("", codigo).getDisplayCountry(espanol))
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
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
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        alerta.setHeaderText(null);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.YES;
    }

    private void advertencia(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK);
        alerta.setHeaderText("Datos incompletos o inválidos");
        alerta.showAndWait();
    }

    private void error(String titulo, PersistenciaException e) {
        lblEstado.setText(titulo);
        Alert alerta = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}