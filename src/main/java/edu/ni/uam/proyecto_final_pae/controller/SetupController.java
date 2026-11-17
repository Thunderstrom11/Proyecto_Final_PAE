package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.GeoUtil;
import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.dao.ClienteDAO;
import edu.ni.uam.proyecto_final_pae.dao.OperadorDAO;
import edu.ni.uam.proyecto_final_pae.dao.RutaDAO;
import edu.ni.uam.proyecto_final_pae.dao.UbicacionDAO;
import edu.ni.uam.proyecto_final_pae.dao.UnidadDAO;
import edu.ni.uam.proyecto_final_pae.dao.UsuarioDAO;
import edu.ni.uam.proyecto_final_pae.model.Cliente;
import edu.ni.uam.proyecto_final_pae.model.Empresa;
import edu.ni.uam.proyecto_final_pae.model.EstadoOperador;
import edu.ni.uam.proyecto_final_pae.model.EstadoUnidad;
import edu.ni.uam.proyecto_final_pae.model.Operador;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Ruta;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;
import edu.ni.uam.proyecto_final_pae.model.Unidad;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.util.ComboBuscador;
import edu.ni.uam.proyecto_final_pae.util.Listas;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Asistente de primer arranque: empresa, cuenta de administrador y datos iniciales
 * (ubicaciones, unidades, operadores, clientes y tarifas) antes del primer login.
 */
public class SetupController {

    private enum Paso { EMPRESA, ADMIN, UBICACIONES, UNIDADES, OPERADORES, CLIENTES, TARIFAS, RESUMEN }

    private static final Map<String, String> ZONA_POR_PAIS = Map.ofEntries(
            Map.entry("Nicaragua", "America/Managua"),
            Map.entry("Costa Rica", "America/Costa_Rica"),
            Map.entry("Panamá", "America/Panama"),
            Map.entry("Honduras", "America/Tegucigalpa"),
            Map.entry("El Salvador", "America/El_Salvador"),
            Map.entry("Guatemala", "America/Guatemala"),
            Map.entry("Belice", "America/Belize"),
            Map.entry("México", "America/Mexico_City"),
            Map.entry("España", "Europe/Madrid"),
            Map.entry("Estados Unidos", "America/New_York"),
            Map.entry("Colombia", "America/Bogota"),
            Map.entry("Perú", "America/Lima"),
            Map.entry("Chile", "America/Santiago"),
            Map.entry("Argentina", "America/Argentina/Buenos_Aires"),
            Map.entry("Ecuador", "America/Guayaquil"),
            Map.entry("Venezuela", "America/Caracas"),
            Map.entry("República Dominicana", "America/Santo_Domingo"),
            Map.entry("Brasil", "America/Sao_Paulo")
    );

    private static final String CLAVE_TEMPORAL = "operador123!";

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final UbicacionDAO ubicacionDAO = new UbicacionDAO();
    private final UnidadDAO unidadDAO = new UnidadDAO();
    private final OperadorDAO operadorDAO = new OperadorDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final RutaDAO rutaDAO = new RutaDAO();

    @FXML private ProgressBar barraAvance;
    @FXML private Label lblPaso;
    @FXML private Label lblAyuda;

    @FXML private VBox pasoEmpresa;
    @FXML private TextField txtNombre;
    @FXML private TextField txtRuc;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtDireccion;
    @FXML private ComboBox<String> cmbEmpresaPais;
    @FXML private ComboBox<String> cmbZona;
    @FXML private ComboBox<String> cmbMoneda;

    @FXML private VBox pasoAdmin;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmar;

    @FXML private VBox pasoUbicaciones;
    @FXML private TextField txtUbicNombre;
    @FXML private TextField txtUbicDepartamento;
    @FXML private ComboBox<String> cmbUbicPais;
    @FXML private TextField txtUbicLat;
    @FXML private TextField txtUbicLon;
    @FXML private ListView<String> listaUbicaciones;
    @FXML private Button btnQuitarUbicacion;

    @FXML private VBox pasoUnidades;
    @FXML private TextField txtUnidadNumero;
    @FXML private TextField txtUnidadPlacas;
    @FXML private TextField txtUnidadModelo;
    @FXML private TextField txtUnidadCapacidad;
    @FXML private DatePicker dpSeguro;
    @FXML private DatePicker dpRevision;
    @FXML private ListView<String> listaUnidades;
    @FXML private Button btnQuitarUnidad;

    @FXML private VBox pasoOperadores;
    @FXML private TextField txtOpCedula;
    @FXML private TextField txtOpNombre;
    @FXML private TextField txtOpTelefono;
    @FXML private TextField txtLicenciaNumero;
    @FXML private TextField txtLicenciaCategoria;
    @FXML private DatePicker dpLicenciaVencimiento;
    @FXML private TextField txtContactoEmergencia;
    @FXML private TextField txtOpHorasMax;
    @FXML private TextField txtOpHorasMin;
    @FXML private ListView<String> listaOperadores;
    @FXML private Button btnQuitarOperador;

