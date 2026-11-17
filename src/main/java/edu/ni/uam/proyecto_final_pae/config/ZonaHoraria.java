package edu.ni.uam.proyecto_final_pae.config;

import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.model.Empresa;

import java.time.ZoneId;

/** Zona horaria configurada en la empresa, con Managua como respaldo. */
public final class ZonaHoraria {

    private static final ZoneId RESPALDO = ZoneId.of("America/Managua");

    private static ZoneId zona;

    private ZonaHoraria() {
    }

    public static synchronized ZoneId actual() {
        if (zona == null) {
            String id = null;
            try {
                id = new EmpresaDAO().obtener().map(Empresa::getZonaHoraria).orElse(null);
            } catch (RuntimeException e) {
                // Sin base de datos disponible todavía: se usa el respaldo.
            }
            zona = valida(id);
        }
        return zona;
    }

    /** La próxima consulta vuelve a leer la zona de la empresa. */
    public static synchronized void recargar() {
        zona = null;
    }

    private static ZoneId valida(String id) {
        if (id != null && !id.isBlank()) {
            try {
                return ZoneId.of(id.trim());
            } catch (RuntimeException e) {
                // Identificador inválido: se usa el respaldo.
            }
        }
        return RESPALDO;
    }
}