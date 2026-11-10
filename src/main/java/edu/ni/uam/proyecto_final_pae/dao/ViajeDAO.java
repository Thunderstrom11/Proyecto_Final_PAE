package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.*;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class ViajeDAO extends GenericDAO<Viaje, Integer> {

    /** Misma zona que hibernate.jdbc.time_zone. */
    private static final ZoneId ZONA = ZoneId.of("America/Managua");

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


    /** Total facturado por operador: [nombreOperador, #viajes, suma]. */
    public List<Object[]> totalFacturadoPorOperador() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select v.operador.nombre, count(v), sum(v.cobroViaje) "
                        + "from Viaje v group by v.operador.nombre "
                        + "order by sum(v.cobroViaje) desc",
                Object[].class).getResultList());
    }

    /** Total facturado por unidad: [numeroUnidad, #viajes, suma]. */
    public List<Object[]> totalFacturadoPorUnidad() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                "select v.unidad.numeroUnidad, count(v), sum(v.cobroViaje) "
                        + "from Viaje v group by v.unidad.numeroUnidad "
                        + "order by sum(v.cobroViaje) desc",
                Object[].class).getResultList());
    }


    /**
     * Crea un viaje aplicando las reglas del despacho:
     *  1) Unidad y operador deben estar DISPONIBLE.
     *  2) Si viene viajePrevio, el origen del nuevo debe coincidir con el destino del previo.
     *  3) Al persistir: unidad y operador pasan a EN_RUTA (salvo en un encadenado, que reutiliza los
     *     del viaje previo) y la carga a ASIGNADA; pasa a EN_TRANSITO cuando el viaje arranca.
     */
    public Viaje crear(Viaje viaje) {
        return Transaccion.ejecutar(sesion -> {
            Carga carga = sesion.find(Carga.class, viaje.getCarga().getId());
            if (carga == null) {
                throw new PersistenciaException("La carga seleccionada ya no existe.", null);
            }

            Unidad unidad;
            Operador operador;
            Viaje previo = null;
            if (viaje.getViajePrevio() != null) {
                // Encadenado: usa la unidad y el operador del viaje previo (ya estan EN_RUTA).
                previo = sesion.find(Viaje.class, viaje.getViajePrevio().getId());
                if (previo == null) {
                    throw new PersistenciaException("El viaje previo ya no existe.", null);
                }
                String estadoPrevio = previo.getEstado().getNombre();
                if ("FINALIZADO".equals(estadoPrevio) || "CANCELADO".equals(estadoPrevio)) {
                    throw new PersistenciaException(
                            "El viaje previo ya terminó; no se puede encadenar otro después de él.", null);
                }
                if (!previo.getDestino().getId().equals(viaje.getOrigen().getId())) {
                    throw new PersistenciaException(
                            "El origen del nuevo viaje debe coincidir con el destino del viaje previo.", null);
                }
                unidad = previo.getUnidad();
                operador = previo.getOperador();
            } else {
                unidad = sesion.find(Unidad.class, viaje.getUnidad().getId());
                operador = sesion.find(Operador.class, viaje.getOperador().getId());
                if (unidad == null || !"DISPONIBLE".equals(unidad.getEstado().getNombre())) {
                    throw new PersistenciaException("La unidad seleccionada ya no está DISPONIBLE.", null);
                }
                if (operador == null || !"DISPONIBLE".equals(operador.getEstado().getNombre())) {
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

    /**
     * Confirma la entrega (RF-09). Lo llama la cabina del operador (P4) o el propio administrador.
     *  - El viaje actual pasa a FINALIZADO con fechaFin.
     *  - Si existe un viaje encadenado (viajePrevio = este), ese pasa a EN_CURSO.
     *  - Si no hay encadenado, unidad y operador vuelven a DISPONIBLE.
     */
    public void finalizar(Integer idViaje) {
        Transaccion.ejecutar(sesion -> {
            Viaje actual = sesion.find(Viaje.class, idViaje);
            if (actual == null) {
                throw new PersistenciaException("El viaje ya no existe.", null);
            }
            LocalDateTime ahora = LocalDateTime.now(ZONA);
            actual.setEstado(catalogo(sesion, EstadoViaje.class, "FINALIZADO"));
            // ck_viaje_fechas exige fecha_inicio cuando hay fecha_fin.
            if (actual.getFechaInicio() == null) {
                actual.setFechaInicio(ahora);
            }
            actual.setFechaFin(ahora);
            actual.getCarga().setEstado(catalogo(sesion, EstadoCarga.class, "ENTREGADA"));

            List<Viaje> encadenados = sesion.createQuery(
                    "select v from Viaje v where v.viajePrevio.id = :id",
                    Viaje.class).setParameter("id", idViaje).getResultList();

            if (!encadenados.isEmpty()) {
                Viaje siguiente = encadenados.get(0);
                siguiente.setEstado(catalogo(sesion, EstadoViaje.class, "EN_CURSO"));
                siguiente.setFechaInicio(ahora);
                siguiente.getCarga().setEstado(catalogo(sesion, EstadoCarga.class, "EN_TRANSITO"));
            } else {
                Unidad unidad = sesion.find(Unidad.class, actual.getUnidad().getId());
                Operador operador = sesion.find(Operador.class, actual.getOperador().getId());
                unidad.setEstado(catalogo(sesion, EstadoUnidad.class, "DISPONIBLE"));
                operador.setEstado(catalogo(sesion, EstadoOperador.class, "DISPONIBLE"));
            }
            return null;
        });
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