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

    /** La cédula es única: se valida antes de guardar para dar un mensaje claro. */
    public boolean existeCedula(String cedula, Integer idExcluir) {
        return Transaccion.consultar(sesion -> {
            String hql = "select count(o) from Operador o where lower(o.cedula) = lower(:cedula)"
                    + (idExcluir == null ? "" : " and o.id <> :id");
            var query = sesion.createQuery(hql, Long.class).setParameter("cedula", cedula);
            if (idExcluir != null) {
                query.setParameter("id", idExcluir);
            }
            return query.getSingleResult() > 0;
        });
    }
}
