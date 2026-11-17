package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.FlotaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UsuarioDAO;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

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

    @FXML private Label lblOperadorSesion;
    @FXML private Label lblEstado;
    @FXML private Label lblActualizado;
    @FXML private Label lblViaje;
    @FXML private Label lblRuta;
    @FXML private Label lblSla;
    @FXML private Label lblManejo;
    @FXML private Label lblDescanso;
    @FXML private Label lblPaso;
    @FXML private Label lblSemaforo;
    @FXML private Label lblMensaje;
    @FXML private Label lblSugerencia;
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
    private FlotaDAO.Opcion operadorSesion;
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
            if (opciones.isEmpty()) {
                mostrarVacio("El operador de la sesión no existe en la base de datos.");
                return;
            }
            operadorSesion = opciones.get(0);
            lblOperadorSesion.setText(operadorSesion.nombre());
        } catch (SecurityException e) {
            mostrarVacio(e.getMessage());
            return;
        } catch (PersistenciaException e) {
            lblEstado.setText("No se pudieron cargar los operadores: " + e.getMessage());
            return;
        }

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

    @FXML
    private void cambiarClave() {
        Usuario usuario;
        try {
            usuario = SesionActual.requerir(RolUsuario.OPERADOR);
        } catch (SecurityException e) {
            advertencia(e.getMessage());
            return;
        }
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Cambiar contraseña");
        dialogo.setHeaderText("Cuenta " + usuario.getUsername());
        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        PasswordField actual = new PasswordField();
        actual.setPromptText("Contraseña actual");
        PasswordField nueva = new PasswordField();
        nueva.setPromptText("Nueva contraseña, mínimo 8");
        PasswordField confirmar = new PasswordField();
        confirmar.setPromptText("Repite la nueva");

        GridPane panel = new GridPane();
        panel.setHgap(8);
        panel.setVgap(8);
        panel.add(new Label("Actual"), 0, 0);
        panel.add(actual, 1, 0);
        panel.add(new Label("Nueva"), 0, 1);
        panel.add(nueva, 1, 1);
        panel.add(new Label("Confirmar"), 0, 2);
        panel.add(confirmar, 1, 2);
        dialogo.getDialogPane().setContent(panel);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != guardar) {
            return;
        }
        String claveActual = actual.getText() == null ? "" : actual.getText();
        String claveNueva = nueva.getText() == null ? "" : nueva.getText();
        String claveConfirmar = confirmar.getText() == null ? "" : confirmar.getText();
        try {
            if (!BCrypt.checkpw(claveActual, usuario.getPasswordHash())) {
                advertencia("La contraseña actual no es correcta.");
                return;
            }
            if (claveNueva.length() < 8) {
                advertencia("La nueva contraseña debe tener al menos 8 caracteres.");
                return;
            }
            if (!claveNueva.equals(claveConfirmar)) {
                advertencia("Las contraseñas nuevas no coinciden.");
                return;
            }
            new UsuarioDAO().restablecerPassword(usuario, claveNueva);
            advertencia("Contraseña actualizada.");
        } catch (PersistenciaException e) {
            advertencia("No se pudo cambiar la contraseña. " + e.getMessage());
        }
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
            Navegacion.sesionExpirada(Navegacion.stageDe(btnIniciar));
            return;
        }
        FlotaDAO.Opcion operador = operadorSesion;
        if (operador == null) {
            idViaje = null;
            mostrarVacio("El operador de la sesión no está disponible.");
            return;
        }
        lblActualizado.setText("Actualizado " + LocalTime.now().format(HORA_CORTA));
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
                    viaje.get(), eventos, LocalDateTime.now(JornadaService.zona()));
            boolean finJornada = resumen.paso() == JornadaService.Paso.EN_MARCHA
                    && resumen.porcentajeManejo() >= 100;
            if (finJornada) {
                resumen = servicio.pausarPorFinJornada(idViaje);
                eventos = servicio.eventos(idViaje);
                lblEstado.setText("Jornada máxima completada: pausa y descanso automáticos.");
            }
            pintar(resumen, eventos);
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
        ButtonType confirmar = new ButtonType("Confirmar entrega", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmar la entrega del viaje #" + idViaje + "?", confirmar, cancelar);
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != confirmar) {
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
        lblDescanso.setText(resumen.descansoRestanteTexto());
        lblPaso.setText(resumen.paso().getEtiqueta());

        lblSemaforo.setText(resumen.semaforo().getEtiqueta());
        lblSemaforo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; "
                + "-fx-padding: 8 14 8 14; -fx-background-radius: 10; -fx-background-color: "
                + resumen.semaforo().getColor() + ";");
        lblMensaje.setText(resumen.mensaje());
        if (resumen.paso() == JornadaService.Paso.EN_PAUSA) {
            if (resumen.porcentajeManejo() >= 100) {
                lblMensaje.setText("Jornada máxima completada: pausa automática. Descanso restante "
                        + resumen.descansoRestanteTexto() + "; se sigue contando mientras la pausa esté activa.");
            } else {
                lblMensaje.setText("Pausa activa: descanso restante " + resumen.descansoRestanteTexto()
                        + ". Puede reanudar cuando lo necesite; el descanso se sigue contando.");
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

        switch (resumen.paso()) {
            case SIN_INICIAR -> lblSugerencia.setText(
                    "Pulse «Iniciar trayecto» para comenzar el viaje.");
            case EN_MARCHA -> lblSugerencia.setText(
                    "Puede pausar el trayecto o confirmar la entrega.");
            case EN_PAUSA -> lblSugerencia.setText(resumen.puedeReanudar()
                    ? "Puede reanudar el trayecto cuando lo necesite."
                    : "Espere a completar el descanso mínimo para reanudar.");
            case FINALIZADO -> lblSugerencia.setText("El viaje ya fue entregado.");
        }
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
        lblSugerencia.setText("Las acciones se habilitarán cuando el operador tenga un viaje activo.");
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
