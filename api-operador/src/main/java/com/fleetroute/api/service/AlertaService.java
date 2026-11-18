package com.fleetroute.api.service;

import com.fleetroute.api.config.ApiException;
import com.fleetroute.api.dto.AlertaRequest;
import com.fleetroute.api.dto.AlertaResponse;
import com.fleetroute.api.dto.PanelResponse;
import com.fleetroute.api.security.Sesion;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
public class AlertaService {

    private static final String INSERTAR = """
            insert into alertas_operador
                (id_viaje, id_operador, id_unidad, latitud, longitud, ubicacion, mensaje, fecha_hora, atendida)
            values (?, ?, ?, ?, ?, ?, ?, now(), false)
            returning id_alerta, fecha_hora
            """;

    private final OperadorPanelService panelService;
    private final JdbcTemplate jdbc;

    public AlertaService(OperadorPanelService panelService, JdbcTemplate jdbc) {
        this.panelService = panelService;
        this.jdbc = jdbc;
    }

    public AlertaResponse registrar(Sesion sesion, AlertaRequest solicitud) {
        PanelResponse panel = panelService.panel(sesion);
        if (!panel.tieneViaje() || panel.viaje() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "No hay un viaje activo para asociar a la alerta.");
        }

        PanelResponse.Viaje viaje = panel.viaje();
        Double latitud = coordenadaValida(solicitud == null ? null : solicitud.latitud(),
                -90.0, 90.0);
        Double longitud = coordenadaValida(solicitud == null ? null : solicitud.longitud(),
                -180.0, 180.0);
        if (latitud == null) {
            latitud = viaje.latitudOrigen();
        }
        if (longitud == null) {
            longitud = viaje.longitudOrigen();
        }

        String ubicacion = solicitud == null ? null : limpiar(solicitud.ubicacion(), 200);
        if (ubicacion == null || ubicacion.isBlank()) {
            ubicacion = "Origen: " + (viaje.origen() == null ? "viaje activo" : viaje.origen());
        }
        String mensaje = solicitud == null ? null : limpiar(solicitud.mensaje(), 300);
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = "Alerta enviada desde la cabina web";
        }

        ZoneId zona = zona(sesion.zonaHoraria());
        String ubicacionFinal = ubicacion;
        String mensajeFinal = mensaje;
        return jdbc.queryForObject(INSERTAR, (rs, rowNum) -> {
            LocalDateTime fecha = rs.getObject("fecha_hora", LocalDateTime.class);
            return new AlertaResponse(
                    rs.getInt("id_alerta"),
                    fecha == null ? OffsetDateTime.now(zona) : fecha.atZone(zona).toOffsetDateTime(),
                    ubicacionFinal,
                    mensajeFinal);
        }, viaje.id(), sesion.idOperador(), viaje.idUnidad(), latitud, longitud, ubicacion, mensaje);
    }

    private static Double coordenadaValida(Double valor, double minimo, double maximo) {
        if (valor == null || valor.isNaN() || valor.isInfinite() || valor < minimo || valor > maximo) {
            return null;
        }
        return valor;
    }

    private static String limpiar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    private static ZoneId zona(String valor) {
        try {
            return valor == null || valor.isBlank() ? ZoneId.of("America/Managua") : ZoneId.of(valor);
        } catch (RuntimeException ex) {
            return ZoneId.of("America/Managua");
        }
    }
}
