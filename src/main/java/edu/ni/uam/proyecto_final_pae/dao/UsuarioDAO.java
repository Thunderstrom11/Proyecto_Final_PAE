package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UsuarioDAO extends GenericDAO<Usuario, Integer> {

    public UsuarioDAO() {
        super(Usuario.class);
    }

    /** Devuelve el usuario si la contraseña coincide con el hash BCrypt; null si no. */
    public Usuario autenticar(String username, String password) {
        Usuario usuario = buscarPorUsername(username);
        if (usuario == null || usuario.getPasswordHash() == null) {
            return null;
        }
        try {
            return BCrypt.checkpw(password, usuario.getPasswordHash()) ? usuario : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Usuario buscarPorUsername(String username) {
        List<Usuario> usuarios = Transaccion.consultar(sesion -> sesion.createQuery(
                "select u from Usuario u left join fetch u.operador "
                        + "where u.username = :username and u.activo = true",
                Usuario.class).setParameter("username", username)
                .setMaxResults(1).getResultList());
        return usuarios.isEmpty() ? null : usuarios.get(0);
    }

    /** true si ya existe un administrador activo (el asistente de primer arranque lo consulta). */
    public boolean existeAdministrador() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select count(u) from Usuario u where u.rol = :rol and u.activo = true", Long.class)
                .setParameter("rol", RolUsuario.ADMIN).getSingleResult() > 0);
    }
}