    @FXML private VBox pasoClientes;
    @FXML private TextField txtClienteNombre;
    @FXML private TextField txtClienteContacto;
    @FXML private TextField txtClienteTelefono;
    @FXML private TextField txtClienteEmail;
    @FXML private TextField txtClienteDireccion;
    @FXML private ListView<String> listaClientes;
    @FXML private Button btnQuitarCliente;

    @FXML private VBox pasoTarifas;
    @FXML private ComboBox<Ubicacion> cmbTarifaOrigen;
    @FXML private ComboBox<Ubicacion> cmbTarifaDestino;
    @FXML private TextField txtTarifaHoras;
    @FXML private TextField txtTarifaPrecio;
    @FXML private ListView<String> listaTarifas;
    @FXML private Button btnQuitarTarifa;

    @FXML private VBox pasoResumen;
    @FXML private Label lblResumen;

    @FXML private Label lblMensaje;
    @FXML private Button btnAtras;
    @FXML private Button btnOmitir;
    @FXML private Button btnSiguiente;

    private final List<Paso> pasos = new ArrayList<>();
    private final List<Ubicacion> nuevasUbicaciones = new ArrayList<>();
    private final List<Unidad> nuevasUnidades = new ArrayList<>();
    private final List<Operador> nuevosOperadores = new ArrayList<>();
    private final List<Cliente> nuevosClientes = new ArrayList<>();
    private final List<Ruta> nuevasRutas = new ArrayList<>();

    private int indice;
    private boolean crearAdmin;
    private Empresa empresa;
    private Usuario admin;

    @FXML
    private void initialize() {
        ComboBuscador.configurar(cmbMoneda, Listas.monedas());
        cmbMoneda.setValue(monedaPorDefecto());
        ComboBuscador.configurar(cmbEmpresaPais, Listas.paises());
        cmbEmpresaPais.setValue("Nicaragua");
        ComboBuscador.configurar(cmbZona, Listas.zonasHorarias());
        cmbZona.setValue("America/Managua");
        cmbEmpresaPais.valueProperty().addListener((o, a, b) -> sugerirZona(b));
        ComboBuscador.configurar(cmbUbicPais, Listas.paises());
        cmbUbicPais.setValue("Nicaragua");
        txtOpHorasMax.setText("8.00");
        txtOpHorasMin.setText("8.00");
        ComboBuscador.configurar(cmbTarifaOrigen, List.of());
        ComboBuscador.configurar(cmbTarifaDestino, List.of());
        cmbTarifaOrigen.valueProperty().addListener((o, a, b) -> sugerirTarifa());
        cmbTarifaDestino.valueProperty().addListener((o, a, b) -> sugerirTarifa());

        try {
            crearAdmin = !usuarioDAO.existeAdministrador();
        } catch (PersistenciaException e) {
            lblMensaje.setText("No hay conexión con la base de datos: " + e.getMessage());
            btnSiguiente.setDisable(true);
            return;
        }

        pasos.add(Paso.EMPRESA);
        if (crearAdmin) {
            pasos.add(Paso.ADMIN);
        }
        pasos.add(Paso.UBICACIONES);
        pasos.add(Paso.UNIDADES);
        pasos.add(Paso.OPERADORES);
        pasos.add(Paso.CLIENTES);
        pasos.add(Paso.TARIFAS);
        pasos.add(Paso.RESUMEN);
        mostrarPaso();
    }

    // ------------------------------------------------------------------ navegación

    @FXML
    private void atras() {
        if (indice > 0) {
            indice--;
            mostrarPaso();
        }
    }

    @FXML
    private void omitir() {
        lblMensaje.setText("");
        if (indice < pasos.size() - 1) {
            indice++;
            mostrarPaso();
        }
    }

    @FXML
    private void siguiente() {
        lblMensaje.setText("");
        try {
            switch (pasos.get(indice)) {
                case EMPRESA -> {
                    empresa = construirEmpresa();
                    indice++;
                    mostrarPaso();
                }
                case ADMIN -> {
                    admin = construirAdmin();
                    indice++;
                    mostrarPaso();
                }
                case RESUMEN -> guardarTodo();
                default -> {
                    indice++;
                    mostrarPaso();
                }
            }
        } catch (IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
        }
    }

