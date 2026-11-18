package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.config.ZonaHoraria;
import edu.ni.uam.proyecto_final_pae.model.AlertaOperador;
import edu.ni.uam.proyecto_final_pae.model.Operador;
import edu.ni.uam.proyecto_final_pae.model.Unidad;
import edu.ni.uam.proyecto_final_pae.model.Viaje;

import java.time.LocalDateTime;
import java.util.List;

public class AlertaOperadorDAO extends GenericDAO<AlertaOperador, Integer> {

    public AlertaOperadorDAO() {
        super(AlertaOperador.class);
    }

    public AlertaOperador registrar(int idViaje, int idOperador, int idUnidad,
                                    Double latitud, Double longitud, String ubicacion, String mensaje) {
        return Transaccion.ejecutar(sesion -> {
            AlertaOperador alerta = new AlertaOperador();
            Viaje viaje = sesion.find(Viaje.class, idViaje);
            Operador operador = sesion.find(Operador.class, idOperador);
            Unidad unidad = sesion.find(Unidad.class, idUnidad);
            if (viaje == null || operador == null || unidad == null) {
                throw new IllegalArgumentException("No se encontró el viaje, operador o unidad para la alerta.");
            }
            alerta.setViaje(viaje);
            alerta.setOperador(operador);
            alerta.setUnidad(unidad);
            alerta.setLatitud(latitud);
            alerta.setLongitud(longitud);
            alerta.setUbicacion(ubicacion);
            alerta.setMensaje(mensaje);
            alerta.setFechaHora(LocalDateTime.now(ZonaHoraria.actual()));
            alerta.setAtendida(false);
            sesion.persist(alerta);
            return alerta;
        });
    }

    public List<AlertaOperador> listarTodas() {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "select a from AlertaOperador a "
                                + "join fetch a.viaje v join fetch a.operador join fetch a.unidad "
                                + "order by a.fechaHora desc",
                        AlertaOperador.class).getResultList());
    }

    public void marcarAtendida(int id) {
        Transaccion.ejecutar(sesion -> {
            AlertaOperador alerta = sesion.find(AlertaOperador.class, id);
            if (alerta != null) {
                alerta.setAtendida(true);
            }
            return null;
        });
    }
}
