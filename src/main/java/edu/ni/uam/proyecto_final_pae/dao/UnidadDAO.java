package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Unidad;

import java.util.List;

public class UnidadDAO extends GenericDAO<Unidad, Integer> {

    public UnidadDAO() {
        super(Unidad.class);
    }

    /** Todas las unidades con su estado cargado para la tabla. */
    @Override
    public List<Unidad> listar() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select u from Unidad u join fetch u.estado order by u.numeroUnidad",
                Unidad.class).getResultList());
    }

    /** Consulta HQL: unidades disponibles para asignar a un viaje. */
    public List<Unidad> listarDisponibles() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select u from Unidad u join fetch u.estado e "
                        + "where e.nombre = :estado order by u.numeroUnidad",
                Unidad.class).setParameter("estado", "DISPONIBLE").getResultList());
    }
}
