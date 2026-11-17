package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.PaisHabilitado;

import java.util.Collection;
import java.util.List;

public class PaisDAO extends GenericDAO<PaisHabilitado, Integer> {

    public PaisDAO() {
        super(PaisHabilitado.class);
    }

    public List<String> listarActivos() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select p.nombre from PaisHabilitado p order by p.nombre", String.class)
                .getResultList());
    }

    /** Reemplaza por completo la lista de países activos. */
    public void reemplazar(Collection<String> paises) {
        Transaccion.ejecutar(sesion -> {
            sesion.createMutationQuery("delete from PaisHabilitado").executeUpdate();
            for (String pais : paises) {
                sesion.persist(new PaisHabilitado(pais));
            }
            return null;
        });
    }
}
