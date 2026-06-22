package com.sena.agendasena.service;

import com.sena.agendasena.dto.OcupacionReporteDto;
import com.sena.agendasena.dto.ReservaRequest;
import com.sena.agendasena.exception.ConflictoHorarioException;
import com.sena.agendasena.exception.RecursoNoEncontradoException;
import com.sena.agendasena.exception.ReglaNegocioException;
import com.sena.agendasena.model.Ambiente;
import com.sena.agendasena.model.EstadoReserva;
import com.sena.agendasena.model.Reserva;
import com.sena.agendasena.repository.AmbienteRepository;
import com.sena.agendasena.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    // Los repositorios se inyectan a través del constructor, garantizando la inmutabilidad y facilitando las pruebas unitarias.
    private final ReservaRepository reservaRepository;
    private final AmbienteRepository ambienteRepository;

    public ReservaService(ReservaRepository reservaRepository, AmbienteRepository ambienteRepository) {
        this.reservaRepository = reservaRepository;
        this.ambienteRepository = ambienteRepository;
    }

    /**
     * Crea una nueva reserva aplicando las validaciones de las reglas de negocio.
     *
     * @param request Datos de la reserva solicitada (DTO)
     * @return La reserva creada y guardada en base de datos
     */
    public Reserva crearReserva(ReservaRequest request) {
        // 1. Obtener el ambiente de la base de datos o lanzar excepción 404 si no existe.
        Ambiente ambiente = ambienteRepository.findById(request.ambienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente no encontrado con ID: " + request.ambienteId()));

        // Validar que los campos requeridos no sean nulos o vacíos
        validarCamposBasicos(request);

        // 2. Validar cada una de las Reglas de Negocio definidas en el requerimiento
        validarReglasDeNegocio(request, ambiente);

        // 3. Si todas las validaciones pasan, creamos la entidad con estado ACTIVA
        Reserva reserva = new Reserva(
                ambiente,
                request.nombreInstructor(),
                request.fechaHoraInicio(),
                request.fechaHoraFin(),
                request.numeroAprendices(),
                EstadoReserva.ACTIVA
        );

        // Guardar y retornar la reserva persistida
        return reservaRepository.save(reserva);
    }

    /**
     * Cancela una reserva existente si cumple con las restricciones de anticipación.
     *
     * @param id ID de la reserva a cancelar
     * @return La reserva con el estado modificado a CANCELADA
     */
    public Reserva cancelarReserva(Long id) {
        // Buscar la reserva por ID
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con ID: " + id));

        // Validar si ya está cancelada
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ReglaNegocioException("La reserva ya se encuentra cancelada.");
        }

        // Regla 6: Cancelación con al menos 2 horas de anticipación.
        // Compara si la hora actual más 2 horas es posterior a la hora de inicio de la reserva.
        if (LocalDateTime.now().plusHours(2).isAfter(reserva.getFechaHoraInicio())) {
            throw new ReglaNegocioException("Una reserva solo puede cancelarse si faltan al menos 2 horas para su inicio.");
        }

        // Cambiar el estado y persistir
        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    /**
     * Obtiene la lista de reservas activas para un ambiente y fecha determinados.
     */
    public List<Reserva> listarReservasActivasPorAmbienteYFecha(Long ambienteId, LocalDate fecha) {
        if (!ambienteRepository.existsById(ambienteId)) {
            throw new RecursoNoEncontradoException("Ambiente no encontrado con ID: " + ambienteId);
        }
        // Buscar reservas activas en el rango completo del día solicitado (de 00:00:00 a 23:59:59)
        return reservaRepository.findActiveReservationsByAmbienteAndDate(
                ambienteId, 
                fecha.atStartOfDay(), 
                fecha.atTime(LocalTime.MAX)
        );
    }

    /**
     * Genera un reporte de ocupación para todos los ambientes en una fecha específica,
     * calculando las horas ocupadas y el porcentaje basado en la jornada institucional (16 horas).
     */
    public List<OcupacionReporteDto> generarReporteOcupacion(LocalDate fecha) {
        // 1. Obtener todas las reservas activas o finalizadas del día
        List<Reserva> reservasDelDia = reservaRepository.findReservationsForReport(
                fecha.atStartOfDay(), 
                fecha.atTime(LocalTime.MAX)
        );

        // 2. Agrupar las reservas por el ID del ambiente para facilitar los cálculos
        Map<Long, List<Reserva>> reservasPorAmbiente = reservasDelDia.stream()
                .collect(Collectors.groupingBy(r -> r.getAmbiente().getId()));

        // 3. Obtener todos los ambientes registrados para incluirlos en el reporte (incluso si tienen 0% ocupación)
        List<Ambiente> todosLosAmbientes = ambienteRepository.findAll();
        List<OcupacionReporteDto> reporte = new ArrayList<>();

        for (Ambiente a : todosLosAmbientes) {
            List<Reserva> reservas = reservasPorAmbiente.getOrDefault(a.getId(), new ArrayList<>());
            
            // Sumar la duración en horas de cada reserva de este ambiente
            double totalHoras = reservas.stream()
                    .mapToDouble(r -> Duration.between(r.getFechaHoraInicio(), r.getFechaHoraFin()).toMinutes() / 60.0)
                    .sum();

            // Calcular el porcentaje basado en 16 horas de jornada institucional (6:00 a 22:00)
            double porcentaje = (totalHoras / 16.0) * 100.0;
            
            // Redondear a 2 decimales para una respuesta limpia en la API
            totalHoras = Math.round(totalHoras * 100.0) / 100.0;
            porcentaje = Math.round(porcentaje * 100.0) / 100.0;

            reporte.add(new OcupacionReporteDto(a.getId(), a.getNombre(), totalHoras, porcentaje));
        }

        return reporte;
    }

    // --- Métodos Privados de Validación Auxiliares ---

    /**
     * Valida que los datos básicos de la solicitud estén presentes y sean correctos.
     */
    private void validarCamposBasicos(ReservaRequest request) {
        if (request.fechaHoraInicio() == null || request.fechaHoraFin() == null) {
            throw new ReglaNegocioException("La fecha/hora de inicio y fin son obligatorias.");
        }
        if (request.nombreInstructor() == null || request.nombreInstructor().trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del instructor es obligatorio.");
        }
        if (request.numeroAprendices() <= 0) {
            throw new ReglaNegocioException("El número de aprendices debe ser mayor a cero.");
        }
    }

    /**
     * Valida detalladamente cada una de las 7 reglas de negocio.
     */
    private void validarReglasDeNegocio(ReservaRequest request, Ambiente ambiente) {
        LocalDateTime inicio = request.fechaHoraInicio();
        LocalDateTime fin = request.fechaHoraFin();

        // Validación preventiva: inicio debe ser anterior a fin
        if (inicio.isAfter(fin) || inicio.isEqual(fin)) {
            throw new ReglaNegocioException("La fecha y hora de inicio debe ser anterior a la fecha y hora de fin.");
        }

        // Regla 7: No se reserva en el pasado (la fecha de inicio debe ser posterior al momento actual)
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha y hora de inicio debe ser posterior al momento actual.");
        }

        // Regla 3: Horario institucional (Entre las 06:00 y las 22:00)
        if (inicio.toLocalTime().isBefore(LocalTime.of(6, 0)) || fin.toLocalTime().isAfter(LocalTime.of(22, 0))) {
            throw new ReglaNegocioException("Las reservas solo pueden estar dentro del horario institucional (entre las 06:00 y las 22:00).");
        }

        // Regla 3: Debe iniciar y terminar el mismo día
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReglaNegocioException("La reserva debe iniciar y terminar el mismo día.");
        }

        // Regla 3: Duración debe ser entre 1 y 4 horas (60 a 240 minutos)
        long duracionMinutos = Duration.between(inicio, fin).toMinutes();
        if (duracionMinutos < 60 || duracionMinutos > 240) {
            throw new ReglaNegocioException("La reserva debe durar entre 1 y 4 horas.");
        }

        // Regla 4: Ambiente inactivo (no se puede reservar)
        if (!ambiente.isActivo()) {
            throw new ReglaNegocioException("No se puede reservar un ambiente que se encuentra inactivo.");
        }

        // Regla 2: Capacidad (el número de aprendices no puede superar la capacidad máxima del ambiente)
        if (request.numeroAprendices() > ambiente.getCapacidad()) {
            throw new ReglaNegocioException("El número de aprendices (" + request.numeroAprendices() + 
                    ") supera la capacidad máxima permitida del ambiente (" + ambiente.getCapacidad() + ").");
        }

        // Regla 5: Límite por instructor (Máximo 3 reservas activas en el mismo día)
        long reservasDelInstructor = reservaRepository.countInstructorReservationsOnDay(
                request.nombreInstructor(), 
                inicio.toLocalDate().atStartOfDay(), 
                inicio.toLocalDate().atTime(LocalTime.MAX)
        );
        if (reservasDelInstructor >= 3) {
            throw new ReglaNegocioException("El instructor " + request.nombreInstructor() + 
                    " ya tiene el límite máximo de 3 reservas activas para el día " + inicio.toLocalDate() + ".");
        }

        // Regla 1: Sin cruces de horario (Solapamiento temporal en el mismo ambiente con otras reservas activas)
        long cruces = reservaRepository.countOverlappingReservations(ambiente.getId(), inicio, fin);
        if (cruces > 0) {
            throw new ConflictoHorarioException("Conflicto de horario: El ambiente ya se encuentra reservado en el rango horario solicitado.");
        }
    }
}
