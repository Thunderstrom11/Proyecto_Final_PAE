package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.GeoUtil;
import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.RutaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UbicacionDAO;
import edu.ni.uam.proyecto_final_pae.dao.ViajeDAO;
import edu.ni.uam.proyecto_final_pae.model.*;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import edu.ni.uam.proyecto_final_pae.service.JornadaService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ViajeController {

    private final ViajeDAO viajeDAO = new ViajeDAO();
    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();
    private final RutaDAO rutaDAO = new RutaDAO();
    private final JornadaService jornadaService = new JornadaService();

    private Timeline reloj;

    @FXML private TextField txtBuscarViaje;
    @FXML private TextField txtCobroMin;
    @FXML private TextField txtCobroMax;
    @FXML private TextField txtBuscarRuta;
    @FXML private RadioButton rbOrigen;
    @FXML private RadioButton rbDestino;
    @FXML private Button btnLimpiarBusquedaViaje;

    private List<Viaje> todos = List.of();
    private String filtroEstado;

    // ------------------- pestaña Viajes
    @FXML private TableView<Viaje> tabla;
    @FXML private TableColumn<Viaje, String> colId;
    @FXML private TableColumn<Viaje, String> colUnidad;
    @FXML private TableColumn<Viaje, String> colOperador;
    @FXML private TableColumn<Viaje, String> colCarga;
    @FXML private TableColumn<Viaje, String> colOrigen;
    @FXML private TableColumn<Viaje, String> colDestino;
    @FXML private TableColumn<Viaje, String> colCobro;
    @FXML private TableColumn<Viaje, String> colEstado;

    @FXML private ComboBox<Unidad> cmbUnidad;
    @FXML private ComboBox<Operador> cmbOperador;
    @FXML private ComboBox<Carga> cmbCarga;
    @FXML private ComboBox<Ubicacion> cmbOrigen;
    @FXML private ComboBox<Ubicacion> cmbDestino;
    @FXML private ComboBox<Viaje> cmbViajePrevio;
    @FXML private TextField txtTiempo;
    @FXML private TextField txtCobro;
    @FXML private Label lblEstado;
    @FXML private CheckBox chkEncadenado;

    @FXML private Button btnVerTodos;
    @FXML private Button btnVerActivos;
    @FXML private Button btnVerFinalizados;
    @FXML private Button btnFinalizar;
    @FXML private Button btnCrear;
    @FXML private Button btnLimpiar;

    @FXML
    private void initialize() {
        if (!sesionAdmin()) {
            return;
        }
        // columnas viajes
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colUnidad.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUnidad() == null ? "—" : c.getValue().getUnidad().getNumeroUnidad()));
        colOperador.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOperador() == null ? "—" : c.getValue().getOperador().getNombre()));
        colCarga.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCarga() == null ? "—" : c.getValue().getCarga().getNumeroCarga()));
        colOrigen.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrigen().getNombre()));
        colDestino.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDestino().getNombre()));
        colCobro.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCobroViaje().toPlainString()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getNombre()));

        // toggle encadenamiento
        chkEncadenado.selectedProperty().addListener((o, a, b) -> {
            cmbUnidad.setDisable(b);
            cmbOperador.setDisable(b);
            cmbViajePrevio.setDisable(!b);
            if (b) {
                cargarViajesEncadenables();
            } else {
                cmbViajePrevio.setItems(FXCollections.observableArrayList());
                recargarDisponibles();
            }
        });

        ToggleGroup rutas = new ToggleGroup();
        rbOrigen.setToggleGroup(rutas);
        rbDestino.setToggleGroup(rutas);
        rbOrigen.setSelected(true);

        cmbOrigen.valueProperty().addListener((o, a, b) -> actualizarTiempo());
        cmbDestino.valueProperty().addListener((o, a, b) -> actualizarTiempo());
        txtBuscarViaje.textProperty().addListener((o, a, b) -> aplicarFiltros("Filtro de viajes"));
        txtCobroMin.textProperty().addListener((o, a, b) -> aplicarFiltros("Filtro de viajes"));
        txtCobroMax.textProperty().addListener((o, a, b) -> aplicarFiltros("Filtro de viajes"));
        txtBuscarRuta.textProperty().addListener((o, a, b) -> aplicarFiltros("Filtro de viajes"));
        rutas.selectedToggleProperty().addListener((o, a, b) -> aplicarFiltros("Filtro de viajes"));

        BooleanBinding datosViajeIncompletos = Bindings.createBooleanBinding(() -> {
            boolean encadenado = chkEncadenado.isSelected();
            boolean base = cmbCarga.getValue() == null
                    || cmbOrigen.getValue() == null
                    || cmbDestino.getValue() == null
                    || vacio(txtTiempo)
                    || vacio(txtCobro);
            if (encadenado) {
                return base || cmbViajePrevio.getValue() == null;
            }
            return base || cmbUnidad.getValue() == null || cmbOperador.getValue() == null;
        }, cmbUnidad.valueProperty(), cmbOperador.valueProperty(),
                cmbCarga.valueProperty(), cmbOrigen.valueProperty(),
                cmbDestino.valueProperty(), cmbViajePrevio.valueProperty(),
                txtTiempo.textProperty(), txtCobro.textProperty(),
                chkEncadenado.selectedProperty());

        BooleanBinding sinViajeEnCurso = Bindings.createBooleanBinding(() -> {
            Viaje viaje = tabla.getSelectionModel().getSelectedItem();
            return viaje == null || viaje.getEstado() == null
                    || !"EN_CURSO".equals(viaje.getEstado().getNombre())
                    || viaje.getFechaInicio() == null;
        }, tabla.getSelectionModel().selectedItemProperty());

        btnCrear.disableProperty().bind(datosViajeIncompletos);
        btnFinalizar.disableProperty().bind(sinViajeEnCurso);

        try {
            recargarDisponibles();
            cmbOrigen.setItems(FXCollections.observableArrayList(ubicacionDAO.listar()));
            cmbDestino.setItems(FXCollections.observableArrayList(ubicacionDAO.listar()));
            verTodos();
        } catch (PersistenciaException e) {
            error("No se pudo conectar con la base de datos", e);
        }

        reloj = new Timeline(new KeyFrame(javafx.util.Duration.seconds(30), evento -> {
            if (!Navegacion.vistaVisible(lblEstado)) {
                reloj.stop();
                return;
            }
            verificarSesion();
        }));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }

    private boolean sesionAdmin() {
        try {
            SesionActual.requerir(RolUsuario.ADMIN);
            return true;
        } catch (SecurityException e) {
            lblEstado.setText(e.getMessage());
            return false;
        }
    }

    private void verificarSesion() {
        if (SesionActual.getUsuario() != null && !SesionActual.activa()) {
            Navegacion.sesionExpirada(Navegacion.stageDe(lblEstado));
        }
    }

    // ------------------- acciones viajes

    @FXML private void verTodos() {
        if (!sesionAdmin()) {
            return;
        }
        filtroEstado = null;
        cargarTodos("Todos los viajes");
    }

    @FXML private void verActivos() {
        if (!sesionAdmin()) {
            return;
        }
        filtroEstado = "ACTIVO";
        cargarTodos("Viajes activos");
    }

    @FXML private void verFinalizados() {
        if (!sesionAdmin()) {
            return;
        }
        filtroEstado = "FINALIZADO";
        cargarTodos("Viajes finalizados");
    }

    @FXML
    private void limpiarBusquedaViaje() {
        txtBuscarViaje.clear();
        txtCobroMin.clear();
        txtCobroMax.clear();
        txtBuscarRuta.clear();
        rbOrigen.setSelected(true);
        aplicarFiltros("Filtro de viajes");
    }

    @FXML
    private void crear() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            Viaje v = new Viaje();
            v.setUnidad(chkEncadenado.isSelected()
                    ? null : requerido(cmbUnidad.getValue(), "unidad"));
            v.setOperador(chkEncadenado.isSelected()
                    ? null : requerido(cmbOperador.getValue(), "operador"));
            v.setViajePrevio(chkEncadenado.isSelected() ? cmbViajePrevio.getValue() : null);
            v.setCarga(requerido(cmbCarga.getValue(), "carga"));
            v.setOrigen(requerido(cmbOrigen.getValue(), "origen"));
            v.setDestino(requerido(cmbDestino.getValue(), "destino"));
            if (v.getOrigen().getId().equals(v.getDestino().getId())) {
                throw new IllegalArgumentException("El origen y el destino deben ser distintos.");
            }
            v.setTiempoEstimado(numero(txtTiempo.getText(), "tiempo estimado"));
            v.setCobroViaje(numero(txtCobro.getText(), "cobro"));
            if (v.getTiempoEstimado().signum() <= 0 || v.getCobroViaje().signum() <= 0) {
                throw new IllegalArgumentException("Tiempo y cobro deben ser mayores que cero.");
            }

            viajeDAO.crear(v);
            limpiar();
            verTodos();
            lblEstado.setText("Viaje " + v.getId() + " creado.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo crear el viaje", e);
        }
    }

    @FXML
    private void finalizar() {
        if (!sesionAdmin()) {
            return;
        }
        Viaje sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            advertencia("Selecciona un viaje de la tabla.");
            return;
        }
        Alert ok = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmar entrega del viaje #" + sel.getId() + "?",
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> r = ok.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.YES) return;
        try {
            jornadaService.confirmarEntrega(sel.getId());
            verTodos();
            lblEstado.setText("Viaje #" + sel.getId() + " finalizado.");
        } catch (PersistenciaException e) {
            error("No se pudo finalizar el viaje", e);
        }
    }

    @FXML
    private void limpiar() {
        tabla.getSelectionModel().clearSelection();
        cmbUnidad.getSelectionModel().clearSelection();
        cmbOperador.getSelectionModel().clearSelection();
        cmbCarga.getSelectionModel().clearSelection();
        cmbOrigen.getSelectionModel().clearSelection();
        cmbDestino.getSelectionModel().clearSelection();
        cmbViajePrevio.getSelectionModel().clearSelection();
        chkEncadenado.setSelected(false);
        txtTiempo.clear();
        txtCobro.clear();
    }

    // ------------------- apoyo

    private void recargarDisponibles() {
        cmbUnidad.setItems(FXCollections.observableArrayList(viajeDAO.unidadesDisponibles()));
        cmbOperador.setItems(FXCollections.observableArrayList(viajeDAO.operadoresDisponibles()));
        cmbCarga.setItems(FXCollections.observableArrayList(viajeDAO.cargasPendientes()));
    }

    private void cargarViajesEncadenables() {
        // Solo se puede encadenar sobre un viaje que ya esta en ruta.
        cmbViajePrevio.setItems(FXCollections.observableArrayList(viajeDAO.listarActivos().stream()
                .filter(viaje -> viaje.getEstado() != null
                        && ("EN_CURSO".equals(viaje.getEstado().getNombre())
                        || "PAUSADO".equals(viaje.getEstado().getNombre())))
                .toList()));
        cmbCarga.setItems(FXCollections.observableArrayList(viajeDAO.cargasPendientes()));
    }

    private void mostrar(java.util.function.Supplier<List<Viaje>> consulta, String titulo) {
        try {
            List<Viaje> lista = consulta.get();
            tabla.setItems(FXCollections.observableArrayList(lista));
            tabla.refresh();
            lblEstado.setText(titulo + ": " + lista.size() + " registro(s).");
        } catch (PersistenciaException e) {
            error("No se pudo consultar los viajes", e);
        }
    }

    private void cargarTodos(String titulo) {
        try {
            todos = viajeDAO.listar();
            aplicarFiltros(titulo);
        } catch (PersistenciaException e) {
            todos = List.of();
            tabla.setItems(FXCollections.observableArrayList());
            error("No se pudo consultar los viajes", e);
        }
    }

    private void aplicarFiltros(String titulo) {
        if (txtBuscarViaje == null) {
            return;
        }
        String busqueda = txtBuscarViaje.getText();
        String ruta = txtBuscarRuta.getText();
        BigDecimal cobroMin = numeroFiltro(txtCobroMin.getText());
        BigDecimal cobroMax = numeroFiltro(txtCobroMax.getText());
        boolean porOrigen = rbOrigen == null || rbOrigen.isSelected();

        List<Viaje> visibles = todos.stream()
                .filter(this::coincideEstado)
                .filter(viaje -> coincideTexto(viaje, busqueda))
                .filter(viaje -> coincideRuta(viaje, ruta, porOrigen))
                .filter(viaje -> cobroEnRango(viaje, cobroMin, cobroMax))
                .toList();

        tabla.setItems(FXCollections.observableArrayList(visibles));
        tabla.refresh();
        lblEstado.setText(titulo + ": " + visibles.size() + " de " + todos.size() + " viaje(s).");
    }

    private boolean coincideEstado(Viaje viaje) {
        if (filtroEstado == null) {
            return true;
        }
        String estado = viaje.getEstado() == null ? "" : viaje.getEstado().getNombre();
        if ("ACTIVO".equals(filtroEstado)) {
            return "PROGRAMADO".equals(estado) || "EN_CURSO".equals(estado) || "PAUSADO".equals(estado);
        }
        return filtroEstado.equals(estado);
    }

    private boolean coincideTexto(Viaje viaje, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String buscado = texto.trim().toLowerCase();
        String unidad = viaje.getUnidad() == null ? "" : viaje.getUnidad().getNumeroUnidad();
        String operador = viaje.getOperador() == null ? "" : viaje.getOperador().getNombre();
        String carga = viaje.getCarga() == null ? "" : viaje.getCarga().getNumeroCarga();
        String descripcion = viaje.getCarga() == null || viaje.getCarga().getDescripcion() == null
                ? "" : viaje.getCarga().getDescripcion();
        return unidad.toLowerCase().contains(buscado)
                || operador.toLowerCase().contains(buscado)
                || carga.toLowerCase().contains(buscado)
                || descripcion.toLowerCase().contains(buscado);
    }

    private boolean coincideRuta(Viaje viaje, String texto, boolean porOrigen) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        Ubicacion ubicacion = porOrigen ? viaje.getOrigen() : viaje.getDestino();
        return ubicacion != null && ubicacion.getNombre() != null
                && ubicacion.getNombre().toLowerCase().contains(texto.trim().toLowerCase());
    }

    private boolean cobroEnRango(Viaje viaje, BigDecimal minimo, BigDecimal maximo) {
        BigDecimal cobro = viaje.getCobroViaje();
        if (cobro == null) {
            return minimo == null && maximo == null;
        }
        if (minimo != null && cobro.compareTo(minimo) < 0) {
            return false;
        }
        return maximo == null || cobro.compareTo(maximo) <= 0;
    }

    private BigDecimal numeroFiltro(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** RF de rutas: usa las horas configuradas o las estima por coordenadas. */
    private void actualizarTiempo() {
        Ubicacion origen = cmbOrigen.getValue();
        Ubicacion destino = cmbDestino.getValue();
        if (origen == null || destino == null) {
            txtTiempo.clear();
            txtTiempo.setPromptText("Se calcula con origen y destino");
            return;
        }
        if (origen.getId() != null && origen.getId().equals(destino.getId())) {
            txtTiempo.clear();
            txtTiempo.setPromptText("Origen y destino deben ser distintos");
            return;
        }
        try {
            Optional<Ruta> ruta = rutaDAO.buscar(origen, destino);
            if (ruta.isPresent() && ruta.get().getTiempoEstimadoHoras() != null) {
                txtTiempo.setText(ruta.get().getTiempoEstimadoHoras().stripTrailingZeros().toPlainString());
                return;
            }
            if (GeoUtil.tieneCoordenadas(origen) && GeoUtil.tieneCoordenadas(destino)) {
                txtTiempo.setText(GeoUtil.horasEstimadas(origen, destino).stripTrailingZeros().toPlainString());
                return;
            }
            txtTiempo.clear();
            txtTiempo.setPromptText("Sin ruta configurada ni coordenadas");
        } catch (PersistenciaException e) {
            txtTiempo.clear();
            txtTiempo.setPromptText("No se pudo calcular el tiempo");
        }
    }

    private <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("Selecciona " + campo + ".");
        }
        return valor;
    }

    private boolean vacio(TextField campo) {
        return campo.getText() == null || campo.getText().isBlank();
    }

    private BigDecimal numero(String texto, String campo) {
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("El " + campo + " debe ser numérico, ej. 12.50");
        }
    }

    private void advertencia(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText("Datos incompletos o inválidos");
        a.showAndWait();
    }

    private void error(String titulo, PersistenciaException e) {
        lblEstado.setText(titulo);
        Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        a.setHeaderText(titulo);
        a.showAndWait();
    }
}