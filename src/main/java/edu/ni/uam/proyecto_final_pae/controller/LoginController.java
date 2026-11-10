package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
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
import javafx.stage.Stage;

import java.io.IOException;

/** Login con BCrypt y control de acceso por rol (RF-11). */
public class LoginController {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnIngresar;
    @FXML private Label lblMensaje;

    @FXML
    private void initialize() {
        btnIngresar.disableProperty().bind(Bindings.createBooleanBinding(
                () -> txtUsuario.getText() == null || txtUsuario.getText().isBlank()
                        || txtPassword.getText() == null || txtPassword.getText().isEmpty(),
                txtUsuario.textProperty(), txtPassword.textProperty()));

        try {
            usuarioDAO.asegurarAdminDemo();
        } catch (PersistenciaException e) {
            lblMensaje.setText("Sin conexión a la base de datos: " + e.getMessage());
        }
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
        } catch (IOException e) {
            lblMensaje.setText("No se pudo abrir la pantalla " + archivo + ".");
        }
    }
}
