package com.fleetroute.api.service;

import com.fleetroute.api.dto.EmpresaInfo;
import com.fleetroute.api.dto.OperadorInfo;
import com.fleetroute.api.dto.PanelResponse;
import com.fleetroute.api.security.Sesion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class OperadorPanelService {

    private static final String ESTADOS_ACTIVOS = "('PROGRAMADO','EN_CURSO','PAUSADO')";

    private static final String SELECT_VIAJE = """
            select v.id_viaje, v.id_unidad, ev.nombre as estado,
                   c.numero_carga, c.descripcion as descripcion_carga, c.peso_toneladas,
                   c.fecha_limite_cliente,
                   u.numero_unidad, u.placas, u.modelo,
                   o.nombre as operador_nombre, o.cedula,
                   o.horas_max_conduccion, o.horas_min_descanso,
                   vo.nombre as origen, vo.departamento as origen_departamento,
                   vo.pais as origen_pais, vo.latitud as origen_lat, vo.longitud as origen_lng,
                   vd.nombre as destino, vd.departamento as destino_departamento,
                   vd.pais as destino_pais, vd.latitud as destino_lat, vd.longitud as destino_lng,
                   v.tiempo_estimado, v.cobro_viaje, v.fecha_inicio
              from viajes v
              join cat_estado_viaje ev on ev.id_estado_viaje = v.id_estado_viaje
              join cargas c on c.id_carga = v.id_carga
              join unidades u on u.id_unidad = v.id_unidad
              join operadores o on o.id_operador = v.id_operador
              join ubicaciones vo on vo.id_ubicacion = v.origen
              join ubicaciones vd on vd.id_ubicacion = v.destino
            """;

    private static final String VIAJE_ACTIVO = SELECT_VIAJE + """
             where v.id_operador = ? and ev.nombre in """ + ESTADOS_ACTIVOS + """
             order by case ev.nombre when 'EN_CURSO' then 0 when 'PAUSADO' then 1 else 2 end, v.id_viaje
             limit 1
            """;

    private static final String VIAJE_SIGUIENTE = SELECT_VIAJE + """
             where v.id_viaje_previo = ?
             limit 1
            """;

    private static final String EVENTOS = """
            select rj.num_evento, te.nombre as tipo, rj.fecha_hora, rj.observacion
              from registro_jornada rj
              join cat_tipo_evento te on te.id_tipo_evento = rj.id_tipo_evento
             where rj.id_viaje = ?
             order by rj.num_evento
            """;

    private final JdbcTemplate jdbc;

    public OperadorPanelService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PanelResponse panel(Sesion sesion) {
        ZoneId zona = zona(sesion.zonaHoraria());
        OffsetDateTime actualizado = OffsetDateTime.now(zona);
        EmpresaInfo empresa = new EmpresaInfo(
                sesion.empresa() == null || sesion.empresa().isBlank()
                        ? "FleetRoute Pro" : sesion.empresa(),
                zona.getId(),
                sesion.moneda() == null || sesion.moneda().isBlank() ? "NIO" : sesion.moneda());
        OperadorInfo operador = new OperadorInfo(sesion.idOperador(), sesion.username(),
                sesion.nombreOperador(), sesion.cedula());

        List<ViajeFila> activos = jdbc.query(VIAJE_ACTIVO, this::mapearViaje, sesion.idOperador());
        if (activos.isEmpty()) {
            return new PanelResponse(operador, empresa, actualizado, false, null, null, null);
        }

        ViajeFila viaje = activos.get(0);
        List<JornadaCalculator.Evento> eventosCalculo = jdbc.query(EVENTOS,
                (rs, rowNum) -> new JornadaCalculator.Evento(
                        rs.getInt("num_evento"),
                        rs.getString("tipo"),
                        rs.getObject("fecha_hora", LocalDateTime.class),
                        rs.getString("observacion")),
                viaje.id());

        JornadaCalculator.Resultado resultado = JornadaCalculator.calcular(
                new JornadaCalculator.Datos(viaje.fechaLimite(), viaje.tiempoEstimado(),
                        viaje.horasMaxConduccion(), viaje.horasMinDescanso()),
                eventosCalculo, LocalDateTime.now(zona));

        List<ViajeFila> siguientes = jdbc.query(VIAJE_SIGUIENTE, this::mapearViaje, viaje.id());
        PanelResponse.Siguiente siguiente = siguientes.isEmpty() ? null : siguiente(siguientes.get(0), zona);

        return new PanelResponse(operador, empresa, actualizado, true,
                viaje(viaje, zona), jornada(resultado, zona), siguiente);
    }

    private PanelResponse.Viaje viaje(ViajeFila fila, ZoneId zona) {
        return new PanelResponse.Viaje(
                fila.id(), fila.idUnidad(), fila.estado(), fila.numeroCarga(), fila.descripcionCarga(), fila.pesoToneladas(),
                fila.numeroUnidad(), fila.placas(), fila.modeloUnidad(),
                fila.origen(), fila.departamentoOrigen(), fila.paisOrigen(),
                fila.latitudOrigen(), fila.longitudOrigen(),
                fila.destino(), fila.departamentoDestino(), fila.paisDestino(),
                fila.latitudDestino(), fila.longitudDestino(),
                offset(fila.fechaLimite(), zona), fila.tiempoEstimado(), fila.cobroViaje(),
                fila.horasMaxConduccion(), fila.horasMinDescanso(), offset(fila.fechaInicio(), zona));
    }

    private PanelResponse.Jornada jornada(JornadaCalculator.Resultado resultado, ZoneId zona) {
        return new PanelResponse.Jornada(
                resultado.paso().name(), resultado.paso().getEtiqueta(),
                resultado.semaforo().name(), resultado.semaforo().getEtiqueta(),
                resultado.semaforo().getColor(), resultado.mensaje(),
                resultado.porcentajeManejo(),
                resultado.manejo().toMinutes(), resultado.descanso().toMinutes(),
                resultado.descansoRestante().toMinutes(),
                resultado.manejoTexto(), resultado.descansoTexto(),
                resultado.descansoRestanteTexto(), resultado.descansoMinimoTexto(),
                offset(resultado.llegadaProyectada(), zona), resultado.puedeReanudar());
    }

    private PanelResponse.Siguiente siguiente(ViajeFila fila, ZoneId zona) {
        return new PanelResponse.Siguiente(fila.id(), fila.estado(), fila.numeroCarga(),
                fila.origen(), fila.destino(), fila.numeroUnidad(),
                offset(fila.fechaLimite(), zona), fila.tiempoEstimado());
    }

    private ViajeFila mapearViaje(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ViajeFila(
                rs.getInt("id_viaje"),
                rs.getInt("id_unidad"),
                rs.getString("estado"),
                rs.getString("numero_carga"),
                rs.getString("descripcion_carga"),
                rs.getBigDecimal("peso_toneladas"),
                rs.getString("numero_unidad"),
                rs.getString("placas"),
                rs.getString("modelo"),
                rs.getString("origen"),
                rs.getString("origen_departamento"),
                rs.getString("origen_pais"),
                rs.getObject("origen_lat", Double.class),
                rs.getObject("origen_lng", Double.class),
                rs.getString("destino"),
                rs.getString("destino_departamento"),
                rs.getString("destino_pais"),
                rs.getObject("destino_lat", Double.class),
                rs.getObject("destino_lng", Double.class),
                rs.getObject("fecha_limite_cliente", LocalDateTime.class),
                rs.getBigDecimal("tiempo_estimado"),
                rs.getBigDecimal("cobro_viaje"),
                rs.getBigDecimal("horas_max_conduccion"),
                rs.getBigDecimal("horas_min_descanso"),
                rs.getObject("fecha_inicio", LocalDateTime.class));
    }

    private static OffsetDateTime offset(LocalDateTime valor, ZoneId zona) {
        return valor == null ? null : valor.atZone(zona).toOffsetDateTime();
    }

    private static ZoneId zona(String valor) {
        if (valor != null && !valor.isBlank()) {
            try {
                return ZoneId.of(valor);
            } catch (RuntimeException ignored) {
                // La empresa pudo haber guardado una zona inválida; se usa la zona del proyecto.
            }
        }
        return ZoneId.of("America/Managua");
    }

    private record ViajeFila(
            int id,
            int idUnidad,
            String estado,
            String numeroCarga,
            String descripcionCarga,
            BigDecimal pesoToneladas,
            String numeroUnidad,
            String placas,
            String modeloUnidad,
            String origen,
            String departamentoOrigen,
            String paisOrigen,
            Double latitudOrigen,
            Double longitudOrigen,
            String destino,
            String departamentoDestino,
            String paisDestino,
            Double latitudDestino,
            Double longitudDestino,
            LocalDateTime fechaLimite,
            BigDecimal tiempoEstimado,
            BigDecimal cobroViaje,
            BigDecimal horasMaxConduccion,
            BigDecimal horasMinDescanso,
            LocalDateTime fechaInicio) {
    }
}
