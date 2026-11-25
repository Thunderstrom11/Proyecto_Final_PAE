package edu.ni.uam.proyecto_final_pae.service;

import edu.ni.uam.proyecto_final_pae.reportes.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JasperReporteService {
    private record CategoriaGrafico(int indice, String etiqueta) implements Comparable<CategoriaGrafico> {
        @Override public int compareTo(CategoriaGrafico otra) { return Integer.compare(indice, otra.indice); }
        @Override public String toString() { return etiqueta; }
    }
    private final Map<String, JasperReport> plantillas = new HashMap<>();
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DocumentoReporte generar(ResultadoReporte datos) {
        var solicitante = ReporteService.requerirPermiso();
        try {
            String recurso = datos.filtro().tipo() == TipoReporte.DETALLE ? "detalle" : "financiero";
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("EMPRESA", datos.empresa());
            parametros.put("TITULO", datos.filtro().tipo().toString());
            parametros.put("FILTROS", datos.filtro().descripcion());
            parametros.put("GENERADO", "Generado: " + datos.generado().format(FECHA) + " | "
                    + datos.generado().getZone() + " | Moneda: " + datos.moneda());
            parametros.put("METRICAS", "Total facturado: " + FormatoReporte.dinero(datos.total(), datos.moneda())
                    + "     Viajes: " + datos.viajes() + "     Promedio: " + FormatoReporte.dinero(datos.promedio(), datos.moneda()));
            parametros.put("CATEGORIA", switch (datos.filtro().tipo()) {
                case OPERADOR -> "Operador";
                case UNIDAD -> "Unidad";
                default -> "Período de finalización";
            });
            parametros.put("GRAFICO", grafico(datos));
            parametros.put("NOTA_GRAFICO", !datos.filtro().tipo().temporal() && datos.filas().size() > 10
                    ? "Gráfico: diez categorías principales y Otros. La tabla incluye todas las categorías." : "");
            parametros.put("VACIO", datos.vacio());
            JasperPrint impresion = JasperFillManager.fillReport(plantilla(recurso), parametros,
                    new JRMapCollectionDataSource(filas(datos)));
            if (ReporteService.requerirPermiso() != solicitante) throw new SecurityException("La sesión cambió durante la generación.");
            return new DocumentoReporte(datos, impresion, solicitante);
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo generar el documento del reporte.", e);
        }
    }

    private synchronized JasperReport plantilla(String nombre) throws IOException, JRException {
        JasperReport compilada = plantillas.get(nombre);
        if (compilada == null) {
            String ruta = "/edu/ni/uam/proyecto_final_pae/reportes/" + nombre + ".jrxml";
            try (InputStream entrada = getClass().getResourceAsStream(ruta)) {
                if (entrada == null) throw new IOException("No se encontró la plantilla " + ruta);
                compilada = JasperCompileManager.compileReport(entrada);
                plantillas.put(nombre, compilada);
            }
        }
        return compilada;
    }

    public BufferedImage pagina(DocumentoReporte documento, int indice, float escala) {
        validar(documento);
        if (indice < 0 || indice >= documento.paginas() || !Float.isFinite(escala) || escala < 0.25f || escala > 2f) {
            throw new IllegalArgumentException("Página o escala fuera de rango.");
        }
        try {
            return (BufferedImage) JasperPrintManager.printPageToImage(documento.impresion(), indice, escala);
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo mostrar la página del reporte.", e);
        }
    }

    public void exportar(DocumentoReporte documento, Path destino) {
        validar(documento);
        Path temporal = null;
        try {
            Path absoluto = destino.toAbsolutePath();
            temporal = Files.createTempFile(absoluto.getParent(), ".fleetroute-reporte-", ".tmp");
            JasperExportManager.exportReportToPdfFile(documento.impresion(), temporal.toString());
            validar(documento);
            try {
                Files.move(temporal, absoluto, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo guardar el PDF. Revise los permisos de la carpeta y que el archivo no esté abierto.", e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException e) { temporal.toFile().deleteOnExit(); }
            }
        }
    }

    private void validar(DocumentoReporte documento) {
        ReporteService.validarDocumento(documento);
    }

    private Collection<Map<String, ?>> filas(ResultadoReporte datos) {
        List<Map<String, ?>> filas = new ArrayList<>();
        if (datos.filtro().tipo() == TipoReporte.DETALLE) {
            for (ViajeReporte viaje : datos.detalle()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("viaje", "#" + viaje.id());
                fila.put("carga", viaje.carga());
                fila.put("cliente", viaje.cliente());
                fila.put("operador", viaje.operador() + " (#" + viaje.operadorId() + ")");
                fila.put("unidad", viaje.unidad());
                fila.put("inicio", viaje.inicio() == null ? "Sin registro" : viaje.inicio().format(FECHA));
                fila.put("fin", viaje.fin().format(FECHA));
                fila.put("importe", FormatoReporte.dinero(viaje.importe(), datos.moneda()));
                filas.add(fila);
            }
        } else if (!datos.vacio()) {
            for (FilaReporte fila : datos.filas()) filas.add(Map.of("categoria", fila.categoria(),
                    "viajes", String.valueOf(fila.viajes()), "total", FormatoReporte.dinero(fila.total(), datos.moneda())));
        }
        return filas;
    }

    private BufferedImage grafico(ResultadoReporte datos) {
        if (datos.vacio() || datos.filtro().grafico() == TipoGrafico.NINGUNO) return null;
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        int salto = (int) Math.ceil(datos.series().size() / 12d);
        for (int i = 0; i < datos.series().size(); i++) {
            FilaReporte fila = datos.series().get(i);
            String etiqueta = fila.categoria();
            if (datos.filtro().tipo().temporal()) {
                etiqueta = i % salto == 0 || i == datos.series().size() - 1 ? etiqueta.substring(0, 10) : "";
            } else if (etiqueta.length() > 60) {
                int id = etiqueta.lastIndexOf(" (#");
                etiqueta = etiqueta.substring(0, 44) + "…" + (id < 0 ? "" : etiqueta.substring(id));
            }
            dataset.addValue(fila.total(), "Total facturado", new CategoriaGrafico(i, etiqueta));
        }
        boolean horizontal = datos.filtro().grafico() == TipoGrafico.BARRAS;
        JFreeChart grafico = datos.filtro().grafico() == TipoGrafico.LINEAS
                ? ChartFactory.createLineChart(null, null, datos.moneda(), dataset, PlotOrientation.VERTICAL, false, false, false)
                : ChartFactory.createBarChart(null, null, datos.moneda(), dataset,
                    horizontal ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, false, false, false);
        grafico.setBackgroundPaint(Color.WHITE);
        var plot = grafico.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(225, 231, 239));
        plot.setOutlineVisible(false);
        plot.getRenderer().setSeriesPaint(0, new Color(35, 105, 170));
        plot.getDomainAxis().setTickLabelFont(new Font("DejaVu Sans", Font.PLAIN, horizontal ? 12 : 10));
        plot.getRangeAxis().setTickLabelFont(new Font("DejaVu Sans", Font.PLAIN, 11));
        plot.getDomainAxis().setMaximumCategoryLabelLines(horizontal ? 3 : 2);
        if (!horizontal) plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        return grafico.createBufferedImage(1080, 500);
    }
}
