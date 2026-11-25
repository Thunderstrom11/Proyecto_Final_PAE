package edu.ni.uam.proyecto_final_pae.reportes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Viaje con su bitácora de jornada, copiado fuera de Hibernate para calcular el reporte
 * sin entidades ni relaciones diferidas.
 */
public record ViajeJornada(Integer id, String carga, Integer operadorId, String operador, String unidad,
                           String estado, LocalDateTime fechaInicio, BigDecimal horasMaxConduccion,
                           BigDecimal horasMinDescanso, List<Evento> eventos) {

    /** Evento de registro_jornada: tipo del catálogo y marca de tiempo. */
    public record Evento(String tipo, LocalDateTime fechaHora) { }

    public ViajeJornada {
        if (id == null || id <= 0) throw new IllegalArgumentException("Hay un viaje sin identificador válido.");
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
    }

    /** Constructor de proyección HQL; la bitácora se agrega después con {@link #conEventos}. */
    public ViajeJornada(Integer id, String carga, Integer operadorId, String operador, String unidad, String estado,
                        LocalDateTime fechaInicio, BigDecimal horasMaxConduccion, BigDecimal horasMinDescanso) {
        this(id, carga, operadorId, operador, unidad, estado, fechaInicio, horasMaxConduccion, horasMinDescanso, List.of());
    }

    public ViajeJornada conEventos(List<Evento> bitacora) {
        return new ViajeJornada(id, carga, operadorId, operador, unidad, estado, fechaInicio,
                horasMaxConduccion, horasMinDescanso, bitacora);
    }
}
