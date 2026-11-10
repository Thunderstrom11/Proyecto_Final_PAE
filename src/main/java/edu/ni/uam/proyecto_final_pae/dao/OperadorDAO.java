package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Operador;

import java.util.List;

public class OperadorDAO extends GenericDAO<Operador, Integer> {

    public OperadorDAO() {
        super(Operador.class);
    }

    /** Todos los operadores con su estado cargado para la tabla. */
    @Override
    public List<Operador> listar() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select o from Operador o join fetch o.estado order by o.nombre",
                Operador.class).getResultList());
    }

    /** Consulta HQL: operadores disponibles para asignar a un viaje. */
    public List<Operador> listarDisponibles() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select o from Operador o join fetch o.estado e "
                        + "where e.nombre = :estado order by o.nombre",
                Operador.class).setParameter("estado", "DISPONIBLE").getResultList());
    }
}
