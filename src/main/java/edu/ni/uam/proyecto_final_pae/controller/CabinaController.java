package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.FlotaDAO;
import edu.ni.uam.proyecto_final_pae.model.RegistroJornada;
import edu.ni.uam.proyecto_final_pae.service.JornadaService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Pantalla del operador (RF-06, RF-07 y RF-08): Iniciar, Pausa, Reanudar y Confirmar entrega.
 *
 * <p>Muestra el viaje activo del operador, las horas acumuladas, el semaforo y la bitacora
 * de eventos. Se refresca sola cada 2 segundos para que el cronometro avance.</p>
 */
public class CabinaController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA_EVENTO = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
    private static final DateTimeFormatter HORA_CORTA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String ESTILO_NEUTRO =
            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 8 14 8 14; "
                    + "-fx-background-radius: 10; -fx-background-color: #E2E8F0; -fx-text-fill: #334155;";

    private final JornadaService servicio = new JornadaService();
    private final FlotaDAO flotaDAO = new FlotaDAO();

    @FXML private ComboBox<FlotaDAO.Opcion> cmbOperador;
    @FXML private Label lblEstado;
    @FXML private Label lblViaje;
    @FXML private Label lblRuta;
    @FXML private Label lblSla;
    @FXML private Label lblManejo;
    @FXML private Label lblDescanso;
    @FXML private Label lblPaso;
    @FXML private Label lblSemaforo;
    @FXML private Label lblMensaje;
    @FXML private ListView<String> lstBitacora;
    @FXML private Button btnIniciar;
    @FXML private Button btnPausar;
    @FXML private Button btnReanudar;
    @FXML private Button btnConfirmar;

    /** Viaje que se esta atendiendo en la cabina (null si el operador no tiene viajes activos). */
    private Integer idViaje;

    @FXML
    private void initialize() {
        try {
            cmbOperador.setItems(FXCollections.observableArrayList(flotaDAO.operadores()));
            if (!cmbOperador.getItems().isEmpty()) {
                cmbOperador.getSelectionModel().selectFirst();
            }
        } catch (PersistenciaException e) {
            lblEstado.setText("No se pudieron cargar los operadores: " + e.getMessage());
        }
        cmbOperador.valueProperty().addListener((observable, anterior, actual) -> refrescar());

        Timeline reloj = new Timeline(new KeyFrame(Duration.seconds(2), evento -> refrescar()));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
        refrescar();
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void refrescar() {
        FlotaDAO.Opcion operador = cmbOperador.getValue();
        if (operador == null) {
            idViaje = null;
            mostrarVacio("Seleccione un operador para ver su cabina.");
            return;
        }
        try {
            Optional<FlotaDAO.FilaViaje> viaje = flotaDAO.viajeDelOperador(operador.id());
            if (viaje.isEmpty()) {
                idViaje = null;
                mostrarVacio("El operador " + operador.nombre() + " no tiene viajes activos.");
                return;
            }
            idViaje = viaje.get().idViaje();
            pintar(servicio.resumen(idViaje));
            lblEstado.setText("Actualizado " + LocalTime.now().format(HORA_CORTA) + ".");
        } catch (PersistenciaException e) {
            idViaje = null;
            mostrarVacio("Error de persistencia: " + e.getMessage());
        }
    }

    @FXML
    private void iniciar() {
        ejecutar(servicio::iniciar);
    }

    @FXML
    private void pausar() {
        ejecutar(servicio::pausar);
    }

    @FXML
    private void reanudar() {
        ejecutar(servicio::reanudar);
    }

    @FXML
    private void confirmarEntrega() {
        ejecutar(servicio::confirmarEntrega);
    }

    /** Ejecuta una accion de la cabina y refresca la pantalla; el error se muestra al usuario. */
    private void ejecutar(IntFunction<JornadaService.Resumen> accion) {
        if (idViaje == null) {
            advertencia("No hay un viaje activo para este operador.");
            return;
        }
        try {
            pintar(accion.apply(idViaje));
            lblEstado.setText("Evento registrado " + LocalTime.now().format(HORA_CORTA) + ".");
        } catch (PersistenciaException e) {
            Alert alerta = new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK);
            alerta.setHeaderText("No se pudo registrar el evento");
            alerta.showAndWait();
        }
    }

    // ------------------------------------------------------------------ pintado

    private void pintar(JornadaService.Resumen resumen) {
        FlotaDAO.FilaViaje viaje = resumen.viaje();

        lblViaje.setText("Viaje #" + viaje.idViaje() + "   ·   Carga " + texto(viaje.numeroCarga()));
        lblRuta.setText("Ruta: " + texto(viaje.origen()) + "  →  " + texto(viaje.destino())
                + "        Unidad: " + texto(viaje.numeroUnidad()));
        lblSla.setText("Límite del cliente: " + (viaje.fechaLimite() == null ? "—" : viaje.fechaLimite().format(FECHA))
                + "        Manejo estimado: " + horas(viaje.tiempoEstimado())
                + "        Máximo permitido: " + horas(viaje.horasMaxConduccion()));

        lblManejo.setText(resumen.manejoTexto() + "  (" + resumen.porcentajeManejo() + " %)");
        lblDescanso.setText(resumen.descansoTexto());
        lblPaso.setText(resumen.paso().getEtiqueta());

        lblSemaforo.setText(resumen.semaforo().getEtiqueta());
        lblSemaforo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; "
                + "-fx-padding: 8 14 8 14; -fx-background-radius: 10; -fx-background-color: "
                + resumen.semaforo().getColor() + ";");
        lblMensaje.setText(resumen.mensaje());

        List<String> bitacora = servicio.eventos(viaje.idViaje()).stream()
                .map(this::lineaBitacora)
                .toList();
        lstBitacora.setItems(FXCollections.observableArrayList(bitacora));

        btnIniciar.setDisable(resumen.paso() != JornadaService.Paso.SIN_INICIAR);
        btnPausar.setDisable(resumen.paso() != JornadaService.Paso.EN_MARCHA);
        btnReanudar.setDisable(!(resumen.paso() == JornadaService.Paso.EN_PAUSA && resumen.puedeReanudar()));
        btnConfirmar.setDisable(resumen.paso() == JornadaService.Paso.FINALIZADO);
    }

    private void mostrarVacio(String mensaje) {
        lblViaje.setText("Sin viaje asignado");
        lblRuta.setText("—");
        lblSla.setText("—");
        lblManejo.setText("0 h 00 min");
        lblDescanso.setText("0 h 00 min");
        lblPaso.setText("—");
        lblSemaforo.setText("Sin datos");
        lblSemaforo.setStyle(ESTILO_NEUTRO);
        lblMensaje.setText(mensaje);
        lstBitacora.getItems().clear();
        btnIniciar.setDisable(true);
        btnPausar.setDisable(true);
        btnReanudar.setDisable(true);
        btnConfirmar.setDisable(true);
    }

    private String lineaBitacora(RegistroJornada registro) {
        return "#" + registro.getNumEvento() + "   " + registro.getTipoEvento().getNombre()
                + (registro.getFechaHora() == null ? "" : "   " + registro.getFechaHora().format(HORA_EVENTO));
    }

    // ------------------------------------------------------------------ apoyo

    private void advertencia(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private static String horas(java.math.BigDecimal valor) {
        return valor == null ? "—" : valor.stripTrailingZeros().toPlainString() + " h";
    }
}
