module edu.ni.uam.proyecto_final_pae {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.ni.uam.proyecto_final_pae to javafx.fxml;
    exports edu.ni.uam.proyecto_final_pae;
}