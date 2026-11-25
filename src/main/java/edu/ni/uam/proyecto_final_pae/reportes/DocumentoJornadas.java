package edu.ni.uam.proyecto_final_pae.reportes;

import edu.ni.uam.proyecto_final_pae.model.Usuario;
import net.sf.jasperreports.engine.JasperPrint;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Reporte de jornadas ya generado; el visor guarda e imprime este mismo documento. */
public record DocumentoJornadas(JasperPrint impresion, ZonedDateTime generado, int viajes, int incumplimientos,
                                Usuario solicitante) implements DocumentoGenerado {
    public String nombreArchivo() {
        return "FleetRoute_jornadas_" + generado.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")) + ".pdf";
    }
}
