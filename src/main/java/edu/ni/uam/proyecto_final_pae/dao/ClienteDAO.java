package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Cliente;

import java.util.List;

public class ClienteDAO extends GenericDAO<Cliente, Integer> {

    public ClienteDAO() {
        super(Cliente.class);
    }

    @Override
    public List<Cliente> listar() {
        return Transaccion.consultar(sesion ->
                sesion.createQuery("from Cliente c order by c.nombreEmpresa", Cliente.class).getResultList());
    }

    public List<Cliente> listarActivos() {
        return Transaccion.consultar(sesion ->
                sesion.createQuery("from Cliente c where c.activo = true order by c.nombreEmpresa", Cliente.class)
                        .getResultList());
    }
}
