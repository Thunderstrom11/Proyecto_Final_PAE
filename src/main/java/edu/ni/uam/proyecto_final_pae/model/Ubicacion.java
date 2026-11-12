package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.util.Objects;

/** Origen o destino de un viaje: tabla ubicaciones. */
@Entity
@Table(name = "ubicaciones",
        uniqueConstraints = @UniqueConstraint(name = "uq_ubicacion", columnNames = {"nombre", "departamento"}))
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "departamento", nullable = false, length = 80)
    private String departamento;

    @Column(name = "pais", nullable = false, length = 80)
    private String pais = "Nicaragua";

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    public Ubicacion() {
    }

    public Ubicacion(String nombre, String departamento) {
        this(nombre, departamento, "Nicaragua", null, null);
    }

    public Ubicacion(String nombre, String departamento, String pais, Double latitud, Double longitud) {
        this.nombre = nombre;
        this.departamento = departamento;
        this.pais = pais;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ubicacion otra)) return false;
        return id != null && Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return nombre + (pais == null || pais.isBlank() ? "" : " · " + pais);
    }
}
