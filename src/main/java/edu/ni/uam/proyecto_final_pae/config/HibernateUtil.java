package edu.ni.uam.proyecto_final_pae.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class HibernateUtil {

    private static final SessionFactory FACTORY = crear();

    private HibernateUtil() {
    }

    private static SessionFactory crear() {
        try {
            Properties propiedades = cargarPropiedades();
            propiedades.setProperty("hibernate.connection.url", Entorno.requerido("DB_URL"));
            propiedades.setProperty("hibernate.connection.username", Entorno.requerido("DB_USER"));
            propiedades.setProperty("hibernate.connection.password", Entorno.requerido("DB_PASSWORD"));

            Configuration configuracion = new Configuration().addProperties(propiedades);
            // Aquí se registrarán las clases @Entity cuando se creen:
            // configuracion.addAnnotatedClass(MiEntidad.class);
            return configuracion.buildSessionFactory();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo iniciar Hibernate. Revise el archivo .env y que PostgreSQL este disponible.", e);
        }
    }

    private static Properties cargarPropiedades() throws IOException {
        try (InputStream archivo = HibernateUtil.class.getResourceAsStream("/hibernate.properties")) {
            if (archivo == null) {
                throw new IllegalStateException("Falta hibernate.properties en src/main/resources");
            }
            Properties propiedades = new Properties();
            propiedades.load(archivo);
            return propiedades;
        }
    }

    public static SessionFactory factory() {
        return FACTORY;
    }

    public static void cerrar() {
        FACTORY.close();
    }
}
