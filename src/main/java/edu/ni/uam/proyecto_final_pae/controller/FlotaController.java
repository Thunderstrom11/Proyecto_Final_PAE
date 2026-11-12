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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    private List<Unidad> todasLasUnidades = List.of();
    private List<Operador> todosLosOperadores = List.of();
    private List<Cliente> todosLosClientes = List.of();

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
        colHorasMax.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorasMaxConduccion().toPlainString()));
        colHorasMin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHorasMinDescanso().toPlainString()));
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

    private void mensaje(String texto) {
        if (lblEstadoUnidades != null) {
            lblEstadoUnidades.setText(texto);
        }
        if (lblEstadoOperadores != null) {
            lblEstadoOperadores.setText(texto);
        }
        if (lblEstadoClientes != null) {
            lblEstadoClientes.setText(texto);
        }
    }

    @FXML
    private void salir() {
        Navegacion.cerrarSesion(Navegacion.stageDe(btnSalir));
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
                informacion("Cuenta del operador creada",
                        "Usuario: " + operador.getCedula()
                                + "\nContraseña temporal: " + temporal
                                + "\nEl administrador principal puede restablecerla desde el panel administrativo.");
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
        if (!confirmar("¿Eliminar al operador " + seleccionado.getNombre() + "?")) {
            return;
        }
        try {
            operadorDAO.eliminar(seleccionado.getId());
            limpiarOperador();
            recargarOperadores();
            mensaje("Operador eliminado.");
        } catch (PersistenciaException e) {
            error("No se pudo eliminar el operador (puede tener un usuario o viajes)", e);
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
        abrirVista("reporte-view.fxml", "Reporte financiero", 900, 680);
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
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edu/ni/uam/proyecto_final_pae/" + archivo));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("FleetRoute Pro — " + titulo);
            stage.setScene(new Scene(root, ancho, alto));
            stage.show();
        } catch (IOException e) {
            advertencia("No se pudo abrir " + titulo + ": " + e.getMessage());
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

    private BigDecimal decimal(String valor, String nombre) {
        try {
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nombre + " debe ser un número, por ejemplo 8.50.");
        }
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
