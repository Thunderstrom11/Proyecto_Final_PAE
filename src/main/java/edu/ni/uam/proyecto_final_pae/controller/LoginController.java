package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.MarcaEmpresa;
import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UsuarioDAO;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import edu.ni.uam.proyecto_final_pae.util.Tooltips;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/** Login con BCrypt, selección de rol y control de acceso (RF-11). */
public class LoginController {

    private static final Color COLOR_ERROR = Color.web("#B42318");
    private static final Color COLOR_AVISO = Color.web("#B45309");
    private static final Color COLOR_INFO = Color.web("#0E7490");

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final SimpleBooleanProperty bloqueado = new SimpleBooleanProperty(false);
    private Timeline cuentaAtras;
    private int intentosFallidos;
    private int bloqueos;

    @FXML private ToggleButton tglAdmin;
    @FXML private ToggleButton tglOperador;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnIngresar;
    @FXML private Label lblMensaje;
    @FXML private Label lblEmpresa;
    @FXML private ImageView imgLogo;

    @FXML
    private void initialize() {
        ToggleGroup grupo = new ToggleGroup();
        tglAdmin.setToggleGroup(grupo);
        tglOperador.setToggleGroup(grupo);
        tglAdmin.setSelected(true);

        BooleanBinding camposVacios = Bindings.createBooleanBinding(
                () -> txtUsuario.getText() == null || txtUsuario.getText().isBlank()
                        || txtPassword.getText() == null || txtPassword.getText().isEmpty(),
                txtUsuario.textProperty(), txtPassword.textProperty());
        btnIngresar.disableProperty().bind(camposVacios.or(bloqueado));

        txtUsuario.textProperty().addListener((o, a, b) -> limpiarMensaje());
        txtPassword.textProperty().addListener((o, a, b) -> {
            limpiarMensaje();
            avisarMayusculas();
        });
        grupo.selectedToggleProperty().addListener((o, a, b) -> limpiarMensaje());
        txtPassword.focusedProperty().addListener((o, a, enfocado) -> {
            if (enfocado) {
                avisarMayusculas();
            }
        });

        MarcaEmpresa.imagen(86).ifPresent(imagen -> {
            imgLogo.setImage(imagen);
            imgLogo.setVisible(true);
            imgLogo.setManaged(true);
        });
        try {
            empresaDAO.obtener().ifPresent(empresa -> lblEmpresa.setText(empresa.getNombre()));
        } catch (PersistenciaException e) {
            lblMensaje.setTextFill(COLOR_ERROR);
            lblMensaje.setText("Sin conexión a la base de datos: " + e.getMessage());
        }
    }

    /** Mensaje informativo (por ejemplo, al llegar desde el asistente de configuración). */
    public void mostrarMensaje(String mensaje) {
        lblMensaje.setTextFill(COLOR_INFO);
        lblMensaje.setText(mensaje);
    }

    /** Enter en el campo de usuario pasa el foco a la contraseña. */
    @FXML
    private void siguienteCampo() {
        txtPassword.requestFocus();
    }

    private void limpiarMensaje() {
        if (bloqueado.get()) {
            return;
        }
        String texto = lblMensaje.getText();
        if (texto == null || texto.isBlank() || texto.startsWith("Demasiados intentos")) {
            return;
        }
        lblMensaje.setText("");
        lblMensaje.setTextFill(COLOR_ERROR);
    }

    private void avisarMayusculas() {
        if (bloqueado.get() || !mayusculasActivas()) {
            return;
        }
        lblMensaje.setTextFill(COLOR_AVISO);
        lblMensaje.setText("Bloq Mayús está activado.");
    }

