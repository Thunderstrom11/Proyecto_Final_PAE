package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.FlotaDAO;
import edu.ni.uam.proyecto_final_pae.dao.OperadorDAO;
import edu.ni.uam.proyecto_final_pae.dao.RegistroJornadaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UnidadDAO;
import edu.ni.uam.proyecto_final_pae.model.Operador;
import edu.ni.uam.proyecto_final_pae.model.RegistroJornada;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Unidad;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import edu.ni.uam.proyecto_final_pae.service.JornadaService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monitor de flota del administrador (RF-11 y RF-12): todos los viajes activos con su unidad,
 * operador, carga, horas de manejo acumuladas y semaforo. Se refresca cada 5 segundos.
 *
 * <p>La lista sale de la consulta HQL de {@link FlotaDAO#viajesActivos()} y el semaforo de
 * {@link JornadaService#resumen(int)} (los mismos calculos que ve el operador en su cabina).</p>
 */
public class MonitorController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA_CORTA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final FlotaDAO flotaDAO = new FlotaDAO();
    private final RegistroJornadaDAO registroDAO = new RegistroJornadaDAO();
    private final UnidadDAO unidadDAO = new UnidadDAO();
    private final OperadorDAO operadorDAO = new OperadorDAO();

    @FXML private TableView<Fila> tabla;
    @FXML private TableColumn<Fila, String> colViaje;
    @FXML private TableColumn<Fila, String> colUnidad;
    @FXML private TableColumn<Fila, String> colOperador;
    @FXML private TableColumn<Fila, String> colCarga;
    @FXML private TableColumn<Fila, String> colRuta;
    @FXML private TableColumn<Fila, String> colSla;
    @FXML private TableColumn<Fila, String> colManejo;
    @FXML private TableColumn<Fila, String> colSemaforo;
    @FXML private TableColumn<Fila, String> colEstado;
    @FXML private TableView<Unidad> tablaUnidades;
    @FXML private TableColumn<Unidad, String> colUnidadNumero;
    @FXML private TableColumn<Unidad, String> colUnidadPlacas;
    @FXML private TableColumn<Unidad, String> colUnidadModelo;
    @FXML private TableColumn<Unidad, String> colUnidadCapacidad;
    @FXML private TableColumn<Unidad, String> colUnidadEstado;
    @FXML private TableView<Operador> tablaOperadores;
    @FXML private TableColumn<Operador, String> colOperadorNombre;
    @FXML private TableColumn<Operador, String> colOperadorCedula;
    @FXML private TableColumn<Operador, String> colOperadorTelefono;
    @FXML private TableColumn<Operador, String> colOperadorMax;
    @FXML private TableColumn<Operador, String> colOperadorMin;
    @FXML private TableColumn<Operador, String> colOperadorEstado;
    @FXML private TextField txtFiltro;
    @FXML private TextField txtFiltroUnidades;
    @FXML private ComboBox<String> cmbEstadoUnidades;
    @FXML private TextField txtFiltroOperadores;
    @FXML private ComboBox<String> cmbEstadoOperadores;
    @FXML private Button btnLimpiarFiltroViaje;
    @FXML private Button btnLimpiarFiltroUnidades;
    @FXML private Button btnLimpiarFiltroOperadores;
    @FXML private Label lblEstado;

    private List<Fila> todas = List.of();
    private List<Unidad> todasUnidades = List.of();
    private List<Operador> todosOperadores = List.of();
    private Timeline reloj;

    /** Fila ya calculada del monitor. */
    public record Fila(
            String id,
            String unidad,
            String operador,
            String carga,
            String ruta,
            String limite,
            String manejo,
            String semaforo,
            String estado,
            JornadaService.Semaforo color,
            String detalle) {

        boolean coincide(String texto) {
            if (texto == null || texto.isBlank()) {
                return true;
            }
            String buscado = texto.trim().toLowerCase(Locale.ROOT);
            return java.util.stream.Stream.of(id, unidad, operador, carga, ruta, estado)
                    .anyMatch(valor -> valor != null && valor.toLowerCase(Locale.ROOT).contains(buscado));
        }
    }

    @FXML
    private void initialize() {
        try {
            SesionActual.requerir(RolUsuario.ADMIN);
        } catch (SecurityException e) {
            lblEstado.setText(e.getMessage());
            return;
        }
        colViaje.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id()));
        colUnidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().unidad()));
        colOperador.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().operador()));
        colCarga.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().carga()));
        colRuta.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().ruta()));
        colSla.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().limite()));
        colManejo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().manejo()));
        colSemaforo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().semaforo()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));

        colUnidadNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroUnidad()));
        colUnidadPlacas.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPlacas()));
        colUnidadModelo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModelo()));
        colUnidadCapacidad.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCapacidadToneladas() == null ? "—" : c.getValue().getCapacidadToneladas().toPlainString()));
        colUnidadEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getNombre()));

        colOperadorNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colOperadorCedula.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCedula()));
        colOperadorTelefono.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() == null ? "—" : c.getValue().getTelefono()));
        colOperadorMax.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getHorasMaxConduccion().toPlainString()));
        colOperadorMin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getHorasMinDescanso().toPlainString()));
        colOperadorEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getNombre()));

        colManejo.setStyle("-fx-alignment: CENTER;");
        colSemaforo.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    Fila fila = getTableRow() == null ? null : getTableRow().getItem();
                    setText(valor);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: "
                            + (fila == null ? "#333333" : fila.color().getColor()) + ";");
                    setTooltip(fila == null ? null : new Tooltip(fila.detalle()));
                }
            }
        });

        txtFiltro.textProperty().addListener((observable, anterior, actual) -> aplicarFiltro());

        cmbEstadoUnidades.getItems().setAll("Todos", "En ruta", "Disponible");
        cmbEstadoUnidades.setValue("Todos");
        cmbEstadoUnidades.valueProperty().addListener((o, a, b) -> aplicarFiltroUnidades());
        txtFiltroUnidades.textProperty().addListener((o, a, b) -> aplicarFiltroUnidades());

        cmbEstadoOperadores.getItems().setAll("Todos", "Descanso", "Disponible");
        cmbEstadoOperadores.setValue("Todos");
        cmbEstadoOperadores.valueProperty().addListener((o, a, b) -> aplicarFiltroOperadores());
        txtFiltroOperadores.textProperty().addListener((o, a, b) -> aplicarFiltroOperadores());

        reloj = new Timeline(new KeyFrame(Duration.seconds(5), evento -> {
            if (!Navegacion.vistaVisible(lblEstado)) {
                reloj.stop();
                return;
            }
            refrescarAutomatico();
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
        refrescarAutomatico();
    }

    @FXML
    private void limpiarFiltroViajes() {
        txtFiltro.clear();
    }

    @FXML
    private void limpiarFiltroUnidades() {
        txtFiltroUnidades.clear();
        cmbEstadoUnidades.setValue("Todos");
    }

    @FXML
    private void limpiarFiltroOperadores() {
        txtFiltroOperadores.clear();
        cmbEstadoOperadores.setValue("Todos");
    }

    // ------------------------------------------------------------------ datos

    private void refrescarAutomatico() {
        if (SesionActual.getUsuario() == null) {
            return;
        }
        if (!SesionActual.activa()) {
            Navegacion.sesionExpirada(Navegacion.stageDe(lblEstado));
            return;
        }
        cargar();
    }

    private void cargar() {
        try {
            List<FlotaDAO.FilaViaje> viajes = flotaDAO.viajesActivos();
            List<Integer> idsViaje = viajes.stream().map(FlotaDAO.FilaViaje::idViaje).toList();
            Map<Integer, List<RegistroJornada>> eventosPorViaje = registroDAO.listarPorViajes(idsViaje)
                    .stream().collect(Collectors.groupingBy(RegistroJornada::getIdViaje));
            LocalDateTime ahora = LocalDateTime.now(JornadaService.ZONA);

            List<Fila> filas = new ArrayList<>();
            for (FlotaDAO.FilaViaje viaje : viajes) {
                JornadaService.Resumen resumen = JornadaService.calcular(viaje,
                        eventosPorViaje.getOrDefault(viaje.idViaje(), List.of()), ahora);
                filas.add(new Fila(
                        "#" + viaje.idViaje(),
                        viaje.numeroUnidad(),
                        viaje.operador(),
                        viaje.numeroCarga(),
                        texto(viaje.origen()) + " → " + texto(viaje.destino()),
                        viaje.fechaLimite() == null ? "—" : viaje.fechaLimite().format(FECHA),
                        resumen.manejoTexto(),
                        resumen.semaforo().getEtiqueta(),
                        viaje.estado(),
                        resumen.semaforo(),
                        resumen.mensaje()));
            }
            todas = filas;
            aplicarFiltro();
            todasUnidades = unidadDAO.listar();
            todosOperadores = operadorDAO.listar();
            aplicarFiltroUnidades();
            aplicarFiltroOperadores();
            lblEstado.setText("Monitor actualizado " + LocalTime.now().format(HORA_CORTA)
                    + "  ·  " + filas.size() + " viaje(s) activo(s).");
        } catch (PersistenciaException e) {
            todas = List.of();
            todasUnidades = List.of();
            todosOperadores = List.of();
            tabla.setItems(FXCollections.observableArrayList());
            tablaUnidades.setItems(FXCollections.observableArrayList());
            tablaOperadores.setItems(FXCollections.observableArrayList());
            lblEstado.setText("Error de persistencia: " + e.getMessage());
        }
    }

    private void aplicarFiltro() {
        String filtro = txtFiltro.getText();
        List<Fila> visibles = todas.stream().filter(fila -> fila.coincide(filtro)).toList();
        tabla.setItems(FXCollections.observableArrayList(visibles));
        tabla.refresh();
    }

    private void aplicarFiltroUnidades() {
        if (txtFiltroUnidades == null || cmbEstadoUnidades == null) {
            return;
        }
        String texto = txtFiltroUnidades.getText();
        String estado = cmbEstadoUnidades.getValue();
        List<Unidad> visibles = todasUnidades.stream()
                .filter(unidad -> coincideUnidad(unidad, texto))
                .filter(unidad -> estadoUnidad(unidad, estado))
                .toList();
        ObservableList<Unidad> items = FXCollections.observableArrayList(visibles);
        tablaUnidades.setItems(items);
        tablaUnidades.refresh();
    }

    private void aplicarFiltroOperadores() {
        if (txtFiltroOperadores == null || cmbEstadoOperadores == null) {
            return;
        }
        String texto = txtFiltroOperadores.getText();
        String estado = cmbEstadoOperadores.getValue();
        List<Operador> visibles = todosOperadores.stream()
                .filter(operador -> coincideOperador(operador, texto))
                .filter(operador -> estadoOperador(operador, estado))
                .toList();
        ObservableList<Operador> items = FXCollections.observableArrayList(visibles);
        tablaOperadores.setItems(items);
        tablaOperadores.refresh();
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
        if ("En ruta".equals(filtro)) {
            return "EN_RUTA".equals(estado);
        }
        return "DISPONIBLE".equals(estado);
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
        if ("Descanso".equals(filtro)) {
            return "DESCANSO".equals(estado);
        }
        return "DISPONIBLE".equals(estado);
    }

    private static boolean contiene(String valor, String buscado) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }
}
