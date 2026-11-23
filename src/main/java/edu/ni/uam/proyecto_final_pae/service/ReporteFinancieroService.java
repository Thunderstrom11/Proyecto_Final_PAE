package edu.ni.uam.proyecto_final_pae.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteFinancieroService {

    private static final String PLANTILLA = "/edu/ni/uam/proyecto_final_pae/reportes/reporte-financiero.jrxml";

    public record Fila(String nombre, long viajes, String total) {
    }

    public void exportar(Path destino, String empresa, String periodo, String resumen,
                        List<Fila> operadores, List<Fila> unidades) throws IOException, JRException {
        List<Map<String, ?>> datos = new ArrayList<>(operadores.size() + unidades.size());
        agregarFilas(datos, "Operador", operadores);
        agregarFilas(datos, "Unidad", unidades);

        try (InputStream plantilla = ReporteFinancieroService.class.getResourceAsStream(PLANTILLA)) {
            if (plantilla == null) {
                throw new IOException("No se encontró la plantilla del reporte financiero.");
            }
            var reporte = JasperCompileManager.compileReport(plantilla);
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("EMPRESA", empresa);
            parametros.put("PERIODO", periodo);
            parametros.put("RESUMEN", resumen);
            var documento = JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(datos));

            Path directorio = destino.toAbsolutePath().getParent();
            Path temporal = Files.createTempFile(directorio, ".reporte-", ".pdf");
            try {
                try (OutputStream salida = Files.newOutputStream(temporal)) {
                    JasperExportManager.exportReportToPdfStream(documento, salida);
                }
                try {
                    Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporal);
            }
        }
    }

    private void agregarFilas(List<Map<String, ?>> datos, String seccion, List<Fila> filas) {
        for (Fila fila : filas) {
            datos.add(Map.of("seccion", seccion, "nombre", fila.nombre(), "viajes", fila.viajes(), "total", fila.total()));
        }
    }
}