    private boolean mayusculasActivas() {
        try {
            return java.awt.Toolkit.getDefaultToolkit()
                    .getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @FXML
    private void ingresar() {
        if (bloqueado.get()) {
            return;
        }
        String username = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblMensaje.setTextFill(COLOR_ERROR);
            lblMensaje.setText("Escriba el usuario y la contraseña.");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(username, password);
            if (usuario == null) {
                registrarFallo("Usuario o contraseña incorrectos.");
                return;
            }
            RolUsuario rolSeleccionado = tglOperador.isSelected() ? RolUsuario.OPERADOR : RolUsuario.ADMIN;
            if (usuario.getRol() != rolSeleccionado) {
                registrarFallo("Ese usuario es "
                        + (usuario.getRol() == RolUsuario.ADMIN ? "administrador" : "operador")
                        + "; seleccione la opción correspondiente.");
                return;
            }
            if (usuario.getRol() == RolUsuario.OPERADOR && usuario.getOperador() == null) {
                registrarFallo("El usuario operador no tiene un operador asociado.");
                return;
            }

            intentosFallidos = 0;
            bloqueos = 0;
            SesionActual.iniciar(usuario);
            edu.ni.uam.proyecto_final_pae.config.ZonaHoraria.recargar();
            if (usuario.getRol() == RolUsuario.ADMIN) {
                abrirVista("flota-view.fxml", "FleetRoute Pro — Administración de flota");
            } else {
                abrirVista("cabina-view.fxml", "FleetRoute Pro — Cabina del operador");
            }
        } catch (PersistenciaException e) {
            lblMensaje.setTextFill(COLOR_ERROR);
            lblMensaje.setText("No se pudo iniciar sesión: " + e.getMessage());
        }
    }

    private void registrarFallo(String mensaje) {
        lblMensaje.setTextFill(COLOR_ERROR);
        intentosFallidos++;
        if (intentosFallidos < 3) {
            lblMensaje.setText(mensaje + " Intento " + intentosFallidos + " de 3.");
            return;
        }
        if (bloqueos == 0) {
            iniciarBloqueo();
        } else {
            lblMensaje.setText("Límite de intentos alcanzado. La aplicación se cerrará.");
            cerrarPorIntentos();
        }
    }

    private void iniciarBloqueo() {
        bloqueos++;
        bloqueado.set(true);
        txtUsuario.setDisable(true);
        txtPassword.setDisable(true);
        tglAdmin.setDisable(true);
        tglOperador.setDisable(true);

        int[] restantes = {30};
        lblMensaje.setTextFill(COLOR_AVISO);
        lblMensaje.setText("Demasiados intentos fallidos. Espere 30 segundos.");
        cuentaAtras = new Timeline(new KeyFrame(Duration.seconds(1), evento -> {
            restantes[0]--;
            if (restantes[0] > 0) {
                lblMensaje.setText("Demasiados intentos fallidos. Espere " + restantes[0] + " segundos.");
                return;
            }
            cuentaAtras.stop();
            intentosFallidos = 0;
            bloqueado.set(false);
            txtUsuario.setDisable(false);
            txtPassword.setDisable(false);
            tglAdmin.setDisable(false);
            tglOperador.setDisable(false);
            lblMensaje.setTextFill(COLOR_INFO);
            lblMensaje.setText("Puede intentar nuevamente.");
        }));
        cuentaAtras.setCycleCount(Timeline.INDEFINITE);
        cuentaAtras.play();
    }

    private void cerrarPorIntentos() {
        if (cuentaAtras != null) {
            cuentaAtras.stop();
        }
        Alert alerta = new Alert(Alert.AlertType.ERROR,
                "Se alcanzó el límite de intentos fallidos. La aplicación se cerrará.",
                ButtonType.OK);
        alerta.setHeaderText("Acceso bloqueado");
        alerta.showAndWait();
        Stage stage = (Stage) txtUsuario.getScene().getWindow();
        stage.close();
        Platform.exit();
    }

    private void abrirVista(String archivo, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edu/ni/uam/proyecto_final_pae/" + archivo));
            Parent root = loader.load();
            Tooltips.instalar(root);
            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(new Scene(root, 1180, 760));
            stage.setTitle(titulo);
            stage.setMinWidth(980);
            stage.setMinHeight(680);
            stage.setFullScreen(false);
            stage.setMaximized(true);
            Navegacion.registrar(stage);
        } catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("No se pudo abrir la pantalla " + archivo + ".");
        }
    }
}