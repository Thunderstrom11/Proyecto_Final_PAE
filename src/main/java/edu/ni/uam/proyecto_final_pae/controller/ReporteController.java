package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.ZonaHoraria;
import edu.ni.uam.proyecto_final_pae.reportes.*;
import edu.ni.uam.proyecto_final_pae.service.JasperReporteService;
import edu.ni.uam.proyecto_final_pae.service.ReporteService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Captura de filtros y visor; las consultas, Jasper y el render se ejecutan en tareas. */
public class ReporteController {
    private final ReporteService reportes;
    private final JasperReporteService jasper;
    private final ExecutorService trabajo = ejecutor("fleetroute-reportes");
    private final ExecutorService render = ejecutor("fleetroute-paginas");
    private Task<?> generacion;
    private Task<Image> renderizado;
    private DocumentoReporte documento;
    private long revision;
    private long revisionPagina;
    private int pagina;
    private float zoom = 1;
    private boolean listo;
    private boolean cerrado;
    private boolean exportando;

    @FXML private BorderPane raiz;
    @FXML private VBox panelFiltros;
    @FXML private ComboBox<TipoReporte> cmbTipo;
    @FXML private ComboBox<OpcionReporte> cmbOperador, cmbUnidad, cmbCliente;
    @FXML private ComboBox<AgrupacionTemporal> cmbAgrupacion;
    @FXML private ComboBox<TipoGrafico> cmbGrafico;
    @FXML private DatePicker dpDesde, dpHasta;
    @FXML private TextField txtCarga;
    @FXML private Label lblAgrupacion, lblCarga, lblEstado, lblTotales, lblPagina, lblZoom;
    @FXML private ImageView imagenPagina;
    @FXML private ScrollPane visor;
    @FXML private Button btnGenerar, btnExportar, btnAnterior, btnSiguiente, btnAcercar, btnAlejar, btnAjustar;
    @FXML private ProgressIndicator progreso;

    public ReporteController() { this(new ReporteService(), new JasperReporteService()); }
    public ReporteController(ReporteService reportes, JasperReporteService jasper) {
        this.reportes = reportes;
        this.jasper = jasper;
    }

    private static ExecutorService ejecutor(String nombre) {
        return Executors.newSingleThreadExecutor(tarea -> {
            Thread hilo = new Thread(tarea, nombre);
            hilo.setDaemon(true);
            return hilo;
        });
    }

