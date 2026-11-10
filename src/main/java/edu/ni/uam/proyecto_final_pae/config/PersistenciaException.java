package edu.ni.uam.proyecto_final_pae.config;

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
        // PostgreSQL agrega lineas de contexto ("Where: ...") que no sirven al usuario.
        int corte = texto.indexOf("\n");
        String mensaje = corte > 0 ? texto.substring(0, corte) : texto;
        return new PersistenciaException(mensaje.replaceFirst("^ERROR:\\s*", ""), error);
    }
}
