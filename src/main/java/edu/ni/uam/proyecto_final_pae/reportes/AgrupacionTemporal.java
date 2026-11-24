package edu.ni.uam.proyecto_final_pae.reportes;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

public enum AgrupacionTemporal {
    DIARIA("Diaria"), SEMANAL("Semanal (lunes)"), MENSUAL("Mensual");
    private final String nombre;
    AgrupacionTemporal(String nombre) { this.nombre = nombre; }

    public LocalDate inicio(LocalDate fecha) {
        return switch (this) {
            case DIARIA -> fecha;
            case SEMANAL -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MENSUAL -> fecha.withDayOfMonth(1);
        };
    }

    public LocalDate siguiente(LocalDate fecha) {
        return switch (this) {
            case DIARIA -> fecha.plusDays(1);
            case SEMANAL -> fecha.plusWeeks(1);
            case MENSUAL -> fecha.plusMonths(1);
        };
    }

    @Override public String toString() { return nombre; }
}
