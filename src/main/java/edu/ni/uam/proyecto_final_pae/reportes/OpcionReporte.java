package edu.ni.uam.proyecto_final_pae.reportes;

/** El identificador, y no el nombre visible, determina el filtro. */
public record OpcionReporte(Integer id, String nombre) {
    public static final OpcionReporte TODOS = new OpcionReporte(null, "Todos");
    public static final OpcionReporte SIN_CLIENTE = new OpcionReporte(0, "Sin cliente");
    @Override public String toString() { return id == null || id == 0 ? nombre : nombre + " (#" + id + ")"; }
}
