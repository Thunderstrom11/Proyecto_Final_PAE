package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.EstadoCarga;
import edu.ni.uam.proyecto_final_pae.model.EstadoOperador;
import edu.ni.uam.proyecto_final_pae.model.EstadoUnidad;
import edu.ni.uam.proyecto_final_pae.model.EstadoViaje;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Consultas HQL de viajes para la cabina del operador y el monitor de flota (Persona 4).
 *
 * <p><b>Consulta HQL #2 de este modulo (la asignada a Persona 4):</b>
 * {@link #viajesActivos()} devuelve los viajes activos con su unidad, operador y carga.</p>
 *
 * <p>Las entidades {@code Viaje}, {@code Unidad} y {@code Operador} las aportan las ramas de
 * Persona 3 y Persona 2, por eso aqui se consultan por HQL (texto) y el resultado se lee como
 * {@code Object[]}. Si esas ramas usan otros nombres de atributo, se corrigen <b>solo</b> en
 * este archivo. Nombres esperados:</p>
 * <ul>
 *   <li>Viaje: {@code id}, {@code unidad}, {@code operador}, {@code carga}, {@code origen},
 *       {@code destino}, {@code tiempoEstimado}, {@code estado}, {@code viajePrevio}</li>
 *   <li>Unidad: {@code id}, {@code numeroUnidad}, {@code estado}</li>
 *   <li>Operador: {@code id}, {@code nombre}, {@code estado}, {@code horasMaxConduccion},
 *       {@code horasMinDescanso}</li>
 * </ul>
 */
public class FlotaDAO {

    private static final ZoneId ZONA = ZoneId.of("America/Managua");

    /**
     * Proyeccion comun: datos del viaje + unidad + operador + carga + origen/destino + estado.
     * Los dos ultimos campos son los limites de jornada del operador (RF-07/RF-08).
     */
    private static final String PROYECCION = """
            select v.id, u.numeroUnidad, u.id, o.nombre, o.id, c.numeroCarga,
                   c.fechaLimiteCliente, v.tiempoEstimado,
                   origen.nombre, destino.nombre, ev.nombre,
                   o.horasMaxConduccion, o.horasMinDescanso
              from Viaje v
              join v.unidad u
              join v.operador o
              join v.carga c
              join v.origen origen
              join v.destino destino
              join v.estado ev
            """;

    private static final String ESTADOS_ACTIVOS = "('PROGRAMADO', 'EN_CURSO', 'PAUSADO')";

    /** Fila de solo lectura con lo que necesitan la cabina y el monitor. */
    public record FilaViaje(
            int idViaje,
            String numeroUnidad,
            int idUnidad,
            String operador,
            int idOperador,
            String numeroCarga,
            LocalDateTime fechaLimite,
            BigDecimal tiempoEstimado,
            String origen,
            String destino,
            String estado,
            BigDecimal horasMaxConduccion,
            BigDecimal horasMinDescanso) {
    }

    /** Operador para el selector de la cabina (mientras el login de Persona 2 no este integrado). */
    public record Opcion(int id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    /**
     * CONSULTA HQL #2 (Persona 4): viajes activos con su unidad, operador y carga,
     * ordenados por el SLA mas cercano.
     */
    public List<FilaViaje> viajesActivos() {
        return Transaccion.consultar(sesion -> mapear(sesion.createQuery(
                        PROYECCION + " where ev.nombre in " + ESTADOS_ACTIVOS
                                + " order by c.fechaLimiteCliente",
                        Object[].class)
                .getResultList()));
    }

    /** Datos completos de un viaje (para la cabina y el calculo de jornada). */
    public Optional<FilaViaje> datosViaje(int idViaje) {
        return Transaccion.consultar(sesion -> datosViaje(sesion, idViaje));
    }

    /** Variante transaccional: el llamador controla la transaccion. */
    public Optional<FilaViaje> datosViaje(Session sesion, int idViaje) {
        List<Object[]> filas = sesion.createQuery(PROYECCION + " where v.id = :id", Object[].class)
                .setParameter("id", idViaje)
                .getResultList();
        return filas.isEmpty() ? Optional.empty() : Optional.of(fila(filas.get(0)));
    }

    /**
     * Viaje que el operador debe atender ahora: primero el que esta EN_CURSO/PAUSADO y,
     * si no hay, el siguiente PROGRAMADO.
     */
    public Optional<FilaViaje> viajeDelOperador(int idOperador) {
        return Transaccion.consultar(sesion -> {
            List<Object[]> filas = sesion.createQuery(
                            PROYECCION + " where o.id = :id and ev.nombre in " + ESTADOS_ACTIVOS
                                    + " order by case ev.nombre when 'EN_CURSO' then 0 when 'PAUSADO' then 1 else 2 end, v.id",
                            Object[].class)
                    .setParameter("id", idOperador)
                    .setMaxResults(1)
                    .getResultList();
            return filas.isEmpty() ? Optional.empty() : Optional.of(fila(filas.get(0)));
        });
    }

    /** Viaje encadenado: el que declara a este viaje como previo (RF-09). */
    public Optional<FilaViaje> viajeEncadenado(int idViaje) {
        return Transaccion.consultar(sesion -> viajeEncadenado(sesion, idViaje));
    }

    /** Variante transaccional: el llamador controla la transaccion. */
    public Optional<FilaViaje> viajeEncadenado(Session sesion, int idViaje) {
        List<Object[]> filas = sesion.createQuery(PROYECCION + " where v.viajePrevio.id = :id", Object[].class)
                .setParameter("id", idViaje)
                .setMaxResults(1)
                .getResultList();
        return filas.isEmpty() ? Optional.empty() : Optional.of(fila(filas.get(0)));
    }

    /** Operadores para el selector de la cabina. */
    public List<Opcion> operadores() {
        return Transaccion.consultar(sesion -> {
            List<Object[]> filas = sesion.createQuery(
                            "select o.id, o.nombre from Operador o order by o.nombre", Object[].class)
                    .getResultList();
            List<Opcion> opciones = new ArrayList<>();
            for (Object[] fila : filas) {
                opciones.add(new Opcion(entero(fila[0]), (String) fila[1]));
            }
            return opciones;
        });
    }

    /**
     * Cambia el estado del viaje por NOMBRE de catalogo (nunca por ID quemado) y deja coherentes las
     * fechas del viaje y el estado de su carga: EN_CURSO -> carga EN_TRANSITO y fecha_inicio;
     * FINALIZADO -> carga ENTREGADA y fecha_fin (ck_viaje_fechas exige fecha_inicio si hay fecha_fin).
     */
    public void cambiarEstadoViaje(int idViaje, String nombreEstado) {
        Transaccion.ejecutar(sesion -> {
            cambiarEstadoViaje(sesion, idViaje, nombreEstado, true);
            return null;
        });
    }

    /** Variante transaccional: el llamador controla la transaccion. */
    public void cambiarEstadoViaje(Session sesion, int idViaje, String nombreEstado) {
        cambiarEstadoViaje(sesion, idViaje, nombreEstado, true);
    }

    /**
     * Cambia el estado del viaje. {@code marcarInicio} solo debe ser true cuando el operador
     * inicia el tramo; al activar un viaje encadenado no se debe arrancar el cronometro.
     */
    public void cambiarEstadoViaje(Session sesion, int idViaje, String nombreEstado, boolean marcarInicio) {
        EstadoViaje estado = catalogo(sesion, EstadoViaje.class, nombreEstado);
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        sesion.createMutationQuery("update Viaje v set v.estado = :estado where v.id = :id")
                .setParameter("estado", estado)
                .setParameter("id", idViaje)
                .executeUpdate();
        if ("EN_CURSO".equals(nombreEstado)) {
            if (marcarInicio) {
                sesion.createMutationQuery("update Viaje v set v.fechaInicio = :ahora "
                                + "where v.id = :id and v.fechaInicio is null")
                        .setParameter("ahora", ahora)
                        .setParameter("id", idViaje)
                        .executeUpdate();
            }
            cambiarEstadoCarga(sesion, idViaje, "EN_TRANSITO");
        } else if ("FINALIZADO".equals(nombreEstado)) {
            sesion.createMutationQuery("update Viaje v set v.fechaInicio = coalesce(v.fechaInicio, :ahora), "
                            + "v.fechaFin = :ahora where v.id = :id")
                    .setParameter("ahora", ahora)
                    .setParameter("id", idViaje)
                    .executeUpdate();
            cambiarEstadoCarga(sesion, idViaje, "ENTREGADA");
        }
    }

    /** Cambia el estado del operador dentro de la transaccion del llamador. */
    public void cambiarEstadoOperador(Session sesion, int idOperador, String nombreEstado) {
        EstadoOperador estado = catalogo(sesion, EstadoOperador.class, nombreEstado);
        sesion.createMutationQuery("update Operador o set o.estado = :estado where o.id = :id")
                .setParameter("estado", estado)
                .setParameter("id", idOperador)
                .executeUpdate();
    }

    /** Cambia el estado de la unidad dentro de la transaccion del llamador. */
    public void cambiarEstadoUnidad(Session sesion, int idUnidad, String nombreEstado) {
        EstadoUnidad estado = catalogo(sesion, EstadoUnidad.class, nombreEstado);
        sesion.createMutationQuery("update Unidad u set u.estado = :estado where u.id = :id")
                .setParameter("estado", estado)
                .setParameter("id", idUnidad)
                .executeUpdate();
    }

    /** Indica si la unidad tiene algún viaje activo (excluyendo opcionalmente uno). */
    public boolean unidadTieneViajeActivo(int idUnidad) {
        return Transaccion.consultar(sesion -> hayViajeActivo(sesion, "unidad", idUnidad, null));
    }

    /** Indica si el operador tiene algún viaje activo (excluyendo opcionalmente uno). */
    public boolean operadorTieneViajeActivo(int idOperador) {
        return Transaccion.consultar(sesion -> hayViajeActivo(sesion, "operador", idOperador, null));
    }

    private static boolean hayViajeActivo(Session sesion, String campo, int id, Integer excluirIdViaje) {
        String hql = "select count(v) from Viaje v join v.estado e "
                + "where v." + campo + ".id = :id and e.nombre in " + ESTADOS_ACTIVOS
                + (excluirIdViaje == null ? "" : " and v.id <> :excluir");
        var query = sesion.createQuery(hql, Long.class).setParameter("id", id);
        if (excluirIdViaje != null) {
            query.setParameter("excluir", excluirIdViaje);
        }
        return query.getSingleResult() > 0;
    }

    private static void cambiarEstadoCarga(Session sesion, int idViaje, String nombreEstado) {
        EstadoCarga estadoCarga = catalogo(sesion, EstadoCarga.class, nombreEstado);
        Integer idCarga = sesion.createQuery("select v.carga.id from Viaje v where v.id = :id", Integer.class)
                .setParameter("id", idViaje)
                .getSingleResultOrNull();
        if (idCarga != null) {
            sesion.createMutationQuery("update Carga c set c.estado = :estado where c.id = :id")
                    .setParameter("estado", estadoCarga)
                    .setParameter("id", idCarga)
                    .executeUpdate();
        }
    }

    /** RF-04: al cerrar el ultimo viaje de la cadena, la unidad y el operador vuelven a DISPONIBLE. */
    public void liberarRecursos(int idUnidad, int idOperador) {
        Transaccion.ejecutar(sesion -> {
            liberarRecursos(sesion, idUnidad, idOperador);
            return null;
        });
    }

    /** Variante transaccional: no libera un recurso que todavia tenga otro viaje activo. */
    public void liberarRecursos(Session sesion, int idUnidad, int idOperador) {
        if (!hayViajeActivo(sesion, "unidad", idUnidad, null)) {
            cambiarEstadoUnidad(sesion, idUnidad, "DISPONIBLE");
        }
        if (!hayViajeActivo(sesion, "operador", idOperador, null)) {
            cambiarEstadoOperador(sesion, idOperador, "DISPONIBLE");
        }
    }

    // ------------------------------------------------------------------ apoyo

    private static <T> T catalogo(Session sesion, Class<T> clase, String nombre) {
        T valor = sesion.createQuery("from " + clase.getSimpleName() + " c where c.nombre = :nombre", clase)
                .setParameter("nombre", nombre)
                .getSingleResultOrNull();
        if (valor == null) {
            throw new PersistenciaException("Falta el valor '" + nombre + "' en el catalogo "
                    + clase.getSimpleName() + ". Ejecute database/fleetroute_pro.sql.", null);
        }
        return valor;
    }

    private static List<FilaViaje> mapear(List<Object[]> filas) {
        List<FilaViaje> salida = new ArrayList<>();
        for (Object[] fila : filas) {
            salida.add(fila(fila));
        }
        return salida;
    }

    private static FilaViaje fila(Object[] f) {
        return new FilaViaje(
                entero(f[0]),
                (String) f[1],
                entero(f[2]),
                (String) f[3],
                entero(f[4]),
                (String) f[5],
                fecha(f[6]),
                decimal(f[7]),
                (String) f[8],
                (String) f[9],
                (String) f[10],
                decimal(f[11]),
                decimal(f[12]));
    }

    private static int entero(Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    private static BigDecimal decimal(Object valor) {
        if (valor == null) return null;
        return valor instanceof BigDecimal numero ? numero : new BigDecimal(valor.toString());
    }

    private static LocalDateTime fecha(Object valor) {
        if (valor == null) return null;
        if (valor instanceof LocalDateTime fecha) return fecha;
        if (valor instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        if (valor instanceof java.util.Date fecha) return LocalDateTime.ofInstant(fecha.toInstant(), ZONA);
        throw new IllegalStateException("Tipo de fecha no soportado: " + valor.getClass().getName());
    }
}
