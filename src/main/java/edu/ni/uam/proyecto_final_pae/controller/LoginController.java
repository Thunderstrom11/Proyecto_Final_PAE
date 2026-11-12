package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UsuarioDAO;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

/** Login con BCrypt, selección de rol y control de acceso (RF-11). */
public class LoginController {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();

    @FXML private ToggleButton tglAdmin;
    @FXML private ToggleButton tglOperador;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnIngresar;
    @FXML private Label lblMensaje;
    @FXML private Label lblEmpresa;

    @FXML
    private void initialize() {
        ToggleGroup grupo = new ToggleGroup();
        tglAdmin.setToggleGroup(grupo);
        tglOperador.setToggleGroup(grupo);
        tglAdmin.setSelected(true);

        btnIngresar.disableProperty().bind(Bindings.createBooleanBinding(
                () -> txtUsuario.getText() == null || txtUsuario.getText().isBlank()
                        || txtPassword.getText() == null || txtPassword.getText().isEmpty(),
                txtUsuario.textProperty(), txtPassword.textProperty()));

        try {
            empresaDAO.obtener().ifPresent(empresa -> lblEmpresa.setText(empresa.getNombre()));
        } catch (PersistenciaException e) {
            lblMensaje.setText("Sin conexión a la base de datos: " + e.getMessage());
        }
    }

    /** Mensaje informativo (por ejemplo, al llegar desde el asistente de configuración). */
    public void mostrarMensaje(String mensaje) {
        lblMensaje.setTextFill(Color.web("#0E7490"));
        lblMensaje.setText(mensaje);
    }

    @FXML
    private void ingresar() {
        String username = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Escriba el usuario y la contraseña.");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(username, password);
            if (usuario == null) {
                lblMensaje.setText("Usuario o contraseña incorrectos.");
                return;
            }
            RolUsuario rolSeleccionado = tglOperador.isSelected() ? RolUsuario.OPERADOR : RolUsuario.ADMIN;
            if (usuario.getRol() != rolSeleccionado) {
                lblMensaje.setText("Ese usuario es "
                        + (usuario.getRol() == RolUsuario.ADMIN ? "administrador" : "operador")
                        + "; seleccione la opción correspondiente.");
                return;
            }
            if (usuario.getRol() == RolUsuario.OPERADOR && usuario.getOperador() == null) {
                lblMensaje.setText("El usuario operador no tiene un operador asociado.");
                return;
            }

            SesionActual.iniciar(usuario);
            if (usuario.getRol() == RolUsuario.ADMIN) {
                abrirVista("flota-view.fxml", "FleetRoute Pro — Administración de flota");
            } else {
                abrirVista("cabina-view.fxml", "FleetRoute Pro — Cabina del operador");
            }
        } catch (PersistenciaException e) {
            lblMensaje.setText("No se pudo iniciar sesión: " + e.getMessage());
        }
    }

    private void abrirVista(String archivo, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edu/ni/uam/proyecto_final_pae/" + archivo));
            Parent root = loader.load();
            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(new Scene(root, 980, 700));
            stage.setTitle(titulo);
            stage.centerOnScreen();
        } catch (IOException e) {
            lblMensaje.setText("No se pudo abrir la pantalla " + archivo + ".");
        }
    }
}