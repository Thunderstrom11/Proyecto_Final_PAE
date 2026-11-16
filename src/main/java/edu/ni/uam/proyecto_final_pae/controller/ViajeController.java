package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.GeoUtil;
import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.RutaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UbicacionDAO;
import edu.ni.uam.proyecto_final_pae.dao.ViajeDAO;
import edu.ni.uam.proyecto_final_pae.model.*;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import edu.ni.uam.proyecto_final_pae.service.JornadaService;
import edu.ni.uam.proyecto_final_pae.util.ComboBuscador;
import edu.ni.uam.proyecto_final_pae.util.Tooltips;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ViajeController {

    private final ViajeDAO viajeDAO = new ViajeDAO();
    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();
    private final RutaDAO rutaDAO = new RutaDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final JornadaService jornadaService = new JornadaService();

    private Timeline reloj;
    private String moneda = "NIO";

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
    @FXML private Label lblCoordenadasOrigen;
    @FXML private Label lblCoordenadasDestino;
    @FXML private Button btnBuscarOrigen;
    @FXML private Button btnBuscarDestino;
    @FXML private Button btnGoogleOrigen;
    @FXML private Button btnGoogleDestino;

    private boolean ajustandoLugar;

    @FXML private Button btnVerTodos;
    @FXML private Button btnVerActivos;
    @FXML private Button btnVerFinalizados;
    @FXML private Button btnFinalizar;
    @FXML private Button btnCancelarViaje;
    @FXML private Button btnCrear;
    @FXML private Button btnLimpiar;

    @FXML
    private void initialize() {
        if (!sesionAdmin()) {
            return;
        }
        try {
            empresaDAO.obtener().ifPresent(empresa -> {
                if (empresa.getMoneda() != null && !empresa.getMoneda().isBlank()) {
                    moneda = empresa.getMoneda();
                }
            });
        } catch (PersistenciaException e) {
            // Se conserva NIO como moneda por defecto.
        }
        colCobro.setText("Cobro (" + moneda + ")");
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
        colCobro.setCellValueFactory(c -> new SimpleStringProperty(formatearMoneda(c.getValue().getCobroViaje())));
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

        cmbOrigen.valueProperty().addListener((o, a, b) -> {
            actualizarTiempo();
            actualizarCoordenadasOrigen();
        });
        cmbDestino.valueProperty().addListener((o, a, b) -> {
            actualizarTiempo();
            actualizarCoordenadasDestino();
        });
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

        BooleanBinding sinViajeCancelable = Bindings.createBooleanBinding(() -> {
            Viaje viaje = tabla.getSelectionModel().getSelectedItem();
            if (viaje == null || viaje.getEstado() == null) {
                return true;
            }
            String estado = viaje.getEstado().getNombre();
            return !("PROGRAMADO".equals(estado) || "EN_CURSO".equals(estado)
                    || "PAUSADO".equals(estado));
        }, tabla.getSelectionModel().selectedItemProperty());

        btnCrear.disableProperty().bind(datosViajeIncompletos);
        btnFinalizar.disableProperty().bind(sinViajeEnCurso);
        btnCancelarViaje.disableProperty().bind(sinViajeCancelable);
        btnBuscarOrigen.disableProperty().bind(cmbOrigen.valueProperty().isNull());
        btnBuscarDestino.disableProperty().bind(cmbDestino.valueProperty().isNull());
        btnGoogleOrigen.setDisable(true);
        btnGoogleDestino.setDisable(true);

        try {
            recargarDisponibles();
            List<Ubicacion> ubicaciones = ubicacionDAO.listar();
            ComboBuscador.configurar(cmbOrigen, ubicaciones);
            ComboBuscador.configurar(cmbDestino, ubicaciones);
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
        marcarFiltro(btnVerTodos);
        cargarTodos("Todos los viajes");
    }

    private void marcarFiltro(Button activo) {
        for (Button boton : List.of(btnVerTodos, btnVerActivos, btnVerFinalizados)) {
            boton.getStyleClass().remove("filter-active");
        }
        activo.getStyleClass().add("filter-active");
    }

    @FXML private void verActivos() {
        if (!sesionAdmin()) {
            return;
        }
        filtroEstado = "ACTIVO";
        marcarFiltro(btnVerActivos);
        cargarTodos("Viajes activos");
    }

    @FXML private void verFinalizados() {
        if (!sesionAdmin()) {
            return;
        }
        filtroEstado = "FINALIZADO";
        marcarFiltro(btnVerFinalizados);
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
            Unidad unidadViaje = chkEncadenado.isSelected() && v.getViajePrevio() != null
                    ? v.getViajePrevio().getUnidad() : v.getUnidad();
            if (unidadViaje != null && unidadViaje.getCapacidadToneladas() != null
                    && v.getCarga().getPesoToneladas() != null
                    && v.getCarga().getPesoToneladas().compareTo(
                    unidadViaje.getCapacidadToneladas()) > 0) {
                throw new IllegalArgumentException("El peso de la carga supera la capacidad de la unidad ("
                        + unidadViaje.getCapacidadToneladas().stripTrailingZeros().toPlainString() + " t).");
            }
            v.setTiempoEstimado(numero(txtTiempo.getText(), "tiempo estimado"));
            v.setCobroViaje(numero(txtCobro.getText(), "cobro"));
            if (v.getTiempoEstimado().signum() <= 0 || v.getCobroViaje().signum() <= 0) {
                throw new IllegalArgumentException("Tiempo y cobro deben ser mayores que cero.");
            }

            viajeDAO.crear(v);
            limpiar();
            recargarDisponibles();
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
            advertencia("Seleccione un viaje de la tabla.");
            return;
        }
        ButtonType finalizar = new ButtonType("Finalizar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert ok = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmar entrega del viaje #" + sel.getId() + "?", finalizar, cancelar);
        Optional<ButtonType> r = ok.showAndWait();
        if (r.isEmpty() || r.get() != finalizar) return;
        try {
            jornadaService.confirmarEntrega(sel.getId());
            recargarDisponibles();
            verTodos();
            lblEstado.setText("Viaje #" + sel.getId() + " finalizado.");
        } catch (PersistenciaException e) {
            error("No se pudo finalizar el viaje", e);
        }
    }

    @FXML
    private void cancelarViaje() {
        if (!sesionAdmin()) {
            return;
        }
        Viaje seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            advertencia("Seleccione un viaje de la tabla.");
            return;
        }
        ButtonType cancelar = new ButtonType("Cancelar viaje", ButtonBar.ButtonData.OK_DONE);
        ButtonType volver = new ButtonType("Volver", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Cancelar el viaje #" + seleccionado.getId()
                        + "? La unidad, el operador y la carga quedarán disponibles.",
                cancelar, volver);
        confirmacion.setHeaderText("Cancelar viaje");
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != cancelar) {
            return;
        }
        try {
            viajeDAO.cancelar(seleccionado.getId());
            recargarDisponibles();
            verTodos();
            lblEstado.setText("Viaje #" + seleccionado.getId() + " cancelado.");
        } catch (PersistenciaException e) {
            error("No se pudo cancelar el viaje", e);
        }
    }

    @FXML
    private void cerrarSesion() {
        Navegacion.cerrarSesion(Navegacion.stageDe(lblEstado));
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
        actualizarCoordenadasOrigen();
        actualizarCoordenadasDestino();
    }

    @FXML
    private void abrirPickerOrigen() {
        Ubicacion base = cmbOrigen.getValue();
        if (base == null) {
            advertencia("Seleccione primero una ubicación base para el origen.");
            return;
        }
        Ubicacion elegida = seleccionarLugar(base, "origen");
        if (elegida != null) {
            ajustandoLugar = true;
            agregarUbicacionACombos(elegida);
            cmbOrigen.setValue(elegida);
            ajustandoLugar = false;
            actualizarTiempo();
            actualizarCoordenadasOrigen();
        }
    }

    @FXML
    private void abrirPickerDestino() {
        Ubicacion base = cmbDestino.getValue();
        if (base == null) {
            advertencia("Seleccione primero una ubicación base para el destino.");
            return;
        }
        Ubicacion elegida = seleccionarLugar(base, "destino");
        if (elegida != null) {
            ajustandoLugar = true;
            agregarUbicacionACombos(elegida);
            cmbDestino.setValue(elegida);
            ajustandoLugar = false;
            actualizarTiempo();
            actualizarCoordenadasDestino();
        }
    }

    private Ubicacion seleccionarLugar(Ubicacion base, String rol) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/edu/ni/uam/proyecto_final_pae/lugar-picker-view.fxml"));
            Parent root = loader.load();
            Tooltips.instalar(root);
            LugarPickerController controller = loader.getController();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(txtBuscarViaje.getScene().getWindow());
            stage.setTitle("Seleccionar lugar exacto");
            stage.setScene(new Scene(root));
            stage.setOnShown(evento -> controller.configurar(base.getPais(), base, stage, rol));
            stage.showAndWait();
            return controller.getSeleccion();
        } catch (IOException e) {
            advertencia("No se pudo abrir el selector de lugares: " + e.getMessage());
            return null;
        }
    }

    private void agregarUbicacionACombos(Ubicacion ubicacion) {
        ComboBuscador.agregarOpcion(cmbOrigen, ubicacion);
        ComboBuscador.agregarOpcion(cmbDestino, ubicacion);
    }

    private void actualizarCoordenadasOrigen() {
        actualizarCoordenadas(cmbOrigen.getValue(), lblCoordenadasOrigen, btnGoogleOrigen);
    }

    private void actualizarCoordenadasDestino() {
        actualizarCoordenadas(cmbDestino.getValue(), lblCoordenadasDestino, btnGoogleDestino);
    }

    private void actualizarCoordenadas(Ubicacion ubicacion, Label etiqueta, Button boton) {
        if (ubicacion == null || ubicacion.getLatitud() == null || ubicacion.getLongitud() == null) {
            etiqueta.setText("Sin coordenadas exactas.");
            boton.setDisable(true);
            return;
        }
        etiqueta.setText(ubicacion.getNombre() + "  ·  "
                + String.format(Locale.ROOT, "%.5f, %.5f",
                ubicacion.getLatitud(), ubicacion.getLongitud()));
        boton.setDisable(false);
    }

    @FXML
    private void abrirGoogleOrigen() {
        abrirGoogleMaps(cmbOrigen.getValue());
    }

    @FXML
    private void abrirGoogleDestino() {
        abrirGoogleMaps(cmbDestino.getValue());
    }

    private void abrirGoogleMaps(Ubicacion ubicacion) {
        if (ubicacion == null || ubicacion.getLatitud() == null || ubicacion.getLongitud() == null) {
            advertencia("Seleccione un lugar con coordenadas para abrir Google Maps.");
            return;
        }
        try {
            String url = "https://www.google.com/maps/search/?api=1&query="
                    + ubicacion.getLatitud() + "," + ubicacion.getLongitud();
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            advertencia("No se pudo abrir Google Maps: " + e.getMessage());
        }
    }

    // ------------------- apoyo

    private void recargarDisponibles() {
        ComboBuscador.configurar(cmbUnidad, viajeDAO.unidadesDisponibles());
        ComboBuscador.configurar(cmbOperador, viajeDAO.operadoresDisponibles());
        ComboBuscador.configurar(cmbCarga, viajeDAO.cargasPendientes());
    }

    private void cargarViajesEncadenables() {
        // Solo se puede encadenar sobre un viaje que ya esta en ruta.
        ComboBuscador.configurar(cmbViajePrevio, viajeDAO.listarActivos().stream()
                .filter(viaje -> viaje.getEstado() != null
                        && ("EN_CURSO".equals(viaje.getEstado().getNombre())
                        || "PAUSADO".equals(viaje.getEstado().getNombre())))
                .toList());
        ComboBuscador.configurar(cmbCarga, viajeDAO.cargasPendientes());
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

        Viaje seleccionado = tabla.getSelectionModel().getSelectedItem();
        List<Viaje> visibles = todos.stream()
                .filter(this::coincideEstado)
                .filter(viaje -> coincideTexto(viaje, busqueda))
                .filter(viaje -> coincideRuta(viaje, ruta, porOrigen))
                .filter(viaje -> cobroEnRango(viaje, cobroMin, cobroMax))
                .toList();

        tabla.setItems(FXCollections.observableArrayList(visibles));
        if (seleccionado != null && visibles.contains(seleccionado)) {
            tabla.getSelectionModel().select(seleccionado);
        }
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
            throw new IllegalArgumentException("Seleccione " + campo + ".");
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

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) {
            return "—";
        }
        String numero = String.format(Locale.US, "%,.2f", valor);
        return switch (moneda) {
            case "NIO" -> "C$ " + numero;
            case "USD" -> "$ " + numero;
            case "EUR" -> "€ " + numero;
            case "CRC" -> "₡ " + numero;
            case "GTQ" -> "Q " + numero;
            case "HNL" -> "L " + numero;
            case "PAB" -> "B/. " + numero;
            case "MXN" -> "MX$ " + numero;
            default -> moneda + " " + numero;
        };
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