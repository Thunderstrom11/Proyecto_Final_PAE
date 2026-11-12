package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.FlotaDAO;
import edu.ni.uam.proyecto_final_pae.model.RegistroJornada;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
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

import java.time.LocalDateTime;
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
    @FXML private Label lblSiguienteCarga;
    @FXML private Label lblSiguienteRuta;
    @FXML private Label lblSiguienteSla;
    @FXML private ListView<String> lstBitacora;
    @FXML private Button btnIniciar;
    @FXML private Button btnPausar;
    @FXML private Button btnReanudar;
    @FXML private Button btnConfirmar;
    @FXML private Button btnSalir;

    /** Viaje que se esta atendiendo en la cabina (null si el operador no tiene viajes activos). */
    private Integer idViaje;
    private Integer idOperadorSesion;
    private Timeline reloj;

    @FXML
    private void initialize() {
        try {
            Usuario usuario = SesionActual.requerir(RolUsuario.OPERADOR);
            if (usuario.getOperador() == null) {
                mostrarVacio("El usuario no tiene un operador asociado.");
                return;
            }
            idOperadorSesion = usuario.getOperador().getId();
            List<FlotaDAO.Opcion> opciones = flotaDAO.operadores().stream()
                    .filter(opcion -> opcion.id() == idOperadorSesion)
                    .toList();
            cmbOperador.setItems(FXCollections.observableArrayList(opciones));
            if (opciones.isEmpty()) {
                mostrarVacio("El operador de la sesión no existe en la base de datos.");
                return;
            }
            cmbOperador.getSelectionModel().selectFirst();
            cmbOperador.setDisable(true);
        } catch (SecurityException e) {
            mostrarVacio(e.getMessage());
            return;
        } catch (PersistenciaException e) {
            lblEstado.setText("No se pudieron cargar los operadores: " + e.getMessage());
            return;
        }
        cmbOperador.valueProperty().addListener((observable, anterior, actual) -> refrescar());

        reloj = new Timeline(new KeyFrame(Duration.seconds(2), evento -> {
            if (!Navegacion.vistaVisible(btnSalir)) {
                reloj.stop();
                return;
            }
            refrescar();
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
        refrescar();
    }

    @FXML
    private void salir() {
        Navegacion.cerrarSesion(Navegacion.stageDe(btnSalir));
    }

    // ------------------------------------------------------------------ acciones

    @FXML
    private void refrescar() {
        if (SesionActual.getUsuario() == null) {
            idViaje = null;
            mostrarVacio("Debe iniciar sesión como operador.");
            return;
        }
        if (!SesionActual.activa()) {
            idViaje = null;
            mostrarVacio("La sesión expiró por inactividad.");
            Navegacion.sesionExpirada(Navegacion.stageDe(cmbOperador));
            return;
        }
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
            List<RegistroJornada> eventos = servicio.eventos(idViaje);
            JornadaService.Resumen resumen = JornadaService.calcular(
                    viaje.get(), eventos, LocalDateTime.now(JornadaService.ZONA));
            pintar(resumen, eventos);
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
        if (idViaje == null) {
            advertencia("No hay un viaje activo para este operador.");
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmar la entrega del viaje #" + idViaje + "?", ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.YES) {
            return;
        }
        ejecutar(servicio::confirmarEntrega);
    }

    /** Ejecuta una accion de la cabina y refresca la pantalla; el error se muestra al usuario. */
    private void ejecutar(IntFunction<JornadaService.Resumen> accion) {
        if (SesionActual.getUsuario() == null) {
            advertencia("Debe iniciar sesión como operador.");
            return;
        }
        if (!SesionActual.activa()) {
            Navegacion.sesionExpirada(Navegacion.stageDe(btnIniciar));
            return;
        }
        SesionActual.tocar();
        if (idViaje == null) {
            advertencia("No hay un viaje activo para este operador.");
            return;
        }
        try {
            accion.apply(idViaje);
            refrescar();
            lblEstado.setText("Evento registrado " + LocalTime.now().format(HORA_CORTA) + ".");
        } catch (PersistenciaException e) {
            Alert alerta = new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK);
            alerta.setHeaderText("No se pudo registrar el evento");
            alerta.showAndWait();
        }
    }

    // ------------------------------------------------------------------ pintado

    private void pintar(JornadaService.Resumen resumen, List<RegistroJornada> eventos) {
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
        if (resumen.paso() == JornadaService.Paso.EN_PAUSA) {
            if (resumen.puedeReanudar()) {
                lblMensaje.setText("Descanso mínimo cumplido. Puede reanudar el manejo.");
            } else {
                lblMensaje.setText("Descanso insuficiente: faltan " + resumen.descansoRestanteTexto()
                        + " (mínimo " + resumen.descansoMinimoTexto() + ") para reanudar.");
            }
        }
        mostrarSiguiente(viaje.idViaje());

        List<String> bitacora = eventos.stream()
                .map(this::lineaBitacora)
                .toList();
        lstBitacora.setItems(FXCollections.observableArrayList(bitacora));

        btnIniciar.setDisable(resumen.paso() != JornadaService.Paso.SIN_INICIAR);
        btnPausar.setDisable(resumen.paso() != JornadaService.Paso.EN_MARCHA);
        btnReanudar.setDisable(!(resumen.paso() == JornadaService.Paso.EN_PAUSA && resumen.puedeReanudar()));
        btnConfirmar.setDisable(resumen.paso() != JornadaService.Paso.EN_MARCHA);
    }

    private void mostrarSiguiente(int idViajeActual) {
        Optional<FlotaDAO.FilaViaje> siguiente = flotaDAO.viajeEncadenado(idViajeActual);
        if (siguiente.isEmpty()
                || "FINALIZADO".equals(siguiente.get().estado())
                || "CANCELADO".equals(siguiente.get().estado())) {
            lblSiguienteCarga.setText("Sin siguiente asignación");
            lblSiguienteRuta.setText("—");
            lblSiguienteSla.setText("—");
            return;
        }
        FlotaDAO.FilaViaje viaje = siguiente.get();
        lblSiguienteCarga.setText("Carga " + texto(viaje.numeroCarga()));
        lblSiguienteRuta.setText("Ruta: " + texto(viaje.origen()) + "  →  " + texto(viaje.destino()));
        lblSiguienteSla.setText("Límite del cliente: "
                + (viaje.fechaLimite() == null ? "—" : viaje.fechaLimite().format(FECHA))
                + "        Manejo estimado: " + horas(viaje.tiempoEstimado()));
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
        lblSiguienteCarga.setText("Sin siguiente asignación");
        lblSiguienteRuta.setText("—");
        lblSiguienteSla.setText("—");
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