    private void mostrarPaso() {
        Paso actual = pasos.get(indice);
        pasoEmpresa.setVisible(actual == Paso.EMPRESA);
        pasoEmpresa.setManaged(actual == Paso.EMPRESA);
        pasoAdmin.setVisible(actual == Paso.ADMIN);
        pasoAdmin.setManaged(actual == Paso.ADMIN);
        pasoUbicaciones.setVisible(actual == Paso.UBICACIONES);
        pasoUbicaciones.setManaged(actual == Paso.UBICACIONES);
        pasoUnidades.setVisible(actual == Paso.UNIDADES);
        pasoUnidades.setManaged(actual == Paso.UNIDADES);
        pasoOperadores.setVisible(actual == Paso.OPERADORES);
        pasoOperadores.setManaged(actual == Paso.OPERADORES);
        pasoClientes.setVisible(actual == Paso.CLIENTES);
        pasoClientes.setManaged(actual == Paso.CLIENTES);
        pasoTarifas.setVisible(actual == Paso.TARIFAS);
        pasoTarifas.setManaged(actual == Paso.TARIFAS);
        pasoResumen.setVisible(actual == Paso.RESUMEN);
        pasoResumen.setManaged(actual == Paso.RESUMEN);

        lblPaso.setText("Paso " + (indice + 1) + " de " + pasos.size() + " · " + titulo(actual));
        lblAyuda.setText(ayuda(actual));
        barraAvance.setProgress((indice + 1.0) / pasos.size());
        btnAtras.setDisable(indice == 0);
        boolean opcional = actual != Paso.EMPRESA && actual != Paso.RESUMEN
                && !(actual == Paso.ADMIN && crearAdmin);
        btnOmitir.setVisible(opcional);
        btnOmitir.setManaged(opcional);
        btnSiguiente.setText(actual == Paso.RESUMEN ? "Finalizar configuración" : "Siguiente");
        if (actual == Paso.TARIFAS) {
            cargarUbicacionesTarifas();
        }
        if (actual == Paso.RESUMEN) {
            actualizarResumen();
        }
    }

    private String titulo(Paso paso) {
        return switch (paso) {
            case EMPRESA -> "Datos de la empresa";
            case ADMIN -> "Cuenta de administrador";
            case UBICACIONES -> "Ubicaciones";
            case UNIDADES -> "Unidades de la flota";
            case OPERADORES -> "Operadores";
            case CLIENTES -> "Clientes";
            case TARIFAS -> "Tarifas por ruta";
            case RESUMEN -> "Resumen y confirmación";
        };
    }

    private String ayuda(Paso paso) {
        return switch (paso) {
            case EMPRESA -> "Con estos datos aparece la empresa en el sistema. El país y la zona horaria definen cómo se registran las horas. La moneda se usa para mostrar los cobros.";
            case ADMIN -> "Esta cuenta maneja el sistema completo. La contraseña se guarda cifrada con BCrypt.";
            case UBICACIONES -> "Vienen cargadas 20 ubicaciones de ejemplo de Centroamérica. Puedes usarlas o agregar las tuyas. Este paso es opcional.";
            case UNIDADES -> "Los camiones con los que va a trabajar la empresa. Quedan disponibles para asignar viajes.";
            case OPERADORES -> "Los choferes y sus reglas de descanso. Cada uno recibe su cuenta con la cédula como usuario y la contraseña "
                    + CLAVE_TEMPORAL + ".";
            case CLIENTES -> "Las empresas que contratan fletes. Puedes cargar las primeras ahora y completar el resto después.";
            case TARIFAS -> "El tiempo y el precio de las rutas que más usas. Al crear un viaje se proponen solos, y siempre puedes cambiarlos.";
            case RESUMEN -> "Revisa lo que se va a guardar. Si algo no cuadra, vuelve atrás.";
        };
    }

    // ------------------------------------------------------------------ empresa y admin

    private Empresa construirEmpresa() {
        Empresa nueva = new Empresa();
        nueva.setNombre(requerido(txtNombre, "El nombre de la empresa", 150));
        nueva.setRuc(opcional(txtRuc, "El RUC", 20));
        nueva.setTelefono(opcional(txtTelefono, "El teléfono", 30));
        nueva.setEmail(opcional(txtEmail, "El correo", 120));
        nueva.setDireccion(opcional(txtDireccion, "La dirección", 200));
        if (nueva.getEmail() != null && !nueva.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("El correo de la empresa no tiene un formato válido.");
        }
        String pais = cmbEmpresaPais.getValue();
        if (pais == null || pais.isBlank()) {
            throw new IllegalArgumentException("Seleccione el país de la empresa.");
        }
        String zona = cmbZona.getValue();
        if (zona == null || zona.isBlank()) {
            throw new IllegalArgumentException("Seleccione la zona horaria de la empresa.");
        }
        String moneda = cmbMoneda.getValue();
        if (moneda == null || moneda.length() < 3) {
            throw new IllegalArgumentException("Seleccione la moneda de la empresa.");
        }
        nueva.setPais(pais);
        nueva.setZonaHoraria(zona.trim());
        nueva.setMoneda(moneda.substring(0, 3));
        return nueva;
    }

