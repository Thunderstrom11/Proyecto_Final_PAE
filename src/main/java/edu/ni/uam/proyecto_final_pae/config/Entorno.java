package edu.ni.uam.proyecto_final_pae.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Lee las credenciales desde el archivo .env ubicado en la raiz del proyecto.
final class Entorno {

    private static Map<String, String> VALORES = cargar();

    private Entorno() {
    }

    static String requerido(String clave) {
        String valor = opcional(clave);
        if (valor == null) {
            throw new IllegalStateException("Falta la variable " + clave + " en el archivo .env");
        }
        return valor;
    }

    static String opcional(String clave) {
        String valor = VALORES.get(clave);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(clave);
        }
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    static synchronized void recargar() {
        VALORES = cargar();
    }

    /** Guarda las claves en el .env existente (o lo crea) conservando las demas lineas. */
    static synchronized void guardar(Map<String, String> nuevos) {
        Path archivo = ubicar();
        if (archivo == null) {
            archivo = Path.of(System.getProperty("user.dir")).toAbsolutePath().resolve(".env");
        }
        List<String> lineas = new ArrayList<>();
        if (Files.isRegularFile(archivo)) {
            try {
                lineas.addAll(Files.readAllLines(archivo, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el archivo .env", e);
            }
        }
        for (Map.Entry<String, String> entrada : nuevos.entrySet()) {
            String prefijo = entrada.getKey() + "=";
            boolean reemplazada = false;
            for (int i = 0; i < lineas.size(); i++) {
                String limpia = lineas.get(i).strip();
                if (limpia.startsWith(prefijo) || limpia.startsWith("export " + prefijo)) {
                    lineas.set(i, prefijo + entrada.getValue());
                    reemplazada = true;
                    break;
                }
            }
            if (!reemplazada) {
                lineas.add(prefijo + entrada.getValue());
            }
        }
        try {
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo .env en " + archivo, e);
        }
        recargar();
    }

    private static Map<String, String> cargar() {
        Map<String, String> valores = new HashMap<>();
        Path archivo = ubicar();
        if (archivo == null) {
            return valores;
        }

        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String limpia = linea.strip();
                if (limpia.startsWith("\uFEFF")) {
                    limpia = limpia.substring(1);
                }
                if (limpia.isEmpty() || limpia.startsWith("#")) {
                    continue;
                }
                if (limpia.startsWith("export ")) {
                    limpia = limpia.substring(7).strip();
                }

                int separador = limpia.indexOf('=');
                if (separador <= 0) {
                    continue;
                }

                String clave = limpia.substring(0, separador).strip();
                String valor = limpia.substring(separador + 1).strip();
                if (valor.length() >= 2
                        && ((valor.startsWith("\"") && valor.endsWith("\""))
                        || (valor.startsWith("'") && valor.endsWith("'")))) {
                    valor = valor.substring(1, valor.length() - 1);
                }
                valores.put(clave, valor);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo .env", e);
        }
        return valores;
    }

    /** Archivo .env en uso, o null si todavia no existe. */
    static Path ubicar() {
        Path directorio = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int nivel = 0; nivel < 4 && directorio != null; nivel++, directorio = directorio.getParent()) {
            Path archivo = directorio.resolve(".env");
            if (Files.isRegularFile(archivo)) {
                return archivo;
            }
        }
        return null;
    }
}
