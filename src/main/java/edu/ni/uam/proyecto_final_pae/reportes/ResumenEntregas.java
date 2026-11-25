package edu.ni.uam.proyecto_final_pae.reportes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record ResumenEntregas(int viajes, int aTiempo, int tardias, int sinDatos) {
    public static ResumenEntregas de(List<EntregaReporte> entregas) {
        int puntuales = 0, atrasadas = 0, incompletas = 0;
        for (EntregaReporte entrega : entregas) {
            if (!entrega.evaluable()) incompletas++;
            else if (entrega.aTiempo()) puntuales++;
            else atrasadas++;
        }
        return new ResumenEntregas(entregas.size(), puntuales, atrasadas, incompletas);
    }

    public String porcentaje() {
        int evaluables = aTiempo + tardias;
        return evaluables == 0 ? "No aplica" : BigDecimal.valueOf(aTiempo).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(evaluables), 1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " %";
    }

    public String descripcion() {
        return "Entregas: " + viajes + " | A tiempo: " + aTiempo + " | Tardías: " + tardias
                + " | Sin datos: " + sinDatos + " | Cumplimiento: " + porcentaje();
    }
}
