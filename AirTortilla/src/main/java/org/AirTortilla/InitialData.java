package org.AirTortilla;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Component
public class InitialData implements ApplicationRunner {
    private final PaisRepository paises;
    private final ReservaRepository reservas;
    private final UserRepository users;

    @Value("${app.csv-dir:../res}")
    private String csvDirectory;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public InitialData(
            PaisRepository paises,
            ReservaRepository reservas,
            UserRepository users
    ) {
        this.paises = paises;
        this.reservas = reservas;
        this.users = users;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (paises.count() == 0 && reservas.count() == 0) {
            importCsv();
        }

        boolean hasAdminCredentials = adminUsername != null
                && !adminUsername.isBlank()
                && adminPassword != null
                && !adminPassword.isBlank();

        if (hasAdminCredentials && !users.existsByUsername(adminUsername)) {
            AppUser admin = new AppUser(
                    adminUsername,
                    adminPassword,
                    AppUser.Role.ADMINISTRADOR
            );
            users.save(admin);
        }
    }

    private void importCsv() throws Exception {
        Path directory = Path.of(csvDirectory);
        Path countryFile = directory.resolve("Paises.csv");
        Path bookingFile = directory.resolve("Reservas.csv");

        if (!Files.exists(countryFile) || !Files.exists(bookingFile)) {
            System.err.println(
                    "No se encontraron Paises.csv y Reservas.csv en "
                            + directory
                            + "; la importación se omitió."
            );
            return;
        }

        importCountries(countryFile);
        importReservations(bookingFile);

        System.out.printf(
                "Importación CSV completada: %d países y %d reservas.%n",
                paises.count(),
                reservas.count()
        );
    }

    private void importCountries(Path countryFile) throws Exception {
        List<String> lines = Files.readAllLines(countryFile, StandardCharsets.UTF_8);

        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] fields = line.split(",", -1);
            if (fields.length != 2) {
                throw new IllegalStateException(
                        "Fila inválida en Paises.csv: " + (index + 1)
                );
            }

            String name = fields[0].trim();
            String isoCode = fields[1].trim().toUpperCase(Locale.ROOT);
            paises.save(new Pais(name, isoCode));
        }
    }

    private void importReservations(Path bookingFile) throws Exception {
        List<String> lines = Files.readAllLines(bookingFile, StandardCharsets.UTF_8);

        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] fields = line.split(",", -1);
            if (fields.length != 5) {
                throw new IllegalStateException(
                        "Fila inválida en Reservas.csv: " + (index + 1)
                );
            }

            String destinationName = fields[0].trim();
            Pais destination = paises.findByNombreIgnoreCase(destinationName)
                    .orElseThrow(() -> new IllegalStateException(
                            "Destino sin país registrado: " + destinationName
                    ));

            ReservaEntity reservation = new ReservaEntity(
                    destination,
                    fields[1].trim(),
                    fields[2].trim(),
                    LocalDate.parse(fields[3].trim()),
                    fields[4].trim()
            );
            reservas.save(reservation);
        }
    }
}
