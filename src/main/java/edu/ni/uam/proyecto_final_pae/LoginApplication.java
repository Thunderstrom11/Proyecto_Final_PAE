package edu.ni.uam.proyecto_final_pae;

import edu.ni.uam.proyecto_final_pae.config.HibernateUtil;
import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.controller.Navegacion;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.util.Tooltips;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        String vista = "login-view.fxml";
        String titulo = "Iniciar sesión";
        double ancho = Navegacion.LOGIN_ANCHO;
        double alto = Navegacion.LOGIN_ALTO;
        try {
            if (!new EmpresaDAO().configurada()) {
                vista = "setup-view.fxml";
                titulo = "Configuración inicial";
                ancho = 580;
                alto = 700;
            }
        } catch (PersistenciaException e) {
            // Sin base de datos: el login muestra el error de conexión.
        }
        FXMLLoader fxmlLoader = new FXMLLoader(LoginApplication.class.getResource(vista));
        Scene scene = new Scene(fxmlLoader.load(), ancho, alto);
        Tooltips.instalar(scene.getRoot());
        stage.setTitle("FleetRoute Pro — " + titulo);
        stage.setScene(scene);
        stage.setMinWidth(Math.min(ancho, 560));
        stage.setMinHeight(620);
        stage.show();
    }

    @Override
    public void stop() {
        HibernateUtil.cerrar();
    }
}
