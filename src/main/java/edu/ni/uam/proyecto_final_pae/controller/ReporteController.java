package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.ViajeDAO;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import edu.ni.uam.proyecto_final_pae.service.ReporteFinancieroService;
import edu.ni.uam.proyecto_final_pae.service.ReporteFinancieroService.Fila;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Reporte financiero de viajes finalizados, con filtros de fecha y exportación a PDF. */
public class ReporteController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final ViajeDAO viajeDAO = new ViajeDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final ReporteFinancieroService reporteService = new ReporteFinancieroService();
    private String nombreEmpresa = "FleetRoute Pro";
    private String moneda = "NIO";
    private boolean reporteDisponible;
    private Task<Void> exportacion;

    @FXML private TableView<Object[]> tablaOperadores;
    @FXML private TableColumn<Object[], String> colOpNombre;
    @FXML private TableColumn<Object[], String> colOpViajes;
    @FXML private TableColumn<Object[], String> colOpTotal;
    @FXML private TableView<Object[]> tablaUnidades;
    @FXML private TableColumn<Object[], String> colUniNumero;
    @FXML private TableColumn<Object[], String> colUniViajes;
    @FXML private TableColumn<Object[], String> colUniTotal;
    @FXML private Label lblEstado;
    @FXML private Label lblTotales;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Button btnExportar;

    @FXML
    private void initialize() {
        if (autorizado()) {
            cargarEmpresa();
        }
        colOpNombre.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue()[0])));
        colOpViajes.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue()[1])));
        colOpTotal.setCellValueFactory(c -> new SimpleStringProperty(dinero(c.getValue()[2])));
        colUniNumero.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue()[0])));
        colUniViajes.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue()[1])));
        colUniTotal.setCellValueFactory(c -> new SimpleStringProperty(dinero(c.getValue()[2])));
        tablaOperadores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaUnidades.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        dpDesde.valueProperty().addListener((o, a, b) -> calcular());
        dpHasta.valueProperty().addListener((o, a, b) -> calcular());
        calcular();
    }

    private void cargarEmpresa() {
        try {
            empresaDAO.obtener().ifPresent(empresa -> {
                if (empresa.getNombre() != null && !empresa.getNombre().isBlank()) {
                    nombreEmpresa = empresa.getNombre();
                }
                if (empresa.getMoneda() != null && !empresa.getMoneda().isBlank()) {
                    moneda = empresa.getMoneda();
                }
            });
        } catch (PersistenciaException e) {
            // Se conservan el nombre y la moneda por defecto.
        }
    }

    private boolean autorizado() {
        try {
            Usuario usuario = SesionActual.requerir(RolUsuario.ADMIN);
            return Boolean.TRUE.equals(usuario.getEsPrincipal())
                    || Boolean.TRUE.equals(usuario.getPuedeVerReportes());
        } catch (SecurityException e) {
            return false;
        }
    }

    @FXML
    private void calcular() {
        reporteDisponible = false;
        actualizarBotonExportar();
        tablaOperadores.getItems().clear();
        tablaUnidades.getItems().clear();
        lblTotales.setText("");
        if (!autorizado()) {
            lblEstado.setText("No tiene permiso para consultar el reporte financiero.");
            return;
        }
        LocalDate desdeDia = dpDesde.getValue();
        LocalDate hastaDia = dpHasta.getValue();
        if (desdeDia != null && hastaDia != null && desdeDia.isAfter(hastaDia)) {
            lblEstado.setText("La fecha desde no puede ser posterior a la fecha hasta.");
            return;
        }
        LocalDateTime desde = desdeDia == null ? null : desdeDia.atStartOfDay();
        // PostgreSQL almacena las fracciones de segundo con precisión de microsegundos.
        LocalDateTime hasta = hastaDia == null ? null : hastaDia.atTime(23, 59, 59, 999_999_000);
        try {
            List<Object[]> operadores = viajeDAO.totalFacturadoPorOperador(desde, hasta);
            List<Object[]> unidades = viajeDAO.totalFacturadoPorUnidad(desde, hasta);
            tablaOperadores.setItems(FXCollections.observableArrayList(operadores));
            tablaOperadores.refresh();
            tablaUnidades.setItems(FXCollections.observableArrayList(unidades));
            tablaUnidades.refresh();
            BigDecimal total = operadores.stream()
                    .map(fila -> aDecimal(fila[2]))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long viajes = operadores.stream()
                    .mapToLong(fila -> fila[1] instanceof Number numero ? numero.longValue() : 0)
                    .sum();
            lblTotales.setText("Total facturado: " + dinero(total) + " en " + viajes
                    + (viajes == 1 ? " viaje finalizado." : " viajes finalizados."));
            lblEstado.setText("Reporte actualizado: " + operadores.size() + " operador(es), "
                    + unidades.size() + " unidad(es).");
            reporteDisponible = true;
            actualizarBotonExportar();
        } catch (PersistenciaException e) {
            lblEstado.setText("No se pudo calcular el reporte.");
            Alert alerta = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alerta.setHeaderText("No se pudo calcular el reporte");
            alerta.showAndWait();
        }
    }

    @FXML
    private void exportarPdf() {
        if (!reporteDisponible || exportacion != null) {
            return;
        }
        if (!autorizado()) {
            calcular();
            return;
        }

        Window ventana = btnExportar.getScene().getWindow();
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte financiero");
        selector.setInitialFileName("reporte-financiero.pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        File destino = selector.showSaveDialog(ventana);
        if (destino == null || !reporteDisponible) {
            return;
        }
        if (!autorizado()) {
            calcular();
            return;
        }

        List<Fila> operadores = filasParaPdf(tablaOperadores);
        List<Fila> unidades = filasParaPdf(tablaUnidades);
        String periodo = periodoParaPdf();
        String resumen = lblTotales.getText();
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                reporteService.exportar(destino.toPath(), nombreEmpresa, periodo, resumen, operadores, unidades);
                return null;
            }
        };
        tarea.setOnSucceeded(evento -> {
            exportacion = null;
            actualizarBotonExportar();
            lblEstado.setText("PDF guardado en " + destino.getAbsolutePath());
        });
        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            exportacion = null;
            actualizarBotonExportar();
            lblEstado.setText("No se pudo exportar el PDF.");
            if (ventana.isShowing()) {
                Alert alerta = new Alert(Alert.AlertType.ERROR, error.getMessage(), ButtonType.OK);
                alerta.initOwner(ventana);
                alerta.setHeaderText("No se pudo exportar el reporte financiero");
                alerta.showAndWait();
            }
        });
        exportacion = tarea;
        actualizarBotonExportar();
        Thread hilo = new Thread(tarea, "exportar-reporte-financiero");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void actualizarBotonExportar() {
        btnExportar.setDisable(!reporteDisponible || exportacion != null);
        btnExportar.setText(exportacion == null ? "Exportar PDF" : "Generando PDF…");
    }

    private List<Fila> filasParaPdf(TableView<Object[]> tabla) {
        return tabla.getItems().stream()
                .map(fila -> new Fila(String.valueOf(fila[0]), ((Number) fila[1]).longValue(), dinero(fila[2])))
                .toList();
    }

    private String periodoParaPdf() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde == null && hasta == null) {
            return "Todos los viajes finalizados";
        }
        String inicio = desde == null ? "" : "desde el " + desde.format(FORMATO_FECHA);
        String fin = hasta == null ? "" : "hasta el " + hasta.format(FORMATO_FECHA);
        return "Viajes finalizados " + (inicio + " " + fin).strip();
    }

    @FXML
    private void limpiarFechas() {
        dpDesde.setValue(null);
        dpHasta.setValue(null);
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) lblEstado.getScene().getWindow();
        stage.close();
    }

    private String dinero(Object valor) {
        BigDecimal numero = aDecimal(valor).setScale(2, java.math.RoundingMode.HALF_UP);
        String texto = String.format(Locale.US, "%,.2f", numero);
        String simbolo = switch (moneda) {
            case "NIO" -> "C$";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "CRC" -> "₡";
            case "GTQ" -> "Q";
            case "HNL" -> "L";
            case "PAB" -> "B/.";
            case "MXN" -> "MX$";
            default -> {
                try {
                    yield java.util.Currency.getInstance(moneda).getSymbol(Locale.forLanguageTag("es"));
                } catch (RuntimeException e) {
                    yield moneda;
                }
            }
        };
        return simbolo + " " + texto;
    }

    private static BigDecimal aDecimal(Object valor) {
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
