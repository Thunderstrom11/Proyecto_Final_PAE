package edu.ni.uam.proyecto_final_pae;

import edu.ni.uam.proyecto_final_pae.config.HibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Arranque independiente del modulo de jornada (cabina del operador + monitor de flota),
 * para probarlo sin depender del login ni del menu principal de otras personas.
 */
public class JornadaApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        TabPane pestanas = new TabPane();
        pestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pestanas.getTabs().add(new Tab("Cabina del operador", cargar("cabina-view.fxml")));
        pestanas.getTabs().add(new Tab("Monitor de flota", cargar("monitor-view.fxml")));

        stage.setTitle("FleetRoute Pro - Jornada del operador y monitor de flota");
        stage.setScene(new Scene(pestanas, 1080, 700));
        stage.show();
    }

    private Parent cargar(String archivo) throws IOException {
        return new FXMLLoader(JornadaApplication.class.getResource(archivo)).load();
    }

    @Override
    public void stop() {
        HibernateUtil.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
