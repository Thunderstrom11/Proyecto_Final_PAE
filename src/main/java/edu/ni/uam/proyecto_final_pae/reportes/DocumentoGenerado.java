package edu.ni.uam.proyecto_final_pae.reportes;

import edu.ni.uam.proyecto_final_pae.model.Usuario;
import net.sf.jasperreports.engine.JasperPrint;

/** Documento asociado a la sesión que lo generó, listo para el visor Jasper. */
public sealed interface DocumentoGenerado permits DocumentoReporte, DocumentoViajes, DocumentoEntregas {
    JasperPrint impresion();
    Usuario solicitante();
    String nombreArchivo();
}
