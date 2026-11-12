package edu.ni.uam.proyecto_final_pae.config;

import edu.ni.uam.proyecto_final_pae.model.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

/**
 * Inserta los catalogos y los maestros de ejemplo cuando las tablas estan vacias.
 * Reemplaza la seccion de datos de database/fleetroute_pro.sql para quien deja que Hibernate cree el esquema.
 * Es idempotente: solo agrega lo que falta.
 * Los usuarios NO se siembran aqui: los crea el asistente de primera configuracion o el script SQL.
 */
final class DatosIniciales {

    private DatosIniciales() {
    }

    static void sembrar(SessionFactory fabrica) {
        try (Session sesion = fabrica.openSession()) {
            Transaction tx = sesion.beginTransaction();
            try {
                asegurar(sesion, EstadoUnidad.class, EstadoUnidad::new, "DISPONIBLE", "EN_RUTA", "MANTENIMIENTO");
                asegurar(sesion, EstadoOperador.class, EstadoOperador::new, "DISPONIBLE", "EN_RUTA", "DESCANSO", "INACTIVO");
                asegurar(sesion, EstadoCarga.class, EstadoCarga::new, "PENDIENTE", "ASIGNADA", "EN_TRANSITO", "ENTREGADA");
                asegurar(sesion, EstadoViaje.class, EstadoViaje::new,
                        "PROGRAMADO", "EN_CURSO", "PAUSADO", "FINALIZADO", "CANCELADO");
                asegurar(sesion, TipoEvento.class, TipoEvento::new,
                        "INICIO_TRAYECTO", "PAUSA_DESCANSO", "REANUDAR", "CONFIRMAR_ENTREGA");
                sesion.flush();

                asegurarUbicacion(sesion, "Managua", "Managua", "Nicaragua", 12.1364, -86.2514);
                asegurarUbicacion(sesion, "León", "León", "Nicaragua", 12.4379, -86.8780);
                asegurarUbicacion(sesion, "Granada", "Granada", "Nicaragua", 11.9344, -85.9560);
                asegurarUbicacion(sesion, "Masaya", "Masaya", "Nicaragua", 11.9744, -86.0941);
                asegurarUbicacion(sesion, "Estelí", "Estelí", "Nicaragua", 13.0919, -86.3539);
                asegurarUbicacion(sesion, "Matagalpa", "Matagalpa", "Nicaragua", 12.9256, -85.9174);
                asegurarUbicacion(sesion, "Chinandega", "Chinandega", "Nicaragua", 12.6290, -87.1310);
                asegurarUbicacion(sesion, "Rivas", "Rivas", "Nicaragua", 11.4372, -85.8260);
                asegurarUbicacion(sesion, "Ciudad de Guatemala", "Guatemala", "Guatemala", 14.6349, -90.5069);
                asegurarUbicacion(sesion, "Quetzaltenango", "Quetzaltenango", "Guatemala", 14.8347, -91.5180);
                asegurarUbicacion(sesion, "San Salvador", "San Salvador", "El Salvador", 13.6929, -89.2182);
                asegurarUbicacion(sesion, "Santa Ana", "Santa Ana", "El Salvador", 13.9942, -89.5597);
                asegurarUbicacion(sesion, "Tegucigalpa", "Francisco Morazán", "Honduras", 14.0723, -87.1921);
                asegurarUbicacion(sesion, "San Pedro Sula", "Cortés", "Honduras", 15.5040, -88.0250);
                asegurarUbicacion(sesion, "San José", "San José", "Costa Rica", 9.9281, -84.0907);
                asegurarUbicacion(sesion, "Liberia", "Guanacaste", "Costa Rica", 10.6346, -85.4400);
                asegurarUbicacion(sesion, "Ciudad de Panamá", "Panamá", "Panamá", 8.9824, -79.5199);
                asegurarUbicacion(sesion, "David", "Chiriquí", "Panamá", 8.4333, -82.4333);
                asegurarUbicacion(sesion, "Belmopán", "Cayo", "Belice", 17.2510, -88.7590);
                asegurarUbicacion(sesion, "Ciudad de Belice", "Belice", "Belice", 17.5046, -88.1962);
                if (total(sesion, Cliente.class) == 0) {
                    sesion.persist(new Cliente("Cliente Demo"));
                }
                if (total(sesion, Unidad.class) == 0) {
                    Unidad unidad = new Unidad();
                    unidad.setNumeroUnidad("U-001");
                    unidad.setPlacas("M 000001");
                    unidad.setModelo("Modelo demo");
                    unidad.setCapacidadToneladas(new BigDecimal("20.00"));
                    unidad.setEstado(porNombre(sesion, EstadoUnidad.class, "DISPONIBLE"));
                    sesion.persist(unidad);
                }

                if (total(sesion, Operador.class) == 0) {
                    Operador demo = new Operador();
                    demo.setCedula("001-000000-0001A");
                    demo.setNombre("Operador Demo");
                    demo.setHorasMaxConduccion(new BigDecimal("8.00"));
                    demo.setHorasMinDescanso(new BigDecimal("8.00"));
                    demo.setFechaIngreso(LocalDate.now());
                    demo.setEstado(porNombre(sesion, EstadoOperador.class, "DISPONIBLE"));
                    sesion.persist(demo);
                }
                asegurarAdminPrincipal(sesion);
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    private static <T> void asegurar(Session sesion, Class<T> clase, Function<String, T> nuevo, String... nombres) {
        for (String nombre : nombres) {
            Long existentes = sesion.createQuery(
                            "select count(c) from " + clase.getSimpleName() + " c where c.nombre = :n", Long.class)
                    .setParameter("n", nombre).getSingleResult();
            if (existentes == 0) {
                sesion.persist(nuevo.apply(nombre));
            }
        }
    }

    private static <T> T porNombre(Session sesion, Class<T> clase, String nombre) {
        return sesion.createQuery("from " + clase.getSimpleName() + " c where c.nombre = :n", clase)
                .setParameter("n", nombre).getSingleResult();
    }

    private static void asegurarUbicacion(Session sesion, String nombre, String departamento,
                                          String pais, double latitud, double longitud) {
        Long existentes = sesion.createQuery(
                        "select count(u) from Ubicacion u where u.nombre = :n and u.departamento = :d", Long.class)
                .setParameter("n", nombre)
                .setParameter("d", departamento)
                .getSingleResult();
        if (existentes == 0) {
            sesion.persist(new Ubicacion(nombre, departamento, pais, latitud, longitud));
        } else {
            sesion.createMutationQuery("update Ubicacion u set u.pais = :pais, u.latitud = :lat, u.longitud = :lon "
                            + "where u.nombre = :n and u.departamento = :d")
                    .setParameter("pais", pais)
                    .setParameter("lat", latitud)
                    .setParameter("lon", longitud)
                    .setParameter("n", nombre)
                    .setParameter("d", departamento)
                    .executeUpdate();
        }
    }

    /**
     * Garantiza que exista un administrador principal: las bases creadas antes del panel
     * administrativo (o con el script viejo) no tienen ninguno y nadie podria gestionar cuentas.
     * Promueve al administrador activo mas antiguo.
     */
    private static void asegurarAdminPrincipal(Session sesion) {
        Long principales = sesion.createQuery(
                        "select count(u) from Usuario u where u.esPrincipal = true", Long.class)
                .getSingleResult();
        if (principales > 0) {
            return;
        }
        Integer id = sesion.createQuery(
                        "select min(u.id) from Usuario u where u.rol = :rol and u.activo = true", Integer.class)
                .setParameter("rol", RolUsuario.ADMIN)
                .getSingleResult();
        if (id == null) {
            return;
        }
        sesion.createMutationQuery(
                        "update Usuario u set u.esPrincipal = true, u.puedeVerReportes = true where u.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    private static long total(Session sesion, Class<?> clase) {
        return sesion.createQuery("select count(e) from " + clase.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }
}
