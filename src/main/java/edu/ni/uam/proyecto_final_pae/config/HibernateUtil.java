package edu.ni.uam.proyecto_final_pae.config;

import jakarta.persistence.Entity;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class HibernateUtil {

    /** Paquete donde viven las clases @Entity. Se detectan solas: nadie tiene que registrarlas aqui. */
    private static final String PAQUETE_MODELO = "edu.ni.uam.proyecto_final_pae.model";

    private static SessionFactory factory;

    private HibernateUtil() {
    }

    private static SessionFactory crear() {
        try {
            Properties propiedades = cargarPropiedades();
            propiedades.setProperty("hibernate.connection.url", Entorno.requerido("DB_URL"));
            propiedades.setProperty("hibernate.connection.username", Entorno.requerido("DB_USER"));
            propiedades.setProperty("hibernate.connection.password", Entorno.requerido("DB_PASSWORD"));

            Configuration configuracion = new Configuration().addProperties(propiedades);
            for (Class<?> entidad : detectarEntidades()) {
                configuracion.addAnnotatedClass(entidad);
            }
            SessionFactory fabrica = configuracion.buildSessionFactory();
            DatosIniciales.sembrar(fabrica);
            return fabrica;
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

    // ---------------------------------------------------------------- deteccion de entidades

    private static List<Class<?>> detectarEntidades() throws IOException {
        ClassLoader cargador = Thread.currentThread().getContextClassLoader();
        String ruta = PAQUETE_MODELO.replace('.', '/');
        List<String> nombres = new ArrayList<>();

        Enumeration<URL> recursos = cargador.getResources(ruta);
        while (recursos.hasMoreElements()) {
            URL url = recursos.nextElement();
            if ("file".equals(url.getProtocol())) {
                File carpeta = new File(java.net.URLDecoder.decode(url.getPath(), java.nio.charset.StandardCharsets.UTF_8));
                File[] archivos = carpeta.listFiles((dir, nombre) -> nombre.endsWith(".class"));
                if (archivos != null) {
                    for (File archivo : archivos) {
                        String simple = archivo.getName();
                        nombres.add(PAQUETE_MODELO + "." + simple.substring(0, simple.length() - 6));
                    }
                }
            } else if ("jar".equals(url.getProtocol())) {
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> entradas = jar.entries();
                while (entradas.hasMoreElements()) {
                    String nombre = entradas.nextElement().getName();
                    if (nombre.startsWith(ruta + "/") && nombre.endsWith(".class")
                            && nombre.indexOf('/', ruta.length() + 1) < 0) {
                        nombres.add(nombre.substring(0, nombre.length() - 6).replace('/', '.'));
                    }
                }
            }
        }

        List<Class<?>> entidades = new ArrayList<>();
        for (String nombre : nombres) {
            try {
                Class<?> clase = Class.forName(nombre, false, cargador);
                if (clase.isAnnotationPresent(Entity.class)) {
                    entidades.add(clase);
                }
            } catch (ClassNotFoundException ignorada) {
                // clase no cargable: se omite
            }
        }
        return entidades;
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
