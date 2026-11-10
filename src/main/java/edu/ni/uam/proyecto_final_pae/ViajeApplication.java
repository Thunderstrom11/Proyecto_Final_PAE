package edu.ni.uam.proyecto_final_pae;

import edu.ni.uam.proyecto_final_pae.config.HibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** Arranque independiente de la pantalla de viajes. */
public class ViajeApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(ViajeApplication.class.getResource("viajes-view.fxml"));
        stage.setTitle("FleetRoute Pro — Viajes");
        stage.setScene(new Scene(loader.load(), 980, 700));
        stage.show();
    }

    @Override
    public void stop() {
        HibernateUtil.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
