package edu.ni.uam.proyecto_final_pae.config;

import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Logo de la empresa elegido en el asistente; se guarda junto al archivo .env. */
public final class MarcaEmpresa {

    private static final String CLAVE = "EMPRESA_LOGO";
    private static final String CARPETA = ".fleetroute";
    private static final List<String> EXTENSIONES = List.of(".png", ".jpg", ".jpeg", ".gif");
    private static final long PESO_MAXIMO = 4L * 1024 * 1024;

    private MarcaEmpresa() {
    }

    /** Ruta del logo guardado, si todavia existe. */
    public static Optional<Path> ruta() {
        String valor = Entorno.opcional(CLAVE);
        if (valor == null) {
            return Optional.empty();
        }
        Path ruta = Path.of(valor);
        if (!ruta.isAbsolute()) {
            Path base = carpetaBase();
            if (base == null) {
                return Optional.empty();
            }
            ruta = base.resolve(ruta);
        }
        return Files.isRegularFile(ruta) ? Optional.of(ruta) : Optional.empty();
    }

    /** Logo guardado listo para mostrar, escalado al alto indicado. */
    public static Optional<Image> imagen(double alto) {
        Optional<Path> archivo = ruta();
        if (archivo.isEmpty()) {
            return Optional.empty();
        }
        Image imagen = leer(archivo.get(), alto);
        return imagen == null ? Optional.empty() : Optional.of(imagen);
    }

    /** Lee una imagen del disco sin copiarla. Devuelve el logo escalado al alto indicado. */
    public static Image leer(Path archivo, double alto) {
        validar(archivo);
        Image imagen = new Image(archivo.toUri().toString(), 0, alto, true, true, false);
        if (imagen.isError()) {
            throw new IllegalArgumentException("El archivo no es una imagen que se pueda mostrar. Use PNG, JPG o GIF.");
        }
        return imagen;
    }

    /** Revisa que el archivo sirva como logo antes de copiarlo. */
    public static void validar(Path origen) {
        if (origen == null || !Files.isRegularFile(origen)) {
            throw new IllegalArgumentException("Elija un archivo de imagen existente.");
        }
        if (!EXTENSIONES.contains(extension(origen))) {
            throw new IllegalArgumentException("El logo debe ser una imagen PNG, JPG o GIF.");
        }
        try {
            if (Files.size(origen) > PESO_MAXIMO) {
                throw new IllegalArgumentException("La imagen pesa más de 4 MB. Use una versión más liviana.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer la imagen elegida.", e);
        }
    }

    /** Copia la imagen junto al .env y la registra en EMPRESA_LOGO. */
    public static void guardar(Path origen) {
        validar(origen);
        Path carpeta = carpetaDestino();
        try {
            Files.createDirectories(carpeta);
            borrarLogos(carpeta);
            Path destino = carpeta.resolve("logo" + extension(origen));
            Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            Entorno.guardar(Map.of(CLAVE, CARPETA + "/" + destino.getFileName()));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo copiar el logo junto al archivo .env.", e);
        }
    }

    /** Borra el logo guardado y deja el login con el nombre de la empresa. */
    public static void quitar() {
        borrarLogos(carpetaDestino());
        Entorno.guardar(Map.of(CLAVE, ""));
    }

    // ------------------------------------------------------------------ apoyo

    private static Path carpetaBase() {
        Path archivo = Entorno.ubicar();
        if (archivo != null) {
            return archivo.getParent();
        }
        return Path.of(System.getProperty("user.dir")).toAbsolutePath();
    }

    private static Path carpetaDestino() {
        return carpetaBase().resolve(CARPETA);
    }

    private static String extension(Path archivo) {
        String nombre = archivo.getFileName().toString();
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto).toLowerCase(Locale.ROOT);
    }

    private static void borrarLogos(Path carpeta) {
        if (!Files.isDirectory(carpeta)) {
            return;
        }
        try (var archivos = Files.list(carpeta)) {
            for (Path archivo : archivos.toList()) {
                String nombre = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
                if (nombre.startsWith("logo.")) {
                    Files.deleteIfExists(archivo);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo limpiar el logo anterior.", e);
        }
    }
}
