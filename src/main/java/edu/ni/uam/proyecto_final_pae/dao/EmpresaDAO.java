package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Empresa;

import java.util.Optional;

public class EmpresaDAO extends GenericDAO<Empresa, Integer> {

    public EmpresaDAO() {
        super(Empresa.class);
    }

    /** La empresa configurada (una sola fila); vacío en el primer arranque. */
    public Optional<Empresa> obtener() {
        return Transaccion.consultar(sesion -> Optional.ofNullable(sesion.createQuery(
                        "from Empresa e order by e.id", Empresa.class)
                .setMaxResults(1).getSingleResultOrNull()));
    }

    public boolean configurada() {
        return obtener().isPresent();
    }
}