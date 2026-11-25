package edu.ni.uam.proyecto_final_pae.reportes;

import edu.ni.uam.proyecto_final_pae.model.Usuario;
import net.sf.jasperreports.engine.JasperPrint;

/** Documento ya generado: la exportación nunca vuelve a consultar viajes. */
public record DocumentoReporte(ResultadoReporte datos, JasperPrint impresion, Usuario solicitante) implements DocumentoGenerado {
    public int paginas() { return impresion.getPages().size(); }
    public String nombreArchivo() {
        return "FleetRoute_" + datos.filtro().tipo().name().toLowerCase(java.util.Locale.ROOT)
                + "_" + datos.filtro().desde() + "_" + datos.filtro().hasta() + ".pdf";
    }
}
