package edu.ni.uam.proyecto_final_pae.service;

import edu.ni.uam.proyecto_final_pae.config.ZonaHoraria;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.ReporteDAO;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.reportes.*;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReporteEntregasService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm:ss");
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private JasperReport plantilla;

    public DocumentoEntregas generar(FiltroReporte filtro) {
        Usuario solicitante = SesionActual.requerir(RolUsuario.ADMIN);
        var filas = new ReporteDAO().consultarEntregas(filtro);
        var empresa = new EmpresaDAO().obtener().orElseThrow(() ->
                new IllegalStateException("Configure los datos de la empresa antes de generar reportes."));
        if (SesionActual.requerir(RolUsuario.ADMIN) != solicitante) throw new SecurityException("La sesión cambió durante la consulta.");
        var documento = generar(filas, filtro, empresa.getNombre(), ZonedDateTime.now(ZonaHoraria.actual()));
        if (documento.solicitante() != solicitante) throw new SecurityException("La sesión cambió durante la generación.");
        return documento;
    }

    DocumentoEntregas generar(List<EntregaReporte> seleccion, FiltroReporte filtro, String empresa, ZonedDateTime generado) {
        Usuario solicitante = SesionActual.requerir(RolUsuario.ADMIN);
        if (filtro.tipo() != TipoReporte.CUMPLIMIENTO_ENTREGAS) throw new IllegalArgumentException("Seleccione Cumplimiento de entregas.");
        List<EntregaReporte> filas = List.copyOf(seleccion);
        if (filas.size() > FiltroReporte.MAX_FILAS) throw new IllegalArgumentException("Aplique más filtros antes de generar el reporte.");
        ResumenEntregas resumen = ResumenEntregas.de(filas);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("EMPRESA", empresa);
        parametros.put("GENERADO", "Generado: " + generado.format(GENERADO) + " | " + generado.getZone());
        parametros.put("FILTROS", filtro.descripcion());
        parametros.put("METRICAS", resumen.descripcion());
        parametros.put("VACIO", filas.isEmpty());
        List<Map<String, ?>> datos = filas.stream().<Map<String, ?>>map(fila -> Map.of(
                "viaje", "#" + fila.id(), "carga", fila.carga(), "cliente", fila.cliente(),
                "operador", fila.operador(), "unidad", fila.unidad(),
                "limite", fila.limite() == null ? "Sin registro" : fila.limite().format(FECHA),
                "fin", fila.fin() == null ? "Sin registro" : fila.fin().format(FECHA),
                "resultado", fila.resultado(), "atraso", fila.atraso())).toList();
        try {
            JasperPrint impresion = JasperFillManager.fillReport(plantilla(), parametros, new JRMapCollectionDataSource(datos));
            if (SesionActual.requerir(RolUsuario.ADMIN) != solicitante) throw new SecurityException("La sesión cambió durante la generación.");
            return new DocumentoEntregas(impresion, generado, resumen, solicitante);
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte de cumplimiento de entregas.", e);
        }
    }

    private synchronized JasperReport plantilla() throws JRException, IOException {
        if (plantilla == null) {
            try (var entrada = getClass().getResourceAsStream("/edu/ni/uam/proyecto_final_pae/reportes/entregas.jrxml")) {
                if (entrada == null) throw new IOException("No se encontró la plantilla de cumplimiento de entregas.");
                plantilla = JasperCompileManager.compileReport(entrada);
            }
        }
        return plantilla;
    }
}
