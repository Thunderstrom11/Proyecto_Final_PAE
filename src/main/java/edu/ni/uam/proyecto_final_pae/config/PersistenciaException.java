package edu.ni.uam.proyecto_final_pae.config;

import java.util.Locale;

/** Error de persistencia con un mensaje legible para mostrar en la interfaz. */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /** Construye el mensaje a partir de la causa raiz (por ejemplo, el texto de un trigger o CHECK de PostgreSQL). */
    public static PersistenciaException de(Throwable error) {
        Throwable raiz = error;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String texto = raiz.getMessage() == null ? error.toString() : raiz.getMessage();
        // Omite las lineas de contexto de PostgreSQL.
        int corte = texto.indexOf("\n");
        String mensaje = corte > 0 ? texto.substring(0, corte) : texto;
        return new PersistenciaException(traducir(mensaje.replaceFirst("^ERROR:\\s*", "").trim()), error);
    }

    /** Convierte errores tecnicos de PostgreSQL en mensajes claros. */
    private static String traducir(String mensaje) {
        String texto = mensaje.toLowerCase(Locale.ROOT);
        if (texto.contains("uq_ubicacion")) {
            return "Ya existe una ubicación con ese nombre, departamento y país.";
        }
        if (texto.contains("uq_ruta_origen_destino")) {
            return "Ya existe una ruta configurada para ese origen y destino.";
        }
        if (texto.contains("numero_carga")) {
            return "Ya existe una carga con ese número.";
        }
        if (texto.contains("nombre_empresa")) {
            return "Ya existe un cliente con ese nombre.";
        }
        if (texto.contains("numero_unidad")) {
            return "Ya existe una unidad con ese número.";
        }
        if (texto.contains("placas")) {
            return "Ya hay una unidad registrada con esas placas.";
        }
        if (texto.contains("cedula")) {
            return "Ya existe un operador con esa cédula.";
        }
        if (texto.contains("username")) {
            return "Ya existe una cuenta con ese usuario.";
        }
        if (texto.contains("duplicate key") || texto.contains("unique constraint")) {
            return "Ya existe un registro con esos datos.";
        }
        if (texto.contains("foreign key")) {
            return "El registro está siendo usado por otros datos; no se puede eliminar ni modificar así.";
        }
        if (texto.contains("not-null") || texto.contains("null value in column")) {
            return "Falta un dato obligatorio.";
        }
        if (texto.contains("check constraint")) {
            return "Algún dato no cumple una regla de validación de la base de datos.";
        }
        if (texto.contains("value too long")) {
            return "Uno de los textos supera la longitud permitida.";
        }
        if (texto.contains("invalid input syntax")) {
            return "Algún dato tiene un formato inválido.";
        }
        if (texto.contains("relation") && texto.contains("does not exist")) {
            return "La base de datos no tiene una tabla necesaria. Ejecute las migraciones del proyecto.";
        }
        if (texto.contains("connection") || texto.contains("refused") || texto.contains("timeout")) {
            return "No se pudo conectar con la base de datos. Verifique que PostgreSQL esté activo.";
        }
        if (texto.contains("deadlock") || texto.contains("could not serialize")) {
            return "La operación coincidió con otro cambio; intente de nuevo.";
        }
        return mensaje;
    }
}
