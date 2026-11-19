package edu.ni.uam.proyecto_final_pae.dao;

import edu.ni.uam.proyecto_final_pae.config.Transaccion;
import edu.ni.uam.proyecto_final_pae.reportes.*;
import java.util.List;

/** Una única lectura parametrizada alimenta todos los totales y representaciones. */
public class ReporteDAO {
    public List<ViajeReporte> consultar(FiltroReporte filtro) {
        return Transaccion.consultar(sesion -> {
            String hql = "select new edu.ni.uam.proyecto_final_pae.reportes.ViajeReporte("
                    + "v.id, c.numeroCarga, cl.id, coalesce(cl.nombreEmpresa, 'Sin cliente'), "
                    + "o.id, o.nombre, u.id, u.numeroUnidad, v.fechaInicio, v.fechaFin, v.cobroViaje) "
                    + "from Viaje v join v.estado e join v.operador o join v.unidad u "
                    + "join v.carga c left join c.cliente cl "
                    + "where e.nombre = 'FINALIZADO' and v.fechaFin >= :desde and v.fechaFin < :hasta"
                    + (filtro.operador().id() == null ? "" : " and o.id = :operador")
                    + (filtro.unidad().id() == null ? "" : " and u.id = :unidad")
                    + (filtro.cliente().id() == null ? "" : filtro.cliente().id() == 0
                        ? " and cl.id is null" : " and cl.id = :cliente")
                    + (filtro.numeroCarga().isEmpty() ? "" : " and lower(c.numeroCarga) = :carga")
                    + " order by v.fechaFin, v.id";
            var consulta = sesion.createQuery(hql, ViajeReporte.class)
                    .setParameter("desde", filtro.desdeInclusivo())
                    .setParameter("hasta", filtro.hastaExclusivo())
                    .setMaxResults(FiltroReporte.MAX_FILAS + 1)
                    .setTimeout(60).setReadOnly(true);
            if (filtro.operador().id() != null) consulta.setParameter("operador", filtro.operador().id());
            if (filtro.unidad().id() != null) consulta.setParameter("unidad", filtro.unidad().id());
            if (filtro.cliente().id() != null && filtro.cliente().id() != 0) {
                consulta.setParameter("cliente", filtro.cliente().id());
            }
            if (!filtro.numeroCarga().isEmpty()) consulta.setParameter("carga", filtro.numeroCarga().toLowerCase(java.util.Locale.ROOT));
            List<ViajeReporte> filas = consulta.getResultList();
            if (filas.size() > FiltroReporte.MAX_FILAS) throw new IllegalArgumentException(
                    "La selección supera " + FiltroReporte.MAX_FILAS + " viajes. Reduzca el período o aplique más filtros.");
            return filas;
        });
    }

    public CatalogosReporte catalogos() {
        return Transaccion.consultar(sesion -> new CatalogosReporte(
                sesion.createQuery("select new edu.ni.uam.proyecto_final_pae.reportes.OpcionReporte(o.id, o.nombre) "
                        + "from Operador o order by o.nombre, o.id", OpcionReporte.class).getResultList(),
                sesion.createQuery("select new edu.ni.uam.proyecto_final_pae.reportes.OpcionReporte(u.id, u.numeroUnidad) "
                        + "from Unidad u order by u.numeroUnidad", OpcionReporte.class).getResultList(),
                sesion.createQuery("select new edu.ni.uam.proyecto_final_pae.reportes.OpcionReporte(c.id, c.nombreEmpresa) "
                        + "from Cliente c order by c.nombreEmpresa", OpcionReporte.class).getResultList()));
    }
}
