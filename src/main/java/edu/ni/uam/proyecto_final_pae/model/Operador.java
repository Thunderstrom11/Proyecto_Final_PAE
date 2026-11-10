package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Chofer con sus reglas de fatiga: tabla operadores. */
@Entity
@Table(name = "operadores")
public class Operador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operador")
    private Integer id;

    @Column(name = "cedula", nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "horas_max_conduccion", nullable = false, precision = 5, scale = 2)
    private BigDecimal horasMaxConduccion;

    @Column(name = "horas_min_descanso", nullable = false, precision = 5, scale = 2)
    private BigDecimal horasMinDescanso;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_estado_operador", nullable = false)
    private EstadoOperador estado;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    public Operador() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public BigDecimal getHorasMaxConduccion() {
        return horasMaxConduccion;
    }

    public void setHorasMaxConduccion(BigDecimal horasMaxConduccion) {
        this.horasMaxConduccion = horasMaxConduccion;
    }

    public BigDecimal getHorasMinDescanso() {
        return horasMinDescanso;
    }

    public void setHorasMinDescanso(BigDecimal horasMinDescanso) {
        this.horasMinDescanso = horasMinDescanso;
    }

    public EstadoOperador getEstado() {
        return estado;
    }

    public void setEstado(EstadoOperador estado) {
        this.estado = estado;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
