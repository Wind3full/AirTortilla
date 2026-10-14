package org.AirTortilla;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {
        GestorReservas gestor = new GestorReservas();

        try (Scanner teclado = new Scanner(System.in)) {
            int opcion;

            do {
                System.out.println("\n========== AirTortilla ==========");
                System.out.println("1. Mostrar todas las reservas");
                System.out.println("2. Consultar reservas por destino");
                System.out.println("3. Generar todos los ficheros CSV");
                System.out.println("4. Procesar reservas línea a línea");
                System.out.println("5. Salir");
                System.out.print("Selecciona una opción: ");

                if (!teclado.hasNextInt()) {
                    System.out.println("Introduce un número válido.");
                    teclado.nextLine();
                    opcion = 0;
                    continue;
                }

                opcion = teclado.nextInt();
                teclado.nextLine();

                try {
                    switch (opcion) {
                        case 1 -> {
                            List<Reserva> reservas = gestor.leerReservas();

                            if (reservas.isEmpty()) {
                                System.out.println("No hay reservas.");
                            } else {
                                for (Reserva reserva : reservas) {
                                    System.out.println(reserva);
                                }
                            }
                        }

                        case 2 -> {
                            System.out.print("Introduce el destino: ");
                            String destino = teclado.nextLine().trim();

                            List<Reserva> reservas = gestor.leerReservas();

                            gestor.consultarPorDestino(reservas, destino);
                        }

                        case 3 -> gestor.generarTodosLosFicheros();

                        case 4 -> gestor.generarLineaALinea(teclado);

                        case 5 -> System.out.println("Cerrando AirTortilla...");

                        case 0 -> {
                            // Entrada no válida.
                        }

                        default ->
                                System.out.println("Opción no válida.");
                    }

                } catch (IOException | IllegalArgumentException e) {
                    System.out.println("Error: " + e.getMessage());
                }

            } while (opcion != 5);
        }

    }
}