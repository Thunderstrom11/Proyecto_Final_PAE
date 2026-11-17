package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import edu.ni.uam.proyecto_final_pae.util.Tooltips;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Utilidades de navegación y cierre de sesión. */
public final class Navegacion {

    public static final double LOGIN_ANCHO = 460;
    public static final double LOGIN_ALTO = 620;
    private static final List<Stage> VENTANAS = new ArrayList<>();

    private Navegacion() {
    }

    /** Registra una ventana para poder cerrarla cuando termine la sesión. */
    public static void registrar(Stage stage) {
        if (stage == null) {
            return;
        }
        VENTANAS.remove(stage);
        VENTANAS.add(stage);
        stage.setOnHidden(evento -> VENTANAS.remove(stage));
    }

    private static void cerrarOtrasVentanas(Stage principal) {
        for (Stage ventana : new ArrayList<>(VENTANAS)) {
            if (ventana != principal) {
                ventana.close();
            }
        }
    }

    public static Stage stageDe(Node nodo) {
        if (nodo == null || nodo.getScene() == null || !(nodo.getScene().getWindow() instanceof Stage stage)) {
            return null;
        }
        return stage;
    }

    /** true si la vista del nodo sigue visible; los relojes periódicos se detienen cuando no. */
    public static boolean vistaVisible(Node nodo) {
        Stage stage = stageDe(nodo);
        return stage != null && stage.isShowing();
    }

    /** Si ya hay una ventana abierta con ese título, la trae al frente en vez de duplicarla. */
    public static boolean enfocarVentana(String titulo) {
        for (Stage ventana : VENTANAS) {
            if (ventana.isShowing() && titulo.equals(ventana.getTitle())) {
                ventana.toFront();
                ventana.requestFocus();
                return true;
            }
        }
        return false;
    }

    public static void abrirLogin(Stage stage) {
        abrirLogin(stage, null);
    }

    public static void abrirLogin(Stage stage, String mensaje) {
        if (stage == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    Navegacion.class.getResource("/edu/ni/uam/proyecto_final_pae/login-view.fxml"));
            Parent root = loader.load();
            if (mensaje != null) {
                LoginController controlador = loader.getController();
                controlador.mostrarMensaje(mensaje);
            }
            stage.setTitle("FleetRoute Pro — Iniciar sesión");
            Scene scene = new Scene(root, LOGIN_ANCHO, LOGIN_ALTO);
            Tooltips.instalar(root);
            stage.setScene(scene);
            stage.setMinWidth(LOGIN_ANCHO);
            stage.setMinHeight(LOGIN_ALTO);
            stage.centerOnScreen();
            registrar(stage);
            stage.show();
        } catch (IOException e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR,
                    "No se pudo abrir la pantalla de inicio de sesión.", ButtonType.OK);
            alerta.setHeaderText(null);
            alerta.showAndWait();
        }
    }

    public static void cerrarSesion(Stage stage) {
        SesionActual.cerrar();
        cerrarOtrasVentanas(stage);
        abrirLogin(stage);
    }

    public static void sesionExpirada(Stage stage) {
        if (SesionActual.getUsuario() == null) {
            return;
        }
        SesionActual.cerrar();
        cerrarOtrasVentanas(stage);
        Alert alerta = new Alert(Alert.AlertType.INFORMATION,
                "La sesión expiró por inactividad. Vuelva a iniciar sesión.", ButtonType.OK);
        alerta.setHeaderText("Sesión expirada");
        alerta.showAndWait();
        abrirLogin(stage);
    }
}
