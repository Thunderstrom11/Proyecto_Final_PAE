package edu.ni.uam.proyecto_final_pae.reportes;

import edu.ni.uam.proyecto_final_pae.model.Usuario;
import net.sf.jasperreports.engine.JasperPrint;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public record DocumentoViajes(JasperPrint impresion, ZonedDateTime generado, int viajes, Usuario solicitante) implements DocumentoGenerado {
    public String nombreArchivo() {
        return "FleetRoute_viajes_" + generado.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")) + ".pdf";
    }
}
