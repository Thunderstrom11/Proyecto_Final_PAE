package edu.ni.uam.proyecto_final_pae.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Lee las credenciales desde el archivo .env ubicado en la raiz del proyecto.
final class Entorno {

    private static final Map<String, String> VALORES = cargar();

    private Entorno() {
    }

    static String requerido(String clave) {
        String valor = VALORES.get(clave);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(clave);
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable " + clave + " en el archivo .env");
        }
        return valor.trim();
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

    private static Path ubicar() {
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
