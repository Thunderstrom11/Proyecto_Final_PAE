package edu.ni.uam.proyecto_final_pae;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** Arranque independiente de la pantalla de cargas (para probarla sin depender del menu principal). */
public class CargasApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(CargasApplication.class.getResource("cargas-view.fxml"));
        stage.setTitle("FleetRoute Pro - Cargas");
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }

    @Override
    public void stop() {
        edu.ni.uam.proyecto_final_pae.config.HibernateUtil.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
