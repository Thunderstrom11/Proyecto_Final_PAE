package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;

import java.util.List;

/**
 * Lectura de los catalogos de estado (EstadoUnidad, EstadoCarga, ...) por NOMBRE.
 * No se quema ningun ID: siempre se consulta por nombre ('PENDIENTE', 'EN_RUTA', ...).
 */
public class CatalogoDAO {

    public <T> T porNombre(Class<T> clase, String nombre) {
        return Transaccion.consultar(sesion ->
                sesion.createQuery("from " + clase.getSimpleName() + " c where c.nombre = :nombre", clase)
                        .setParameter("nombre", nombre)
                        .uniqueResultOptional()
                        .orElseThrow(() -> new PersistenciaException(
                                "No existe el valor '" + nombre + "' en el catalogo " + clase.getSimpleName()
                                        + ". Ejecute database/fleetroute_pro.sql.", null)));
    }

    public <T> List<T> listar(Class<T> clase) {
        return Transaccion.consultar(sesion ->
                sesion.createQuery("from " + clase.getSimpleName() + " c order by c.id", clase).getResultList());
    }
}
