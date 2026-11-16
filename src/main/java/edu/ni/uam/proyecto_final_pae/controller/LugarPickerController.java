package edu.ni.uam.proyecto_final_pae.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.UbicacionDAO;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.awt.Desktop;
import java.text.Normalizer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Selector de un lugar exacto con búsqueda Nominatim y mapa Leaflet. */
public class LugarPickerController {

    private static final String NOMINATIM = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT =
            "FleetRouteProAcademic/1.0 (contacto: estudiante-uam)";
    private static final String COLOR_POR_DEFECTO = "#0891B2";
    private static final List<String> CATEGORIAS = List.of(
            "Casa", "Negocio", "Bodega", "Oficina", "Otro");
    private static final List<OpcionColor> COLORES_UBICACION = List.of(
            new OpcionColor("Turquesa", "#0891B2"),
            new OpcionColor("Violeta", "#7C3AED"),
            new OpcionColor("Ámbar", "#B45309"),
            new OpcionColor("Verde", "#15803D"),
            new OpcionColor("Rojo", "#BE123C"),
            new OpcionColor("Azul", "#1D4ED8")
    );
    private static final Map<String, double[]> LIMITES_CENTROAMERICA = Map.of(
            "nicaragua", new double[]{10.707, -87.691, 15.025, -82.566},
            "honduras", new double[]{12.982, -89.350, 16.510, -83.155},
            "el salvador", new double[]{13.149, -90.128, 14.451, -87.692},
            "guatemala", new double[]{13.740, -92.236, 17.816, -88.223},
            "costa rica", new double[]{8.032, -85.951, 11.219, -82.552},
            "panama", new double[]{7.202, -83.051, 9.648, -77.174},
            "belice", new double[]{15.886, -89.228, 18.496, -87.277}
    );
    private static final Pattern PATRON_DMS = Pattern.compile(
            "(?i)(\\d{1,3})\\s*[°º]\\s*(\\d{1,2})\\s*['′]\\s*"
                    + "(\\d{1,2}(?:\\.\\d+)?)\\s*[\"″]?\\s*([NS])\\s*[, ]+\\s*"
                    + "(\\d{1,3})\\s*[°º]\\s*(\\d{1,2})\\s*['′]\\s*"
                    + "(\\d{1,2}(?:\\.\\d+)?)\\s*[\"″]?\\s*([EW])");

    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();
    private final Map<String, double[]> limitesPais = new HashMap<>();
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @FXML private Label lblPais;
    @FXML private TextField txtBuscar;
    @FXML private Button btnBuscar;
    @FXML private Label lblEstado;
    @FXML private ListView<Resultado> lista;
    @FXML private StackPane contenedorMapa;
    @FXML private VBox panelCargaMapa;
    @FXML private ProgressBar barraMapa;
    @FXML private Label lblCargaMapa;
    @FXML private Label lblSeleccion;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<OpcionColor> cmbColor;
    @FXML private TextField txtNombreLugar;
    @FXML private Button btnEditarLugar;
    @FXML private Button btnGuardarCambios;
    @FXML private Button btnConfirmar;
    @FXML private Button btnGoogle;
    @FXML private Button btnCancelar;

    private WebView mapa;
    private String pais;
    private String rol = "ubicación";
    private Ubicacion seleccion;
    private boolean mapaListo;
    private boolean aplicandoSugerencia;
    private boolean modoEdicion = true;
    private boolean ajustandoEstilo;
    private Resultado puntoPendiente;
    private int generacionBusqueda;
    private Stage ventana;
    private final PauseTransition esperaBusqueda =
            new PauseTransition(javafx.util.Duration.millis(700));