    private String monedaPorDefecto() {
        return Listas.monedas().stream()
                .filter(m -> m.startsWith("NIO"))
                .findFirst()
                .orElse(Listas.monedas().get(0));
    }

    private void sugerirZona(String pais) {
        String zona = ZONA_POR_PAIS.get(pais);
        if (zona != null) {
            cmbZona.setValue(zona);
        }
    }

    private Usuario construirAdmin() {
        String username = requerido(txtUsuario, "El usuario administrador", 50);
        if (username.contains(" ")) {
            throw new IllegalArgumentException("El usuario no puede contener espacios.");
        }
        if (usuarioDAO.buscarPorUsername(username) != null) {
            throw new IllegalArgumentException("Ese usuario ya existe; elija otro nombre.");
        }
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();
        String confirmar = txtConfirmar.getText() == null ? "" : txtConfirmar.getText();
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (!password.equals(confirmar)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        nuevo.setRol(RolUsuario.ADMIN);
        nuevo.setEsPrincipal(true);
        nuevo.setPuedeVerReportes(true);
        return nuevo;
    }

    // ------------------------------------------------------------------ ubicaciones

    @FXML
    private void agregarUbicacion() {
        lblMensaje.setText("");
        try {
            String nombre = requerido(txtUbicNombre, "El nombre de la ubicación", 120);
            String departamento = requerido(txtUbicDepartamento, "El departamento", 80);
            String pais = cmbUbicPais.getValue();
            if (pais == null || pais.isBlank()) {
                throw new IllegalArgumentException("Seleccione el país de la ubicación.");
            }
            Double latitud = coordenada(txtUbicLat, "La latitud", -90, 90);
            Double longitud = coordenada(txtUbicLon, "La longitud", -180, 180);
            if ((latitud == null) != (longitud == null)) {
                throw new IllegalArgumentException("Ingrese latitud y longitud juntas, o deje ambas vacías.");
            }
            boolean repetida = nuevasUbicaciones.stream().anyMatch(u ->
                    u.getNombre().equalsIgnoreCase(nombre)
                            && u.getDepartamento().equalsIgnoreCase(departamento)
                            && u.getPais().equalsIgnoreCase(pais));
            if (repetida || ubicacionDAO.buscarPorNombreDepartamentoYPais(nombre, departamento, pais) != null) {
                throw new IllegalArgumentException("Ya existe una ubicación con ese nombre, departamento y país.");
            }
            nuevasUbicaciones.add(new Ubicacion(nombre, departamento, pais, latitud, longitud));
            actualizarListas();
            txtUbicNombre.clear();
            txtUbicDepartamento.clear();
            txtUbicLat.clear();
            txtUbicLon.clear();
            lblMensaje.setText("Ubicación agregada.");
        } catch (IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo verificar la ubicación", e);
        }
    }

    @FXML
    private void quitarUbicacion() {
        int seleccion = listaUbicaciones.getSelectionModel().getSelectedIndex();
        if (seleccion < 0 || seleccion >= nuevasUbicaciones.size()) {
            return;
        }
        Ubicacion ubicacion = nuevasUbicaciones.get(seleccion);
        boolean usadaEnTarifa = nuevasRutas.stream().anyMatch(r ->
                mismaUbicacion(r.getOrigen(), ubicacion) || mismaUbicacion(r.getDestino(), ubicacion));
        if (usadaEnTarifa) {
            lblMensaje.setText("Esa ubicación tiene una tarifa asociada. Quita la tarifa primero.");
            return;
        }
        nuevasUbicaciones.remove(seleccion);
        actualizarListas();
    }

    // ------------------------------------------------------------------ unidades

    @FXML
    private void agregarUnidad() {
        lblMensaje.setText("");
        try {
            String numero = requerido(txtUnidadNumero, "El número de unidad", 20);
            String placas = requerido(txtUnidadPlacas, "Las placas", 15);
            String modelo = requerido(txtUnidadModelo, "El modelo", 80);
            BigDecimal capacidad = null;
            String textoCapacidad = txtUnidadCapacidad.getText() == null ? "" : txtUnidadCapacidad.getText().trim();
            if (!textoCapacidad.isEmpty()) {
                capacidad = decimal(textoCapacidad, "La capacidad");
                if (capacidad.signum() <= 0) {
                    throw new IllegalArgumentException("La capacidad debe ser mayor que 0.");
                }
            }
            boolean repetida = nuevasUnidades.stream().anyMatch(u ->
                    u.getNumeroUnidad().equalsIgnoreCase(numero) || u.getPlacas().equalsIgnoreCase(placas));
            if (repetida || unidadDAO.existeNumero(numero) || unidadDAO.existePlacas(placas)) {
                throw new IllegalArgumentException("Ya existe una unidad con ese número o esas placas.");
            }
            Unidad unidad = new Unidad();
            unidad.setNumeroUnidad(numero);
            unidad.setPlacas(placas);
            unidad.setModelo(modelo);
            unidad.setCapacidadToneladas(capacidad);
            unidad.setSeguroVencimiento(dpSeguro.getValue());
            unidad.setRevisionVencimiento(dpRevision.getValue());
            nuevasUnidades.add(unidad);
            actualizarListas();
            txtUnidadNumero.clear();
            txtUnidadPlacas.clear();
            txtUnidadModelo.clear();
            txtUnidadCapacidad.clear();
            dpSeguro.setValue(null);
            dpRevision.setValue(null);
            lblMensaje.setText("Unidad agregada.");
        } catch (IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo verificar la unidad", e);
        }
    }

    @FXML
    private void quitarUnidad() {
        int seleccion = listaUnidades.getSelectionModel().getSelectedIndex();
        if (seleccion >= 0 && seleccion < nuevasUnidades.size()) {
            nuevasUnidades.remove(seleccion);
            actualizarListas();
        }
    }

    // ------------------------------------------------------------------ operadores

    @FXML
    private void agregarOperador() {
        lblMensaje.setText("");
        try {
            String cedula = requerido(txtOpCedula, "La cédula", 20);
            if (cedula.contains(" ")) {
                throw new IllegalArgumentException("La cédula no puede contener espacios.");
            }
            String nombre = requerido(txtOpNombre, "El nombre", 150);
            String telefono = opcional(txtOpTelefono, "El teléfono", 30);
            BigDecimal horasMax = decimal(requerido(txtOpHorasMax, "Las horas máximas de conducción", 5),
                    "Las horas máximas");
            BigDecimal horasMin = decimal(requerido(txtOpHorasMin, "Las horas mínimas de descanso", 5),
                    "Las horas mínimas");
            if (horasMax.signum() <= 0 || horasMax.compareTo(new BigDecimal("24")) > 0) {
                throw new IllegalArgumentException("Las horas máximas deben ser mayores a 0 y máximo 24.");
            }
            if (horasMin.signum() <= 0 || horasMin.compareTo(new BigDecimal("24")) > 0) {
                throw new IllegalArgumentException("Las horas mínimas deben ser mayores a 0 y máximo 24.");
            }
            boolean repetido = nuevosOperadores.stream().anyMatch(o -> o.getCedula().equalsIgnoreCase(cedula));
            if (repetido || operadorDAO.existeCedula(cedula, null) || usuarioDAO.existeUsername(cedula)) {
                throw new IllegalArgumentException("Ya existe un operador o una cuenta con esa cédula.");
            }
            Operador operador = new Operador();
            operador.setCedula(cedula);
            operador.setNombre(nombre);
            operador.setTelefono(telefono);
            operador.setLicenciaNumero(opcional(txtLicenciaNumero, "La licencia", 30));
            operador.setLicenciaCategoria(opcional(txtLicenciaCategoria, "La categoría de la licencia", 10));
            operador.setLicenciaVencimiento(dpLicenciaVencimiento.getValue());
            operador.setContactoEmergencia(opcional(txtContactoEmergencia, "El contacto de emergencia", 120));
            operador.setHorasMaxConduccion(horasMax);
            operador.setHorasMinDescanso(horasMin);
            nuevosOperadores.add(operador);
            actualizarListas();
            txtOpCedula.clear();
            txtOpNombre.clear();
            txtOpTelefono.clear();
            txtLicenciaNumero.clear();
            txtLicenciaCategoria.clear();
            dpLicenciaVencimiento.setValue(null);
            txtContactoEmergencia.clear();
            txtOpHorasMax.setText("8.00");
            txtOpHorasMin.setText("8.00");
            lblMensaje.setText("Operador agregado.");
        } catch (IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo verificar el operador", e);
        }
    }

    @FXML
    private void quitarOperador() {
        int seleccion = listaOperadores.getSelectionModel().getSelectedIndex();
        if (seleccion >= 0 && seleccion < nuevosOperadores.size()) {
            nuevosOperadores.remove(seleccion);
            actualizarListas();
        }
    }

    // ------------------------------------------------------------------ clientes

    @FXML
    private void agregarCliente() {
        lblMensaje.setText("");
        try {
            String nombre = requerido(txtClienteNombre, "El nombre del cliente", 150);
            String contacto = opcional(txtClienteContacto, "El contacto", 120);
            String telefono = opcional(txtClienteTelefono, "El teléfono", 30);
            String email = opcional(txtClienteEmail, "El correo", 120);
            String direccion = opcional(txtClienteDireccion, "La dirección", 200);
            if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException("El correo del cliente no tiene un formato válido.");
            }
            boolean repetido = nuevosClientes.stream()
                    .anyMatch(c -> c.getNombreEmpresa().equalsIgnoreCase(nombre));
            if (repetido || clienteDAO.existeNombreEmpresa(nombre)) {
                throw new IllegalArgumentException("Ya existe un cliente con ese nombre.");
            }
            Cliente cliente = new Cliente(nombre);
            cliente.setContacto(contacto);
            cliente.setTelefono(telefono);
            cliente.setEmail(email);
            cliente.setDireccion(direccion);
            cliente.setActivo(true);
            nuevosClientes.add(cliente);
            actualizarListas();
            txtClienteNombre.clear();
            txtClienteContacto.clear();
            txtClienteTelefono.clear();
            txtClienteEmail.clear();
            txtClienteDireccion.clear();
            lblMensaje.setText("Cliente agregado.");
        } catch (IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo verificar el cliente", e);
        }
    }

