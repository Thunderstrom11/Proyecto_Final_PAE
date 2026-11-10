package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.util.Objects;

/** Catalogo de apoyo: tabla cat_estado_carga. */
@Entity
@Table(name = "cat_estado_carga")
public class EstadoCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_carga")
    private Short id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    public EstadoCarga() {
    }

    public EstadoCarga(String nombre) {
        this.nombre = nombre;
    }

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EstadoCarga otro)) return false;
        return id != null && Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
