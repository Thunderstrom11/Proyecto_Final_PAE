package edu.ni.uam.proyecto_final_pae.security;

import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;

import java.time.Duration;
import java.time.Instant;

/** Usuario autenticado y control de inactividad de la sesión. */
public final class SesionActual {

    private static final Duration INACTIVIDAD_MAXIMA = Duration.ofMinutes(15);

    private static Usuario usuario;
    private static Instant ultimaActividad;

    private SesionActual() {
    }

    public static void iniciar(Usuario usuario) {
        SesionActual.usuario = usuario;
        tocar();
    }

    public static Usuario getUsuario() {
        return usuario;
    }

    /** Registra actividad del usuario para reiniciar el contador de inactividad. */
    public static void tocar() {
        if (usuario != null) {
            ultimaActividad = Instant.now();
        }
    }

    public static boolean expirada() {
        return usuario != null && (ultimaActividad == null
                || Duration.between(ultimaActividad, Instant.now()).compareTo(INACTIVIDAD_MAXIMA) >= 0);
    }

    public static boolean activa() {
        return usuario != null && !expirada();
    }

    /**
     * Devuelve el usuario si la sesión sigue activa y tiene uno de los roles permitidos.
     * Lanza SecurityException en caso contrario para que el controlador no consulte datos.
     */
    public static Usuario requerir(RolUsuario... rolesPermitidos) {
        if (usuario == null) {
            throw new SecurityException("Debe iniciar sesión.");
        }
        if (expirada()) {
            cerrar();
            throw new SecurityException("La sesión expiró por inactividad.");
        }
        for (RolUsuario rol : rolesPermitidos) {
            if (usuario.getRol() == rol) {
                tocar();
                return usuario;
            }
        }
        throw new SecurityException("No tiene permisos para esta pantalla.");
    }

    public static void cerrar() {
        usuario = null;
        ultimaActividad = null;
    }
}
