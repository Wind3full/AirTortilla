package org.AirTortilla;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="auditoria")
public class AuditEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String username;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(name = "registro_afectado", nullable = false, length = 120)
    private String registroAfectado;

    @Column(nullable = false)
    private Instant fecha;

    protected AuditEntry() {
    }

    public AuditEntry(String username, String accion, String registroAfectado) {
        this.username = username;
        this.accion = accion;
        this.registroAfectado = registroAfectado;
        this.fecha = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAccion() {
        return accion;
    }

    public String getRegistroAfectado() {
        return registroAfectado;
    }

    public Instant getFecha() {
        return fecha;
    }
}
