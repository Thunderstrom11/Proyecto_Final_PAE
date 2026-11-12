package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Tiempo de viaje configurado entre dos ubicaciones. */
@Entity
@Table(name = "rutas", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ruta_origen_destino", columnNames = {"id_origen", "id_destino"})
})
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_origen", nullable = false)
    private Ubicacion origen;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_destino", nullable = false)
    private Ubicacion destino;

    @Column(name = "tiempo_estimado_horas", nullable = false, precision = 6, scale = 2)
    private BigDecimal tiempoEstimadoHoras;

    public Ruta() {
    }

    public Ruta(Ubicacion origen, Ubicacion destino, BigDecimal tiempoEstimadoHoras) {
        this.origen = origen;
        this.destino = destino;
        this.tiempoEstimadoHoras = tiempoEstimadoHoras;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Ubicacion getOrigen() {
        return origen;
    }

    public void setOrigen(Ubicacion origen) {
        this.origen = origen;
    }

    public Ubicacion getDestino() {
        return destino;
    }

    public void setDestino(Ubicacion destino) {
        this.destino = destino;
    }

    public BigDecimal getTiempoEstimadoHoras() {
        return tiempoEstimadoHoras;
    }

    public void setTiempoEstimadoHoras(BigDecimal tiempoEstimadoHoras) {
        this.tiempoEstimadoHoras = tiempoEstimadoHoras;
    }

    @Override
    public String toString() {
        return origen + " → " + destino;
    }
}
