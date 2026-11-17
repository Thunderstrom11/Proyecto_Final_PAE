package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;

import java.util.List;

public class UbicacionDAO extends GenericDAO<Ubicacion, Integer> {

    public UbicacionDAO() {
        super(Ubicacion.class);
    }

    @Override
    public List<Ubicacion> listar() {
        return Transaccion.consultar(sesion ->
                sesion.createQuery("from Ubicacion u order by u.nombre", Ubicacion.class).getResultList());
    }

    public List<Ubicacion> listarPorPais(String pais) {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "from Ubicacion u where lower(u.pais) = lower(:pais) order by u.nombre",
                        Ubicacion.class)
                .setParameter("pais", pais)
                .getResultList());
    }

    public List<Ubicacion> buscarPorPais(String pais, String texto) {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "from Ubicacion u where lower(u.pais) = lower(:pais) "
                                + "and lower(u.nombre) like :texto order by u.nombre",
                        Ubicacion.class)
                .setParameter("pais", pais)
                .setParameter("texto", "%" + (texto == null ? "" : texto.trim().toLowerCase()) + "%")
                .setMaxResults(15)
                .getResultList());
    }

    public Ubicacion buscarPorPaisYNombre(String pais, String nombre) {
        List<Ubicacion> lista = Transaccion.consultar(sesion -> sesion.createQuery(
                        "from Ubicacion u where lower(u.pais) = lower(:pais) "
                                + "and lower(u.nombre) = lower(:nombre)",
                        Ubicacion.class)
                .setParameter("pais", pais)
                .setParameter("nombre", nombre)
                .setMaxResults(1)
                .getResultList());
        return lista.isEmpty() ? null : lista.get(0);
    }

    public Ubicacion buscarPorNombreDepartamentoYPais(String nombre, String departamento,
                                                      String pais) {
        List<Ubicacion> lista = Transaccion.consultar(sesion -> sesion.createQuery(
                        "from Ubicacion u where lower(u.nombre) = lower(:nombre) "
                                + "and lower(u.departamento) = lower(:departamento) "
                                + "and lower(u.pais) = lower(:pais)",
                        Ubicacion.class)
                .setParameter("nombre", nombre)
                .setParameter("departamento", departamento)
                .setParameter("pais", pais)
                .setMaxResults(1)
                .getResultList());
        return lista.isEmpty() ? null : lista.get(0);
    }

    /** Elimina rutas y ubicaciones; no toca viajes, por eso exige que no existan. */
    public int[] eliminarTodas() {
        return Transaccion.ejecutar(sesion -> {
            Long viajes = sesion.createQuery("select count(v) from Viaje v", Long.class)
                    .getSingleResult();
            if (viajes != null && viajes > 0) {
                throw new PersistenciaException("No se pueden eliminar todas las ubicaciones: hay "
                        + viajes + " viaje(s) que las usan. Elimine primero los viajes.", null);
            }
            int rutas = sesion.createMutationQuery("delete from Ruta").executeUpdate();
            int ubicaciones = sesion.createMutationQuery("delete from Ubicacion").executeUpdate();
            return new int[]{rutas, ubicaciones};
        });
    }
}
