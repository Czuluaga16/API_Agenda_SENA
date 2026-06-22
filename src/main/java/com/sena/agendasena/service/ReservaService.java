package com.sena.agendasena.service;

import com.sena.agendasena.dto.*;
import com.sena.agendasena.exception.*;
import com.sena.agendasena.model.*;
import com.sena.agendasena.repository.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AmbienteRepository ambienteRepository;

    // Constructor estándar para inyectar dependencias sin requerir Lombok
    public ReservaService(ReservaRepository reservaRepository, AmbienteRepository ambienteRepository) {
        this.reservaRepository = reservaRepository;
        this.ambienteRepository = ambienteRepository;
    }

    public Reserva crearReserva(ReservaRequest request) {
        Ambiente ambiente = ambienteRepository.findById(request.ambienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente no encontrado: " + request.ambienteId()));

        validarReglasDeNegocio(request, ambiente);

        return reservaRepository.save(new Reserva(ambiente, request.nombreInstructor(), 
                request.fechaHoraInicio(), request.fechaHoraFin(), request.numeroAprendices(), EstadoReserva.ACTIVA));
    }

    public Reserva cancelarReserva(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada: " + id));

        if (reserva.getEstado() == EstadoReserva.CANCELADA) throw new ReglaNegocioException("Ya cancelada.");
        if (LocalDateTime.now().plusHours(2).isAfter(reserva.getFechaHoraInicio())) 
            throw new ReglaNegocioException("Anticipación mínima de 2 horas requerida.");

        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    public List<Reserva> listarReservasActivasPorAmbienteYFecha(Long ambienteId, LocalDate fecha) {
        if (!ambienteRepository.existsById(ambienteId)) {
            throw new RecursoNoEncontradoException("Ambiente no encontrado: " + ambienteId);
        }
        return reservaRepository.findActiveReservationsByAmbienteAndDate(
                ambienteId, 
                fecha.atStartOfDay(), 
                fecha.atTime(LocalTime.MAX)
        );
    }

    public List<OcupacionReporteDto> generarReporteOcupacion(LocalDate fecha) {
        var reservasPorAmbiente = reservaRepository.findReservationsForReport(fecha.atStartOfDay(), fecha.atTime(LocalTime.MAX))
                .stream().collect(Collectors.groupingBy(r -> r.getAmbiente().getId()));

        return ambienteRepository.findAll().stream().map(a -> {
            double horas = reservasPorAmbiente.getOrDefault(a.getId(), List.of()).stream()
                    .mapToDouble(r -> Duration.between(r.getFechaHoraInicio(), r.getFechaHoraFin()).toMinutes() / 60.0).sum();
            return new OcupacionReporteDto(a.getId(), a.getNombre(), Math.round(horas * 100.0) / 100.0, Math.round((horas / 16.0) * 10000.0) / 100.0);
        }).toList();
    }

    private void validarReglasDeNegocio(ReservaRequest r, Ambiente a) {
        if (r.fechaHoraInicio().isAfter(r.fechaHoraFin()) || !r.fechaHoraInicio().toLocalDate().equals(r.fechaHoraFin().toLocalDate()))
            throw new ReglaNegocioException("Fechas inválidas o no coinciden.");
        if (r.fechaHoraInicio().isBefore(LocalDateTime.now())) throw new ReglaNegocioException("No puedes reservar en el pasado.");
        if (r.fechaHoraInicio().getHour() < 6 || r.fechaHoraFin().getHour() > 22) throw new ReglaNegocioException("Fuera de horario (06:00-22:00).");
        long mins = Duration.between(r.fechaHoraInicio(), r.fechaHoraFin()).toMinutes();
        if (mins < 60 || mins > 240) throw new ReglaNegocioException("Duración entre 1 y 4 horas.");
        if (!a.isActivo()) throw new ReglaNegocioException("Ambiente inactivo.");
        if (r.numeroAprendices() > a.getCapacidad()) throw new ReglaNegocioException("Capacidad superada.");
        if (reservaRepository.countInstructorReservationsOnDay(r.nombreInstructor(), r.fechaHoraInicio().toLocalDate().atStartOfDay(), r.fechaHoraInicio().toLocalDate().atTime(LocalTime.MAX)) >= 3)
            throw new ReglaNegocioException("Límite de 3 reservas diario alcanzado.");
        if (reservaRepository.countOverlappingReservations(a.getId(), r.fechaHoraInicio(), r.fechaHoraFin()) > 0)
            throw new ConflictoHorarioException("Conflicto de horario.");
    }
}