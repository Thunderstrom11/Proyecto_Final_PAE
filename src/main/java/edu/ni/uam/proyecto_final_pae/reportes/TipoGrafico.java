package edu.ni.uam.proyecto_final_pae.reportes;

public enum TipoGrafico {
    NINGUNO("Sin gráfico"), BARRAS("Barras horizontales"), COLUMNAS("Columnas"), LINEAS("Líneas");
    private final String nombre;
    TipoGrafico(String nombre) { this.nombre = nombre; }
    @Override public String toString() { return nombre; }
}
