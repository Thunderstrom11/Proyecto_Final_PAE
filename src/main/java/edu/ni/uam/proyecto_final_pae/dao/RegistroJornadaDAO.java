package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.RegistroJornada;
import edu.ni.uam.proyecto_final_pae.model.TipoEvento;
import edu.ni.uam.proyecto_final_pae.model.Viaje;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistencia de la bitacora de jornada (registro_jornada).
 *
 * <p><b>Consulta HQL #1 de este modulo:</b> {@link #listarPorViaje(Integer)} recupera
 * los eventos de un viaje junto con su tipo ({@code join fetch r.tipoEvento}), que es la
 * relacion RegistroJornada - TipoEvento.</p>
 */
public class RegistroJornadaDAO extends GenericDAO<RegistroJornada, Integer> {

    public RegistroJornadaDAO() {
        super(RegistroJornada.class);
    }

    /** Eventos del viaje en orden cronologico, con el tipo de evento ya cargado (HQL). */
    public List<RegistroJornada> listarPorViaje(Integer idViaje) {
        return Transaccion.consultar(sesion -> listarPorViaje(sesion, idViaje));
    }

    /** Variante transaccional: el llamador controla la transaccion. */
    public List<RegistroJornada> listarPorViaje(Session sesion, Integer idViaje) {
        return sesion.createQuery(
                        "select r from RegistroJornada r join fetch r.tipoEvento "
                                + "where r.viaje.id = :id order by r.fechaHora, r.id",
                        RegistroJornada.class)
                .setParameter("id", idViaje)
                .getResultList();
    }

    /** Eventos de varios viajes en una sola consulta (para el monitor). */
    public List<RegistroJornada> listarPorViajes(List<Integer> idsViaje) {
        return Transaccion.consultar(sesion -> listarPorViajes(sesion, idsViaje));
    }

    /** Variante transaccional: el llamador controla la transaccion. */
    public List<RegistroJornada> listarPorViajes(Session sesion, List<Integer> idsViaje) {
        if (idsViaje == null || idsViaje.isEmpty()) {
            return List.of();
        }
        return sesion.createQuery(
                        "select r from RegistroJornada r join fetch r.tipoEvento "
                                + "where r.viaje.id in :ids order by r.viaje.id, r.fechaHora, r.id",
                        RegistroJornada.class)
                .setParameter("ids", idsViaje)
                .getResultList();
    }

    /** Ultimo evento registrado del viaje (el que define el paso actual de la cabina). */
    public RegistroJornada ultimo(Integer idViaje) {
        return Transaccion.consultar(sesion -> ultimo(sesion, idViaje));
    }

    /** Variante transaccional: el llamador controla la transaccion. */
    public RegistroJornada ultimo(Session sesion, Integer idViaje) {
        return sesion.createQuery(
                        "select r from RegistroJornada r join fetch r.tipoEvento "
                                + "where r.viaje.id = :id order by r.id desc",
                        RegistroJornada.class)
                .setParameter("id", idViaje)
                .setMaxResults(1)
                .getSingleResultOrNull();
    }

    /**
     * Inserta un evento resolviendo su tipo por NOMBRE en cat_tipo_evento
     * (nunca se queman IDs de catalogo). El num_evento lo calcula la aplicacion (y el trigger, si existe).
     */
    public RegistroJornada registrar(Integer idViaje, String nombreTipoEvento, LocalDateTime fechaHora, String observacion) {
        return Transaccion.ejecutar(sesion -> {
            return registrar(sesion, idViaje, nombreTipoEvento, fechaHora, observacion);
        });
    }

    /** Variante transaccional: el llamador controla la transaccion. */
    public RegistroJornada registrar(Session sesion, Integer idViaje, String nombreTipoEvento,
                                     LocalDateTime fechaHora, String observacion) {
        TipoEvento tipo = sesion.createQuery(
                        "from TipoEvento t where t.nombre = :nombre", TipoEvento.class)
                .setParameter("nombre", nombreTipoEvento)
                .getSingleResultOrNull();
        if (tipo == null) {
            throw new PersistenciaException("No existe el tipo de evento '" + nombreTipoEvento
                    + "' en cat_tipo_evento. Ejecute database/fleetroute_pro.sql.", null);
        }
        RegistroJornada registro = new RegistroJornada(
                sesion.getReference(Viaje.class, idViaje), tipo, fechaHora, observacion);
        // num_evento lo asigna el trigger trg_rj_num_evento; la entidad no lo escribe.
        sesion.persist(registro);
        return registro;
    }
}
