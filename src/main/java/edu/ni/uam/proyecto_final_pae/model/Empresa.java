package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Datos de la empresa transportista; una sola fila, creada en el asistente de primer arranque. */
@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "ruc", length = 20)
    private String ruc;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "direccion", length = 200)
    private String direccion;

    /** La llena la base de datos (DEFAULT NOW()). */
    @Column(name = "fecha_configuracion", insertable = false, updatable = false)
    private LocalDateTime fechaConfiguracion;

    public Empresa() {
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

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public LocalDateTime getFechaConfiguracion() {
        return fechaConfiguracion;
    }

    @Override
    public String toString() {
        return nombre;
    }
}