package edu.ni.uam.proyecto_final_pae.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Bitacora de jornada del operador (RF-06 a RF-08): tabla registro_jornada.
 *
 * <p>Cada fila es un evento del viaje: INICIO_TRAYECTO, PAUSA_DESCANSO, REANUDAR o
 * CONFIRMAR_ENTREGA. Con esa secuencia el modulo calcula las horas de manejo y de
 * descanso acumuladas y el semaforo del viaje.</p>
 *
 * <p>{@code num_evento} es el consecutivo por viaje y lo asigna el trigger
 * {@code trg_rj_num_evento} de PostgreSQL, por eso se mapea de solo lectura
 * (insertable/updatable = false): Java nunca lo escribe.</p>
 *
 * <p><b>Pendiente de integracion (Persona 3):</b> la columna {@code id_viaje} se
 * mapea como entero porque la entidad {@code Viaje} vive en la rama de la Persona 3.
 * Cuando esa rama se integre solo hay que reemplazar el campo {@code idViaje} por:</p>
 * <pre>
 *   &#64;ManyToOne(fetch = FetchType.LAZY, optional = false)
 *   &#64;JoinColumn(name = "id_viaje", nullable = false)
 *   private Viaje viaje;
 * </pre>
 * <p>y ajustar el constructor. La columna de la base y el esquema no cambian.</p>
 */
@Entity
@Table(name = "registro_jornada")
public class RegistroJornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer id;

    /** FK a viajes.id_viaje (ver nota de la clase). */
    @Column(name = "id_viaje", nullable = false)
    private Integer idViaje;

    /** Consecutivo por viaje; lo calcula la base de datos. */
    @Column(name = "num_evento", nullable = false)
    private Integer numEvento;

    /** Catalogo pequeno: EAGER para leerlo fuera de la sesion sin LazyInitializationException. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_evento", nullable = false)
    private TipoEvento tipoEvento;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "observacion", length = 200)
    private String observacion;

    public RegistroJornada() {
    }

    public RegistroJornada(Integer idViaje, TipoEvento tipoEvento, LocalDateTime fechaHora, String observacion) {
        this.idViaje = idViaje;
        this.tipoEvento = tipoEvento;
        this.fechaHora = fechaHora;
        this.observacion = observacion;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(Integer idViaje) {
        this.idViaje = idViaje;
    }

    public void setNumEvento(Integer numEvento) {
        this.numEvento = numEvento;
    }

    public Integer getNumEvento() {
        return numEvento;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistroJornada otro)) return false;
        return id != null && Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "#" + numEvento + " " + tipoEvento;
    }
}
