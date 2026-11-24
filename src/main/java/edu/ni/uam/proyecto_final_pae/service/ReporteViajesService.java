package edu.ni.uam.proyecto_final_pae.service;

import edu.ni.uam.proyecto_final_pae.config.ZonaHoraria;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.reportes.DocumentoViajes;
import edu.ni.uam.proyecto_final_pae.reportes.FilaViajeOperativo;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import java.io.IOException;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Reporte operativo disponible para los administradores que pueden consultar Gestión de viajes. */
public class ReporteViajesService {
    public static final int MAX_FILAS = 10_000;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private JasperReport plantilla;

    public DocumentoViajes generar(List<FilaViajeOperativo> seleccion, String filtros) {
        Usuario solicitante = SesionActual.requerir(RolUsuario.ADMIN);
        List<FilaViajeOperativo> filas = List.copyOf(seleccion);
        if (filas.size() > MAX_FILAS) throw new IllegalArgumentException(
                "El reporte admite hasta " + MAX_FILAS + " viajes. Aplique más filtros antes de exportar.");
        var empresa = new EmpresaDAO().obtener().orElseThrow(() ->
                new IllegalStateException("Configure los datos de la empresa antes de exportar viajes."));
        ZonedDateTime generado = ZonedDateTime.now(ZonaHoraria.actual());
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("EMPRESA", empresa.getNombre());
        parametros.put("GENERADO", "Generado: " + generado.format(FECHA) + " | " + generado.getZone());
        parametros.put("FILTROS", filtros == null || filtros.isBlank() ? "Estado: Todos | Sin filtros adicionales" : filtros);
        parametros.put("TOTAL", "Viajes incluidos: " + filas.size());
        parametros.put("VACIO", filas.isEmpty());
        List<Map<String, ?>> datos = filas.stream().<Map<String, ?>>map(fila -> Map.of(
                "viaje", "#" + fila.id(), "carga", fila.carga(), "operador", fila.operador(),
                "unidad", fila.unidad(), "origen", fila.origen(), "destino", fila.destino(),
                "estado", fila.estado(), "limite", fila.fechaLimite() == null ? "Sin fecha" : fila.fechaLimite().format(FECHA))).toList();
        try {
            JasperPrint impresion = JasperFillManager.fillReport(plantilla(), parametros, new JRMapCollectionDataSource(datos));
            if (SesionActual.requerir(RolUsuario.ADMIN) != solicitante) throw new SecurityException("La sesión cambió durante la generación.");
            return new DocumentoViajes(impresion, generado, filas.size(), solicitante);
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte de viajes.", e);
        }
    }

    private synchronized JasperReport plantilla() throws JRException, IOException {
        if (plantilla == null) {
            try (var entrada = getClass().getResourceAsStream("/edu/ni/uam/proyecto_final_pae/reportes/viajes.jrxml")) {
                if (entrada == null) throw new IOException("No se encontró la plantilla del reporte de viajes.");
                plantilla = JasperCompileManager.compileReport(entrada);
            }
        }
        return plantilla;
    }

    public void exportar(DocumentoViajes documento, Path destino) {
        validar(documento);
        Path temporal = null;
        try {
            Path absoluto = destino.toAbsolutePath();
            temporal = Files.createTempFile(absoluto.getParent(), ".fleetroute-viajes-", ".tmp");
            JasperExportManager.exportReportToPdfFile(documento.impresion(), temporal.toString());
            validar(documento);
            try {
                Files.move(temporal, absoluto, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo guardar el PDF de viajes. Revise los permisos de la carpeta y que el archivo no esté abierto.", e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException e) { temporal.toFile().deleteOnExit(); }
            }
        }
    }

    private void validar(DocumentoViajes documento) {
        if (SesionActual.requerir(RolUsuario.ADMIN) != documento.solicitante()) {
            throw new SecurityException("El reporte pertenece a otra sesión. Genere una nueva exportación.");
        }
    }
}
