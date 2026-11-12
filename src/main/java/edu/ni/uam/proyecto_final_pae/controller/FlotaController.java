package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.CatalogoDAO;
import edu.ni.uam.proyecto_final_pae.dao.ClienteDAO;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.FlotaDAO;
import edu.ni.uam.proyecto_final_pae.dao.OperadorDAO;
import edu.ni.uam.proyecto_final_pae.dao.UnidadDAO;
import edu.ni.uam.proyecto_final_pae.model.Cliente;
import edu.ni.uam.proyecto_final_pae.model.EstadoOperador;
import edu.ni.uam.proyecto_final_pae.model.EstadoUnidad;
import edu.ni.uam.proyecto_final_pae.model.Operador;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Unidad;
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
import java.util.Optional;
import java.util.function.Supplier;

/** CRUD de unidades, operadores y clientes conectado a Hibernate (RF-01, RF-02 y login). */
public class FlotaController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UnidadDAO unidadDAO = new UnidadDAO();
    private final OperadorDAO operadorDAO = new OperadorDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();
    private final FlotaDAO flotaDAO = new FlotaDAO();

    private Timeline reloj;

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

    @FXML private Label lblEstado;
    @FXML private Label lblEmpresa;

    @FXML private Button btnVerUnidades;
    @FXML private Button btnVerUnidadesDisponibles;
    @FXML private Button btnGuardarUnidad;
    @FXML private Button btnActualizarUnidad;
    @FXML private Button btnEliminarUnidad;
    @FXML private Button btnLimpiarUnidad;

    @FXML private Button btnCargas;
    @FXML private Button btnViajes;
    @FXML private Button btnMonitor;
    @FXML private Button btnSalir;

    @FXML private Button btnVerOperadores;
    @FXML private Button btnVerOperadoresDisponibles;
    @FXML private Button btnGuardarOperador;
    @FXML private Button btnActualizarOperador;
    @FXML private Button btnEliminarOperador;
    @FXML private Button btnLimpiarOperador;

    @FXML private Button btnVerClientes;
    @FXML private Button btnVerClientesActivos;
    @FXML private Button btnGuardarCliente;
    @FXML private Button btnActualizarCliente;
    @FXML private Button btnEliminarCliente;
    @FXML private Button btnLimpiarCliente;

    @FXML
    private void initialize() {
        if (!sesionAdmin()) {
            return;
        }
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
            if (!Navegacion.vistaVisible(lblEstado)) {
                reloj.stop();
                return;
            }
            verificarSesion();
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
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
            lblEstado.setText(e.getMessage());
            return false;
        }
    }

    private void verificarSesion() {
        if (SesionActual.getUsuario() != null && !SesionActual.activa()) {
            Navegacion.sesionExpirada(Navegacion.stageDe(lblEstado));
        }
    }

    // ------------------------------------------------------------------ unidades

    @FXML
    private void verUnidades() {
        if (!sesionAdmin()) {
            return;
        }
        mostrarUnidades(unidadDAO::listar, "Todas las unidades");
    }

    @FXML
    private void verUnidadesDisponibles() {
        if (!sesionAdmin()) {
            return;
        }
        mostrarUnidades(unidadDAO::listarDisponibles, "Unidades disponibles (consulta HQL)");
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
            verUnidades();
            lblEstado.setText("Unidad " + unidad.getNumeroUnidad() + " guardada.");
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
            verUnidades();
            lblEstado.setText("Unidad " + unidad.getNumeroUnidad() + " actualizada.");
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
            verUnidades();
            lblEstado.setText("Unidad eliminada.");
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
        mostrarOperadores(operadorDAO::listar, "Todos los operadores");
    }

    @FXML
    private void verOperadoresDisponibles() {
        if (!sesionAdmin()) {
            return;
        }
        mostrarOperadores(operadorDAO::listarDisponibles, "Operadores disponibles (consulta HQL)");
    }

    @FXML
    private void guardarOperador() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            Operador operador = construirOperador(new Operador());
            operadorDAO.guardar(operador);
            limpiarOperador();
            verOperadores();
            lblEstado.setText("Operador " + operador.getNombre() + " guardado.");
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
            if (flotaDAO.operadorTieneViajeActivo(operador.getId())
                    && !estadoAnterior.equals(operador.getEstado().getNombre())) {
                throw new IllegalArgumentException(
                        "No se puede cambiar el estado de un operador con viajes activos. "
                                + "Finalice o cancele esos viajes primero.");
            }
            operadorDAO.actualizar(operador);
            limpiarOperador();
            verOperadores();
            lblEstado.setText("Operador " + operador.getNombre() + " actualizado.");
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
            verOperadores();
            lblEstado.setText("Operador eliminado.");
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
        mostrarClientes(clienteDAO::listar, "Todos los clientes");
    }

    @FXML
    private void verClientesActivos() {
        if (!sesionAdmin()) {
            return;
        }
        mostrarClientes(clienteDAO::listarActivos, "Clientes activos (consulta HQL)");
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
            verClientes();
            lblEstado.setText("Cliente " + cliente.getNombreEmpresa() + " guardado.");
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
            verClientes();
            lblEstado.setText("Cliente " + cliente.getNombreEmpresa() + " actualizado.");
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
            verClientes();
            lblEstado.setText("Cliente eliminado.");
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

    private void mostrarClientes(Supplier<List<Cliente>> consulta, String titulo) {
        try {
            List<Cliente> clientes = consulta.get();
            tablaClientes.setItems(FXCollections.observableArrayList(clientes));
            tablaClientes.refresh();
            lblEstado.setText(titulo + ": " + clientes.size() + " registro(s).");
        } catch (PersistenciaException e) {
            error("No se pudieron consultar los clientes", e);
        }
    }

    // ------------------------------------------------------------------ apoyo

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
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edu/ni/uam/proyecto_final_pae/" + archivo));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("FleetRoute Pro — " + titulo);
            stage.setScene(new Scene(root, 980, 700));
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

    private void mostrarUnidades(Supplier<List<Unidad>> consulta, String titulo) {
        try {
            List<Unidad> unidades = consulta.get();
            tablaUnidades.setItems(FXCollections.observableArrayList(unidades));
            tablaUnidades.refresh();
            lblEstado.setText(titulo + ": " + unidades.size() + " registro(s).");
        } catch (PersistenciaException e) {
            error("No se pudieron consultar las unidades", e);
        }
    }

    private void mostrarOperadores(Supplier<List<Operador>> consulta, String titulo) {
        try {
            List<Operador> operadores = consulta.get();
            tablaOperadores.setItems(FXCollections.observableArrayList(operadores));
            tablaOperadores.refresh();
            lblEstado.setText(titulo + ": " + operadores.size() + " registro(s).");
        } catch (PersistenciaException e) {
            error("No se pudieron consultar los operadores", e);
        }
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