    @FXML
    private void quitarCliente() {
        int seleccion = listaClientes.getSelectionModel().getSelectedIndex();
        if (seleccion >= 0 && seleccion < nuevosClientes.size()) {
            nuevosClientes.remove(seleccion);
            actualizarListas();
        }
    }

    // ------------------------------------------------------------------ tarifas

    private void cargarUbicacionesTarifas() {
        try {
            List<Ubicacion> opciones = new ArrayList<>(ubicacionDAO.listar());
            for (Ubicacion nueva : nuevasUbicaciones) {
                boolean existe = opciones.stream().anyMatch(u -> u.getNombre().equalsIgnoreCase(nueva.getNombre())
                        && u.getDepartamento().equalsIgnoreCase(nueva.getDepartamento())
                        && u.getPais().equalsIgnoreCase(nueva.getPais()));
                if (!existe) {
                    opciones.add(nueva);
                }
            }
            ComboBuscador.configurar(cmbTarifaOrigen, opciones);
            ComboBuscador.configurar(cmbTarifaDestino, opciones);
        } catch (PersistenciaException e) {
            lblMensaje.setText("No se pudieron cargar las ubicaciones.");
        }
    }

    @FXML
    private void agregarTarifa() {
        lblMensaje.setText("");
        try {
            Ubicacion origen = cmbTarifaOrigen.getValue();
            Ubicacion destino = cmbTarifaDestino.getValue();
            if (origen == null || destino == null) {
                throw new IllegalArgumentException("Selecciona el origen y el destino de la ruta.");
            }
            if (origen.getId() != null && origen.getId().equals(destino.getId())) {
                throw new IllegalArgumentException("El origen y el destino deben ser distintos.");
            }
            if (origen.getId() == null && destino.getId() == null
                    && origen.getNombre().equalsIgnoreCase(destino.getNombre())
                    && origen.getDepartamento().equalsIgnoreCase(destino.getDepartamento())) {
                throw new IllegalArgumentException("El origen y el destino deben ser distintos.");
            }
            BigDecimal horas = decimal(requerido(txtTarifaHoras, "Las horas", 6), "Las horas");
            if (horas.signum() <= 0 || horas.compareTo(new BigDecimal("999")) > 0) {
                throw new IllegalArgumentException("Las horas deben ser mayores a 0 y menores a 999.");
            }
            BigDecimal precio = decimal(requerido(txtTarifaPrecio, "El precio", 12), "El precio");
            if (precio.signum() <= 0) {
                throw new IllegalArgumentException("El precio debe ser mayor que 0.");
            }
            boolean repetida = nuevasRutas.stream().anyMatch(r ->
                    mismaUbicacion(r.getOrigen(), origen) && mismaUbicacion(r.getDestino(), destino));
            if (repetida || buscarRutaGuardada(origen, destino).isPresent()) {
                throw new IllegalArgumentException("Ya hay una tarifa para ese origen y destino.");
            }
            nuevasRutas.add(new Ruta(origen, destino, horas, precio));
            actualizarListas();
            txtTarifaHoras.clear();
            txtTarifaPrecio.clear();
            cmbTarifaOrigen.setValue(null);
            cmbTarifaDestino.setValue(null);
            lblMensaje.setText("Tarifa agregada.");
        } catch (IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
        } catch (PersistenciaException e) {
            error("No se pudo verificar la tarifa", e);
        }
    }

