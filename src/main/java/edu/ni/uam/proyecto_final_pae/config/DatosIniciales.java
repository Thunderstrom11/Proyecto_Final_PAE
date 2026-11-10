package edu.ni.uam.proyecto_final_pae.config;

import edu.ni.uam.proyecto_final_pae.model.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

/**
 * Inserta los catalogos y los datos de ejemplo cuando las tablas estan vacias.
 * Reemplaza la seccion de datos de database/fleetroute_pro.sql para quien deja que Hibernate cree el esquema.
 * Es idempotente: solo agrega lo que falta.
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

                if (total(sesion, Ubicacion.class) == 0) {
                    for (String ciudad : new String[]{"Managua", "León", "Granada", "Masaya", "Estelí",
                            "Matagalpa", "Chinandega", "Rivas"}) {
                        sesion.persist(new Ubicacion(ciudad, ciudad));
                    }
                }
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

                Operador demo = null;
                if (total(sesion, Operador.class) == 0) {
                    demo = new Operador();
                    demo.setCedula("001-000000-0001A");
                    demo.setNombre("Operador Demo");
                    demo.setHorasMaxConduccion(new BigDecimal("10.00"));
                    demo.setHorasMinDescanso(new BigDecimal("1.00"));
                    demo.setFechaIngreso(LocalDate.now());
                    demo.setEstado(porNombre(sesion, EstadoOperador.class, "DISPONIBLE"));
                    sesion.persist(demo);
                }

                if (total(sesion, Usuario.class) == 0) {
                    sesion.persist(usuario("admin", "Admin123!", RolUsuario.ADMIN, null));
                    if (demo == null) {
                        demo = sesion.createQuery("from Operador o order by o.id", Operador.class)
                                .setMaxResults(1).getSingleResultOrNull();
                    }
                    if (demo != null) {
                        sesion.persist(usuario("operador", "Operador123!", RolUsuario.OPERADOR, demo));
                    }
                }
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    private static Usuario usuario(String nombre, String clave, RolUsuario rol, Operador operador) {
        Usuario usuario = new Usuario();
        usuario.setUsername(nombre);
        usuario.setPasswordHash(BCrypt.hashpw(clave, BCrypt.gensalt()));
        usuario.setRol(rol);
        usuario.setOperador(operador);
        return usuario;
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

    private static long total(Session sesion, Class<?> clase) {
        return sesion.createQuery("select count(e) from " + clase.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }
}
