module edu.ni.uam.proyecto_final_pae {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires java.naming;
    requires org.slf4j;
    requires org.slf4j.simple;

    opens edu.ni.uam.proyecto_final_pae to javafx.fxml;
    // Al agregar entidades, abra su paquete para Hibernate:
    // opens edu.ni.uam.proyecto_final_pae.model to org.hibernate.orm.core;
    exports edu.ni.uam.proyecto_final_pae;
}