    @FXML
    private void quitarTarifa() {
        int seleccion = listaTarifas.getSelectionModel().getSelectedIndex();
        if (seleccion >= 0 && seleccion < nuevasRutas.size()) {
            nuevasRutas.remove(seleccion);
            actualizarListas();
        }
    }

    private void sugerirTarifa() {
        Ubicacion origen = cmbTarifaOrigen.getValue();
        Ubicacion destino = cmbTarifaDestino.getValue();
        if (origen == null || destino == null) {
            return;
        }
        try {
            Optional<Ruta> guardada = buscarRutaGuardada(origen, destino);
            if (guardada.isPresent()) {
                txtTarifaHoras.setText(guardada.get().getTiempoEstimadoHoras().stripTrailingZeros().toPlainString());
                if (guardada.get().getPrecio() != null) {
                    txtTarifaPrecio.setText(guardada.get().getPrecio().stripTrailingZeros().toPlainString());
                }
                return;
            }
            nuevasRutas.stream()
                    .filter(r -> mismaUbicacion(r.getOrigen(), origen) && mismaUbicacion(r.getDestino(), destino))
                    .findFirst()
                    .ifPresent(r -> {
                        txtTarifaHoras.setText(r.getTiempoEstimadoHoras().stripTrailingZeros().toPlainString());
                        txtTarifaPrecio.setText(r.getPrecio().stripTrailingZeros().toPlainString());
                    });
            if (txtTarifaHoras.getText() == null || txtTarifaHoras.getText().isBlank()) {
                if (GeoUtil.tieneCoordenadas(origen) && GeoUtil.tieneCoordenadas(destino)) {
                    txtTarifaHoras.setText(GeoUtil.horasEstimadas(origen, destino).stripTrailingZeros().toPlainString());
                }
            }
        } catch (PersistenciaException e) {
            // Sin conexión momentánea: se deja escribir las horas a mano.
        }
    }

