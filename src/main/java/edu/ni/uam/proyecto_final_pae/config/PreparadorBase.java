package edu.ni.uam.proyecto_final_pae.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Crea una base vacía o actualiza el esquema existente sin borrar datos. */
public final class PreparadorBase {

    /** Datos de conexion del asistente. */
    public record Conexion(String host, int puerto, String usuario, String password, String base) {
    }

    /** Resultado de la revision del servidor y la base. */
    public record Estado(boolean servidorOk, boolean baseExiste, int tablas, String detalle) {
    }

    public static final int TABLAS_ESPERADAS = 17;

    private static final String TABLAS = "'cat_estado_unidad', 'cat_estado_operador', 'cat_estado_carga', "
            + "'cat_estado_viaje', 'cat_tipo_evento', 'empresa', 'clientes', 'ubicaciones', 'rutas', "
            + "'unidades', 'operadores', 'usuarios', 'cargas', 'viajes', 'registro_jornada', "
            + "'paises_habilitados', 'alertas_operador'";
    private static final String TABLAS_BASE = "'cat_estado_unidad', 'cat_estado_operador', 'cat_estado_carga', "
            + "'cat_estado_viaje', 'cat_tipo_evento', 'empresa', 'clientes', 'ubicaciones', "
            + "'unidades', 'operadores', 'usuarios', 'cargas', 'viajes', 'registro_jornada'";
    private static final int CANTIDAD_TABLAS_BASE = 14;
    private static final Map<String, List<String>> COLUMNAS_ACTUALES = Map.of(
            "usuarios", List.of("es_principal", "puede_ver_reportes"),
            "ubicaciones", List.of("pais", "latitud", "longitud", "categoria", "color"),
            "empresa", List.of("moneda", "pais", "zona_horaria"),
            "clientes", List.of("direccion", "activo"),
            "rutas", List.of("precio"));

    private static final Pattern PATRON_URL = Pattern.compile("jdbc:postgresql://([^/:?]+):(\\d+)/([^?;\\s]+)");
    private static final Pattern NOMBRE_BASE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");

    private PreparadorBase() {
    }

    /** Conexion guardada en .env, o los valores por defecto si todavia no hay archivo. */
    public static Conexion actual() {
        String host = "localhost";
        int puerto = 5432;
        String base = "fleetroute_pro";
        String url = Entorno.opcional("DB_URL");
        if (url != null) {
            Matcher coincidencia = PATRON_URL.matcher(url);
            if (coincidencia.find()) {
                host = coincidencia.group(1);
                puerto = Integer.parseInt(coincidencia.group(2));
                base = coincidencia.group(3);
            }
        }
        String usuario = Entorno.opcional("DB_USER");
        String password = Entorno.opcional("DB_PASSWORD");
        return new Conexion(host, puerto, usuario == null ? "postgres" : usuario,
                password == null ? "" : password, base);
    }

    /** Revisa el servidor y la base sin modificar nada. */
    public static Estado revisar(Conexion conexion) {
        validar(conexion);
        try (Connection servidor = conectar(conexion, "postgres")) {
            boolean existe = existeBase(servidor, conexion.base());
            if (!existe) {
                return new Estado(true, false, 0,
                        "Conexión correcta. La base " + conexion.base() + " no existe todavía y se creará al continuar.");
            }
            try (Connection base = conectar(conexion, conexion.base())) {
                int tablas = contarTablas(base);
                if (tablas == 0) {
                    return new Estado(true, true, 0,
                            "Conexión correcta. La base " + conexion.base() + " está vacía y se llenará con las tablas al continuar.");
                }
                if (tablas == TABLAS_ESPERADAS && !requiereActualizacion(base)) {
                    return new Estado(true, true, tablas,
                            "Conexión correcta. La base " + conexion.base() + " ya tiene todas las tablas.");
                }
                if (contarTablas(base, TABLAS_BASE) == CANTIDAD_TABLAS_BASE) {
                    return new Estado(true, true, tablas,
                            "Conexión correcta. El esquema se actualizará al continuar, conservando los datos existentes.");
                }
                return new Estado(true, true, tablas,
                        "Conexión correcta, pero la base " + conexion.base() + " tiene " + tablas + " de "
                                + TABLAS_ESPERADAS + " tablas. Use una base nueva o revise el esquema incompleto sin borrar sus datos.");
            }
        } catch (SQLException e) {
            return new Estado(false, false, 0, traducir(e));
        }
    }

