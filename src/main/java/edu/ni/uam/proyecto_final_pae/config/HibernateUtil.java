package edu.ni.uam.proyecto_final_pae.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/** Carga hibernate.cfg.xml y sobrescribe la conexion con las credenciales del .env. */
public final class HibernateUtil {

    private static SessionFactory factory;

    private HibernateUtil() {
    }

    private static SessionFactory crear() {
        try {
            Configuration configuracion = new Configuration().configure();
            configuracion.setProperty("hibernate.connection.url", Entorno.requerido("DB_URL"));
            configuracion.setProperty("hibernate.connection.username", Entorno.requerido("DB_USER"));
            configuracion.setProperty("hibernate.connection.password", Entorno.requerido("DB_PASSWORD"));
            SessionFactory fabrica = configuracion.buildSessionFactory();
            DatosIniciales.sembrar(fabrica);
            return fabrica;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo iniciar Hibernate. Revise hibernate.cfg.xml, el archivo .env y que PostgreSQL este disponible.", e);
        }
    }

    public static synchronized SessionFactory factory() {
        if (factory == null) {
            try {
                factory = crear();
            } catch (RuntimeException e) {
                throw new PersistenciaException(
                        "No se pudo iniciar la conexión con PostgreSQL. Revise el archivo .env y el servidor.", e);
            }
        }
        return factory;
    }

    public static synchronized void cerrar() {
        if (factory != null) {
            factory.close();
            factory = null;
        }
    }
}