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

    /** Constructor de proyección HQL: evita transportar entidades Hibernate al visor. */
    public FilaViajeOperativo(Integer id, String carga, String operador, Integer operadorId, String unidad,
                              String origen, String departamentoOrigen, String paisOrigen,
                              String destino, String departamentoDestino, String paisDestino,
                              String estado, LocalDateTime fechaLimite) {
        this(id, carga, operador + " (#" + operadorId + ")", unidad,
                lugar(origen, departamentoOrigen, paisOrigen), lugar(destino, departamentoDestino, paisDestino), estado, fechaLimite);
    }

    private static String texto(String valor) { return valor == null || valor.isBlank() ? "Sin registro" : valor; }

    private static String lugar(Ubicacion ubicacion) {
        if (ubicacion == null) return null;
        return lugar(ubicacion.getNombre(), ubicacion.getDepartamento(), ubicacion.getPais());
    }

    private static String lugar(String valor, String departamento, String pais) {
        String nombre = texto(valor);
        if (departamento != null && !departamento.isBlank() && !departamento.equalsIgnoreCase(nombre)) nombre += " · " + departamento;
        if (pais != null && !pais.isBlank()) nombre += " · " + pais;
        return nombre;
    }
}