    private Optional<Ruta> buscarRutaGuardada(Ubicacion origen, Ubicacion destino) {
        if (origen.getId() == null || destino.getId() == null) {
            return Optional.empty();
        }
        return rutaDAO.buscar(origen, destino);
    }

    private boolean mismaUbicacion(Ubicacion a, Ubicacion b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a.getNombre().equalsIgnoreCase(b.getNombre())
                && a.getDepartamento().equalsIgnoreCase(b.getDepartamento());
    }

    // ------------------------------------------------------------------ listas y resumen

    private void actualizarListas() {
        listaUbicaciones.getItems().setAll(nuevasUbicaciones.stream().map(u -> {
            String coordenadas = u.getLatitud() == null ? "" : String.format(Locale.ROOT,
                    "  ·  %.4f, %.4f", u.getLatitud(), u.getLongitud());
            return u.getNombre() + " · " + u.getDepartamento() + " · " + u.getPais() + coordenadas;
        }).toList());
        listaUnidades.getItems().setAll(nuevasUnidades.stream().map(u -> {
            StringBuilder linea = new StringBuilder(u.getNumeroUnidad() + " · " + u.getPlacas() + " · " + u.getModelo());
            if (u.getCapacidadToneladas() != null) {
                linea.append(" · ").append(u.getCapacidadToneladas().toPlainString()).append(" t");
            }
            if (u.getSeguroVencimiento() != null) {
                linea.append(" · seguro hasta ").append(u.getSeguroVencimiento());
            }
            if (u.getRevisionVencimiento() != null) {
                linea.append(" · revisión hasta ").append(u.getRevisionVencimiento());
            }
            return linea.toString();
        }).toList());
        listaOperadores.getItems().setAll(nuevosOperadores.stream().map(o -> {
            StringBuilder linea = new StringBuilder(o.getCedula() + " · " + o.getNombre()
                    + " · " + o.getHorasMaxConduccion().toPlainString() + " h manejo"
                    + " / " + o.getHorasMinDescanso().toPlainString() + " h descanso");
            if (o.getLicenciaNumero() != null) {
                linea.append(" · licencia ").append(o.getLicenciaNumero());
                if (o.getLicenciaCategoria() != null) {
                    linea.append(" (").append(o.getLicenciaCategoria()).append(")");
                }
                if (o.getLicenciaVencimiento() != null) {
                    linea.append(" vence ").append(o.getLicenciaVencimiento());
                }
            }
            return linea.toString();
        }).toList());
        listaClientes.getItems().setAll(nuevosClientes.stream().map(c ->
                c.getNombreEmpresa() + (c.getContacto() == null ? "" : " · " + c.getContacto())
                        + (c.getTelefono() == null ? "" : " · " + c.getTelefono())
        ).toList());
        listaTarifas.getItems().setAll(nuevasRutas.stream().map(r ->
                r.getOrigen().getNombre() + " → " + r.getDestino().getNombre()
                        + " · " + r.getTiempoEstimadoHoras().stripTrailingZeros().toPlainString() + " h"
                        + " · " + r.getPrecio().toPlainString()
        ).toList());
        btnQuitarUbicacion.setDisable(nuevasUbicaciones.isEmpty());
        btnQuitarUnidad.setDisable(nuevasUnidades.isEmpty());
        btnQuitarOperador.setDisable(nuevosOperadores.isEmpty());
        btnQuitarCliente.setDisable(nuevosClientes.isEmpty());
        btnQuitarTarifa.setDisable(nuevasRutas.isEmpty());
    }

