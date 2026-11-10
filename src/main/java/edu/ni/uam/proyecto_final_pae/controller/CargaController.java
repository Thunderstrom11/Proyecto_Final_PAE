package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.CargaDAO;
import edu.ni.uam.proyecto_final_pae.dao.CatalogoDAO;
import edu.ni.uam.proyecto_final_pae.dao.ClienteDAO;
import edu.ni.uam.proyecto_final_pae.model.Carga;
import edu.ni.uam.proyecto_final_pae.model.Cliente;
import edu.ni.uam.proyecto_final_pae.model.EstadoCarga;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/** Pantalla CRUD de cargas (RF-03) conectada a Hibernate. */
public class CargaController {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CargaDAO cargaDAO = new CargaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();

    @FXML private TableView<Carga> tabla;
    @FXML private TableColumn<Carga, String> colNumero;
    @FXML private TableColumn<Carga, String> colDescripcion;
    @FXML private TableColumn<Carga, String> colPeso;
    @FXML private TableColumn<Carga, String> colCliente;
    @FXML private TableColumn<Carga, String> colLimite;
    @FXML private TableColumn<Carga, String> colEstado;

    @FXML private TextField txtNumero;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtPeso;
    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHora;
    @FXML private Label lblEstado;

    @FXML private Button btnVerTodas;
    @FXML private Button btnVerPendientes;
    @FXML private Button btnGuardar;
    @FXML private Button btnActualizar;
    @FXML private Button btnEliminar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnNuevoCliente;

