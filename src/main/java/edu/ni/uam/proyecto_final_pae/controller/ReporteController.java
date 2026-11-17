package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.ViajeDAO;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Reporte financiero de viajes finalizados (RF de control financiero, movido a la pantalla inicial). */
public class ReporteController {

    private final ViajeDAO viajeDAO = new ViajeDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private String moneda = "NIO";

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
    @FXML private javafx.scene.control.DatePicker dpDesde;
    @FXML private javafx.scene.control.DatePicker dpHasta;

    @FXML
    private void initialize() {
        try {
            empresaDAO.obtener().ifPresent(empresa -> {
                if (empresa.getMoneda() != null && !empresa.getMoneda().isBlank()) {
                    moneda = empresa.getMoneda();
                }
            });
        } catch (PersistenciaException e) {
            // Se conserva NIO como moneda por defecto.
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

    private boolean autorizado() {
        try {
            Usuario usuario = SesionActual.requerir(RolUsuario.ADMIN);
            return Boolean.TRUE.equals(usuario.getEsPrincipal())
                    || Boolean.TRUE.equals(usuario.getPuedeVerReportes());
        } catch (SecurityException e) {
            lblEstado.setText(e.getMessage());
            return false;
        }
    }

    @FXML
    private void calcular() {
        if (!autorizado()) {
            lblEstado.setText("No tiene permiso para consultar el reporte financiero.");
            tablaOperadores.setItems(FXCollections.observableArrayList());
            tablaUnidades.setItems(FXCollections.observableArrayList());
            lblTotales.setText("");
            return;
        }
        java.time.LocalDate desdeDia = dpDesde.getValue();
        java.time.LocalDate hastaDia = dpHasta.getValue();
        if (desdeDia != null && hastaDia != null && desdeDia.isAfter(hastaDia)) {
            lblEstado.setText("La fecha desde no puede ser posterior a la fecha hasta.");
            return;
        }
        java.time.LocalDateTime desde = desdeDia == null ? null : desdeDia.atStartOfDay();
        java.time.LocalDateTime hasta = hastaDia == null ? null : hastaDia.atTime(23, 59, 59);
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
                    + " viaje(s) finalizado(s).");
            lblEstado.setText("Reporte actualizado: " + operadores.size() + " operador(es), "
                    + unidades.size() + " unidad(es).");
        } catch (PersistenciaException e) {
            lblEstado.setText("No se pudo calcular el reporte.");
            Alert alerta = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alerta.setHeaderText("No se pudo calcular el reporte");
            alerta.showAndWait();
        }
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
