package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.ZonaHoraria;
import edu.ni.uam.proyecto_final_pae.reportes.*;
import edu.ni.uam.proyecto_final_pae.service.JasperReporteService;
import edu.ni.uam.proyecto_final_pae.service.ReporteService;
import edu.ni.uam.proyecto_final_pae.service.ReporteViajesService;
import edu.ni.uam.proyecto_final_pae.service.ReporteEntregasService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import net.sf.jasperreports.swing.JRViewer;
import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Filtros JavaFX y visor JRViewer nativo; cada toolkit se utiliza desde su propio hilo. */
public class ReporteController {
    private final ReporteService reportes;
    private final JasperReporteService jasper;
    private final ReporteViajesService viajes;
    private final ReporteEntregasService entregas;
    private final ExecutorService trabajo = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "fleetroute-reportes");
        hilo.setDaemon(true);
        return hilo;
    });
    private Task<?> generacion;
    private DocumentoGenerado documento;
    private JRViewer visorActual;
    private volatile long revision;
    private volatile boolean cerrado;
    private boolean listo;
    private boolean generando;
    private TipoReporte tipoConfigurado;

    @FXML private BorderPane raiz;
    @FXML private VBox panelFiltros;
    @FXML private ComboBox<TipoReporte> cmbTipo;
    @FXML private ComboBox<OpcionReporte> cmbOperador, cmbUnidad, cmbCliente;
    @FXML private ComboBox<AgrupacionTemporal> cmbAgrupacion;
    @FXML private ComboBox<TipoGrafico> cmbGrafico;
    @FXML private ComboBox<EstadoReporte> cmbEstado;
    @FXML private CheckBox chkFiltrarFechas;
    @FXML private DatePicker dpDesde, dpHasta;
    @FXML private TextField txtCarga;
    @FXML private Label lblAgrupacion, lblCarga, lblEstado, lblTotales, lblDesde, lblSinPrevia;
    @FXML private SwingNode visorJasper;
    @FXML private Button btnGenerar, btnMesActual, btnMesAnterior;
    @FXML private ProgressIndicator progreso;

    public ReporteController() { this(new ReporteService(), new JasperReporteService(), new ReporteViajesService()); }
    public ReporteController(ReporteService reportes, JasperReporteService jasper) { this(reportes, jasper, new ReporteViajesService()); }
    public ReporteController(ReporteService reportes, JasperReporteService jasper, ReporteViajesService viajes) {
        this(reportes, jasper, viajes, new ReporteEntregasService());
    }
    public ReporteController(ReporteService reportes, JasperReporteService jasper, ReporteViajesService viajes, ReporteEntregasService entregas) {
        this.reportes = reportes;
        this.jasper = jasper;
        this.viajes = viajes;
        this.entregas = entregas;
    }

    @FXML private void initialize() {
        try {
            boolean financieros = ReporteService.puedeVerFinancieros();
            cmbTipo.setItems(FXCollections.observableArrayList(Arrays.stream(TipoReporte.values())
                    .filter(tipo -> financieros || !tipo.financiero()).toList()));
        } catch (SecurityException e) { lblEstado.setText(e.getMessage()); btnGenerar.setDisable(true); return; }
        cmbAgrupacion.setItems(FXCollections.observableArrayList(AgrupacionTemporal.values()));
        cmbAgrupacion.setValue(AgrupacionTemporal.MENSUAL);
        cmbTipo.getSelectionModel().selectFirst();
        configurarTipo();
        cmbTipo.valueProperty().addListener((o, a, b) -> { configurarTipo(); invalidar(); });
        for (ComboBox<?> combo : new ComboBox<?>[]{cmbOperador, cmbUnidad, cmbCliente, cmbAgrupacion, cmbGrafico, cmbEstado}) {
            combo.valueProperty().addListener((o, a, b) -> invalidar());
        }
        chkFiltrarFechas.selectedProperty().addListener((o, a, b) -> { configurarFechas(); invalidar(); });
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
        cargarCatalogos();
    }

    private record Inicializacion(CatalogosReporte catalogos, LocalDate hoy) { }
    private record ResultadoVista(DocumentoGenerado documento, String totales, boolean vacio) { }

    private void cargarCatalogos() {
        panelFiltros.setDisable(true);
        indicarCarga(true);
        btnGenerar.setDisable(true);
        lblEstado.setText("Cargando opciones de reportes…");
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
            lblEstado.setText("Seleccione el reporte y los filtros. Genere la vista previa para usar el visor Jasper.");
            actualizarBoton();
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
        cmbGrafico.setDisable(tipo.graficos().size() == 1);
        cmbAgrupacion.setVisible(tipo.temporal()); cmbAgrupacion.setManaged(tipo.temporal());
        lblAgrupacion.setVisible(tipo.temporal()); lblAgrupacion.setManaged(tipo.temporal());
        txtCarga.setVisible(tipo.detallado()); txtCarga.setManaged(tipo.detallado());
        lblCarga.setVisible(tipo.detallado()); lblCarga.setManaged(tipo.detallado());
        if (!tipo.detallado()) txtCarga.clear();
        boolean operativo = tipo == TipoReporte.GESTION_VIAJES;
        chkFiltrarFechas.setVisible(operativo); chkFiltrarFechas.setManaged(operativo);
        if (tipoConfigurado != tipo) {
            cmbEstado.getItems().setAll(operativo ? java.util.List.of(EstadoReporte.values()) : java.util.List.of(EstadoReporte.FINALIZADO));
            cmbEstado.setValue(operativo ? EstadoReporte.TODOS : EstadoReporte.FINALIZADO);
        }
        cmbEstado.setDisable(!operativo);
        tipoConfigurado = tipo;
        configurarFechas();
    }

    private void configurarFechas() {
        boolean operativo = cmbTipo.getValue() == TipoReporte.GESTION_VIAJES;
        lblDesde.setText(operativo ? "Creación del viaje: desde" : "Finalización: desde");
        boolean sinFechas = operativo && !chkFiltrarFechas.isSelected();
        dpDesde.setDisable(sinFechas); dpHasta.setDisable(sinFechas);
        btnMesActual.setDisable(sinFechas); btnMesAnterior.setDisable(sinFechas);
    }

    private void invalidar() {
        revision++;
        documento = null;
        limpiarVisor();
        lblTotales.setText("");
        if (listo) {
            if (generacion != null) generacion.cancel(true);
            generando = false;
            indicarCarga(false);
            lblEstado.setText("Los filtros cambiaron. Genere una nueva vista previa.");
        }
        actualizarBoton();
    }

    @FXML private void generar() {
        if (cerrado || generando) return;
        if (!listo) { cargarCatalogos(); return; }
        try {
            boolean usarFechas = cmbTipo.getValue().finalizados() || chkFiltrarFechas.isSelected();
            if (usarFechas) {
                try { dpDesde.commitValue(); dpHasta.commitValue(); }
                catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Ingrese fechas válidas o selecciónelas en el calendario."); }
            }
            FiltroReporte filtro = new FiltroReporte(cmbTipo.getValue(), usarFechas ? dpDesde.getValue() : null,
                    usarFechas ? dpHasta.getValue() : null, cmbOperador.getValue(), cmbUnidad.getValue(), cmbCliente.getValue(),
                    txtCarga.getText(), cmbAgrupacion.getValue(), cmbGrafico.getValue(), cmbEstado.getValue());
            invalidar();
            long solicitud = revision;
            generando = true; indicarCarga(true); actualizarBoton();
            lblEstado.setText("Consultando viajes y generando el documento…");
            Task<ResultadoVista> tarea = new Task<>() {
                @Override protected ResultadoVista call() {
                    if (filtro.tipo() == TipoReporte.CUMPLIMIENTO_ENTREGAS) {
                        var doc = entregas.generar(filtro);
                        return new ResultadoVista(doc, doc.resumen().descripcion(), doc.resumen().viajes() == 0);
                    }
                    if (filtro.tipo() == TipoReporte.GESTION_VIAJES) {
                        var doc = viajes.generar(filtro);
                        return new ResultadoVista(doc, "Viajes incluidos: " + doc.viajes(), doc.viajes() == 0);
                    }
                    var datos = reportes.generar(filtro);
                    if (isCancelled()) return null;
                    return new ResultadoVista(jasper.generar(datos), "Total facturado: " + FormatoReporte.dinero(datos.total(), datos.moneda())
                            + " | Viajes: " + datos.viajes() + " | Promedio: " + FormatoReporte.dinero(datos.promedio(), datos.moneda()), datos.vacio());
                }
            };
            generacion = tarea;
            tarea.setOnSucceeded(evento -> {
                if (cerrado || solicitud != revision || tarea.getValue() == null) return;
                var resultado = tarea.getValue();
                documento = resultado.documento();
                lblTotales.setText(resultado.totales());
                montarVisor(resultado, solicitud);
            });
            tarea.setOnFailed(evento -> {
                if (!cerrado && solicitud == revision) {
                    generando = false; indicarCarga(false); actualizarBoton();
                    mostrarError("No se pudo generar el reporte", tarea.getException());
                }
            });
            trabajo.submit(tarea);
        } catch (RuntimeException e) { mostrarError("Revise la configuración del reporte", e); }
    }

    private void montarVisor(ResultadoVista resultado, long solicitud) {
        DocumentoGenerado actual = resultado.documento();
        SwingUtilities.invokeLater(() -> {
            if (cerrado || solicitud != revision) return;
            try {
                var nuevo = new VisorJasper(actual.impresion(), () -> {
                    if (cerrado || solicitud != revision) throw new SecurityException("La vista previa ya no está vigente. Genere otra vez el reporte.");
                    ReporteService.validarDocumento(actual);
                }, error -> Platform.runLater(() -> {
                    if (!cerrado && documento == actual) { invalidar(); lblEstado.setText(error.getMessage()); }
                }));
                Platform.runLater(() -> {
                    if (cerrado || solicitud != revision) { SwingUtilities.invokeLater(nuevo::clear); return; }
                    visorActual = nuevo;
                    visorJasper.setContent(nuevo);
                    visorJasper.setVisible(true);
                    SwingUtilities.invokeLater(() -> { nuevo.revalidate(); nuevo.repaint(); });
                    lblSinPrevia.setVisible(false);
                    generando = false; indicarCarga(false); actualizarBoton();
                    lblEstado.setText((resultado.vacio() ? "No hay viajes que coincidan con los filtros. " : "")
                            + "Use la barra del visor Jasper para guardar, imprimir y navegar.");
                });
            } catch (RuntimeException e) {
                Platform.runLater(() -> {
                    if (!cerrado && solicitud == revision) { invalidar(); mostrarError("No se pudo abrir el visor Jasper", e); }
                });
            }
        });
    }

    private void limpiarVisor() {
        visorJasper.setVisible(false);
        visorJasper.setContent(null);
        lblSinPrevia.setVisible(true);
        JRViewer anterior = visorActual;
        visorActual = null;
        if (anterior != null) SwingUtilities.invokeLater(anterior::clear);
    }

    @FXML private void mesActual() { seleccionarMes(0); }
    @FXML private void mesAnterior() { seleccionarMes(-1); }
    private void seleccionarMes(int desplazamiento) {
        LocalDate inicio = LocalDate.now(ZonaHoraria.actual()).withDayOfMonth(1).plusMonths(desplazamiento);
        dpDesde.setValue(inicio); dpHasta.setValue(inicio.withDayOfMonth(inicio.lengthOfMonth()));
    }
    private void actualizarBoton() { btnGenerar.setDisable(!listo || generando); }
    private void indicarCarga(boolean cargando) { progreso.setVisible(cargando); progreso.setManaged(cargando); }
    private void mostrarError(String titulo, Throwable error) {
        if (error instanceof SecurityException) invalidar();
        lblEstado.setText(titulo + ": " + (error.getMessage() == null ? "Intente de nuevo." : error.getMessage()));
        Alert alerta = new Alert(Alert.AlertType.ERROR, lblEstado.getText(), ButtonType.OK);
        if (raiz.getScene() != null) alerta.initOwner(raiz.getScene().getWindow());
        alerta.setHeaderText(titulo); alerta.showAndWait();
    }
    private void liberar() {
        if (cerrado) return;
        cerrado = true; revision++;
        if (generacion != null) generacion.cancel(true);
        trabajo.shutdownNow(); documento = null;
        // Dejar que SwingNode termine WINDOW_HIDDEN antes de desmontar su ventana nativa.
        Platform.runLater(this::limpiarVisor);
    }
}
