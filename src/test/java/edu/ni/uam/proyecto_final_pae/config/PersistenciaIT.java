package edu.ni.uam.proyecto_final_pae.config;

import edu.ni.uam.proyecto_final_pae.dao.UbicacionDAO;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Cada prueba crea y elimina exclusivamente su propia base temporal. */
class PersistenciaIT {
    @TempDir Path carpeta;
    private String directorioOriginal, adminUrl, usuario, password, nombre, url;
    private PreparadorBase.Conexion conexion;
    private boolean creada;

    @BeforeEach void preparar() throws Exception {
        adminUrl = System.getProperty("fleetroute.it.admin-url", "");
        assertFalse(adminUrl.isBlank(), "Configure fleetroute.it.admin-url/user/password para probar PostgreSQL.");
        usuario = System.getProperty("fleetroute.it.user", "postgres");
        password = System.getProperty("fleetroute.it.password", System.getenv().getOrDefault("FLEETROUTE_IT_PASSWORD", ""));
        URI servidor = URI.create(adminUrl.substring("jdbc:".length()));
        assertTrue(servidor.getHost().equals("127.0.0.1") || servidor.getHost().equals("localhost"),
                "Estas pruebas solo usan una instancia local de PostgreSQL.");
        nombre = "fleetroute_it_" + UUID.randomUUID().toString().replace("-", "");
        url = "jdbc:postgresql://" + servidor.getHost() + ":" + servidor.getPort() + "/" + nombre;
        conexion = new PreparadorBase.Conexion(servidor.getHost(), servidor.getPort(), usuario, password, nombre);
        try (Connection c = admin(); Statement s = c.createStatement()) { s.execute("CREATE DATABASE \"" + nombre + "\""); }
        creada = true;
        directorioOriginal = System.getProperty("user.dir");
        Files.writeString(carpeta.resolve(".env"), "DB_URL=" + url + "\nDB_USER=" + usuario + "\nDB_PASSWORD=" + password + "\n");
        System.setProperty("user.dir", carpeta.toString());
        Entorno.recargar();
    }

    @AfterEach void limpiar() throws Exception {
        HibernateUtil.cerrar();
        if (directorioOriginal != null) System.setProperty("user.dir", directorioOriginal);
        if (creada && nombre.startsWith("fleetroute_it_") && nombre.matches("fleetroute_it_[0-9a-f]{32}")) {
            try (Connection c = admin()) {
                try (PreparedStatement s = c.prepareStatement("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname=?")) {
                    s.setString(1, nombre); s.execute();
                }
                try (Statement s = c.createStatement()) { s.execute("DROP DATABASE \"" + nombre + "\""); }
            }
        }
    }

