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

    private static long total(Session sesion, Class<?> clase) {
        return sesion.createQuery("select count(e) from " + clase.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }
}
