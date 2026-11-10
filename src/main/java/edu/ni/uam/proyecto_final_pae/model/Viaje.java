package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "viajes")
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_viaje")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_unidad", nullable = false)
    private Unidad unidad;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_operador", nullable = false)
    private Operador operador;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_carga", nullable = false)
    private Carga carga;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "origen", nullable = false)
    private Ubicacion origen;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destino", nullable = false)
    private Ubicacion destino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_viaje_previo")
    private Viaje viajePrevio;

    @Column(name = "tiempo_estimado", nullable = false, precision = 6, scale = 2)
    private BigDecimal tiempoEstimado;

    @Column(name = "cobro_viaje", nullable = false, precision = 12, scale = 2)
    private BigDecimal cobroViaje;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_estado_viaje", nullable = false)
    private EstadoViaje estado;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Viaje() {
    }


    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Unidad getUnidad() { return unidad; }
    public void setUnidad(Unidad unidad) { this.unidad = unidad; }

    public Operador getOperador() { return operador; }
    public void setOperador(Operador operador) { this.operador = operador; }

    public Carga getCarga() { return carga; }
    public void setCarga(Carga carga) { this.carga = carga; }

    public Ubicacion getOrigen() { return origen; }
    public void setOrigen(Ubicacion origen) { this.origen = origen; }

    public Ubicacion getDestino() { return destino; }
    public void setDestino(Ubicacion destino) { this.destino = destino; }

    public Viaje getViajePrevio() { return viajePrevio; }
    public void setViajePrevio(Viaje viajePrevio) { this.viajePrevio = viajePrevio; }

    public BigDecimal getTiempoEstimado() { return tiempoEstimado; }
    public void setTiempoEstimado(BigDecimal tiempoEstimado) { this.tiempoEstimado = tiempoEstimado; }

    public BigDecimal getCobroViaje() { return cobroViaje; }
    public void setCobroViaje(BigDecimal cobroViaje) { this.cobroViaje = cobroViaje; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public EstadoViaje getEstado() { return estado; }
    public void setEstado(EstadoViaje estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Viaje otro)) return false;
        return id != null && Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Viaje #" + id + " (" + origen + " → " + destino + ")";
    }
}