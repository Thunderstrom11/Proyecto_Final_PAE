package edu.ni.uam.proyecto_final_pae;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ViajesApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                ViajesApplication.class.getResource("viajes-view.fxml"));
        stage.setTitle("FleetRoute Pro — Viajes");
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