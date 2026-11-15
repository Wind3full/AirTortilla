package org.AirTortilla;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
public class AirTortillaController {
    private final ReservaRepository reservas;
    private final PaisRepository paises;
    private final UserRepository users;
    private final AuditRepository audit;

    public AirTortillaController(
            ReservaRepository reservas,
            PaisRepository paises,
            UserRepository users,
            AuditRepository audit
    ) {
        this.reservas = reservas;
        this.paises = paises;
        this.users = users;
        this.audit = audit;
    }

    public record ReservaView(
            Long id,
            String destino,
            String origen,
            String nombrePasajero,
            LocalDate fechaReserva,
            String localizador
    ) {
    }

    public record ReservaRequest(
            @NotBlank String destino,
            @NotBlank String origen,
            @NotBlank String nombrePasajero,
            @NotNull LocalDate fechaReserva,
            @NotBlank String localizador
    ) {
    }

    public record UserRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String role
    ) {
    }

    public record UserView(Long id, String username, String role) {
    }

    public record AuditView(
            Long id,
            String username,
            String accion,
            String registroAfectado,
            java.time.Instant fecha
    ) {
    }

    @GetMapping("/reservas")
    public List<ReservaView> listReservations(
            @RequestParam(required = false) String destino
    ) {
        List<ReservaEntity> results;
        if (destino == null || destino.isBlank()) {
            results = reservas.findAllByOrderByIdAsc();
        } else {
            results = reservas.findByDestinoNombreIgnoreCaseOrderByIdAsc(
                    destino.trim()
            );
        }

        return results.stream()
                .map(this::toReservaView)
                .toList();
    }

    @PostMapping("/reservas")
    @Transactional
    public ReservaView createReservation(
            @Valid @RequestBody ReservaRequest request,
            Authentication authentication
    ) {
        Pais destination = paises.findByNombreIgnoreCase(
                        request.destino().trim()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Destino desconocido"
                ));

        String locator = request.localizador().trim();
        String origin = request.origen().trim();
        String passengerName = request.nombrePasajero().trim();

        var existingReservation = reservas.findByLocalizador(locator);
        if (existingReservation.isPresent()) {
            ReservaEntity saved = existingReservation.get();
            if (isSameReservation(
                    saved,
                    destination,
                    origin,
                    passengerName,
                    request.fechaReserva()
            )) {
                return toReservaView(saved);
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El localizador ya pertenece a otra reserva"
            );
        }

        ReservaEntity reservation = new ReservaEntity(
                destination,
                origin,
                passengerName,
                request.fechaReserva(),
                locator
        );
        ReservaEntity savedReservation = reservas.save(reservation);

        audit.save(new AuditEntry(
                authentication.getName(),
                "ALTA_RESERVA",
                savedReservation.getLocalizador()
        ));

        return toReservaView(savedReservation);
    }

    @PostMapping("/users")
    @Transactional
    public UserView createUser(
            @Valid @RequestBody UserRequest request,
            Authentication authentication
    ) {
        if (request.password().length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres"
            );
        }

        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario ya existe"
            );
        }

        AppUser.Role role;
        try {
            role = AppUser.Role.valueOf(
                    request.role().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rol inválido"
            );
        }

        AppUser user = users.save(new AppUser(
                username,
                request.password(),
                role
        ));

        audit.save(new AuditEntry(
                authentication.getName(),
                "ALTA_USUARIO",
                user.getUsername()
        ));

        return new UserView(user.getId(), user.getUsername(), user.getRole().name());
    }

    @GetMapping("/audit")
    public List<AuditView> listAuditEntries() {
        return audit.findAllByOrderByFechaDesc()
                .stream()
                .map(entry -> new AuditView(
                        entry.getId(),
                        entry.getUsername(),
                        entry.getAccion(),
                        entry.getRegistroAfectado(),
                        entry.getFecha()
                ))
                .toList();
    }

    @GetMapping("/reservas/csv")
    public ResponseEntity<byte[]> downloadCsvFiles() throws Exception {
        Map<String, List<ReservaEntity>> reservationsByFile = groupByCsvFile();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(
                output,
                StandardCharsets.UTF_8
        )) {
            for (Map.Entry<String, List<ReservaEntity>> group
                    : reservationsByFile.entrySet()) {
                zip.putNextEntry(new ZipEntry(group.getKey()));
                zip.write(buildCsv(group.getValue()));
                zip.closeEntry();
            }
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=AirTortilla_CSV.zip"
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(output.toByteArray());
    }

    private ReservaView toReservaView(ReservaEntity reservation) {
        return new ReservaView(
                reservation.getId(),
                reservation.getDestino().getNombre(),
                reservation.getOrigen(),
                reservation.getNombrePasajero(),
                reservation.getFechaReserva(),
                reservation.getLocalizador()
        );
    }

    private boolean isSameReservation(
            ReservaEntity saved,
            Pais destination,
            String origin,
            String passengerName,
            LocalDate reservationDate
    ) {
        return saved.getDestino().getId().equals(destination.getId())
                && saved.getOrigen().equals(origin)
                && saved.getNombrePasajero().equals(passengerName)
                && saved.getFechaReserva().equals(reservationDate);
    }

    private Map<String, List<ReservaEntity>> groupByCsvFile() {
        Map<String, List<ReservaEntity>> grouped = new TreeMap<>();

        for (ReservaEntity reservation : reservas.findAllByOrderByIdAsc()) {
            String filename = String.format(
                    "AirTortilla_%s_%s.csv",
                    reservation.getDestino().getCodigoIso(),
                    reservation.getFechaReserva().toString().replace('-', '_')
            );

            grouped.computeIfAbsent(filename, key -> new ArrayList<>())
                    .add(reservation);
        }

        return grouped;
    }

    private byte[] buildCsv(List<ReservaEntity> reservations) {
        StringBuilder csv = new StringBuilder(
                "Destino,Origen,NombrePasajero,FechaReserva,Localizador\n"
        );

        for (ReservaEntity reservation : reservations) {
            csv.append(reservation.getDestino().getNombre())
                    .append(',')
                    .append(reservation.getOrigen())
                    .append(',')
                    .append(reservation.getNombrePasajero())
                    .append(',')
                    .append(reservation.getFechaReserva())
                    .append(',')
                    .append(reservation.getLocalizador())
                    .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
