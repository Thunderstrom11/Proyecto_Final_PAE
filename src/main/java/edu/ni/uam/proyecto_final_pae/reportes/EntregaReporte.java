package edu.ni.uam.proyecto_final_pae.reportes;

import java.time.Duration;
import java.time.LocalDateTime;

/** Proyección de las fechas registradas del viaje y del plazo vigente de su carga. */
public record EntregaReporte(Integer id, String carga, String cliente, String operador, String unidad,
                             LocalDateTime limite, LocalDateTime fin) {
    public boolean evaluable() { return limite != null && fin != null; }
    public boolean aTiempo() { return evaluable() && !fin.isAfter(limite); }
    public String resultado() { return !evaluable() ? "Sin datos" : aTiempo() ? "A tiempo" : "Tardía"; }

    public String atraso() {
        if (!evaluable()) return "—";
        if (aTiempo()) return "0 min";
        long minutos = Duration.between(limite, fin).toMinutes();
        if (minutos == 0) return "< 1 min";
        return (minutos >= 60 ? minutos / 60 + " h " : "") + minutos % 60 + " min";
    }
}
