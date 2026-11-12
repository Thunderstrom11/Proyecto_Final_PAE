package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UsuarioDAO;
import edu.ni.uam.proyecto_final_pae.model.Empresa;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;

/** Asistente de primer arranque: perfil de la empresa y cuenta de administrador. */
public class SetupController {

    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML private TextField txtNombre;
    @FXML private TextField txtRuc;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtDireccion;

    @FXML private VBox panelAdmin;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmar;

    @FXML private Button btnGuardar;
    @FXML private Label lblMensaje;

    private boolean crearAdmin;

    @FXML
    private void initialize() {
        try {
            crearAdmin = !usuarioDAO.existeAdministrador();
        } catch (PersistenciaException e) {
            lblMensaje.setText("Sin conexión a la base de datos: " + e.getMessage());
            btnGuardar.setDisable(true);
            return;
        }
        panelAdmin.setVisible(crearAdmin);
        panelAdmin.setManaged(crearAdmin);
    }

    @FXML
    private void guardar() {
        try {
            Empresa empresa = construirEmpresa();
            Usuario admin = crearAdmin ? construirAdmin() : null;
            Transaccion.ejecutar(sesion -> {
                sesion.persist(empresa);
                if (admin != null) {
                    sesion.persist(admin);
                }
                return null;
            });
            Navegacion.abrirLogin(Navegacion.stageDe(btnGuardar),
                    "Configuración guardada. Inicie sesión con su cuenta.");
        } catch (IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
        } catch (PersistenciaException e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alerta.setHeaderText("No se pudo guardar la configuración");
            alerta.showAndWait();
        }
    }

    private Empresa construirEmpresa() {
        Empresa empresa = new Empresa();
        empresa.setNombre(requerido(txtNombre, "El nombre de la empresa", 150));
        empresa.setRuc(opcional(txtRuc, "El RUC", 20));
        empresa.setTelefono(opcional(txtTelefono, "El teléfono", 30));
        empresa.setEmail(opcional(txtEmail, "El correo", 120));
        empresa.setDireccion(opcional(txtDireccion, "La dirección", 200));
        if (empresa.getEmail() != null && !empresa.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("El correo de la empresa no tiene un formato válido.");
        }
        return empresa;
    }

    private Usuario construirAdmin() {
        String username = requerido(txtUsuario, "El usuario administrador", 50);
        if (username.contains(" ")) {
            throw new IllegalArgumentException("El usuario no puede contener espacios.");
        }
        if (usuarioDAO.buscarPorUsername(username) != null) {
            throw new IllegalArgumentException("Ese usuario ya existe; elija otro nombre.");
        }
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();
        String confirmar = txtConfirmar.getText() == null ? "" : txtConfirmar.getText();
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (!password.equals(confirmar)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        Usuario admin = new Usuario();
        admin.setUsername(username);
        admin.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        admin.setRol(RolUsuario.ADMIN);
        admin.setEsPrincipal(true);
        admin.setPuedeVerReportes(true);
        return admin;
    }

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

    private String opcional(TextField campo, String nombre, int maximo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(nombre + " no puede pasar de " + maximo + " caracteres.");
        }
        return valor.isEmpty() ? null : valor;
    }
}