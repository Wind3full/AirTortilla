package org.AirTortilla;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.text.Normalizer;
import java.util.Locale;

public class GestorReservas {

    private final Path archivoEntrada = Paths.get("..","res", "Reservas.csv");

    private final Path carpetaSalida = Paths.get("..","salida");

    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Path archivoPaises = Paths.get("..", "res", "Paises.csv");

    private Map<String, String> paises;

    public List<Reserva> leerReservas() throws IOException {
        List<Reserva> reservas = new ArrayList<>();

        try (BufferedReader lector = Files.newBufferedReader(
                archivoEntrada, StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                // Saltamos la cabecera.
                if (numeroLinea == 1) {
                    continue;
                }

                if (linea.isBlank()) {
                    continue;
                }

                try {
                    reservas.add(convertirReserva(linea));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Error en la línea " + numeroLinea + ": " + e.getMessage(), e);
                }
            }
        }

        return reservas;
    }

    private Reserva convertirReserva(String linea) {
        String[] campos = linea.split(",", -1);

        if (campos.length != 5) {
            throw new IllegalArgumentException("Se esperaban 5 campos y se encontraron " + campos.length);
        }

        String destino = campos[0].trim();
        String origen = campos[1].trim();
        String nombre = campos[2].trim();
        String fecha = campos[3].trim();
        String localizador = campos[4].trim();

        if (destino.isEmpty() || origen.isEmpty() || nombre.isEmpty() || fecha.isEmpty() || localizador.isEmpty()) {
            throw new IllegalArgumentException("Hay campos obligatorios vacíos");
        }

        try {
            LocalDate.parse(fecha, formatoFecha);
        } catch (Exception e) {
            throw new IllegalArgumentException("La fecha debe tener formato YYYY-MM-DD");
        }

        return new Reserva(destino, origen, nombre, fecha, localizador);
    }

    public void consultarPorDestino(
            List<Reserva> reservas, String destino) {

        boolean encontrado = false;

        for (Reserva reserva : reservas) {
            if (reserva.getDestino().equalsIgnoreCase(destino)) {
                System.out.println(reserva);
                encontrado = true;
            }
        }

        if (!encontrado) {
            System.out.println("No se encontraron reservas para ese destino.");
        }
    }

    public void generarTodosLosFicheros() throws IOException {
        List<Reserva> reservas = leerReservas();

        prepararCarpetaSalida();

        Map<String, List<Reserva>> grupos = new HashMap<>();

        for (Reserva reserva : reservas) {
            String nombreArchivo = obtenerNombreArchivo(reserva);

            grupos.computeIfAbsent(nombreArchivo, clave -> new ArrayList<>()).add(reserva);
        }

        for (Map.Entry<String, List<Reserva>> grupo : grupos.entrySet()) {

            Path archivo = carpetaSalida.resolve(grupo.getKey());

            try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {

                escribirCabecera(escritor);

                for (Reserva reserva : grupo.getValue()) {
                    escribirReserva(escritor, reserva);
                }
            }

            System.out.println("Generado: " + archivo);
        }

        System.out.println("Proceso finalizado. Ficheros generados: " + grupos.size());
    }