    @FXML
    private void initialize() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroCarga()));
        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescripcion()));
        colPeso.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPesoToneladas().toPlainString()));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCliente() == null ? "—" : c.getValue().getCliente().getNombreEmpresa()));
        colLimite.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaLimiteCliente().format(FORMATO)));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getNombre()));

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual != null) {
                llenarFormulario(actual);
            }
        });

        BooleanBinding datosIncompletos = Bindings.createBooleanBinding(
                () -> vacio(txtNumero) || vacio(txtPeso) || dpFecha.getValue() == null || vacio(txtHora),
                txtNumero.textProperty(), txtPeso.textProperty(),
                dpFecha.valueProperty(), txtHora.textProperty());
        BooleanBinding sinSeleccion = tabla.getSelectionModel().selectedItemProperty().isNull();

        btnGuardar.disableProperty().bind(datosIncompletos);
        btnActualizar.disableProperty().bind(sinSeleccion.or(datosIncompletos));
        btnEliminar.disableProperty().bind(sinSeleccion);

        try {
            recargarClientes();
            verTodas();
        } catch (PersistenciaException e) {
            error("No se pudo conectar con la base de datos", e);
        }
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void verTodas() {
        mostrar(() -> cargaDAO.listar(), "Cargas");
    }

    @FXML
    private void verPendientes() {
        mostrar(() -> cargaDAO.listarPendientes(), "Cargas pendientes (consulta HQL)");
    }

    @FXML
    private void guardar() {
        try {
            Carga carga = construir(new Carga());
            EstadoCarga pendiente = catalogoDAO.porNombre(EstadoCarga.class, "PENDIENTE");
            carga.setEstado(pendiente);
            cargaDAO.guardar(carga);
            limpiar();
            verTodas();
            lblEstado.setText("Carga " + carga.getNumeroCarga() + " guardada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo guardar la carga", e);
        }
    }

    @FXML
    private void actualizar() {
        Carga seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una carga de la tabla para actualizarla.");
            return;
        }
        try {
            Carga carga = construir(seleccionada);
            cargaDAO.actualizar(carga);
            limpiar();
            verTodas();
            lblEstado.setText("Carga " + carga.getNumeroCarga() + " actualizada.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo actualizar la carga", e);
        }
    }

    @FXML
    private void eliminar() {
        Carga seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una carga de la tabla para eliminarla.");
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la carga " + seleccionada.getNumeroCarga() + "?", ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.YES) {
            return;
        }
        try {
            cargaDAO.eliminar(seleccionada.getId());
            limpiar();
            verTodas();
            lblEstado.setText("Carga eliminada.");
        } catch (PersistenciaException e) {
            error("No se pudo eliminar la carga (puede estar asignada a un viaje)", e);
        }
    }

    @FXML
    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        txtNumero.clear();
        txtDescripcion.clear();
        txtPeso.clear();
        cmbCliente.getSelectionModel().clearSelection();
        dpFecha.setValue(null);
        txtHora.setText("17:00");
    }

    @FXML
    private void nuevoCliente() {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Nuevo cliente");
        dialogo.setHeaderText(null);
        dialogo.setContentText("Nombre de la empresa:");
        Optional<String> nombre = dialogo.showAndWait();
        if (nombre.isEmpty() || nombre.get().isBlank()) {
            return;
        }
        try {
            Cliente cliente = clienteDAO.guardar(new Cliente(nombre.get().trim()));
            recargarClientes();
            cmbCliente.getSelectionModel().select(cliente);
        } catch (PersistenciaException e) {
            error("No se pudo crear el cliente", e);
        }
    }

    // ------------------------------------------------------------------ validaciones

    /** Copia el formulario a la carga validando los datos (RF-03). Lanza IllegalArgumentException si hay error. */
    private Carga construir(Carga carga) {
        String numero = txtNumero.getText() == null ? "" : txtNumero.getText().trim();
        if (numero.isEmpty()) {
            throw new IllegalArgumentException("El número de carga es obligatorio.");
        }
        if (numero.length() > 30) {
            throw new IllegalArgumentException("El número de carga no puede pasar de 30 caracteres.");
        }

        BigDecimal peso;
        try {
            peso = new BigDecimal(txtPeso.getText().trim().replace(',', '.'));
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("El peso debe ser un número, por ejemplo 12.50.");
        }
        if (peso.signum() <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor que 0.");
        }

        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            throw new IllegalArgumentException("Seleccione la fecha límite del cliente.");
        }
        LocalTime hora;
        try {
            hora = LocalTime.parse(txtHora.getText().trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("La hora debe tener formato HH:mm, por ejemplo 17:30.");
        }
        LocalDateTime limite = LocalDateTime.of(fecha, hora);
        if (limite.toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha límite no puede ser anterior a hoy.");
        }

        carga.setNumeroCarga(numero);
        carga.setDescripcion(txtDescripcion.getText() == null || txtDescripcion.getText().isBlank()
                ? null : txtDescripcion.getText().trim());
        carga.setPesoToneladas(peso);
        carga.setCliente(cmbCliente.getValue());
        carga.setFechaLimiteCliente(limite);
        return carga;
    }

    // ------------------------------------------------------------------ apoyo

    private void llenarFormulario(Carga carga) {
        txtNumero.setText(carga.getNumeroCarga());
        txtDescripcion.setText(carga.getDescripcion());
        txtPeso.setText(carga.getPesoToneladas().toPlainString());
        cmbCliente.setValue(carga.getCliente());
        dpFecha.setValue(carga.getFechaLimiteCliente().toLocalDate());
        txtHora.setText(carga.getFechaLimiteCliente().toLocalTime().withSecond(0).withNano(0).toString());
    }

    private void recargarClientes() {
        cmbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarActivos()));
    }

    private boolean vacio(TextField campo) {
        return campo.getText() == null || campo.getText().isBlank();
    }

    private void mostrar(java.util.function.Supplier<List<Carga>> consulta, String titulo) {
        try {
            List<Carga> cargas = consulta.get();
            tabla.setItems(FXCollections.observableArrayList(cargas));
            lblEstado.setText(titulo + ": " + cargas.size() + " registro(s).");
        } catch (PersistenciaException e) {
            error("No se pudo consultar las cargas", e);
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
