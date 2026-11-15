package org.AirTortilla;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "reservas",
        uniqueConstraints = @UniqueConstraint(columnNames = "localizador")
)
public class ReservaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "destino_id", nullable = false)
    private Pais destino;

    @Column(nullable = false, length = 100)
    private String origen;

    @Column(name = "nombre_pasajero", nullable = false, length = 150)
    private String nombrePasajero;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @Column(nullable = false, length = 30)
    private String localizador;

    protected ReservaEntity() {
    }

    public ReservaEntity(
            Pais destino,
            String origen,
            String nombrePasajero,
            LocalDate fechaReserva,
            String localizador
    ) {
        this.destino = destino;
        this.origen = origen;
        this.nombrePasajero = nombrePasajero;
        this.fechaReserva = fechaReserva;
        this.localizador = localizador;
    }

    public Long getId() {
        return id;
    }

    public Pais getDestino() {
        return destino;
    }

    public String getOrigen() {
        return origen;
    }

    public String getNombrePasajero() {
        return nombrePasajero;
    }

    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    public String getLocalizador() {
        return localizador;
    }
}
