package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Carga;

import java.util.List;

public class CargaDAO extends GenericDAO<Carga, Integer> {

    public CargaDAO() {
        super(Carga.class);
    }

    /** Todas las cargas con su cliente ya cargado (evita LazyInitializationException en la tabla). */
    @Override
    public List<Carga> listar() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select c from Carga c left join fetch c.cliente join fetch c.estado order by c.id",
                Carga.class).getResultList());
    }

    /**
     * Consulta HQL #1 (relaciona Carga, Cliente y EstadoCarga):
     * cargas en estado PENDIENTE con su cliente, ordenadas por fecha limite (SLA mas cercano primero).
     * La usa el planificador de viajes (P3).
     */
    public List<Carga> listarPendientes() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select c from Carga c "
                        + "left join fetch c.cliente "
                        + "join fetch c.estado e "
                        + "where e.nombre = :estado "
                        + "order by c.fechaLimiteCliente",
                Carga.class).setParameter("estado", "PENDIENTE").getResultList());
    }
}
