package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class UbicacionDAO extends GenericDAO<Ubicacion, Integer> {

    public UbicacionDAO() {
        super(Ubicacion.class);
    }

    @Override
    public List<Ubicacion> listar() {
        return Transaccion.consultar(sesion ->
                sesion.createQuery("from Ubicacion u order by u.nombre", Ubicacion.class).getResultList());
    }

    public List<Ubicacion> listarPorPais(String pais) {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "from Ubicacion u where lower(u.pais) = lower(:pais) order by u.nombre",
                        Ubicacion.class)
                .setParameter("pais", pais)
                .getResultList());
    }

    public List<Ubicacion> buscarPorPais(String pais, String texto) {
        return Transaccion.consultar(sesion -> sesion.createQuery(
                        "from Ubicacion u where lower(u.pais) = lower(:pais) "
                                + "and lower(u.nombre) like :texto order by u.nombre",
                        Ubicacion.class)
                .setParameter("pais", pais)
                .setParameter("texto", "%" + (texto == null ? "" : texto.trim().toLowerCase()) + "%")
                .setMaxResults(15)
                .getResultList());
    }

    public Ubicacion buscarPorPaisYNombre(String pais, String nombre) {
        List<Ubicacion> lista = Transaccion.consultar(sesion -> sesion.createQuery(
                        "from Ubicacion u where lower(u.pais) = lower(:pais) "
                                + "and lower(u.nombre) = lower(:nombre)",
                        Ubicacion.class)
                .setParameter("pais", pais)
                .setParameter("nombre", nombre)
                .setMaxResults(1)
                .getResultList());
        return lista.isEmpty() ? null : lista.get(0);
    }

    public Ubicacion buscarPorNombreDepartamentoYPais(String nombre, String departamento,
                                                      String pais) {
        List<Ubicacion> lista = Transaccion.consultar(sesion -> sesion.createQuery(
                        "from Ubicacion u where lower(u.nombre) = lower(:nombre) "
                                + "and lower(u.departamento) = lower(:departamento) "
                                + "and lower(u.pais) = lower(:pais)",
                        Ubicacion.class)
                .setParameter("nombre", nombre)
                .setParameter("departamento", departamento)
                .setParameter("pais", pais)
                .setMaxResults(1)
                .getResultList());
        return lista.isEmpty() ? null : lista.get(0);
    }

    /**
     * Guarda en lote las ubicaciones que todavía no existen y devuelve cuántas agregó.
     * Se apoya en la clave nombre, departamento y país para no repetir lugares.
     */
    public int guardarNuevas(Collection<Ubicacion> candidatas) {
        if (candidatas == null || candidatas.isEmpty()) {
            return 0;
        }
        Set<String> paises = candidatas.stream()
                .map(ubicacion -> ubicacion.getPais() == null ? "" : ubicacion.getPais().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return Transaccion.ejecutar(sesion -> {
            Set<String> existentes = new HashSet<>(sesion.createQuery(
                            "select lower(u.nombre)||'|'||lower(u.departamento)||'|'||lower(u.pais) "
                                    + "from Ubicacion u where lower(u.pais) in (:paises)", String.class)
                    .setParameter("paises", paises)
                    .getResultList());
            int agregadas = 0;
            int lote = 0;
            for (Ubicacion ubicacion : candidatas) {
                if (!existentes.add(clave(ubicacion))) {
                    continue;
                }
                sesion.persist(ubicacion);
                agregadas++;
                if (++lote % 400 == 0) {
                    sesion.flush();
                    sesion.clear();
                }
            }
            return agregadas;
        });
    }

    private String clave(Ubicacion ubicacion) {
        return texto(ubicacion.getNombre()) + "|" + texto(ubicacion.getDepartamento())
                + "|" + texto(ubicacion.getPais());
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.toLowerCase(Locale.ROOT);
    }

    /** Elimina rutas y ubicaciones; no toca viajes, por eso exige que no existan. */
    public int[] eliminarTodas() {
        return Transaccion.ejecutar(sesion -> {
            Long viajes = sesion.createQuery("select count(v) from Viaje v", Long.class)
                    .getSingleResult();
            if (viajes != null && viajes > 0) {
                throw new PersistenciaException("No se pueden eliminar todas las ubicaciones: hay "
                        + viajes + " viaje(s) que las usan. Elimine primero los viajes.", null);
            }
            int rutas = sesion.createMutationQuery("delete from Ruta").executeUpdate();
            int ubicaciones = sesion.createMutationQuery("delete from Ubicacion").executeUpdate();
            return new int[]{rutas, ubicaciones};
        });
    }
}
