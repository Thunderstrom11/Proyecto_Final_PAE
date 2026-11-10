package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.util.Objects;

/** Catalogo de apoyo: tabla cat_estado_operador. */
@Entity
@Table(name = "cat_estado_operador")
public class EstadoOperador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_operador")
    private Short id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    public EstadoOperador() {
    }

    public EstadoOperador(String nombre) {
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
        if (!(o instanceof EstadoOperador otro)) return false;
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
