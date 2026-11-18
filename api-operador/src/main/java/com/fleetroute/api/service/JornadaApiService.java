package com.fleetroute.api.service;

import com.fleetroute.api.config.ApiException;
import com.fleetroute.api.security.Sesion;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JornadaApiService {

    private static final String ESTADOS_ACTIVOS = "('PROGRAMADO','EN_CURSO','PAUSADO')";

    private final JdbcTemplate jdbc;

    public JornadaApiService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public String iniciar(Sesion sesion) {
        ViajeActivo viaje = activo(sesion);
        if (!"SIN_INICIAR".equals(paso(viaje.id()))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Ya hay un tramo abierto o el viaje ya fue iniciado.");
        }
        if (!"PROGRAMADO".equals(viaje.estado()) && !"EN_CURSO".equals(viaje.estado())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El viaje está en estado " + viaje.estado() + " y no se puede iniciar.");
        }
        registrarEvento(viaje.id(), "INICIO_TRAYECTO", "Inicio de trayecto desde la web");
        estadoViaje(viaje.id(), "EN_CURSO", true, false);
        estadoCarga(viaje.id(), "EN_TRANSITO");
        estadoOperador(viaje.idOperador(), "EN_RUTA");
        estadoUnidad(viaje.idUnidad(), "EN_RUTA");
        return "Trayecto iniciado.";
    }

    @Transactional
    public String pausar(Sesion sesion) {
        ViajeActivo viaje = activo(sesion);
        if (!"EN_CURSO".equals(viaje.estado()) || !"EN_MARCHA".equals(paso(viaje.id()))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Solo se puede pausar un viaje en marcha.");
        }
        registrarEvento(viaje.id(), "PAUSA_DESCANSO", "Pausa de descanso desde la web");
        estadoViaje(viaje.id(), "PAUSADO", false, false);
        estadoOperador(viaje.idOperador(), "DESCANSO");
        return "Viaje pausado.";
    }

    @Transactional
    public String reanudar(Sesion sesion) {
        ViajeActivo viaje = activo(sesion);
        if (!"PAUSADO".equals(viaje.estado()) || !"EN_PAUSA".equals(paso(viaje.id()))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El viaje no está en pausa.");
        }
        registrarEvento(viaje.id(), "REANUDAR", "Reanudación desde la web");
        estadoViaje(viaje.id(), "EN_CURSO", false, false);
        estadoOperador(viaje.idOperador(), "EN_RUTA");
        estadoUnidad(viaje.idUnidad(), "EN_RUTA");
        return "Viaje reanudado.";
    }

    @Transactional
    public String confirmar(Sesion sesion) {
        ViajeActivo viaje = activo(sesion);
        if (!"EN_CURSO".equals(viaje.estado()) || !"EN_MARCHA".equals(paso(viaje.id()))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Solo se puede confirmar la entrega de un viaje en marcha.");
        }
        registrarEvento(viaje.id(), "CONFIRMAR_ENTREGA", "Entrega confirmada desde la web");
        estadoViaje(viaje.id(), "FINALIZADO", false, true);
        estadoCarga(viaje.id(), "ENTREGADA");

        List<ViajeSiguiente> siguientes = jdbc.query("""
                select v.id_viaje, ev.nombre as estado
                  from viajes v
                  join cat_estado_viaje ev on ev.id_estado_viaje = v.id_estado_viaje
                 where v.id_viaje_previo = ?
                 limit 1
                """, (rs, rowNum) -> new ViajeSiguiente(
                rs.getInt("id_viaje"), rs.getString("estado")), viaje.id());

        if (!siguientes.isEmpty() && "PROGRAMADO".equals(siguientes.get(0).estado())) {
            estadoViaje(siguientes.get(0).id(), "EN_CURSO", false, false);
            estadoOperador(viaje.idOperador(), "EN_RUTA");
            estadoUnidad(viaje.idUnidad(), "EN_RUTA");
        } else {
            liberarSiCorresponde(viaje.idOperador(), viaje.idUnidad());
        }
        return "Entrega confirmada.";
    }

    private ViajeActivo activo(Sesion sesion) {
        List<ViajeActivo> viajes = jdbc.query("""
                select v.id_viaje, v.id_operador, v.id_unidad, ev.nombre as estado
                  from viajes v
                  join cat_estado_viaje ev on ev.id_estado_viaje = v.id_estado_viaje
                 where v.id_operador = ? and ev.nombre in """ + ESTADOS_ACTIVOS + """
                 order by case ev.nombre when 'EN_CURSO' then 0 when 'PAUSADO' then 1 else 2 end, v.id_viaje
                 limit 1
                """, (rs, rowNum) -> new ViajeActivo(
                rs.getInt("id_viaje"),
                rs.getInt("id_operador"),
                rs.getInt("id_unidad"),
                rs.getString("estado")), sesion.idOperador());
        if (viajes.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "No hay un viaje activo para este operador.");
        }
        return viajes.get(0);
    }

    private String paso(int idViaje) {
        List<String> tipos = jdbc.query("""
                select te.nombre
                  from registro_jornada rj
                  join cat_tipo_evento te on te.id_tipo_evento = rj.id_tipo_evento
                 where rj.id_viaje = ?
                 order by rj.num_evento desc
                 limit 1
                """, (rs, rowNum) -> rs.getString("nombre"), idViaje);
        if (tipos.isEmpty()) {
            return "SIN_INICIAR";
        }
        return switch (tipos.get(0)) {
            case "INICIO_TRAYECTO", "REANUDAR" -> "EN_MARCHA";
            case "PAUSA_DESCANSO" -> "EN_PAUSA";
            case "CONFIRMAR_ENTREGA" -> "FINALIZADO";
            default -> "SIN_INICIAR";
        };
    }

    private void registrarEvento(int idViaje, String tipo, String observacion) {
        jdbc.update("""
                insert into registro_jornada (id_viaje, id_tipo_evento, fecha_hora, observacion)
                select ?, id_tipo_evento, now(), ?
                  from cat_tipo_evento
                 where nombre = ?
                """, idViaje, observacion, tipo);
    }

    private void estadoViaje(int idViaje, String estado, boolean marcarInicio, boolean marcarFin) {
        jdbc.update("""
                update viajes
                   set id_estado_viaje = (select id_estado_viaje from cat_estado_viaje where nombre = ?)
                 where id_viaje = ?
                """, estado, idViaje);
        if (marcarInicio) {
            jdbc.update("update viajes set fecha_inicio = coalesce(fecha_inicio, now()) where id_viaje = ?",
                    idViaje);
        }
        if (marcarFin) {
            jdbc.update("update viajes set fecha_inicio = coalesce(fecha_inicio, now()), fecha_fin = now() "
                    + "where id_viaje = ?", idViaje);
        }
    }

    private void estadoCarga(int idViaje, String estado) {
        jdbc.update("""
                update cargas
                   set id_estado_carga = (select id_estado_carga from cat_estado_carga where nombre = ?)
                 where id_carga = (select id_carga from viajes where id_viaje = ?)
                """, estado, idViaje);
    }

    private void estadoOperador(int idOperador, String estado) {
        jdbc.update("""
                update operadores
                   set id_estado_operador = (select id_estado_operador from cat_estado_operador where nombre = ?)
                 where id_operador = ?
                """, estado, idOperador);
    }

    private void estadoUnidad(int idUnidad, String estado) {
        jdbc.update("""
                update unidades
                   set id_estado_unidad = (select id_estado_unidad from cat_estado_unidad where nombre = ?)
                 where id_unidad = ?
                """, estado, idUnidad);
    }

    private void liberarSiCorresponde(int idOperador, int idUnidad) {
        Integer viajesOperador = jdbc.queryForObject("""
                select count(*)
                  from viajes v
                  join cat_estado_viaje ev on ev.id_estado_viaje = v.id_estado_viaje
                 where v.id_operador = ? and ev.nombre in """ + ESTADOS_ACTIVOS,
                Integer.class, idOperador);
        if (viajesOperador != null && viajesOperador == 0) {
            estadoOperador(idOperador, "DISPONIBLE");
        }
        Integer viajesUnidad = jdbc.queryForObject("""
                select count(*)
                  from viajes v
                  join cat_estado_viaje ev on ev.id_estado_viaje = v.id_estado_viaje
                 where v.id_unidad = ? and ev.nombre in """ + ESTADOS_ACTIVOS,
                Integer.class, idUnidad);
        if (viajesUnidad != null && viajesUnidad == 0) {
            estadoUnidad(idUnidad, "DISPONIBLE");
        }
    }

    private record ViajeActivo(int id, int idOperador, int idUnidad, String estado) {
    }

    private record ViajeSiguiente(int id, String estado) {
    }
}