    private void actualizarResumen() {
        StringBuilder texto = new StringBuilder();
        texto.append("Empresa: ").append(empresa == null ? "—" : empresa.getNombre())
                .append(" (").append(empresa == null ? "—" : empresa.getMoneda()).append(")\n");
        texto.append("Administrador: ")
                .append(admin == null ? "se conserva el existente" : admin.getUsername())
                .append("\n");
        texto.append("Ubicaciones nuevas: ").append(nuevasUbicaciones.size()).append("\n");
        texto.append("Unidades nuevas: ").append(nuevasUnidades.size()).append("\n");
        texto.append("Operadores nuevos: ").append(nuevosOperadores.size()).append("\n");
        texto.append("Clientes nuevos: ").append(nuevosClientes.size()).append("\n");
        texto.append("Tarifas de ruta: ").append(nuevasRutas.size()).append("\n");
        if (!nuevosOperadores.isEmpty()) {
            texto.append("\nCada operador recibe su cuenta de acceso con la contraseña temporal ")
                    .append(CLAVE_TEMPORAL).append(".\n");
        }
        texto.append("\nAntes de empezar a operar, guarda un respaldo de la base de datos (pg_dump).");
        lblResumen.setText(texto.toString());
    }

    // ------------------------------------------------------------------ guardado

    private void guardarTodo() {
        if (empresa == null) {
            lblMensaje.setText("Faltan los datos de la empresa: vuelve al primer paso.");
            return;
        }
        try {
            Transaccion.ejecutar(sesion -> {
                sesion.persist(empresa);
                if (admin != null) {
                    sesion.persist(admin);
                }
                EstadoUnidad estadoUnidad = sesion.createQuery(
                                "from EstadoUnidad e where e.nombre = :n", EstadoUnidad.class)
                        .setParameter("n", "DISPONIBLE").getSingleResult();
                EstadoOperador estadoOperador = sesion.createQuery(
                                "from EstadoOperador e where e.nombre = :n", EstadoOperador.class)
                        .setParameter("n", "DISPONIBLE").getSingleResult();
                for (Ubicacion ubicacion : nuevasUbicaciones) {
                    sesion.persist(ubicacion);
                }
                for (Unidad unidad : nuevasUnidades) {
                    unidad.setEstado(estadoUnidad);
                    sesion.persist(unidad);
                }
                for (Operador operador : nuevosOperadores) {
                    operador.setEstado(estadoOperador);
                    operador.setFechaIngreso(LocalDate.now());
                    sesion.persist(operador);
                    Usuario cuenta = new Usuario();
                    cuenta.setUsername(operador.getCedula());
                    cuenta.setPasswordHash(BCrypt.hashpw(CLAVE_TEMPORAL, BCrypt.gensalt()));
                    cuenta.setRol(RolUsuario.OPERADOR);
                    cuenta.setOperador(operador);
                    cuenta.setActivo(true);
                    cuenta.setEsPrincipal(false);
                    cuenta.setPuedeVerReportes(false);
                    sesion.persist(cuenta);
                }
                for (Cliente cliente : nuevosClientes) {
                    sesion.persist(cliente);
                }
                for (Ruta ruta : nuevasRutas) {
                    sesion.persist(ruta);
                }
                return null;
            });
            edu.ni.uam.proyecto_final_pae.config.ZonaHoraria.recargar();
            Navegacion.abrirLogin(Navegacion.stageDe(btnSiguiente),
                    "Configuración guardada. Inicie sesión con su cuenta.");
        } catch (PersistenciaException e) {
            error("No se pudo guardar la configuración", e);
        }
    }

    // ------------------------------------------------------------------ apoyo

    private String requerido(TextField campo, String nombre, int maximo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Complete " + nombre + ".");
        }
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(nombre + " no puede pasar de " + maximo + " caracteres.");
        }
        return valor;
    }

    private String opcional(TextField campo, String nombre, int maximo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(nombre + " no puede pasar de " + maximo + " caracteres.");
        }
        return valor.isEmpty() ? null : valor;
    }

    private BigDecimal decimal(String valor, String nombre) {
        try {
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nombre + " debe ser un número, por ejemplo 8.50.");
        }
    }

    private Double coordenada(TextField campo, String nombre, double minimo, double maximo) {
        String valor = campo.getText() == null ? "" : campo.getText().trim();
        if (valor.isEmpty()) {
            return null;
        }
        try {
            double numero = Double.parseDouble(valor.replace(',', '.'));
            if (numero < minimo || numero > maximo) {
                throw new IllegalArgumentException(nombre + " está fuera del rango permitido (" + minimo + " a " + maximo + ").");
            }
            return numero;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(nombre + " debe ser un número decimal, por ejemplo 12.1364.");
        }
    }

    private void error(String titulo, PersistenciaException e) {
        lblMensaje.setText(titulo);
        Alert alerta = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}