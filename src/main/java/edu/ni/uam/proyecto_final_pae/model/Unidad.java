package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Camion o tractocamion de la flota: tabla unidades. */
@Entity
@Table(name = "unidades")
public class Unidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Integer id;

    @Column(name = "numero_unidad", nullable = false, unique = true, length = 20)
    private String numeroUnidad;

    @Column(name = "placas", nullable = false, unique = true, length = 15)
    private String placas;

    @Column(name = "modelo", nullable = false, length = 80)
    private String modelo;

    @Column(name = "capacidad_toneladas", precision = 8, scale = 2)
    private BigDecimal capacidadToneladas;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_estado_unidad", nullable = false)
    private EstadoUnidad estado;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public Unidad() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumeroUnidad() {
        return numeroUnidad;
    }

    public void setNumeroUnidad(String numeroUnidad) {
        this.numeroUnidad = numeroUnidad;
    }

    public String getPlacas() {
        return placas;
    }

    public void setPlacas(String placas) {
        this.placas = placas;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public BigDecimal getCapacidadToneladas() {
        return capacidadToneladas;
    }

    public void setCapacidadToneladas(BigDecimal capacidadToneladas) {
        this.capacidadToneladas = capacidadToneladas;
    }

    public EstadoUnidad getEstado() {
        return estado;
    }

    public void setEstado(EstadoUnidad estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    @Override
    public String toString() {
        return numeroUnidad;
    }
}
