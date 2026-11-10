package edu.ni.uam.proyecto_final_pae.dao;

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
}
