package edu.ni.uam.proyecto_final_pae.reportes;

import edu.ni.uam.proyecto_final_pae.model.Ubicacion;
import edu.ni.uam.proyecto_final_pae.model.Viaje;
import java.time.LocalDateTime;

/** Copia de una fila visible: Jasper no conserva entidades ni consulta relaciones diferidas. */
public record FilaViajeOperativo(Integer id, String carga, String operador, String unidad,
                                 String origen, String destino, String estado, LocalDateTime fechaLimite) {
    public FilaViajeOperativo {
        if (id == null || id <= 0) throw new IllegalArgumentException("Hay un viaje sin identificador válido.");
        carga = texto(carga);
        operador = texto(operador);
        unidad = texto(unidad);
        origen = texto(origen);
        destino = texto(destino);
        estado = texto(estado);
    }

    public static FilaViajeOperativo de(Viaje viaje) {
        return new FilaViajeOperativo(viaje.getId(),
                viaje.getCarga() == null ? null : viaje.getCarga().getNumeroCarga(),
                viaje.getOperador() == null ? null : viaje.getOperador().getNombre() + " (#" + viaje.getOperador().getId() + ")",
                viaje.getUnidad() == null ? null : viaje.getUnidad().getNumeroUnidad(),
                lugar(viaje.getOrigen()), lugar(viaje.getDestino()),
                viaje.getEstado() == null ? null : viaje.getEstado().getNombre(),
                viaje.getCarga() == null ? null : viaje.getCarga().getFechaLimiteCliente());
    }

    private static String texto(String valor) { return valor == null || valor.isBlank() ? "Sin registro" : valor; }

    private static String lugar(Ubicacion ubicacion) {
        if (ubicacion == null) return null;
        String nombre = texto(ubicacion.getNombre());
        if (ubicacion.getDepartamento() != null && !ubicacion.getDepartamento().isBlank()
                && !ubicacion.getDepartamento().equalsIgnoreCase(nombre)) nombre += " · " + ubicacion.getDepartamento();
        if (ubicacion.getPais() != null && !ubicacion.getPais().isBlank()) nombre += " · " + ubicacion.getPais();
        return nombre;
    }
}
