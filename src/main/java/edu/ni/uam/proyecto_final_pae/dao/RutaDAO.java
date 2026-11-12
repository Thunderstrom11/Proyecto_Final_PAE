package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Ruta;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;

import java.util.List;
import java.util.Optional;

public class RutaDAO extends GenericDAO<Ruta, Integer> {

    public RutaDAO() {
        super(Ruta.class);
    }

    @Override
    public List<Ruta> listar() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select r from Ruta r join fetch r.origen join fetch r.destino order by r.origen.nombre, r.destino.nombre",
                Ruta.class).getResultList());
    }

    public Optional<Ruta> buscar(Integer idOrigen, Integer idDestino) {
        return Transaccion.consultar(sesion -> {
            List<Ruta> rutas = sesion.createQuery(
                            "select r from Ruta r join fetch r.origen join fetch r.destino "
                                    + "where r.origen.id = :origen and r.destino.id = :destino",
                            Ruta.class)
                    .setParameter("origen", idOrigen)
                    .setParameter("destino", idDestino)
                    .setMaxResults(1)
                    .getResultList();
            return rutas.isEmpty() ? Optional.empty() : Optional.of(rutas.get(0));
        });
    }

    public Optional<Ruta> buscar(Ubicacion origen, Ubicacion destino) {
        if (origen == null || destino == null) {
            return Optional.empty();
        }
        return buscar(origen.getId(), destino.getId());
    }
}