    /** Crea la base o el esquema segun falte, guarda .env y deja Hibernate listo. */
    public static String preparar(Conexion conexion, Consumer<String> progreso) {
        validar(conexion);
        boolean creada = false;
        boolean esquemaNuevo = false;
        boolean actualizada = false;
        progreso.accept("Conectando con el servidor PostgreSQL...");
        try (Connection servidor = conectar(conexion, "postgres")) {
            if (!existeBase(servidor, conexion.base())) {
                progreso.accept("Creando la base de datos " + conexion.base() + "...");
                try (Statement sentencia = servidor.createStatement()) {
                    sentencia.execute("CREATE DATABASE \"" + conexion.base() + "\"");
                }
                creada = true;
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException(traducir(e), e);
        }
        try (Connection base = conectar(conexion, conexion.base())) {
            int tablas = contarTablas(base);
            if (tablas > 0 && contarTablas(base, TABLAS_BASE) != CANTIDAD_TABLAS_BASE) {
                throw new IllegalArgumentException("La base " + conexion.base() + " tiene " + tablas + " de "
                        + TABLAS_ESPERADAS + " tablas y no corresponde a un esquema anterior completo. "
                        + "Use una base nueva o revise las tablas faltantes sin borrar sus datos.");
            }
            if (tablas == 0) {
                if (contarTablasPublicas(base) != 0) {
                    throw new IllegalArgumentException("La base contiene tablas de otra aplicación. Use una base vacía para FleetRoute.");
                }
                progreso.accept("Creando tablas, catálogos, índices y triggers...");
                aplicarEsquema(base);
                esquemaNuevo = true;
            } else if (requiereActualizacion(base)) {
                progreso.accept("Actualizando el esquema sin borrar los datos...");
                aplicarScript(base, "/database/actualizar_esquema.sql", false);
                actualizada = true;
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException(traducir(e), e);
        }
        progreso.accept("Guardando la conexión en el archivo .env...");
        Entorno.guardar(Map.of(
                "DB_URL", url(conexion, conexion.base()),
                "DB_USER", conexion.usuario(),
                "DB_PASSWORD", conexion.password()));
        progreso.accept("Preparando los catálogos del sistema...");
        HibernateUtil.cerrar();
        HibernateUtil.factory();
        ZonaHoraria.recargar();
        if (creada) {
            return "Base de datos creada desde cero con sus tablas, catálogos y triggers.";
        }
        if (esquemaNuevo) {
            return "Tablas y catálogos creados dentro de la base que ya existía.";
        }
        if (actualizada) {
            return "Esquema actualizado. Se conservaron los datos y la conexión quedó guardada.";
        }
        return "La base ya estaba completa. La conexión quedó guardada.";
    }

    // ------------------------------------------------------------------ apoyo

    private static void validar(Conexion conexion) {
        if (conexion.host() == null || conexion.host().isBlank()) {
            throw new IllegalArgumentException("Escriba el servidor donde está PostgreSQL, por ejemplo localhost.");
        }
        if (conexion.puerto() < 1 || conexion.puerto() > 65535) {
            throw new IllegalArgumentException("El puerto debe ser un número entre 1 y 65535. Por defecto es 5432.");
        }
        if (conexion.usuario() == null || conexion.usuario().isBlank()) {
            throw new IllegalArgumentException("Escriba el usuario de PostgreSQL, por defecto postgres.");
        }
        if (conexion.base() == null || !NOMBRE_BASE.matcher(conexion.base()).matches()) {
            throw new IllegalArgumentException("El nombre de la base solo puede llevar letras, números y guiones bajos, "
                    + "y empezar con letra o guion bajo. Por ejemplo fleetroute_pro.");
        }
    }

    private static String url(Conexion conexion, String base) {
        return "jdbc:postgresql://" + conexion.host() + ":" + conexion.puerto() + "/" + base;
    }

    private static Connection conectar(Conexion conexion, String base) throws SQLException {
        Properties propiedades = new Properties();
        propiedades.setProperty("user", conexion.usuario());
        propiedades.setProperty("password", conexion.password());
        propiedades.setProperty("loginTimeout", "5");
        propiedades.setProperty("connectTimeout", "5");
        return DriverManager.getConnection(url(conexion, base), propiedades);
    }

    private static boolean existeBase(Connection servidor, String nombre) throws SQLException {
        try (var consulta = servidor.prepareStatement("select 1 from pg_database where datname = ?")) {
            consulta.setString(1, nombre);
            try (ResultSet filas = consulta.executeQuery()) {
                return filas.next();
            }
        }
    }

    private static int contarTablas(Connection base) throws SQLException {
        return contarTablas(base, TABLAS);
    }

    private static int contarTablas(Connection base, String nombres) throws SQLException {
        try (Statement sentencia = base.createStatement();
             ResultSet filas = sentencia.executeQuery(
                     "select count(*) from information_schema.tables where table_schema = 'public' "
                             + "and table_type = 'BASE TABLE' and table_name in (" + nombres + ")")) {
            filas.next();
            return filas.getInt(1);
        }
    }

    private static int contarTablasPublicas(Connection base) throws SQLException {
        try (Statement sentencia = base.createStatement(); ResultSet filas = sentencia.executeQuery(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_type = 'BASE TABLE'")) {
            filas.next();
            return filas.getInt(1);
        }
    }

    private static boolean requiereActualizacion(Connection base) throws SQLException {
        if (contarTablas(base) != TABLAS_ESPERADAS) return true;
        for (var tabla : COLUMNAS_ACTUALES.entrySet()) {
            List<String> columnas = new ArrayList<>();
            try (ResultSet filas = base.getMetaData().getColumns(null, "public", tabla.getKey(), "%")) {
                while (filas.next()) columnas.add(filas.getString("COLUMN_NAME"));
            }
            if (!columnas.containsAll(tabla.getValue())) return true;
        }
        try (Statement sentencia = base.createStatement(); ResultSet filas = sentencia.executeQuery(
                "SELECT NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'public.ubicaciones'::regclass "
                        + "AND conname = 'uq_ubicacion' AND contype = 'u' AND pg_get_constraintdef(oid) LIKE '%pais%')")) {
            filas.next();
            return filas.getBoolean(1);
        }
    }

    /** Solo migra una base reconocible del proyecto; Hibernate valida el resto del esquema. */
    static void actualizarEsquemaExistente(String jdbcUrl, String usuario, String password) throws SQLException {
        Properties propiedades = new Properties();
        propiedades.setProperty("user", usuario);
        propiedades.setProperty("password", password);
        propiedades.setProperty("connectTimeout", "5");
        try (Connection base = DriverManager.getConnection(jdbcUrl, propiedades)) {
            if (contarTablas(base, TABLAS_BASE) == CANTIDAD_TABLAS_BASE && requiereActualizacion(base)) {
                aplicarScript(base, "/database/actualizar_esquema.sql", false);
            }
        }
    }

    /** Ejecuta el script incluido en la aplicacion, sin los datos de ejemplo ni las cuentas demo. */
    private static void aplicarEsquema(Connection base) throws SQLException {
        aplicarScript(base, "/database/fleetroute_pro.sql", true);
    }

    private static void aplicarScript(Connection base, String recurso, boolean omitirDemo) throws SQLException {
        String script;
        try (InputStream archivo = PreparadorBase.class.getResourceAsStream(recurso)) {
            if (archivo == null) {
                throw new IllegalStateException("No se encontró el script de esquema dentro de la aplicación.");
            }
            script = new String(archivo.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el script de esquema incluido en la aplicación.", e);
        }
        boolean autoCommit = base.getAutoCommit();
        base.setAutoCommit(false);
        try (Statement sentencia = base.createStatement()) {
            if (!omitirDemo) sentencia.execute("SELECT pg_advisory_xact_lock(7675489721)");
            for (String parte : sentencias(script)) {
                String limpia = parte.strip();
                if (limpia.equalsIgnoreCase("begin") || limpia.equalsIgnoreCase("commit")
                        || (omitirDemo && esSemillaDemo(limpia))) {
                    continue;
                }
                sentencia.execute(limpia);
            }
            base.commit();
        } catch (SQLException e) {
            base.rollback();
            throw e;
        } finally {
            base.setAutoCommit(autoCommit);
        }
    }

    /** Los maestros y las cuentas los crea el asistente, no el script. */
    private static boolean esSemillaDemo(String sentencia) {
        String baja = sentencia.toLowerCase(Locale.ROOT);
        return baja.startsWith("insert into ubicaciones")
                || baja.startsWith("insert into clientes")
                || baja.startsWith("insert into unidades")
                || baja.startsWith("insert into operadores")
                || baja.startsWith("insert into usuarios");
    }

    /** Corta el script por sentencias respetando comentarios, comillas y bloques $$ de los triggers. */
    private static List<String> sentencias(String sql) {
        List<String> salida = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char caracter = sql.charAt(i);
            if (caracter == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                int fin = sql.indexOf('\n', i);
                i = fin < 0 ? n : fin + 1;
                continue;
            }
            if (caracter == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int fin = sql.indexOf("*/", i + 2);
                i = fin < 0 ? n : fin + 2;
                continue;
            }
            if (caracter == '\'') {
                int j = i + 1;
                while (j < n) {
                    if (sql.charAt(j) == '\'') {
                        if (j + 1 < n && sql.charAt(j + 1) == '\'') {
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    j++;
                }
                int fin = Math.min(j + 1, n);
                actual.append(sql, i, fin);
                i = fin;
                continue;
            }
            if (caracter == '$') {
                int finEtiqueta = sql.indexOf('$', i + 1);
                if (finEtiqueta > i) {
                    String etiqueta = sql.substring(i, finEtiqueta + 1);
                    if (etiqueta.matches("\\$[A-Za-z0-9_]*\\$")) {
                        int cierre = sql.indexOf(etiqueta, finEtiqueta + 1);
                        if (cierre >= 0) {
                            int fin = cierre + etiqueta.length();
                            actual.append(sql, i, fin);
                            i = fin;
                            continue;
                        }
                    }
                }
            }
            if (caracter == ';') {
                String parte = actual.toString().strip();
                if (!parte.isEmpty()) {
                    salida.add(parte);
                }
                actual.setLength(0);
                i++;
                continue;
            }
            actual.append(caracter);
            i++;
        }
        String resto = actual.toString().strip();
        if (!resto.isEmpty()) {
            salida.add(resto);
        }
        return salida;
    }

    /** Traduce errores de PostgreSQL a mensajes claros. */
    private static String traducir(SQLException e) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String texto = e.getMessage() == null ? "" : e.getMessage();
        if ("28P01".equals(estado) || texto.contains("password authentication failed")) {
            return "El usuario o la contraseña no coinciden con los de PostgreSQL. Escriba la contraseña que eligió al instalarlo.";
        }
        if ("28000".equals(estado)) {
            return "PostgreSQL rechazó la conexión de este usuario. Revise el archivo pg_hba.conf o conéctese desde localhost.";
        }
        if ("08001".equals(estado) || texto.contains("Connection refused") || texto.contains("connect timed out")
                || texto.contains("Connection timed out") || texto.contains("UnknownHostException")) {
            return "No se pudo conectar con el servidor. Revise que PostgreSQL esté iniciado y que el servidor y el puerto sean correctos (5432 por defecto).";
        }
        if ("42501".equals(estado)) {
            return "El usuario no tiene permisos para crear la base o sus objetos. Use el usuario postgres o uno con permisos de creación.";
        }
        if ("3D000".equals(estado)) {
            return "La base indicada no existe en el servidor. Revise el nombre o deje que el sistema la cree.";
        }
        return "PostgreSQL respondió: " + texto;
    }
}
