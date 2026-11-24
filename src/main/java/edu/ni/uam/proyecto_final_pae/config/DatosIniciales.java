package edu.ni.uam.proyecto_final_pae.config;

import edu.ni.uam.proyecto_final_pae.model.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.function.Function;

/**
 * Inserta únicamente los catálogos necesarios para las reglas de negocio.
 * Los maestros (clientes, unidades, operadores) y los usuarios se cargan desde
 * el asistente de configuracion o con el script SQL.
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
}