    @FXML private void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList(TipoReporte.values()));
        cmbAgrupacion.setItems(FXCollections.observableArrayList(AgrupacionTemporal.values()));
        cmbAgrupacion.setValue(AgrupacionTemporal.MENSUAL);
        cmbTipo.setValue(TipoReporte.RESUMEN);
        configurarTipo();
        cmbTipo.valueProperty().addListener((o, a, b) -> { configurarTipo(); invalidar(); });
        for (ComboBox<?> combo : new ComboBox<?>[]{cmbOperador, cmbUnidad, cmbCliente, cmbAgrupacion, cmbGrafico}) {
            combo.valueProperty().addListener((o, a, b) -> invalidar());
        }
        dpDesde.valueProperty().addListener((o, a, b) -> invalidar());
        dpHasta.valueProperty().addListener((o, a, b) -> invalidar());
        dpDesde.getEditor().textProperty().addListener((o, a, b) -> invalidar());
        dpHasta.getEditor().textProperty().addListener((o, a, b) -> invalidar());
        txtCarga.textProperty().addListener((o, a, b) -> invalidar());
        raiz.sceneProperty().addListener((o, a, escena) -> {
            if (escena != null) {
                escena.windowProperty().addListener((w, anterior, ventana) -> {
                    if (ventana != null) ventana.addEventHandler(WindowEvent.WINDOW_HIDDEN, evento -> liberar());
                });
                if (escena.getWindow() != null) escena.getWindow().addEventHandler(WindowEvent.WINDOW_HIDDEN, evento -> liberar());
            }
        });
        actualizarBotones();
        cargarCatalogos();
    }

    private record Inicializacion(CatalogosReporte catalogos, LocalDate hoy) { }

    private void cargarCatalogos() {
        panelFiltros.setDisable(true);
        indicarCarga(true);
        lblEstado.setText("Cargando opciones del reporte…");
        Task<Inicializacion> tarea = new Task<>() {
            @Override protected Inicializacion call() {
                return new Inicializacion(reportes.catalogos(), LocalDate.now(ZonaHoraria.actual()));
            }
        };
        generacion = tarea;
        tarea.setOnSucceeded(evento -> {
            if (cerrado) return;
            var inicial = tarea.getValue();
            cargarOpciones(cmbOperador, inicial.catalogos().operadores(), false);
            cargarOpciones(cmbUnidad, inicial.catalogos().unidades(), false);
            cargarOpciones(cmbCliente, inicial.catalogos().clientes(), true);
            dpDesde.setValue(inicial.hoy().withDayOfMonth(1));
            dpHasta.setValue(inicial.hoy().withDayOfMonth(inicial.hoy().lengthOfMonth()));
            listo = true;
            btnGenerar.setText("Generar vista previa");
            panelFiltros.setDisable(false);
            indicarCarga(false);
            lblEstado.setText("Configure los filtros y genere la vista previa. Solo viajes finalizados.");
            actualizarBotones();
        });
        tarea.setOnFailed(evento -> {
            if (!cerrado) {
                indicarCarga(false);
                mostrarError("No se pudieron cargar las opciones", tarea.getException());
                btnGenerar.setText("Reintentar carga");
                btnGenerar.setDisable(false);
            }
        });
        trabajo.submit(tarea);
    }

    private void cargarOpciones(ComboBox<OpcionReporte> combo, java.util.List<OpcionReporte> opciones, boolean cliente) {
        combo.getItems().setAll(OpcionReporte.TODOS);
        if (cliente) combo.getItems().add(OpcionReporte.SIN_CLIENTE);
        combo.getItems().addAll(opciones);
        combo.setValue(OpcionReporte.TODOS);
    }

    private void configurarTipo() {
        TipoReporte tipo = cmbTipo.getValue();
        if (tipo == null) return;
        TipoGrafico anterior = cmbGrafico.getValue();
        cmbGrafico.getItems().setAll(tipo.graficos());
        cmbGrafico.setValue(anterior != null && tipo.graficos().contains(anterior) ? anterior : tipo.graficos().get(0));
        cmbAgrupacion.setVisible(tipo.temporal());
        cmbAgrupacion.setManaged(tipo.temporal());
        lblAgrupacion.setVisible(tipo.temporal());
        lblAgrupacion.setManaged(tipo.temporal());
        boolean detalle = tipo == TipoReporte.DETALLE;
        txtCarga.setVisible(detalle);
        txtCarga.setManaged(detalle);
        lblCarga.setVisible(detalle);
        lblCarga.setManaged(detalle);
        if (!detalle) txtCarga.clear();
    }

    private void invalidar() {
        revision++;
        revisionPagina++;
        documento = null;
        imagenPagina.setImage(null);
        lblTotales.setText("");
        if (renderizado != null) renderizado.cancel(true);
        if (listo) {
            if (generacion != null) generacion.cancel(true);
            indicarCarga(false);
            lblEstado.setText("Los filtros cambiaron. Genere una nueva vista previa.");
        }
        actualizarBotones();
    }

    @FXML private void generar() {
        if (cerrado || exportando) return;
        if (!listo) { btnGenerar.setDisable(true); cargarCatalogos(); return; }
        try {
            ReporteService.requerirPermiso();
            try {
                dpDesde.commitValue();
                dpHasta.commitValue();
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("Ingrese fechas de finalización válidas o selecciónelas en el calendario.");
            }
            FiltroReporte filtro = new FiltroReporte(cmbTipo.getValue(), dpDesde.getValue(), dpHasta.getValue(),
                    cmbOperador.getValue(), cmbUnidad.getValue(), cmbCliente.getValue(), txtCarga.getText(),
                    cmbAgrupacion.getValue(), cmbGrafico.getValue());
            invalidar();
            long solicitud = revision;
            indicarCarga(true);
            btnGenerar.setDisable(true);
            lblEstado.setText("Consultando viajes y generando el documento…");
            Task<DocumentoReporte> tarea = new Task<>() {
                @Override protected DocumentoReporte call() {
                    ResultadoReporte datos = reportes.generar(filtro);
                    return isCancelled() ? null : jasper.generar(datos);
                }
            };
            generacion = tarea;
            tarea.setOnSucceeded(evento -> {
                if (cerrado || solicitud != revision || tarea.getValue() == null) return;
                documento = tarea.getValue();
                indicarCarga(false);
                pagina = 0;
                zoom = 1;
                var datos = documento.datos();
                lblTotales.setText("Total facturado: " + FormatoReporte.dinero(datos.total(), datos.moneda())
                        + " | Viajes: " + datos.viajes() + " | Promedio: " + FormatoReporte.dinero(datos.promedio(), datos.moneda()));
                lblEstado.setText(datos.vacio() ? "No hay viajes que coincidan con los filtros."
                        : "Vista previa generada. El PDF conservará este documento.");
                mostrarPagina();
            });
            tarea.setOnFailed(evento -> {
                if (!cerrado && solicitud == revision) {
                    indicarCarga(false);
                    actualizarBotones();
                    mostrarError("No se pudo generar el reporte", tarea.getException());
                }
            });
            trabajo.submit(tarea);
        } catch (RuntimeException e) { mostrarError("Revise la configuración del reporte", e); }
    }

    private void mostrarPagina() {
        if (documento == null || cerrado) return;
        if (renderizado != null) renderizado.cancel(true);
        DocumentoReporte actual = documento;
        int numero = pagina;
        float escala = zoom;
        long solicitud = ++revisionPagina;
        imagenPagina.setImage(null);
        Task<Image> tarea = new Task<>() {
            @Override protected Image call() { return SwingFXUtils.toFXImage(jasper.pagina(actual, numero, escala), null); }
        };
        renderizado = tarea;
        tarea.setOnSucceeded(evento -> {
            if (!cerrado && solicitud == revisionPagina && documento == actual) {
                imagenPagina.setImage(tarea.getValue());
                visor.setVvalue(0);
                visor.setHvalue(0);
            }
        });
        tarea.setOnFailed(evento -> {
            if (!cerrado && solicitud == revisionPagina) {
                invalidar();
                mostrarError("No se pudo mostrar el reporte", tarea.getException());
            }
        });
        render.submit(tarea);
        actualizarBotones();
    }

    @FXML private void exportar() {
        if (documento == null || cerrado || exportando) return;
        try {
            ReporteService.requerirPermiso();
            FileChooser selector = new FileChooser();
            selector.setTitle("Exportar reporte a PDF");
            selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
            selector.setInitialFileName(documento.nombreArchivo());
            File archivo = selector.showSaveDialog(raiz.getScene().getWindow());
            if (archivo == null) return;
            if (!archivo.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) archivo = new File(archivo.getPath() + ".pdf");
            if (archivo.exists()) {
                Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION, "¿Reemplazar " + archivo.getName() + "?", ButtonType.YES, ButtonType.NO);
                confirmar.initOwner(raiz.getScene().getWindow());
                confirmar.setHeaderText("El archivo ya existe");
                if (confirmar.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
            }
            DocumentoReporte actual = documento;
            java.nio.file.Path destino = archivo.toPath();
            exportando = true;
            panelFiltros.setDisable(true);
            indicarCarga(true);
            actualizarBotones();
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() { jasper.exportar(actual, destino); return null; }
            };
            tarea.setOnSucceeded(evento -> terminarExportacion("PDF guardado en " + destino));
            tarea.setOnFailed(evento -> {
                terminarExportacion("No se pudo guardar el PDF.");
                if (!cerrado) mostrarError("No se pudo exportar el reporte", tarea.getException());
            });
            trabajo.submit(tarea);
        } catch (RuntimeException e) { mostrarError("No se pudo exportar el reporte", e); }
    }

    private void terminarExportacion(String mensaje) {
        if (cerrado) return;
        exportando = false;
        panelFiltros.setDisable(false);
        indicarCarga(false);
        lblEstado.setText(mensaje);
        actualizarBotones();
    }

    @FXML private void mesActual() { seleccionarMes(0); }
    @FXML private void mesAnterior() { seleccionarMes(-1); }
    private void seleccionarMes(int desplazamiento) {
        LocalDate inicio = LocalDate.now(ZonaHoraria.actual()).withDayOfMonth(1).plusMonths(desplazamiento);
        dpDesde.setValue(inicio);
        dpHasta.setValue(inicio.withDayOfMonth(inicio.lengthOfMonth()));
    }
    @FXML private void anterior() { if (documento != null && pagina > 0) { pagina--; mostrarPagina(); } }
    @FXML private void siguiente() { if (documento != null && pagina + 1 < documento.paginas()) { pagina++; mostrarPagina(); } }
    @FXML private void acercar() { zoom = Math.min(2f, zoom + .25f); mostrarPagina(); }
    @FXML private void alejar() { zoom = Math.max(.25f, zoom - .25f); mostrarPagina(); }
    @FXML private void ajustar() {
        if (documento == null) return;
        zoom = (float) Math.max(.25, Math.min(2, (visor.getViewportBounds().getWidth() - 32) / documento.impresion().getPageWidth()));
        mostrarPagina();
    }

    private void actualizarBotones() {
        boolean sinDocumento = documento == null;
        btnGenerar.setDisable(!listo || exportando);
        btnExportar.setDisable(sinDocumento || exportando);
        btnAnterior.setDisable(sinDocumento || pagina == 0);
        btnSiguiente.setDisable(sinDocumento || pagina + 1 >= documento.paginas());
        btnAcercar.setDisable(sinDocumento || zoom >= 2);
        btnAlejar.setDisable(sinDocumento || zoom <= .25f);
        btnAjustar.setDisable(sinDocumento);
        lblPagina.setText(sinDocumento ? "Sin vista previa" : "Página " + (pagina + 1) + " de " + documento.paginas());
        lblZoom.setText(Math.round(zoom * 100) + "%");
    }

    private void indicarCarga(boolean cargando) { progreso.setVisible(cargando); progreso.setManaged(cargando); }
    private void mostrarError(String titulo, Throwable error) {
        if (error instanceof SecurityException) invalidar();
        lblEstado.setText(titulo + ": " + (error.getMessage() == null ? "Intente de nuevo." : error.getMessage()));
        Alert alerta = new Alert(Alert.AlertType.ERROR, lblEstado.getText(), ButtonType.OK);
        if (raiz.getScene() != null) alerta.initOwner(raiz.getScene().getWindow());
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
    @FXML private void cerrar() { liberar(); ((Stage) raiz.getScene().getWindow()).close(); }
    private void liberar() {
        cerrado = true;
        revision++;
        revisionPagina++;
        if (generacion != null) generacion.cancel(true);
        if (renderizado != null) renderizado.cancel(true);
        trabajo.shutdownNow();
        render.shutdownNow();
        documento = null;
        imagenPagina.setImage(null);
    }
}