    public void generarLineaALinea(Scanner teclado) throws IOException {

        prepararCarpetaSalida();

        int procesadas = 0;

        try (BufferedReader lector = Files.newBufferedReader(archivoEntrada, StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                // Saltar cabecera y líneas vacías
                if (numeroLinea == 1 || linea.isBlank()) {
                    continue;
                }

                Reserva reserva;

                try {
                    reserva = convertirReserva(linea);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Error en la línea " + numeroLinea + ": " + e.getMessage(), e);
                }

                // Mostrar la reserva antes de guardarla
                System.out.println("\n================================");
                System.out.println("RESERVA " + (procesadas + 1));
                System.out.println("================================");
                System.out.println(reserva);

                System.out.println("\nPulsa ENTER para guardar esta reserva...");
                teclado.nextLine();

                // Obtener el fichero correspondiente
                Path archivo = carpetaSalida.resolve(obtenerNombreArchivo(reserva));

                boolean existe = Files.exists(archivo);

                // Guardar la reserva después de pulsar ENTER
                try (BufferedWriter escritor = Files.newBufferedWriter(
                        archivo,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)) {

                    if (!existe) {
                        escribirCabecera(escritor);
                    }

                    escribirReserva(escritor, reserva);
                }

                procesadas++;

                System.out.println("Reserva guardada correctamente.");
            }
        }

        System.out.println("\nSe han procesado " + procesadas
                + " reservas.");
        System.out.println("Proceso finalizado.");
    }

    private String obtenerNombreArchivo(Reserva reserva) throws IOException{
        String codigo = obtenerCodigoPais(reserva.getDestino());

        LocalDate fecha = LocalDate.parse(reserva.getFechaReserva(), formatoFecha);

        return String.format(
                "AirTortilla_%s_%04d_%02d_%02d.csv",
                codigo,
                fecha.getYear(),
                fecha.getMonthValue(),
                fecha.getDayOfMonth());
    }

    private String obtenerCodigoPais(String destino)
            throws IOException {

        // Cargar los países solamente la primera vez.
        if (paises == null) {
            paises = cargarPaises();
        }

        String nombreNormalizado = normalizar(destino);
        String codigo = paises.get(nombreNormalizado);

        if (codigo == null) {
            throw new IllegalArgumentException("No existe un código ISO para el destino: " + destino);
        }

        return codigo;
    }

    private Map<String, String> cargarPaises() throws IOException {

        Map<String, String> mapa = new HashMap<>();

        try (BufferedReader lector = Files.newBufferedReader(archivoPaises, StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;

                // Saltar la cabecera y las líneas vacías.
                if (numeroLinea == 1 || linea.isBlank()) {
                    continue;
                }

                String[] campos = linea.split(",", -1);

                if (campos.length != 2) {
                    throw new IOException("Formato incorrecto en Paises.csv, línea " + numeroLinea);
                }

                String pais = campos[0].trim();
                String codigo = campos[1].trim().toUpperCase(Locale.ROOT);

                if (pais.isEmpty()
                        || !codigo.matches("[A-Z]{2}")) {
                    throw new IOException("País o código ISO incorrecto en la línea " + numeroLinea);
                }

                String clave = normalizar(pais);

                if (mapa.putIfAbsent(clave, codigo) != null) {
                    throw new IOException("País duplicado en Paises.csv: " + pais);
                }
            }
        }

        return mapa;
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(
                        texto.trim().toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private void escribirCabecera(BufferedWriter escritor)
            throws IOException {
        escritor.write(
                "Destino,Origen,NombrePasajero,FechaReserva,Localizador");
        escritor.newLine();
    }

    private void escribirReserva(
            BufferedWriter escritor, Reserva reserva)
            throws IOException {

        escritor.write(
                reserva.getDestino() + ","
                        + reserva.getOrigen() + ","
                        + reserva.getNombrePasajero() + ","
                        + reserva.getFechaReserva() + ","
                        + reserva.getLocalizador());

        escritor.newLine();
    }

    private void prepararCarpetaSalida() throws IOException {
        Files.createDirectories(carpetaSalida);

        // Eliminamos los CSV anteriores para evitar duplicados.
        try (var archivos = Files.list(carpetaSalida)) {
            archivos
                    .filter(Files::isRegularFile)
                    .filter(archivo ->
                            archivo.getFileName().toString().endsWith(".csv"))
                    .forEach(archivo -> {
                        try {
                            Files.delete(archivo);
                        } catch (IOException e) {
                            throw new RuntimeException("No se pudo borrar " + archivo, e);
                        }
                    });
        }
    }
}