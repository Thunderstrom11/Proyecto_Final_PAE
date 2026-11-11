package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.*;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.util.List;

public class ViajeDAO extends GenericDAO<Viaje, Integer> {

    public ViajeDAO() { super(Viaje.class); }


    @Override
    public List<Viaje> listar() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select v from Viaje v "
                        + "join fetch v.unidad "
                        + "join fetch v.operador "
                        + "join fetch v.carga "
                        + "join fetch v.origen "
                        + "join fetch v.destino "
                        + "join fetch v.estado "
                        + "order by v.id", Viaje.class).getResultList());
    }

    /** Viajes activos: los que el monitor de flota debe mostrar. */
    public List<Viaje> listarActivos() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select v from Viaje v "
                        + "join fetch v.unidad "
                        + "join fetch v.operador "
                        + "join fetch v.carga "
                        + "join fetch v.origen "
                        + "join fetch v.destino "
                        + "join fetch v.estado e "
                        + "where e.nombre in ('PROGRAMADO', 'EN_CURSO', 'PAUSADO') "
                        + "order by v.fechaInicio", Viaje.class).getResultList());
    }


    public List<Unidad> unidadesDisponibles() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select u from Unidad u join fetch u.estado e "
                        + "where e.nombre = 'DISPONIBLE' order by u.numeroUnidad",
                Unidad.class).getResultList());
    }

    public List<Operador> operadoresDisponibles() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select o from Operador o join fetch o.estado e "
                        + "where e.nombre = 'DISPONIBLE' order by o.nombre",
                Operador.class).getResultList());
    }

    public List<Carga> cargasPendientes() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select c from Carga c join fetch c.estado e "
                        + "where e.nombre = 'PENDIENTE' order by c.fechaLimiteCliente",
                Carga.class).getResultList());
    }


    /** Total facturado por operador: solo viajes FINALIZADO [nombreOperador, #viajes, suma]. */
    public List<Object[]> totalFacturadoPorOperador() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select v.operador.nombre, count(v), sum(v.cobroViaje) "
                        + "from Viaje v join v.estado e "
                        + "where e.nombre = 'FINALIZADO' "
                        + "group by v.operador.id, v.operador.nombre "
                        + "order by sum(v.cobroViaje) desc",
                Object[].class).getResultList());
    }

    /** Total facturado por unidad: solo viajes FINALIZADO [numeroUnidad, #viajes, suma]. */
    public List<Object[]> totalFacturadoPorUnidad() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select v.unidad.numeroUnidad, count(v), sum(v.cobroViaje) "
                        + "from Viaje v join v.estado e "
                        + "where e.nombre = 'FINALIZADO' "
                        + "group by v.unidad.id, v.unidad.numeroUnidad "
                        + "order by sum(v.cobroViaje) desc",
                Object[].class).getResultList());
    }


    /**
     * Crea un viaje aplicando las reglas del despacho:
     *  1) Unidad y operador deben estar DISPONIBLE y sin otro viaje activo.
     *  2) La carga debe seguir PENDIENTE.
     *  3) Si viene viajePrevio, el origen del nuevo debe coincidir con el destino del previo y
     *     el previo debe estar en ruta (EN_CURSO o PAUSADO).
     *  4) Al persistir: unidad y operador pasan a EN_RUTA (salvo en encadenado, que reutiliza los
     *     del viaje previo) y la carga a ASIGNADA; pasa a EN_TRANSITO cuando el viaje arranca.
     */
    public Viaje crear(Viaje viaje) {
        return Transaccion.ejecutar(sesion -> {
            if (viaje.getCarga() == null || viaje.getCarga().getId() == null) {
                throw new PersistenciaException("Seleccione una carga para el viaje.", null);
            }
            if (viaje.getOrigen() == null || viaje.getDestino() == null) {
                throw new PersistenciaException("Seleccione el origen y el destino del viaje.", null);
            }
            if (viaje.getOrigen().getId().equals(viaje.getDestino().getId())) {
                throw new PersistenciaException("El origen y el destino deben ser distintos.", null);
            }
            if (viaje.getTiempoEstimado() == null || viaje.getTiempoEstimado().signum() <= 0) {
                throw new PersistenciaException("El tiempo estimado debe ser mayor que cero.", null);
            }
            if (viaje.getCobroViaje() == null || viaje.getCobroViaje().signum() <= 0) {
                throw new PersistenciaException("El cobro del viaje debe ser mayor que cero.", null);
            }

            Carga carga = sesion.find(Carga.class, viaje.getCarga().getId(), LockModeType.PESSIMISTIC_WRITE);
            if (carga == null) {
                throw new PersistenciaException("La carga seleccionada ya no existe.", null);
            }
            if (!"PENDIENTE".equals(carga.getEstado().getNombre())) {
                throw new PersistenciaException(
                        "La carga seleccionada ya no esta PENDIENTE; actualice la pantalla.", null);
            }

            Unidad unidad;
            Operador operador;
            Viaje previo = null;
            if (viaje.getViajePrevio() != null) {
                // Encadenado: usa la unidad y el operador del viaje previo (ya estan en ruta).
                previo = sesion.find(Viaje.class, viaje.getViajePrevio().getId(), LockModeType.PESSIMISTIC_WRITE);
                if (previo == null) {
                    throw new PersistenciaException("El viaje previo ya no existe.", null);
                }
                String estadoPrevio = previo.getEstado().getNombre();
                if (!"EN_CURSO".equals(estadoPrevio) && !"PAUSADO".equals(estadoPrevio)) {
                    throw new PersistenciaException(
                            "Solo se puede encadenar un viaje que este en ruta (EN_CURSO o PAUSADO); "
                                    + "el previo esta " + estadoPrevio + ".", null);
                }
                if (!previo.getDestino().getId().equals(viaje.getOrigen().getId())) {
                    throw new PersistenciaException(
                            "El origen del nuevo viaje debe coincidir con el destino del viaje previo.", null);
                }
                unidad = previo.getUnidad();
                operador = previo.getOperador();
            } else {
                if (viaje.getUnidad() == null || viaje.getOperador() == null) {
                    throw new PersistenciaException("Seleccione la unidad y el operador del viaje.", null);
                }
                unidad = sesion.find(Unidad.class, viaje.getUnidad().getId(), LockModeType.PESSIMISTIC_WRITE);
                operador = sesion.find(Operador.class, viaje.getOperador().getId(), LockModeType.PESSIMISTIC_WRITE);
                if (unidad == null || !"DISPONIBLE".equals(unidad.getEstado().getNombre())
                        || hayViajeActivo(sesion, "unidad", unidad.getId(), null)) {
                    throw new PersistenciaException("La unidad seleccionada ya no está DISPONIBLE.", null);
                }
                if (operador == null || !"DISPONIBLE".equals(operador.getEstado().getNombre())
                        || hayViajeActivo(sesion, "operador", operador.getId(), null)) {
                    throw new PersistenciaException("El operador seleccionado ya no está DISPONIBLE.", null);
                }
            }

            viaje.setViajePrevio(previo);
            viaje.setUnidad(unidad);
            viaje.setOperador(operador);
            viaje.setCarga(carga);
            viaje.setEstado(catalogo(sesion, EstadoViaje.class, "PROGRAMADO"));
            sesion.persist(viaje);

            if (previo == null) {
                unidad.setEstado(catalogo(sesion, EstadoUnidad.class, "EN_RUTA"));
                operador.setEstado(catalogo(sesion, EstadoOperador.class, "EN_RUTA"));
            }
            carga.setEstado(catalogo(sesion, EstadoCarga.class, "ASIGNADA"));
            return viaje;
        });
    }

    private static boolean hayViajeActivo(Session sesion, String campo, int id, Integer excluirIdViaje) {
        String hql = "select count(v) from Viaje v join v.estado e "
                + "where v." + campo + ".id = :id "
                + "and e.nombre in ('PROGRAMADO', 'EN_CURSO', 'PAUSADO')"
                + (excluirIdViaje == null ? "" : " and v.id <> :excluir");
        var query = sesion.createQuery(hql, Long.class).setParameter("id", id);
        if (excluirIdViaje != null) {
            query.setParameter("excluir", excluirIdViaje);
        }
        return query.getSingleResult() > 0;
    }

    private <T> T catalogo(Session sesion, Class<T> clase, String nombre) {
        return sesion.createQuery(
                        "from " + clase.getSimpleName() + " c where c.nombre = :n", clase)
                .setParameter("n", nombre)
                .uniqueResultOptional()
                .orElseThrow(() -> new PersistenciaException(
                        "Falta el valor '" + nombre + "' en " + clase.getSimpleName(), null));
    }
}