    public record Resultado(String nombre, String direccion, String departamento,
                            String pais, double latitud, double longitud,
                            Ubicacion guardada, String enlaceGoogle) {
        public Resultado(String nombre, String direccion, String departamento,
                         double latitud, double longitud, Ubicacion guardada) {
            this(nombre, direccion, departamento, null, latitud, longitud, guardada, null);
        }

        public Resultado(String nombre, String direccion, String departamento,
                         double latitud, double longitud, Ubicacion guardada,
                         String enlaceGoogle) {
            this(nombre, direccion, departamento, null, latitud, longitud, guardada, enlaceGoogle);
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private record Coordenada(double latitud, double longitud) {
    }

    private record OpcionColor(String nombre, String hex) {
        @Override
        public String toString() {
            return nombre + " · " + hex;
        }
    }

    private enum DecisionPais {
        USAR, CANCELAR
    }

    public void configurar(String pais, Ubicacion actual, Stage ventana, String rol) {
        this.pais = pais;
        this.ventana = ventana;
        this.rol = rol == null || rol.isBlank() ? "ubicación" : rol;
        lblPais.setText("País actual: " + (pais == null || pais.isBlank() ? "sin seleccionar" : pais));
        if (cmbCategoria.getItems().isEmpty()) {
            cmbCategoria.setItems(FXCollections.observableArrayList(CATEGORIAS));
            cmbCategoria.setValue("Otro");
            cmbColor.setItems(FXCollections.observableArrayList(COLORES_UBICACION));
            cmbColor.setValue(COLORES_UBICACION.get(0));
            cmbCategoria.valueProperty().addListener((o, anterior, valor) -> actualizarEstiloSeleccion());
            cmbColor.valueProperty().addListener((o, anterior, valor) -> actualizarEstiloSeleccion());
        }
        lista.setCellFactory(columna -> new ListCell<>() {
            @Override
            protected void updateItem(Resultado item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String detalle = item.direccion() == null || item.direccion().isBlank()
                        ? String.format(Locale.ROOT, "%.5f, %.5f", item.latitud(), item.longitud())
                        : item.direccion();
                setText(item.nombre() + "\n" + detalle);
                setStyle("-fx-padding: 7 8 7 8;");
            }
        });
        lista.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                marcarSeleccion(b);
            }
        });
        txtBuscar.setOnAction(evento -> buscar());
        txtBuscar.textProperty().addListener((o, anterior, texto) -> programarBusqueda(texto));
        esperaBusqueda.setOnFinished(evento -> buscar());
        txtNombreLugar.clear();
        btnConfirmar.setDisable(true);
        btnGoogle.setDisable(true);
        btnEditarLugar.setDisable(true);
        cargarGuardadas("");
        txtBuscar.requestFocus();
        PauseTransition pausaMapa = new PauseTransition(javafx.util.Duration.millis(200));
        pausaMapa.setOnFinished(evento -> {
            if (ventana.isShowing()) {
                crearMapa(actual);
            }
        });
        pausaMapa.play();
    }

    private void programarBusqueda(String texto) {
        esperaBusqueda.stop();
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.length() < 3) {
            generacionBusqueda++;
            btnBuscar.setDisable(false);
            cargarGuardadas(limpio);
            return;
        }
        esperaBusqueda.playFromStart();
    }

    public Ubicacion getSeleccion() {
        return seleccion;
    }

    private void crearMapa(Ubicacion actual) {
        try {
            Path html = prepararMapaLocal();
            WebView web = new WebView();
            this.mapa = web;
            contenedorMapa.getChildren().add(0, web);
            WebEngine motor = web.getEngine();
            motor.setUserAgent(USER_AGENT);
            motor.getLoadWorker().progressProperty().addListener((o, anterior, progreso) -> {
                double valor = progreso == null ? 0 : progreso.doubleValue();
                if (valor >= 0) {
                    barraMapa.setProgress(valor);
                    lblCargaMapa.setText(Math.round(valor * 100) + " %");
                }
            });
            motor.getLoadWorker().stateProperty().addListener((o, anterior, estado) -> {
                if (estado == Worker.State.SUCCEEDED) {
                    mapaListo = true;
                    JSObject ventanaJs = (JSObject) motor.executeScript("window");
                    ventanaJs.setMember("javaApp", this);
                    if (actual != null && actual.getLatitud() != null && actual.getLongitud() != null) {
                        motor.executeScript("centrar(" + actual.getLatitud() + ","
                                + actual.getLongitud() + ",9)");
                    }
                    Resultado elegido = lista.getSelectionModel().getSelectedItem();
                    if (elegido != null) {
                        motor.executeScript("centrar(" + elegido.latitud() + ","
                                + elegido.longitud() + ",12)");
                        motor.executeScript("marcar(" + elegido.latitud() + ","
                                + elegido.longitud() + ",'" + escapar(elegido.nombre()) + "')");
                    }
                    barraMapa.setProgress(0.98);
                    lblCargaMapa.setText("98 %");
                    PauseTransition respaldo = new PauseTransition(
                            javafx.util.Duration.millis(1200));
                    respaldo.setOnFinished(evento -> mostrarMapa());
                    respaldo.play();
                } else if (estado == Worker.State.FAILED) {
                    lblCargaMapa.setText("No se pudo cargar el mapa.");
                }
            });
            motor.load(html.toUri().toString());
        } catch (IOException e) {
            lblCargaMapa.setText("No se pudo preparar el mapa.");
            e.printStackTrace();
        }
    }

    /** Invocado desde JavaScript cuando Leaflet termina de pintar los tiles visibles. */
    public void mapaCargado() {
        Platform.runLater(this::mostrarMapa);
    }

    private void mostrarMapa() {
        barraMapa.setProgress(1);
        lblCargaMapa.setText("100 %");
        panelCargaMapa.setVisible(false);
        panelCargaMapa.setManaged(false);
        actualizarMarcadoresGuardados();
        actualizarMarcadoresResultados(lista.getItems());
        aplicarLimitesPais();
    }

    private void actualizarMarcadoresGuardados() {
        if (!mapaListo || mapa == null) {
            return;
        }
        List<Map<String, Object>> puntos = new ArrayList<>();
        for (Resultado resultado : guardadas("")) {
            if (resultado.guardada() == null || resultado.guardada().getId() == null) {
                continue;
            }
            puntos.add(Map.of(
                    "id", resultado.guardada().getId(),
                    "lat", resultado.latitud(),
                    "lng", resultado.longitud(),
                    "nombre", resultado.nombre(),
                    "detalle", resultado.direccion(),
                    "categoria", categoriaSegura(resultado.guardada().getCategoria()),
                    "color", colorSeguro(resultado.guardada().getColor())));
        }
        enviarPuntos("marcarGuardadas", puntos);
    }

    private String categoriaSegura(String categoria) {
        return CATEGORIAS.contains(categoria) ? categoria : "Otro";
    }

    private String colorSeguro(String color) {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$")
                ? color : COLOR_POR_DEFECTO;
    }

    private OpcionColor colorOpcion(String color) {
        return COLORES_UBICACION.stream()
                .filter(opcion -> opcion.hex().equalsIgnoreCase(color))
                .findFirst()
                .orElse(COLORES_UBICACION.get(0));
    }

    private void aplicarLimitesPais() {
        if (!mapaListo || mapa == null || pais == null || pais.isBlank()) {
            return;
        }
        double[] limites = limitesPais.get(pais);
        if (limites != null) {
            enviarLimitesPais(limites);
            return;
        }
        double[] conocido = LIMITES_CENTROAMERICA.get(normalizar(pais));
        if (conocido != null) {
            limitesPais.put(pais, conocido);
            enviarLimitesPais(conocido);
            return;
        }
        String solicitado = pais;
        String url = NOMINATIM + "/search?format=jsonv2&limit=1&featuretype=country"
                + "&accept-language=es&q=" + URLEncoder.encode(solicitado, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(respuesta -> Platform.runLater(() -> {
                    if (!solicitado.equals(pais) || !mapaListo || mapa == null
                            || respuesta.statusCode() != 200) {
                        return;
                    }
                    try {
                        JsonNode raiz = json.readTree(respuesta.body());
                        if (!raiz.isArray() || raiz.isEmpty()) {
                            return;
                        }
                        JsonNode bbox = raiz.get(0).path("boundingbox");
                        if (!bbox.isArray() || bbox.size() < 4) {
                            return;
                        }
                        double[] valores = {
                                bbox.get(0).asDouble(), bbox.get(2).asDouble(),
                                bbox.get(1).asDouble(), bbox.get(3).asDouble()
                        };
                        limitesPais.put(solicitado, valores);
                        enviarLimitesPais(valores);
                    } catch (Exception ignored) {
                        // Si no se puede limitar, el mapa sigue funcionando sin restricción.
                    }
                }))
                .exceptionally(error -> null);
    }

    private void enviarLimitesPais(double[] limites) {
        if (mapaListo && mapa != null) {
            mapa.getEngine().executeScript("restringirPais(" + limites[0] + ","
                    + limites[1] + "," + limites[2] + "," + limites[3] + ")");
        }
    }

    private double[] limitesActuales() {
        if (pais == null || pais.isBlank()) {
            return null;
        }
        double[] limites = limitesPais.get(pais);
        return limites != null ? limites : LIMITES_CENTROAMERICA.get(normalizar(pais));
    }

    private boolean dentroDeLimites(double[] limites, double latitud, double longitud) {
        return latitud >= limites[0] && latitud <= limites[2]
                && longitud >= limites[1] && longitud <= limites[3];
    }

    private void actualizarMarcadoresResultados(List<Resultado> resultados) {
        if (!mapaListo || mapa == null) {
            return;
        }
        List<Map<String, Object>> puntos = new ArrayList<>();
        for (Resultado resultado : resultados) {
            if (resultado.guardada() != null || resultado == puntoPendiente) {
                continue;
            }
            puntos.add(Map.of(
                    "lat", resultado.latitud(),
                    "lng", resultado.longitud(),
                    "nombre", resultado.nombre()));
        }
        enviarPuntos("marcarResultados", puntos);
    }

    private void enviarPuntos(String funcion, List<Map<String, Object>> puntos) {
        try {
            mapa.getEngine().executeScript(
                    funcion + "(" + json.writeValueAsString(puntos) + ")");
        } catch (Exception ignored) {
            // El mapa sigue funcionando aunque no se puedan refrescar los marcadores auxiliares.
        }
    }

    /** Invocado desde JavaScript al hacer clic en un marcador de lugar guardado. */
    public void seleccionarGuardada(int id) {
        Platform.runLater(() -> {
            Resultado encontrado = lista.getItems().stream()
                    .filter(r -> r.guardada() != null && r.guardada().getId() != null
                            && r.guardada().getId() == id)
                    .findFirst()
                    .orElse(null);
            if (encontrado == null) {
                encontrado = guardadas("").stream()
                        .filter(r -> r.guardada() != null && r.guardada().getId() != null
                                && r.guardada().getId() == id)
                        .findFirst()
                        .orElse(null);
                if (encontrado != null && !lista.getItems().contains(encontrado)) {
                    lista.getItems().add(0, encontrado);
                }
            }
            if (encontrado != null) {
                lista.getSelectionModel().select(encontrado);
                if (mapaListo && mapa != null) {
                    mapa.getEngine().executeScript("centrar(" + encontrado.latitud() + ","
                            + encontrado.longitud() + ",14)");
                }
            }
        });
    }

    /** Invocado desde JavaScript al hacer clic en un marcador de resultado de búsqueda. */
    public void seleccionarResultado(double latitud, double longitud) {
        Platform.runLater(() -> lista.getItems().stream()
                .filter(r -> Math.abs(r.latitud() - latitud) < 0.000001
                        && Math.abs(r.longitud() - longitud) < 0.000001)
                .findFirst()
                .ifPresent(r -> lista.getSelectionModel().select(r)));
    }

    private Path prepararMapaLocal() throws IOException {
        Path raiz = Files.createTempDirectory("fleetroute-pro-mapa-");
        Path leaflet = raiz.resolve("leaflet");
        Path imagenes = leaflet.resolve("images");
        Files.createDirectories(imagenes);
        raiz.toFile().deleteOnExit();
        leaflet.toFile().deleteOnExit();
        imagenes.toFile().deleteOnExit();
        copiarRecurso("leaflet.css", leaflet.resolve("leaflet.css"));
        copiarRecurso("leaflet.js", leaflet.resolve("leaflet.js"));
        for (String imagen : List.of("marker-icon.png", "marker-icon-2x.png", "marker-shadow.png",
                "layers.png", "layers-2x.png")) {
            copiarRecurso("images/" + imagen, imagenes.resolve(imagen));
        }
        Path html = raiz.resolve("mapa.html");
        Files.writeString(html, HTML_MAPA, StandardCharsets.UTF_8);
        html.toFile().deleteOnExit();
        return html;
    }

    private void copiarRecurso(String nombre, Path destino) throws IOException {
        String ruta = "/edu/ni/uam/proyecto_final_pae/web/leaflet/" + nombre;
        try (InputStream entrada = getClass().getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IOException("Falta el recurso " + ruta);
            }
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            destino.toFile().deleteOnExit();
        }
    }

    @FXML
    private void buscar() {
        esperaBusqueda.stop();
        if (pais == null || pais.isBlank()) {
            advertencia("Primero seleccione una ubicación de referencia para conocer el país.");
            return;
        }
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        if (esEnlaceCorto(texto)) {
            esperaBusqueda.stop();
            btnBuscar.setDisable(false);
            resolverEnlaceCorto(texto);
            return;
        }
        Coordenada coordenada = extraerCoordenadas(texto);
        if (coordenada != null) {
            generacionBusqueda++;
            btnBuscar.setDisable(false);
            txtBuscar.setText(String.format(Locale.ROOT, "%.5f, %.5f",
                    coordenada.latitud(), coordenada.longitud()));
            esperaBusqueda.stop();
            aplicarCoordenada(coordenada, esEnlaceGoogle(texto) ? texto : null);
            return;
        }
        if (texto.length() < 3) {
            generacionBusqueda++;
            btnBuscar.setDisable(false);
            cargarGuardadas(texto);
            lblEstado.setText("Escriba al menos 3 letras o seleccione un lugar guardado.");
            return;
        }
        int generacion = ++generacionBusqueda;
        btnBuscar.setDisable(true);
        lblEstado.setText("Buscando en OpenStreetMap…");
        List<Resultado> guardadas = guardadas(texto);
        double[] limites = limitesActuales();
        if (limites == null) {
            aplicarLimitesPais();
        }
        String url = NOMINATIM + "/search?format=jsonv2&addressdetails=1&limit=8&accept-language=es"
                + "&q=" + URLEncoder.encode(texto + ", " + pais, StandardCharsets.UTF_8);
        if (limites != null) {
            url += "&viewbox=" + limites[1] + "," + limites[2] + ","
                    + limites[3] + "," + limites[0] + "&bounded=1";
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(respuesta -> Platform.runLater(() -> {
                    if (generacion != generacionBusqueda) {
                        return;
                    }
                    btnBuscar.setDisable(false);
                    if (respuesta.statusCode() != 200) {
                        mostrarResultados(guardadas, "No se pudo consultar Nominatim. Se muestran los lugares guardados.");
                        return;
                    }
                    try {
                        List<Resultado> resultados = new ArrayList<>(guardadas);
                        JsonNode raiz = json.readTree(respuesta.body());
                        for (JsonNode nodo : raiz) {
                            Resultado resultado = desdeNominatim(nodo);
                            if (resultado != null && noDuplicado(resultados, resultado)) {
                                resultados.add(resultado);
                            }
                        }
                        if (limites != null) {
                            resultados.removeIf(resultado -> resultado.guardada() == null
                                    && !dentroDeLimites(limites, resultado.latitud(),
                                    resultado.longitud()));
                        }
                        if (!resultados.isEmpty()) {
                            mostrarResultados(resultados, resultados.size() + " resultado(s).");
                            if (mapaListo && mapa != null) {
                                mapa.getEngine().executeScript("centrar(" + resultados.get(0).latitud() + ","
                                        + resultados.get(0).longitud() + ",10)");
                            }
                        } else {
                            mostrarResultados(List.of(), "No se encontraron lugares para esa búsqueda.");
                        }
                    } catch (Exception e) {
                        mostrarResultados(guardadas, "No se pudo interpretar la respuesta del buscador.");
                    }
                }))
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        btnBuscar.setDisable(false);
                        mostrarResultados(guardadas, "No se pudo conectar con Nominatim. Se muestran los lugares guardados.");
                    });
                    return null;
                });
    }

    private void cargarGuardadas(String texto) {
        if (pais == null || pais.isBlank()) {
            return;
        }
        List<Resultado> guardadas = guardadas(texto);
        mostrarResultados(guardadas, guardadas.isEmpty()
                ? "No hay lugares guardados para este país."
                : guardadas.size() + " lugar(es) guardado(s).");
    }

    private List<Resultado> guardadas(String texto) {
        if (pais == null || pais.isBlank()) {
            return List.of();
        }
        try {
            List<Ubicacion> base = texto == null || texto.isBlank()
                    ? ubicacionDAO.listarPorPais(pais)
                    : ubicacionDAO.buscarPorPais(pais, texto);
            return base.stream()
                    .filter(u -> u.getLatitud() != null && u.getLongitud() != null)
                    .map(u -> new Resultado(u.getNombre(),
                            u.getDepartamento() + " · " + u.getPais(), u.getDepartamento(),
                            u.getPais(), u.getLatitud(), u.getLongitud(), u, null))
                    .toList();
        } catch (PersistenciaException e) {
            lblEstado.setText("No se pudieron consultar los lugares guardados.");
            return List.of();
        }
    }

    private Resultado desdeNominatim(JsonNode nodo) {
        if (!nodo.hasNonNull("lat") || !nodo.hasNonNull("lon")) {
            return null;
        }
        double lat;
        double lon;
        try {
            lat = Double.parseDouble(nodo.get("lat").asText());
            lon = Double.parseDouble(nodo.get("lon").asText());
        } catch (NumberFormatException e) {
            return null;
        }
        String display = nodo.path("display_name").asText("");
        String nombre = nodo.path("name").asText("");
        if (nombre.isBlank()) {
            int coma = display.indexOf(',');
            nombre = coma > 0 ? display.substring(0, coma) : display;
        }
        if (nombre.isBlank()) {
            nombre = "Lugar sin nombre";
        }
        if (nombre.length() > 120) {
            nombre = nombre.substring(0, 120);
        }
        JsonNode direccion = nodo.path("address");
        String departamento = primerTexto(direccion, "city", "town", "village",
                "municipality", "county", "state");
        String paisResultado = direccion.path("country").asText("");
        return new Resultado(nombre, display, departamento, paisResultado, lat, lon, null, null);
    }

    private String primerTexto(JsonNode nodo, String... campos) {
        for (String campo : campos) {
            String valor = nodo.path(campo).asText("");
            if (!valor.isBlank()) {
                return valor;
            }
        }
        return "";
    }

    private String nombreSugerido(JsonNode nodo) {
        String nombre = nodo.path("name").asText("").trim();
        if (!nombre.isBlank()) {
            return recortar(nombre);
        }
        JsonNode direccion = nodo.path("address");
        String via = primerTexto(direccion, "road", "pedestrian", "footway", "cycleway", "path");
        if (!via.isBlank()) {
            String numero = direccion.path("house_number").asText("").trim();
            return recortar(numero.isBlank() ? via : via + " " + numero);
        }
        String referencia = primerTexto(direccion, "neighbourhood", "suburb", "quarter",
                "village", "town", "city", "municipality", "county", "state");
        if (!referencia.isBlank()) {
            return recortar(referencia);
        }
        String display = nodo.path("display_name").asText("").trim();
        int coma = display.indexOf(',');
        return recortar(coma > 0 ? display.substring(0, coma) : display);
    }

    private String recortar(String texto) {
        return texto.length() > 120 ? texto.substring(0, 120) : texto;
    }

    private boolean noDuplicado(List<Resultado> resultados, Resultado resultado) {
        return resultados.stream().noneMatch(otro -> otro.nombre().equalsIgnoreCase(resultado.nombre())
                && Math.abs(otro.latitud() - resultado.latitud()) < 0.0005
                && Math.abs(otro.longitud() - resultado.longitud()) < 0.0005);
    }

    private void mostrarResultados(List<Resultado> resultados, String mensaje) {
        lista.getItems().setAll(resultados);
        if (!resultados.isEmpty()) {
            lista.getSelectionModel().selectFirst();
        }
        lblEstado.setText(mensaje);
        actualizarMarcadoresGuardados();
        actualizarMarcadoresResultados(resultados);
    }

    private void marcarSeleccion(Resultado resultado) {
        seleccion = resultado.guardada();
        if (resultado.guardada() != null) {
            ajustandoEstilo = true;
            try {
                cmbCategoria.setValue(categoriaSegura(resultado.guardada().getCategoria()));
                cmbColor.setValue(colorOpcion(colorSeguro(resultado.guardada().getColor())));
            } finally {
                ajustandoEstilo = false;
            }
            btnEditarLugar.setDisable(false);
            setModoEdicion(false);
        } else {
            btnEditarLugar.setDisable(true);
            setModoEdicion(true);
        }
        if (!aplicandoSugerencia) {
            txtNombreLugar.setText(resultado.nombre());
        }
        lblSeleccion.setText(resultado.nombre() + "  ·  "
                + String.format(Locale.ROOT, "%.5f, %.5f", resultado.latitud(), resultado.longitud()));
        btnConfirmar.setDisable(false);
        btnGoogle.setDisable(false);
        marcarSeleccionEnMapa(resultado);
    }

    private void marcarSeleccionEnMapa(Resultado resultado) {
        if (!mapaListo || mapa == null) {
            return;
        }
        String categoria = cmbCategoria.getValue() == null ? "Otro" : cmbCategoria.getValue();
        String color = cmbColor.getValue() == null
                ? COLOR_POR_DEFECTO : cmbColor.getValue().hex();
        mapa.getEngine().executeScript("marcarConEstilo(" + resultado.latitud() + ","
                + resultado.longitud() + ",'" + escapar(resultado.nombre()) + "','"
                + escapar(categoria) + "','" + color + "')");
    }

    private void actualizarEstiloSeleccion() {
        if (ajustandoEstilo) {
            return;
        }
        Resultado resultado = lista.getSelectionModel().getSelectedItem();
        if (resultado != null) {
            marcarSeleccionEnMapa(resultado);
        }
    }

    @FXML
    private void alternarEdicion() {
        if (btnEditarLugar.isDisable()) {
            return;
        }
        if (modoEdicion) {
            setModoEdicion(false);
        } else {
            setModoEdicion(true);
            txtNombreLugar.requestFocus();
            txtNombreLugar.selectAll();
        }
    }

    private void setModoEdicion(boolean edicion) {
        modoEdicion = edicion;
        txtNombreLugar.setDisable(!edicion);
        cmbCategoria.setDisable(!edicion);
        cmbColor.setDisable(!edicion);
        Resultado seleccionado = lista.getSelectionModel().getSelectedItem();
        boolean mostrarGuardar = edicion && seleccionado != null
                && seleccionado.guardada() != null;
        btnGuardarCambios.setVisible(mostrarGuardar);
        btnGuardarCambios.setManaged(mostrarGuardar);
        if (!edicion) {
            Resultado resultado = lista.getSelectionModel().getSelectedItem();
            if (resultado != null && resultado.guardada() != null) {
                txtNombreLugar.setText(resultado.guardada().getNombre());
                ajustandoEstilo = true;
                try {
                    cmbCategoria.setValue(categoriaSegura(resultado.guardada().getCategoria()));
                    cmbColor.setValue(colorOpcion(colorSeguro(resultado.guardada().getColor())));
                } finally {
                    ajustandoEstilo = false;
                }
                marcarSeleccionEnMapa(resultado);
            }
        }
    }

    /** Invocado desde JavaScript al hacer clic en el mapa. */
    public void clicEnMapa(double latitud, double longitud) {
        clicEnMapa(latitud, longitud, null);
    }

    private void clicEnMapa(double latitud, double longitud, String enlaceGoogle) {
        Platform.runLater(() -> {
            if (mapaListo && mapa != null) {
                mapa.getEngine().executeScript("marcar(" + latitud + "," + longitud + ",'Punto seleccionado')");
            }
            Resultado seleccionado = lista.getSelectionModel().getSelectedItem();
            if (puntoPendiente != null) {
                if (seleccionado == puntoPendiente) {
                    seleccionado = null;
                }
                lista.getItems().remove(puntoPendiente);
            }
            Resultado anterior = seleccionado;
            cmbCategoria.setValue("Otro");
            cmbColor.setValue(COLORES_UBICACION.get(0));
            Resultado provisional = new Resultado("Punto seleccionado", "", "", latitud, longitud,
                    null, enlaceGoogle);
            puntoPendiente = provisional;
            lista.getItems().add(0, provisional);
            lista.getSelectionModel().select(provisional);
            txtNombreLugar.setText("Punto seleccionado");
            txtNombreLugar.requestFocus();
            txtNombreLugar.selectAll();
            lblEstado.setText("Buscando una referencia cercana… Aún no está guardado.");
            String url = NOMINATIM + "/reverse?format=jsonv2&accept-language=es&lat=" + latitud
                    + "&lon=" + longitud;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(respuesta -> Platform.runLater(() -> {
                        if (lista.getSelectionModel().getSelectedItem() != provisional
                                || respuesta.statusCode() != 200) {
                            return;
                        }
                        try {
                            JsonNode nodo = json.readTree(respuesta.body());
                            String sugerido = nombreSugerido(nodo);
                            if (sugerido.isBlank()) {
                                lblEstado.setText("No se encontró una referencia cercana; escriba el nombre manualmente.");
                                return;
                            }
                            String display = nodo.path("display_name").asText("");
                            JsonNode direccion = nodo.path("address");
                            String departamento = primerTexto(direccion, "city", "town",
                                    "village", "municipality", "county", "state");
                            String paisResultado = direccion.path("country").asText("");
                            boolean cambioPais = false;
                            if (!paisResultado.isBlank() && !mismoPais(paisResultado, pais)) {
                                DecisionPais decision = preguntarPais(paisResultado);
                                if (decision == DecisionPais.CANCELAR) {
                                    lista.getItems().remove(provisional);
                                    if (puntoPendiente == provisional) {
                                        puntoPendiente = null;
                                    }
                                    if (anterior != null) {
                                        lista.getSelectionModel().select(anterior);
                                    } else {
                                        lista.getSelectionModel().clearSelection();
                                        txtNombreLugar.clear();
                                        lblSeleccion.setText("Ningún lugar seleccionado.");
                                        btnConfirmar.setDisable(true);
                                        btnGoogle.setDisable(true);
                                        seleccion = null;
                                    }
                                    if (mapaListo && mapa != null) {
                                        mapa.getEngine().executeScript("quitarMarcador()");
                                    }
                                    lblEstado.setText("Enlace descartado; no se guardó nada.");
                                    aplicarLimitesPais();
                                    return;
                                }
                                this.pais = paisResultado;
                                lblPais.setText("País actual: " + paisResultado);
                                cambioPais = true;
                            }
                            boolean nombreSinEditar = txtNombreLugar.getText() == null
                                    || txtNombreLugar.getText().isBlank()
                                    || "Punto seleccionado".equals(txtNombreLugar.getText().trim());
                            Resultado actualizado = new Resultado(sugerido, display, departamento,
                                    paisResultado, latitud, longitud, null, enlaceGoogle);
                            puntoPendiente = actualizado;
                            aplicandoSugerencia = true;
                            try {
                                if (cambioPais) {
                                    List<Resultado> items = new ArrayList<>();
                                    items.add(actualizado);
                                    for (Resultado guardada : guardadas("")) {
                                        if (noDuplicado(items, guardada)) {
                                            items.add(guardada);
                                        }
                                    }
                                    lista.getItems().setAll(items);
                                } else {
                                    int indice = lista.getItems().indexOf(provisional);
                                    if (indice >= 0) {
                                        lista.getItems().set(indice, actualizado);
                                    }
                                }
                                lista.getSelectionModel().select(actualizado);
                            } finally {
                                aplicandoSugerencia = false;
                            }
                            if (nombreSinEditar) {
                                txtNombreLugar.setText(sugerido);
                                txtNombreLugar.requestFocus();
                                txtNombreLugar.selectAll();
                            }
                            marcarSeleccionEnMapa(actualizado);
                            String detallePais = cambioPais
                                    ? " El país actual y el " + rol + " cambiarán a " + paisResultado + "."
                                    : "";
                            lblEstado.setText("Sugerencia: " + sugerido + "."
                                    + detallePais + " Aún no está guardado; edite el nombre y confirme.");
                            actualizarMarcadoresGuardados();
                            actualizarMarcadoresResultados(lista.getItems());
                            aplicarLimitesPais();
                        } catch (Exception ignored) {
                            lblEstado.setText("No se pudo interpretar la referencia cercana; puede escribir el nombre.");
                        }
                    }))
                    .exceptionally(error -> {
                        Platform.runLater(() -> {
                            lblEstado.setText("No se pudo obtener la dirección sugerida; "
                                    + "escriba el nombre y confirme para guardarlo.");
                            aplicarLimitesPais();
                        });
                        return null;
                    });
        });
    }

    @FXML
    private void pegarUbicacion() {
        Clipboard portapapeles = Clipboard.getSystemClipboard();
        String texto = portapapeles.hasString() ? portapapeles.getString() : null;
        if (texto == null || texto.isBlank()) {
            lblEstado.setText("El portapapeles no contiene texto.");
            return;
        }
        texto = texto.trim();
        if (esEnlaceCorto(texto)) {
            txtBuscar.setText(texto);
            esperaBusqueda.stop();
            resolverEnlaceCorto(texto);
            return;
        }
        Coordenada coordenada = extraerCoordenadas(texto);
        if (coordenada == null) {
            txtBuscar.setText(texto);
            esperaBusqueda.stop();
            buscar();
            return;
        }
        txtBuscar.setText(String.format(Locale.ROOT, "%.5f, %.5f",
                coordenada.latitud(), coordenada.longitud()));
        esperaBusqueda.stop();
        aplicarCoordenada(coordenada, esEnlaceGoogle(texto) ? texto : null);
    }

    private void aplicarCoordenada(Coordenada coordenada, String enlaceGoogle) {
        if (mapaListo && mapa != null) {
            mapa.getEngine().executeScript("quitarLimitesPais();");
            mapa.getEngine().executeScript("centrar(" + coordenada.latitud() + ","
                    + coordenada.longitud() + ",16)");
        }
        clicEnMapa(coordenada.latitud(), coordenada.longitud(), enlaceGoogle);
    }

    private boolean esEnlaceCorto(String texto) {
        if (texto == null) {
            return false;
        }
        String limpio = texto.toLowerCase(Locale.ROOT);
        return limpio.contains("maps.app.goo.gl") || limpio.contains("goo.gl/maps");
    }

    private boolean esEnlaceGoogle(String texto) {
        if (texto == null) {
            return false;
        }
        String limpio = texto.toLowerCase(Locale.ROOT);
        return limpio.contains("google.com/maps") || limpio.contains("maps.google.com")
                || limpio.contains("maps.app.goo.gl") || limpio.contains("goo.gl/maps");
    }

    private void resolverEnlaceCorto(String texto) {
        String enlace = texto.startsWith("http://") || texto.startsWith("https://")
                ? texto : "https://" + texto;
        int generacion = ++generacionBusqueda;
        lblEstado.setText("Resolviendo enlace de Google Maps…");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(enlace))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(respuesta -> Platform.runLater(() -> {
                        if (generacion != generacionBusqueda
                                || ventana == null || !ventana.isShowing()) {
                            return;
                        }
                        Coordenada coordenada = extraerCoordenadas(respuesta.uri().toString());
                        if (coordenada == null) {
                            lblEstado.setText("El enlace no contiene coordenadas visibles. "
                                    + "Copie la URL completa desde el navegador.");
                            return;
                        }
                        txtBuscar.setText(String.format(Locale.ROOT, "%.5f, %.5f",
                                coordenada.latitud(), coordenada.longitud()));
                        esperaBusqueda.stop();
                        aplicarCoordenada(coordenada, respuesta.uri().toString());
                    }))
                    .exceptionally(error -> {
                        Platform.runLater(() -> {
                            if (generacion == generacionBusqueda
                                    && ventana != null && ventana.isShowing()) {
                                lblEstado.setText("No se pudo resolver el enlace corto de Google Maps.");
                            }
                        });
                        return null;
                    });
        } catch (IllegalArgumentException e) {
            lblEstado.setText("El enlace del portapapeles no es válido.");
        }
    }

    private Coordenada extraerCoordenadas(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        Coordenada dms = extraerDms(texto);
        if (dms != null) {
            return dms;
        }
        String normalizado = texto.replace("%2C", ",").replace("%2c", ",");
        List<String> patrones = List.of(
                "!3d(-?\\d{1,2}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)",
                "[?&](?:q|query|destination)=(?:loc:)?"
                        + "(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)",
                "@(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)",
                "[?&](?:ll|center)=(?:loc:)?"
                        + "(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)",
                "^\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*[, ]\\s*"
                        + "(-?\\d{1,3}(?:\\.\\d+)?)\\s*$");
        for (String patron : patrones) {
            Matcher coincidencia = Pattern.compile(patron).matcher(normalizado);
            if (!coincidencia.find()) {
                continue;
            }
            try {
                double latitud = Double.parseDouble(coincidencia.group(1));
                double longitud = Double.parseDouble(coincidencia.group(2));
                if (latitud >= -90 && latitud <= 90 && longitud >= -180 && longitud <= 180) {
                    return new Coordenada(latitud, longitud);
                }
            } catch (NumberFormatException ignored) {
                // Se prueba el siguiente patrón.
            }
        }
        return null;
    }

    private Coordenada extraerDms(String texto) {
        Matcher coincidencia = PATRON_DMS.matcher(texto);
        if (!coincidencia.find()) {
            return null;
        }
        try {
            double latitud = Double.parseDouble(coincidencia.group(1))
                    + Double.parseDouble(coincidencia.group(2)) / 60
                    + Double.parseDouble(coincidencia.group(3)) / 3600;
            if ("S".equalsIgnoreCase(coincidencia.group(4))) {
                latitud = -latitud;
            }
            double longitud = Double.parseDouble(coincidencia.group(5))
                    + Double.parseDouble(coincidencia.group(6)) / 60
                    + Double.parseDouble(coincidencia.group(7)) / 3600;
            if ("W".equalsIgnoreCase(coincidencia.group(8))) {
                longitud = -longitud;
            }
            if (latitud >= -90 && latitud <= 90 && longitud >= -180 && longitud <= 180) {
                return new Coordenada(latitud, longitud);
            }
        } catch (NumberFormatException ignored) {
            // Se intenta con los demás formatos.
        }
        return null;
    }

    @FXML
    private void confirmar() {
        Resultado resultado = lista.getSelectionModel().getSelectedItem();
        if (resultado == null) {
            advertencia("Seleccione un resultado o haga clic en el mapa.");
            return;
        }
        String nombre = txtNombreLugar.getText() == null ? "" : txtNombreLugar.getText().trim();
        if (nombre.isEmpty()) {
            advertencia("Escriba el nombre con el que desea guardar este lugar.");
            txtNombreLugar.requestFocus();
            return;
        }
        if (nombre.length() > 120) {
            advertencia("El nombre del lugar no puede pasar de 120 caracteres.");
            return;
        }
        String paisResultado = resultado.pais() == null
                || resultado.pais().isBlank() ? pais : resultado.pais();
        String departamento = resultado.departamento() == null
                || resultado.departamento().isBlank() ? paisResultado : resultado.departamento();
        String categoria = cmbCategoria.getValue() == null ? "Otro" : cmbCategoria.getValue();
        String color = cmbColor.getValue() == null
                ? COLOR_POR_DEFECTO : cmbColor.getValue().hex();
        try {
            if (resultado.guardada() != null) {
                Ubicacion guardada = resultado.guardada();
                if (!hayCambios(guardada, nombre, categoria, color)) {
                    seleccion = guardada;
                } else {
                    try {
                        seleccion = guardarCambiosGuardada(
                                guardada, nombre, categoria, color);
                    } catch (IllegalArgumentException e) {
                        advertencia(e.getMessage());
                        return;
                    }
                }
            } else {
                Ubicacion existente = ubicacionDAO.buscarPorNombreDepartamentoYPais(
                        nombre, departamento, paisResultado);
                if (existente != null) {
                    if (mismasCoordenadas(existente, resultado)) {
                        if (estiloCambio(existente, categoria, color)) {
                            existente.setCategoria(categoria);
                            existente.setColor(color);
                            seleccion = ubicacionDAO.actualizar(existente);
                        } else {
                            seleccion = existente;
                        }
                    } else {
                        advertencia("Ya existe un lugar llamado \"" + nombre + "\" en "
                                + departamento + " con otras coordenadas. "
                                + "Cambie el nombre para guardar este punto.");
                        return;
                    }
                } else {
                    Ubicacion nueva = new Ubicacion(nombre, departamento, paisResultado,
                            resultado.latitud(), resultado.longitud());
                    nueva.setCategoria(categoria);
                    nueva.setColor(color);
                    seleccion = ubicacionDAO.guardar(nueva);
                }
            }
            puntoPendiente = null;
            ventana.close();
        } catch (PersistenciaException e) {
            advertencia("No se pudo guardar el lugar: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            advertencia("No se pudo confirmar el lugar: " + e.getMessage());
        }
    }

    private boolean hayCambios(Ubicacion guardada, String nombre, String categoria, String color) {
        return !nombre.equalsIgnoreCase(guardada.getNombre())
                || estiloCambio(guardada, categoria, color);
    }

    @FXML
    private void guardarCambiosLugar() {
        Resultado resultado = lista.getSelectionModel().getSelectedItem();
        if (resultado == null || resultado.guardada() == null || !modoEdicion) {
            return;
        }
        String nombre = txtNombreLugar.getText() == null ? "" : txtNombreLugar.getText().trim();
        if (nombre.isEmpty() || nombre.length() > 120) {
            advertencia(nombre.isEmpty()
                    ? "Escriba el nombre del lugar."
                    : "El nombre del lugar no puede pasar de 120 caracteres.");
            return;
        }
        String categoria = cmbCategoria.getValue() == null ? "Otro" : cmbCategoria.getValue();
        String color = cmbColor.getValue() == null
                ? COLOR_POR_DEFECTO : cmbColor.getValue().hex();
        if (!hayCambios(resultado.guardada(), nombre, categoria, color)) {
            setModoEdicion(false);
            lblEstado.setText("No hay cambios para guardar.");
            return;
        }
        try {
            Ubicacion actualizada = guardarCambiosGuardada(
                    resultado.guardada(), nombre, categoria, color);
            Resultado nuevo = new Resultado(actualizada.getNombre(),
                    actualizada.getDepartamento() + " · " + actualizada.getPais(),
                    actualizada.getDepartamento(), actualizada.getPais(),
                    actualizada.getLatitud(), actualizada.getLongitud(), actualizada, null);
            int indice = lista.getItems().indexOf(resultado);
            if (indice >= 0) {
                lista.getItems().set(indice, nuevo);
            }
            lista.getSelectionModel().select(nuevo);
            setModoEdicion(false);
            actualizarMarcadoresGuardados();
            lblEstado.setText("Cambios guardados.");
        } catch (IllegalArgumentException e) {
            advertencia(e.getMessage());
        } catch (PersistenciaException e) {
            advertencia("No se pudieron guardar los cambios: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            advertencia("No se pudieron guardar los cambios: " + e.getMessage());
        }
    }

    private Ubicacion guardarCambiosGuardada(Ubicacion guardada, String nombre,
                                              String categoria, String color)
            throws PersistenciaException {
        Ubicacion conflicto = ubicacionDAO.buscarPorNombreDepartamentoYPais(
                nombre, guardada.getDepartamento(), guardada.getPais());
        if (conflicto != null && !Objects.equals(conflicto.getId(), guardada.getId())) {
            throw new IllegalArgumentException("Ya existe otro lugar llamado \"" + nombre
                    + "\" en " + guardada.getDepartamento() + ".");
        }
        guardada.setNombre(nombre);
        guardada.setCategoria(categoria);
        guardada.setColor(color);
        return ubicacionDAO.actualizar(guardada);
    }

    private boolean estiloCambio(Ubicacion ubicacion, String categoria, String color) {
        return !Objects.equals(categoriaSegura(ubicacion.getCategoria()), categoria)
                || !Objects.equals(colorSeguro(ubicacion.getColor()), color);
    }

    private boolean mismasCoordenadas(Ubicacion ubicacion, Resultado resultado) {
        if (ubicacion.getLatitud() == null || ubicacion.getLongitud() == null) {
            return false;
        }
        return Math.abs(ubicacion.getLatitud() - resultado.latitud()) < 0.0001
                && Math.abs(ubicacion.getLongitud() - resultado.longitud()) < 0.0001;
    }

    private DecisionPais preguntarPais(String paisResultado) {
        ButtonType usar = new ButtonType("Usar ubicación", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "El lugar está en " + paisResultado + ", pero el país actual es " + pais
                        + ". Si lo usa, el " + rol + " cambiará a " + paisResultado + ".",
                usar, cancelar);
        alerta.setTitle("Selección de lugar");
        alerta.setHeaderText("El enlace pertenece a otro país");
        if (ventana != null) {
            alerta.initOwner(ventana);
            alerta.initModality(Modality.WINDOW_MODAL);
        }
        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() == cancelar) {
            return DecisionPais.CANCELAR;
        }
        return DecisionPais.USAR;
    }

    private boolean mismoPais(String uno, String otro) {
        if (uno == null || uno.isBlank() || otro == null || otro.isBlank()) {
            return true;
        }
        return normalizar(uno).equals(normalizar(otro));
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    @FXML
    private void abrirGoogleMaps() {
        Resultado resultado = lista.getSelectionModel().getSelectedItem();
        if (resultado == null) {
            return;
        }
        try {
            String url = resultado.enlaceGoogle();
            if (url == null || url.isBlank()) {
                url = "https://www.google.com/maps/search/?api=1&query="
                        + resultado.latitud() + "," + resultado.longitud();
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            advertencia("No se pudo abrir Google Maps: " + e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        seleccion = null;
        puntoPendiente = null;
        ventana.close();
    }

    private void advertencia(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK);
        alerta.setHeaderText("Selección de lugar");
        alerta.showAndWait();
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static final String HTML_MAPA = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <link rel="stylesheet" href="leaflet/leaflet.css"/>
              <style>
                html, body { margin: 0; height: 100%; font-family: Segoe UI, sans-serif; }
                #mapa { width: 100%; height: 100%; background: #D6EAF8; }
                .leaflet-control-attribution { font-size: 9px; }
              </style>
            </head>
            <body>
              <div id="mapa"></div>
              <script>window.L_DISABLE_3D = true; /* El WebView de JavaFX no pinta bien translate3d. */</script>
              <script src="leaflet/leaflet.js"></script>
              <script>
                L.Browser.any3d = false;
                var MAX_SATELITE = 17;
                var MAX_CALLE = 19;
                var mapa = L.map('mapa', {
                  zoomControl: true,
                  zoomAnimation: false,
                  fadeAnimation: false,
                  markerZoomAnimation: false,
                  maxZoom: MAX_SATELITE
                }).setView([12.8, -85.5], 6);
                var capaCalles = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
                  maxZoom: MAX_CALLE,
                  keepBuffer: 4,
                  attribution: 'Tiles &copy; Esri · Geocoding &copy; OpenStreetMap/Nominatim'
                });
                var capaSatelite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                  maxZoom: MAX_CALLE,
                  maxNativeZoom: MAX_SATELITE,
                  keepBuffer: 4,
                  attribution: 'Tiles &copy; Esri · Geocoding &copy; OpenStreetMap/Nominatim'
                }).addTo(mapa);
                var capaGuardados = L.layerGroup().addTo(mapa);
                var capaResultados = L.layerGroup().addTo(mapa);
                L.control.layers(
                  { 'Calles': capaCalles, 'Satélite': capaSatelite },
                  { 'Lugares guardados': capaGuardados, 'Resultados': capaResultados },
                  { position: 'topright', collapsed: true }
                ).addTo(mapa);
                mapa.on('baselayerchange', function (e) {
                  if (e.name === 'Satélite') {
                    mapa.setMaxZoom(MAX_SATELITE);
                    if (mapa.getZoom() > MAX_SATELITE) { mapa.setZoom(MAX_SATELITE); }
                  } else {
                    mapa.setMaxZoom(MAX_CALLE);
                  }
                });
                var marcador = null;
                var mapaAvisado = false;

                function avisarMapaListo() {
                  if (mapaAvisado || !window.javaApp || !window.javaApp.mapaCargado) {
                    return;
                  }
                  mapaAvisado = true;
                  window.javaApp.mapaCargado();
                }

                function marcar(lat, lng, nombre) {
                  if (marcador) { mapa.removeLayer(marcador); }
                  marcador = L.marker([lat, lng]).addTo(mapa).bindPopup(nombre).openPopup();
                }

                function marcarConEstilo(lat, lng, nombre, categoria, color) {
                  if (marcador) { mapa.removeLayer(marcador); }
                  var icono = L.divIcon({
                    className: '',
                    html: svgGuardado(categoria, color),
                    iconSize: [26, 26],
                    iconAnchor: [13, 26],
                    popupAnchor: [0, -24]
                  });
                  marcador = L.marker([lat, lng], { icon: icono })
                    .addTo(mapa).bindPopup(nombre).openPopup();
                }

                function quitarMarcador() {
                  if (marcador) { mapa.removeLayer(marcador); marcador = null; }
                }

                function restringirPais(sur, oeste, norte, este) {
                  var limites = L.latLngBounds([sur, oeste], [norte, este]);
                  mapa.setMaxBounds(limites);
                  mapa.options.maxBoundsViscosity = 1.0;
                  if (!limites.contains(mapa.getCenter())) {
                    mapa.panInsideBounds(limites, { animate: false });
                  }
                }

                function quitarLimitesPais() {
                  mapa.setMaxBounds([[-85, -180], [85, 180]]);
                }

                function svgGuardado(categoria, color) {
                  var path = 'M12 2C8.1 2 5 5.1 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-3.9-3.1-7-7-7z';
                  if (categoria === 'Casa') {
                    path = 'M12 3 2 12h3v9h6v-6h2v6h6v-9h3L12 3z';
                  } else if (categoria === 'Negocio') {
                    path = 'M4 4h16l2 5H2l2-5zm0 7h16v9H4v-9zm3 2v5h4v-5H7zm6 0v5h4v-5h-4z';
                  } else if (categoria === 'Bodega') {
                    path = 'M3 10 12 4l9 6v10H3V10zm5 2v6h8v-6H8z';
                  } else if (categoria === 'Oficina') {
                    path = 'M4 3h16v18H4V3zm3 3v3h3V6H7zm7 0v3h3V6h-3zM7 11v3h3v-3H7zm7 0v3h3v-3h-3zM7 16v3h3v-3H7zm7 0v3h3v-3h-3z';
                  }
                  return '<svg width="26" height="26" viewBox="0 0 24 24">'
                    + '<path d="' + path + '" fill="' + color
                    + '" stroke="#ffffff" stroke-width="1.5"/></svg>';
                }

                function marcarGuardadas(puntos) {
                  capaGuardados.clearLayers();
                  for (var i = 0; i < puntos.length; i += 1) {
                    (function (p) {
                      var color = p.color || '#0891B2';
                      var icono = L.divIcon({
                        className: '',
                        html: svgGuardado(p.categoria, color),
                        iconSize: [26, 26],
                        iconAnchor: [13, 26],
                        popupAnchor: [0, -24]
                      });
                      var m = L.marker([p.lat, p.lng], { icon: icono });
                      m.bindPopup(p.nombre + (p.detalle ? '<br/>' + p.detalle : ''));
                      m.on('click', function () {
                        if (window.javaApp) { window.javaApp.seleccionarGuardada(p.id); }
                      });
                      capaGuardados.addLayer(m);
                    })(puntos[i]);
                  }
                }

                function marcarResultados(puntos) {
                  capaResultados.clearLayers();
                  for (var i = 0; i < puntos.length; i += 1) {
                    (function (p) {
                      var m = L.circleMarker([p.lat, p.lng], {
                        radius: 5, color: '#BE123C', weight: 2,
                        fillColor: '#FB7185', fillOpacity: 0.85
                      });
                      m.bindPopup(p.nombre);
                      m.on('click', function () {
                        if (window.javaApp) { window.javaApp.seleccionarResultado(p.lat, p.lng); }
                      });
                      capaResultados.addLayer(m);
                    })(puntos[i]);
                  }
                }

                function centrar(lat, lng, zoom) {
                  mapa.setView([lat, lng], zoom || 10);
                  setTimeout(function () { mapa.invalidateSize(); }, 150);
                }

                mapa.on('click', function (e) {
                  if (window.javaApp) {
                    window.javaApp.clicEnMapa(e.latlng.lat, e.latlng.lng);
                  }
                });
                capaCalles.on('load', avisarMapaListo);
                capaSatelite.on('load', avisarMapaListo);
                mapa.whenReady(function () { setTimeout(avisarMapaListo, 800); });
                setTimeout(avisarMapaListo, 6000);
                function reajustar() { mapa.invalidateSize(); }
                window.addEventListener('resize', reajustar);
                setTimeout(reajustar, 300);
                setTimeout(reajustar, 900);
              </script>
            </body>
            </html>
            """;
}
