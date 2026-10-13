package org.AirTortilla;

public class Reserva {
    private String destino;
    private String origen;
    private String nombrePasajero;
    private String fechaReserva;
    private String localizador;

    public Reserva(String destino, String origen,
                   String nombrePasajero, String fechaReserva,
                   String localizador) {
        this.destino = destino;
        this.origen = origen;
        this.nombrePasajero = nombrePasajero;
        this.fechaReserva = fechaReserva;
        this.localizador = localizador;
    }

    public String getDestino() {
        return destino;
    }

    public String getOrigen() {
        return origen;
    }

    public String getNombrePasajero() {
        return nombrePasajero;
    }

    public String getFechaReserva() {
        return fechaReserva;
    }

    public String getLocalizador() {
        return localizador;
    }

    @Override
    public String toString() {
        return "Localizador: " + localizador
                + " | Pasajero: " + nombrePasajero
                + " | Origen: " + origen
                + " | Destino: " + destino
                + " | Fecha: " + fechaReserva;
    }
}
