package edu.ni.uam.proyecto_final_pae.config;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

/** Ejecuta trabajo de Hibernate en UNA transaccion: commit si todo sale bien, rollback si falla. */
public final class Transaccion {

    private Transaccion() {
    }

    public static <R> R ejecutar(Function<Session, R> trabajo) {
        try (Session sesion = HibernateUtil.factory().openSession()) {
            Transaction tx = sesion.beginTransaction();
            try {
                R resultado = trabajo.apply(sesion);
                tx.commit();
                return resultado;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw PersistenciaException.de(e);
            }
        }
    }

    public static void ejecutar(Consumer<Session> trabajo) {
        ejecutar(sesion -> {
            trabajo.accept(sesion);
            return null;
        });
    }

    /** Para consultas de solo lectura (sin transaccion explicita). */
    public static <R> R consultar(Function<Session, R> consulta) {
        try (Session sesion = HibernateUtil.factory().openSession()) {
            return consulta.apply(sesion);
        } catch (RuntimeException e) {
            throw PersistenciaException.de(e);
        }
    }
}
