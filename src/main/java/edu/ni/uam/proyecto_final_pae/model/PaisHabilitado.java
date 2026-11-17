package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** País disponible para crear ubicaciones y rutas desde el panel administrativo. */
@Entity
@Table(name = "paises_habilitados")
public class PaisHabilitado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pais_habilitado")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "fecha_activacion", insertable = false, updatable = false)
    private LocalDateTime fechaActivacion;

    public PaisHabilitado() {
    }

    public PaisHabilitado(String nombre) {
        this.nombre = nombre;
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

    public LocalDateTime getFechaActivacion() {
        return fechaActivacion;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
