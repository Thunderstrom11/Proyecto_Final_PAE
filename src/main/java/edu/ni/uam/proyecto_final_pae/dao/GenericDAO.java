package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/** CRUD basico con Hibernate para cualquier entidad. Los DAO de cada modulo lo extienden. */
public abstract class GenericDAO<T, ID extends Serializable> {

    private final Class<T> tipo;

    protected GenericDAO(Class<T> tipo) {
        this.tipo = tipo;
    }

    public T guardar(T entidad) {
        return Transaccion.ejecutar(sesion -> {
            sesion.persist(entidad);
            return entidad;
        });
    }

    public T actualizar(T entidad) {
        // Bloque con return explicito: evita la ambiguedad entre ejecutar(Function) y ejecutar(Consumer).
        return Transaccion.ejecutar(sesion -> {
            return sesion.merge(entidad);
        });
    }

    public void eliminar(ID id) {
        Transaccion.ejecutar(sesion -> {
            T entidad = sesion.find(tipo, id);
            if (entidad != null) {
                sesion.remove(entidad);
            }
        });
    }

    public Optional<T> buscar(ID id) {
        return Transaccion.consultar(sesion -> Optional.ofNullable(sesion.find(tipo, id)));
    }

    public List<T> listar() {
        return Transaccion.consultar(sesion ->
                sesion.createQuery("from " + tipo.getSimpleName(), tipo).getResultList());
    }
}
