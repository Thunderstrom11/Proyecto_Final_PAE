package edu.ni.uam.proyecto_final_pae.reportes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record ResultadoReporte(FiltroReporte filtro, String empresa, String moneda,
                                ZonedDateTime generado, List<ViajeReporte> detalle,
                                List<FilaReporte> filas, List<FilaReporte> series,
                                BigDecimal total, long viajes, BigDecimal promedio) {
    public ResultadoReporte {
        detalle = List.copyOf(detalle);
        filas = List.copyOf(filas);
        series = List.copyOf(series);
    }
    public boolean vacio() { return viajes == 0; }
}
