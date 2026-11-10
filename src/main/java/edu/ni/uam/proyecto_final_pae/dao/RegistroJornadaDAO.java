package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.RegistroJornada;
import edu.ni.uam.proyecto_final_pae.model.TipoEvento;

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
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select r from RegistroJornada r join fetch r.tipoEvento "
                                + "where r.idViaje = :id order by r.fechaHora, r.id",
                        RegistroJornada.class)
                .setParameter("id", idViaje)
                .getResultList());
    }

    /** Ultimo evento registrado del viaje (el que define el paso actual de la cabina). */
    public RegistroJornada ultimo(Integer idViaje) {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select r from RegistroJornada r join fetch r.tipoEvento "
                                + "where r.idViaje = :id order by r.id desc",
                        RegistroJornada.class)
                .setParameter("id", idViaje)
                .setMaxResults(1)
                .getSingleResultOrNull());
    }

    /**
     * Inserta un evento resolviendo su tipo por NOMBRE en cat_tipo_evento
     * (nunca se queman IDs de catalogo). El num_evento lo calcula la aplicacion (y el trigger, si existe).
     */
    public RegistroJornada registrar(Integer idViaje, String nombreTipoEvento, LocalDateTime fechaHora, String observacion) {
        return Transaccion.ejecutar(sesion -> {
            TipoEvento tipo = sesion.createQuery(
                            "from TipoEvento t where t.nombre = :nombre", TipoEvento.class)
                    .setParameter("nombre", nombreTipoEvento)
                    .getSingleResultOrNull();
            if (tipo == null) {
                throw new PersistenciaException("No existe el tipo de evento '" + nombreTipoEvento
                        + "' en cat_tipo_evento. Ejecute database/fleetroute_pro.sql.", null);
            }
            RegistroJornada registro = new RegistroJornada(idViaje, tipo, fechaHora, observacion);
            // Consecutivo por viaje. Si el trigger trg_rj_num_evento existe, recalcula el mismo valor.
            Integer maximo = sesion.createQuery(
                            "select max(r.numEvento) from RegistroJornada r where r.idViaje = :id", Integer.class)
                    .setParameter("id", idViaje).getSingleResult();
            registro.setNumEvento(maximo == null ? 1 : maximo + 1);
            sesion.persist(registro);
            return registro;
        });
    }
}
