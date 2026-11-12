package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Operador;
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

    /** Lista todas las cuentas con su operador cargado para el panel de cuentas. */
    @Override
    public List<Usuario> listar() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select u from Usuario u left join fetch u.operador order by u.username",
                Usuario.class).getResultList());
    }

    public boolean existeCuentaOperador(Integer idOperador) {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select count(u) from Usuario u where u.operador.id = :id", Long.class)
                .setParameter("id", idOperador)
                .getSingleResult() > 0);
    }

    /** Comprueba el nombre de usuario sin filtrar por activo, para respetar la restricción unique. */
    public boolean existeUsername(String username) {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select count(u) from Usuario u where lower(u.username) = lower(:username)", Long.class)
                .setParameter("username", username)
                .getSingleResult() > 0);
    }

    public boolean existeUsernameExcepto(String username, Integer idExcluir) {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select count(u) from Usuario u "
                                + "where lower(u.username) = lower(:username) and u.id <> :id", Long.class)
                .setParameter("username", username)
                .setParameter("id", idExcluir)
                .getSingleResult() > 0);
    }

    public Usuario buscarPorOperador(Integer idOperador) {
        List<Usuario> usuarios = Transaccion.consultar(sesion -> sesion.createQuery(
                        "select u from Usuario u left join fetch u.operador where u.operador.id = :id",
                        Usuario.class).setParameter("id", idOperador)
                .setMaxResults(1).getResultList());
        return usuarios.isEmpty() ? null : usuarios.get(0);
    }

    /**
     * Crea la cuenta del operador usando su cédula como usuario.
     * La contraseña temporal es fija y conocida por la administración para que pueda
     * entregarla y luego cambiarla desde el panel; se guarda con hash BCrypt.
     */
    public String crearCuentaOperador(Operador operador) {
        if (operador == null || operador.getId() == null || existeCuentaOperador(operador.getId())) {
            return null;
        }
        if (existeUsername(operador.getCedula())) {
            throw new IllegalStateException("Ya existe una cuenta con el usuario " + operador.getCedula() + ".");
        }
        String temporal = "operador123!";
        Usuario usuario = new Usuario();
        usuario.setUsername(operador.getCedula());
        usuario.setPasswordHash(BCrypt.hashpw(temporal, BCrypt.gensalt()));
        usuario.setRol(RolUsuario.OPERADOR);
        usuario.setOperador(operador);
        usuario.setActivo(true);
        usuario.setEsPrincipal(false);
        usuario.setPuedeVerReportes(false);
        guardar(usuario);
        return temporal;
    }

    public void restablecerPassword(Usuario usuario, String nuevaPassword) {
        if (usuario == null || nuevaPassword == null || nuevaPassword.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
        }
        usuario.setPasswordHash(BCrypt.hashpw(nuevaPassword, BCrypt.gensalt()));
        actualizar(usuario);
    }

    /** true si ya existe un administrador activo (el asistente de primer arranque lo consulta). */
    public boolean existeAdministrador() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select count(u) from Usuario u where u.rol = :rol and u.activo = true", Long.class)
                .setParameter("rol", RolUsuario.ADMIN).getSingleResult() > 0);
    }
}
