package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Envio con su fecha limite pactada (SLA): tabla cargas. */
@Entity
@Table(name = "cargas")
public class Carga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carga")
    private Integer id;

    @Column(name = "numero_carga", nullable = false, unique = true, length = 30)
    private String numeroCarga;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @Column(name = "peso_toneladas", nullable = false, precision = 8, scale = 2)
    private BigDecimal pesoToneladas;

    /** Opcional (id_cliente admite NULL). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @Column(name = "fecha_limite_cliente", nullable = false)
    private LocalDateTime fechaLimiteCliente;

    /** Catalogo pequeno: EAGER para que nunca falle al leerlo fuera de la sesion. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_estado_carga", nullable = false)
    private EstadoCarga estado;

    /** La llena la base de datos (DEFAULT NOW()). */
    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Carga() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumeroCarga() {
        return numeroCarga;
    }

    public void setNumeroCarga(String numeroCarga) {
        this.numeroCarga = numeroCarga;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public void setPesoToneladas(BigDecimal pesoToneladas) {
        this.pesoToneladas = pesoToneladas;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getFechaLimiteCliente() {
        return fechaLimiteCliente;
    }

    public void setFechaLimiteCliente(LocalDateTime fechaLimiteCliente) {
        this.fechaLimiteCliente = fechaLimiteCliente;
    }

    public EstadoCarga getEstado() {
        return estado;
    }

    public void setEstado(EstadoCarga estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Carga otra)) return false;
        return id != null && Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return numeroCarga;
    }
}
