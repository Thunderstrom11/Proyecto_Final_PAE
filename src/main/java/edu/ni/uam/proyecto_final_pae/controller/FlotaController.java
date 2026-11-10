package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.CatalogoDAO;
import edu.ni.uam.proyecto_final_pae.dao.OperadorDAO;
import edu.ni.uam.proyecto_final_pae.dao.UnidadDAO;
import edu.ni.uam.proyecto_final_pae.model.EstadoOperador;
import edu.ni.uam.proyecto_final_pae.model.EstadoUnidad;
import edu.ni.uam.proyecto_final_pae.model.Operador;
import edu.ni.uam.proyecto_final_pae.model.Unidad;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** CRUD de unidades y operadores conectado a Hibernate (RF-01, RF-02 y login). */
public class FlotaController {

    private final UnidadDAO unidadDAO = new UnidadDAO();
    private final OperadorDAO operadorDAO = new OperadorDAO();
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();

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

    @FXML private Label lblEstado;

    @FXML
    private void initialize() {
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

        try {
            cmbEstadoUnidad.setItems(FXCollections.observableArrayList(catalogoDAO.listar(EstadoUnidad.class)));
            cmbEstadoOperador.setItems(FXCollections.observableArrayList(catalogoDAO.listar(EstadoOperador.class)));
            verUnidades();
            verOperadores();
        } catch (PersistenciaException e) {
            error("No se pudo conectar con la base de datos", e);
        }
    }

    // ------------------------------------------------------------------ unidades

    @FXML
    private void verUnidades() {
        mostrarUnidades(unidadDAO::listar, "Todas las unidades");
    }

    @FXML
    private void verUnidadesDisponibles() {
        mostrarUnidades(unidadDAO::listarDisponibles, "Unidades disponibles (consulta HQL)");
    }

    @FXML
    private void guardarUnidad() {
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
        Unidad seleccionada = tablaUnidades.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            advertencia("Seleccione una unidad de la tabla para actualizarla.");
            return;
        }
        try {
            Unidad unidad = construirUnidad(seleccionada);
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
        mostrarOperadores(operadorDAO::listar, "Todos los operadores");
    }

    @FXML
    private void verOperadoresDisponibles() {
        mostrarOperadores(operadorDAO::listarDisponibles, "Operadores disponibles (consulta HQL)");
    }

    @FXML
    private void guardarOperador() {
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
        Operador seleccionado = tablaOperadores.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            advertencia("Seleccione un operador de la tabla para actualizarlo.");
            return;
        }
        try {
            Operador operador = construirOperador(seleccionado);
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

    // ------------------------------------------------------------------ apoyo

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
            lblEstado.setText(titulo + ": " + unidades.size() + " registro(s).");
        } catch (PersistenciaException e) {
            error("No se pudieron consultar las unidades", e);
        }
    }

    private void mostrarOperadores(Supplier<List<Operador>> consulta, String titulo) {
        try {
            List<Operador> operadores = consulta.get();
            tablaOperadores.setItems(FXCollections.observableArrayList(operadores));
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
