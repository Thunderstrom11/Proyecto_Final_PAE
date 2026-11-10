package edu.ni.uam.proyecto_final_pae.security;

import edu.ni.uam.proyecto_final_pae.model.Usuario;

/** Usuario autenticado en la sesión actual de la aplicación. */
public final class SesionActual {

    private static Usuario usuario;

    private SesionActual() {
    }

    public static void iniciar(Usuario usuario) {
        SesionActual.usuario = usuario;
    }

    public static Usuario getUsuario() {
        return usuario;
    }

    public static void cerrar() {
        usuario = null;
    }
}