    private Connection admin() throws SQLException { return DriverManager.getConnection(adminUrl, usuario, password); }
    private Connection base() throws SQLException { return DriverManager.getConnection(url, usuario, password); }
    private void instalar() { PreparadorBase.preparar(conexion, mensaje -> {}); }
    private void ejecutar(String sql) throws SQLException {
        try (Connection c = base(); Statement s = c.createStatement()) { s.execute(sql); }
    }
    private Object valor(String sql) throws SQLException {
        try (Connection c = base(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            assertTrue(r.next()); return r.getObject(1);
        }
    }
    private long cuenta(String tabla) throws SQLException { return ((Number) valor("SELECT count(*) FROM " + tabla)).longValue(); }

    @Test void creaDiecisieteTablasSinDatosDeDemostracion() throws Exception {
        assertEquals(0, PreparadorBase.revisar(conexion).tablas());
        instalar();
        assertEquals(17, PreparadorBase.revisar(conexion).tablas());
        for (String tabla : new String[]{"empresa", "usuarios", "ubicaciones", "paises_habilitados", "clientes", "unidades", "operadores", "viajes"}) {
            assertEquals(0, cuenta(tabla), tabla);
        }
        assertEquals(5, cuenta("cat_estado_viaje"));
        assertNotNull(HibernateUtil.factory());
    }

    @Test void conservaCoordenadasEditadasYNoRestauraUnaUbicacionEliminada() throws Exception {
        instalar();
        var dao = new UbicacionDAO();
        var managua = new Ubicacion("Managua", "Managua", "Nicaragua", 12.1364, -86.2514);
        var granada = new Ubicacion("Granada", "Granada", "Nicaragua", 11.9344, -85.9560);
        dao.guardar(managua); dao.guardar(granada);
        managua.setLatitud(12.34567); managua.setLongitud(-86.54321);
        dao.actualizar(managua); dao.eliminar(granada.getId());
        HibernateUtil.cerrar();
        HibernateUtil.factory();
        assertEquals(1, cuenta("ubicaciones"));
        assertEquals(12.34567, ((Number) valor("SELECT latitud FROM ubicaciones WHERE nombre='Managua'")).doubleValue(), 0.000001);
        assertEquals(-86.54321, ((Number) valor("SELECT longitud FROM ubicaciones WHERE nombre='Managua'")).doubleValue(), 0.000001);
    }

    @Test void permiteEliminarTodasLasUbicacionesYDesactivarTodosLosPaises() throws Exception {
        instalar();
        ejecutar("INSERT INTO ubicaciones(nombre,departamento,pais,latitud,longitud) VALUES ('Managua','Managua','Nicaragua',12,-86)");
        ejecutar("INSERT INTO paises_habilitados(nombre) VALUES ('Nicaragua')");
        ejecutar("DELETE FROM ubicaciones"); ejecutar("DELETE FROM paises_habilitados");
        HibernateUtil.cerrar(); HibernateUtil.factory();
        assertEquals(0, cuenta("ubicaciones")); assertEquals(0, cuenta("paises_habilitados"));
    }

    @Test void actualizaLaBaseAnteriorConservandoCuentasEmpresaViajesYCoordenadas() throws Exception {
        instalar();
        ejecutar("INSERT INTO empresa(nombre,moneda) VALUES ('Empresa del equipo','USD')");
        ejecutar("INSERT INTO usuarios(username,password_hash,rol) VALUES ('cuenta_equipo','hash-sintetico-preservado','ADMIN')");
        ejecutar("INSERT INTO ubicaciones(nombre,departamento,pais,latitud,longitud) VALUES ('Managua','Managua','Nicaragua',12.34567,-86.54321),('Granada','Granada','Nicaragua',11.9,-85.9)");
        ejecutar("INSERT INTO unidades(numero_unidad,placas,modelo,id_estado_unidad) SELECT 'IT-01','IT-PLACA','Modelo',id_estado_unidad FROM cat_estado_unidad WHERE nombre='DISPONIBLE'");
        ejecutar("INSERT INTO operadores(cedula,nombre,horas_max_conduccion,horas_min_descanso,id_estado_operador) SELECT 'IT-OPERADOR','Operador',8,1,id_estado_operador FROM cat_estado_operador WHERE nombre='DISPONIBLE'");
        ejecutar("INSERT INTO cargas(numero_carga,peso_toneladas,fecha_limite_cliente,id_estado_carga) SELECT 'IT-CARGA',1,CURRENT_TIMESTAMP+interval '1 day',id_estado_carga FROM cat_estado_carga WHERE nombre='PENDIENTE'");
        ejecutar("INSERT INTO viajes(id_unidad,id_operador,id_carga,origen,destino,tiempo_estimado,cobro_viaje,id_estado_viaje) SELECT u.id_unidad,o.id_operador,c.id_carga,a.id_ubicacion,b.id_ubicacion,2,100.25,e.id_estado_viaje FROM unidades u,operadores o,cargas c,ubicaciones a,ubicaciones b,cat_estado_viaje e WHERE a.nombre='Managua' AND b.nombre='Granada' AND e.nombre='PROGRAMADO'");
        HibernateUtil.cerrar();
        ejecutar("DROP TABLE paises_habilitados; DROP TABLE alertas_operador; ALTER TABLE empresa DROP COLUMN pais, DROP COLUMN zona_horaria; ALTER TABLE usuarios DROP COLUMN es_principal, DROP COLUMN puede_ver_reportes; ALTER TABLE rutas DROP COLUMN precio");
        assertEquals(15, PreparadorBase.revisar(conexion).tablas());
        HibernateUtil.factory();
        assertEquals(17, PreparadorBase.revisar(conexion).tablas());
        assertEquals("Empresa del equipo", valor("SELECT nombre FROM empresa"));
        assertEquals("USD", valor("SELECT moneda FROM empresa"));
        assertEquals("hash-sintetico-preservado", valor("SELECT password_hash FROM usuarios"));
        assertEquals(12.34567, ((Number) valor("SELECT latitud FROM ubicaciones WHERE nombre='Managua'")).doubleValue(), 0.000001);
        assertEquals(1, cuenta("viajes"));
        assertEquals(100.25, ((Number) valor("SELECT cobro_viaje FROM viajes")).doubleValue(), 0.001);
        assertEquals(1, cuenta("paises_habilitados"));
        assertEquals(Boolean.TRUE, valor("SELECT es_principal FROM usuarios"));
        instalar();
        assertEquals(1, cuenta("viajes")); assertEquals(2, cuenta("ubicaciones"));
    }

    @Test void actualizaLaUnicidadAnteriorAunqueTodasLasColumnasExistan() throws Exception {
        instalar(); HibernateUtil.cerrar();
        ejecutar("INSERT INTO ubicaciones(nombre,departamento,pais) VALUES ('San José','Central','Nicaragua')");
        ejecutar("ALTER TABLE ubicaciones DROP CONSTRAINT uq_ubicacion; ALTER TABLE ubicaciones ADD CONSTRAINT uq_ubicacion UNIQUE(nombre,departamento)");
        assertEquals(17, PreparadorBase.revisar(conexion).tablas());
        HibernateUtil.factory();
        ejecutar("INSERT INTO ubicaciones(nombre,departamento,pais) VALUES ('San José','Central','Costa Rica')");
        assertEquals(2, cuenta("ubicaciones"));
    }

    @Test void rechazaBasesAjenasOIncompletasSinBorrarDatos() throws Exception {
        ejecutar("CREATE TABLE notas_personales(id INTEGER); INSERT INTO notas_personales VALUES (1)");
        assertThrows(IllegalArgumentException.class, this::instalar);
        assertEquals(1, cuenta("notas_personales"));
        ejecutar("CREATE TABLE clientes(id_cliente INTEGER); INSERT INTO clientes VALUES (1)");
        assertThrows(IllegalArgumentException.class, this::instalar);
        assertEquals(1, cuenta("clientes")); assertEquals(1, cuenta("notas_personales"));
    }

    @Test void revierteUnaActualizacionFallidaSinEliminarDuplicados() throws Exception {
        instalar(); HibernateUtil.cerrar();
        ejecutar("ALTER TABLE ubicaciones DROP CONSTRAINT uq_ubicacion; ALTER TABLE empresa DROP COLUMN zona_horaria");
        ejecutar("INSERT INTO ubicaciones(nombre,departamento,pais) VALUES ('Lugar','Zona','Nicaragua'),('Lugar','Zona','Nicaragua')");
        assertThrows(SQLException.class, () -> PreparadorBase.actualizarEsquemaExistente(url, usuario, password));
        assertEquals(2, cuenta("ubicaciones"));
        assertEquals(0L, ((Number) valor("SELECT count(*) FROM information_schema.columns WHERE table_name='empresa' AND column_name='zona_horaria'")).longValue());
    }
}
