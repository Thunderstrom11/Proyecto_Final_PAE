package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.FlotaDAO;
import edu.ni.uam.proyecto_final_pae.service.JornadaService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private final JornadaService servicio = new JornadaService();
    private final FlotaDAO flotaDAO = new FlotaDAO();

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
    @FXML private TextField txtFiltro;
    @FXML private Label lblEstado;

    private List<Fila> todas = List.of();

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
        colViaje.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id()));
        colUnidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().unidad()));
        colOperador.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().operador()));
        colCarga.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().carga()));
        colRuta.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().ruta()));
        colSla.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().limite()));
        colManejo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().manejo()));
        colSemaforo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().semaforo()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));

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

        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(5), evento -> refrescar()));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
        refrescar();
    }

    // ------------------------------------------------------------------ datos

    @FXML
    private void refrescar() {
        try {
            List<Fila> filas = new ArrayList<>();
            for (FlotaDAO.FilaViaje viaje : flotaDAO.viajesActivos()) {
                JornadaService.Resumen resumen = servicio.resumen(viaje.idViaje());
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
            lblEstado.setText("Monitor actualizado " + LocalTime.now().format(HORA_CORTA)
                    + "  ·  " + filas.size() + " viaje(s) activo(s).");
        } catch (PersistenciaException e) {
            todas = List.of();
            tabla.setItems(FXCollections.observableArrayList());
            lblEstado.setText("Error de persistencia: " + e.getMessage());
        }
    }

    private void aplicarFiltro() {
        String filtro = txtFiltro.getText();
        List<Fila> visibles = todas.stream().filter(fila -> fila.coincide(filtro)).toList();
        tabla.setItems(FXCollections.observableArrayList(visibles));
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }
}
