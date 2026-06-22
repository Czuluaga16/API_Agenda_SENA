package com.sena.agendasena.config;

import com.sena.agendasena.model.Ambiente;
import com.sena.agendasena.model.EstadoReserva;
import com.sena.agendasena.model.Reserva;
import com.sena.agendasena.model.TipoAmbiente;
import com.sena.agendasena.repository.AmbienteRepository;
import com.sena.agendasena.repository.ReservaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(AmbienteRepository ambienteRepository, ReservaRepository reservaRepository) {
        return args -> {
            // Load 4 environments (ambientes)
            Ambiente sala101 = new Ambiente("Sala 101 - Sistemas", TipoAmbiente.SALA, 15, true);
            Ambiente lab201 = new Ambiente("Laboratorio 201 - Quimica", TipoAmbiente.LABORATORIO, 30, true);
            Ambiente auditorioA = new Ambiente("Auditorio Central A", TipoAmbiente.AUDITORIO, 100, true);
            Ambiente sala102Mantenimiento = new Ambiente("Sala 102 - Mantenimiento", TipoAmbiente.SALA, 10, false);

            ambienteRepository.save(sala101);
            ambienteRepository.save(lab201);
            ambienteRepository.save(auditorioA);
            ambienteRepository.save(sala102Mantenimiento);

            System.out.println("--- Ambientes de prueba inicializados correctamente ---");

            LocalDate tomorrow = LocalDate.now().plusDays(1);

            // Reserva 1: Sala 101, Tomorrow 08:00 - 10:00, Instructor: Carlos Zuluaga, 12 apprentices
            Reserva r1 = new Reserva(
                    sala101,
                    "Carlos Zuluaga",
                    tomorrow.atTime(8, 0),
                    tomorrow.atTime(10, 0),
                    12,
                    EstadoReserva.ACTIVA
            );

            // Reserva 2: Lab 201, Tomorrow 14:00 - 16:30, Instructor: Maria Gomez, 25 apprentices
            Reserva r2 = new Reserva(
                    lab201,
                    "Maria Gomez",
                    tomorrow.atTime(14, 0),
                    tomorrow.atTime(16, 30),
                    25,
                    EstadoReserva.ACTIVA
            );

            // Reserva 3: Auditorio A, starts in 30 minutes (to test Rule 6: cancellation error)
            Reserva r3 = new Reserva(
                    auditorioA,
                    "Juan Pérez",
                    LocalDateTime.now().plusMinutes(30),
                    LocalDateTime.now().plusMinutes(150),
                    40,
                    EstadoReserva.ACTIVA
            );

            reservaRepository.save(r1);
            reservaRepository.save(r2);
            reservaRepository.save(r3);

            System.out.println("--- Reservas de prueba inicializadas correctamente ---");
        };
    }
}
