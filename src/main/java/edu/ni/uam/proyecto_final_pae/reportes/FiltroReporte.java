package edu.ni.uam.proyecto_final_pae.reportes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record FiltroReporte(TipoReporte tipo, LocalDate desde, LocalDate hasta,
                            OpcionReporte operador, OpcionReporte unidad, OpcionReporte cliente,
                            String numeroCarga, AgrupacionTemporal agrupacion, TipoGrafico grafico, EstadoReporte estado) {
    public static final int MAX_DIAS = 3660;
    public static final int MAX_PERIODOS = 366;
    public static final int MAX_FILAS = 10_000;

    public FiltroReporte(TipoReporte tipo, LocalDate desde, LocalDate hasta, OpcionReporte operador,
                         OpcionReporte unidad, OpcionReporte cliente, String numeroCarga,
                         AgrupacionTemporal agrupacion, TipoGrafico grafico) {
        this(tipo, desde, hasta, operador, unidad, cliente, numeroCarga, agrupacion, grafico, null);
    }

    public FiltroReporte {
        Objects.requireNonNull(tipo, "Seleccione el tipo de reporte.");
        if ((tipo.finalizados() && (desde == null || hasta == null)) || (desde == null) != (hasta == null)) {
            throw new IllegalArgumentException("Seleccione ambas fechas del período.");
        }
        if (desde != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta.");
        if (hasta != null && (hasta.equals(LocalDate.MAX) || ChronoUnit.DAYS.between(desde, hasta) >= MAX_DIAS)) {
            throw new IllegalArgumentException("Seleccione un período de hasta " + MAX_DIAS + " días.");
        }
        operador = operador == null ? OpcionReporte.TODOS : operador;
        unidad = unidad == null ? OpcionReporte.TODOS : unidad;
        cliente = cliente == null ? OpcionReporte.TODOS : cliente;
        numeroCarga = numeroCarga == null ? "" : numeroCarga.trim();
        if (!tipo.detallado() && !numeroCarga.isEmpty()) {
            throw new IllegalArgumentException("El número de carga solo se aplica al detalle de viajes.");
        }
        if (numeroCarga.length() > 30) throw new IllegalArgumentException("El número de carga admite hasta 30 caracteres.");
        agrupacion = agrupacion == null ? AgrupacionTemporal.MENSUAL : agrupacion;
        if (grafico == null || !tipo.graficos().contains(grafico)) {
            throw new IllegalArgumentException("Seleccione un gráfico compatible con el reporte.");
        }
        estado = estado == null ? (tipo.finalizados() ? EstadoReporte.FINALIZADO : EstadoReporte.TODOS) : estado;
        if (tipo.finalizados() && estado != EstadoReporte.FINALIZADO) {
            throw new IllegalArgumentException("Este reporte solo incluye viajes finalizados.");
        }
        if (tipo.temporal()) {
            int periodos = 0;
            for (LocalDate dia = agrupacion.inicio(desde); !dia.isAfter(hasta); dia = agrupacion.siguiente(dia)) {
                if (++periodos > MAX_PERIODOS) throw new IllegalArgumentException(
                        "El reporte admite hasta " + MAX_PERIODOS + " períodos. Use agrupación semanal o mensual.");
            }
        }
    }

    public LocalDateTime desdeInclusivo() { return desde == null ? null : desde.atStartOfDay(); }
    public LocalDateTime hastaExclusivo() { return hasta == null ? null : hasta.plusDays(1).atStartOfDay(); }

    public String descripcion() {
        String periodo = tipo.finalizados() ? "Finalización: " : "Creación del viaje: ";
        return periodo + (desde == null ? "Todas las fechas" : desde + " a " + hasta) + " | Estado: " + estado + "\n"
                + "Operador: " + operador + " | Unidad: " + unidad + " | Cliente: " + cliente
                + (numeroCarga.isEmpty() ? "" : " | Carga exacta: " + numeroCarga)
                + (tipo.temporal() ? "\nAgrupación: " + agrupacion : "") + " | Gráfico: " + grafico;
    }
}
