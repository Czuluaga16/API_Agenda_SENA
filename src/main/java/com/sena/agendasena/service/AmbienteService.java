package com.sena.agendasena.service;

import com.sena.agendasena.exception.ReglaNegocioException;
import com.sena.agendasena.model.Ambiente;
import com.sena.agendasena.repository.AmbienteRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AmbienteService {

    private final AmbienteRepository ambienteRepository;

    public AmbienteService(AmbienteRepository ambienteRepository) {
        this.ambienteRepository = ambienteRepository;
    }

    public Ambiente registrarAmbiente(Ambiente ambiente) {
        // Validaciones básicas de campos obligatorios
        if (ambiente.getNombre() == null || ambiente.getNombre().trim().isEmpty()) {
            throw new ReglaNegocioException("El nombre del ambiente no puede estar vacío.");
        }
        if (ambiente.getCapacidad() <= 0) {
            throw new ReglaNegocioException("La capacidad del ambiente debe ser mayor a cero.");
        }
        if (ambiente.getTipo() == null) {
            throw new ReglaNegocioException("El tipo de ambiente es requerido (SALA, LABORATORIO o AUDITORIO).");
        }
        
        // Guardar y retornar el nuevo ambiente en base de datos
        return ambienteRepository.save(ambiente);
    }

    /**
     * Retorna la lista de todos los ambientes registrados.
     */
    public List<Ambiente> listarAmbientes() {
        return ambienteRepository.findAll();
    }

    /**
     * Lista los ambientes que se encuentran libres (sin reservas cruzadas) en un rango de tiempo dado.
     *
     * @param inicio Fecha/hora de inicio del rango
     * @param fin Fecha/hora de fin del rango
     * @return Lista de ambientes disponibles
     */
    public List<Ambiente> listarAmbientesDisponibles(LocalDateTime inicio, LocalDateTime fin) {
        // 1. Validar que las fechas no sean nulas
        if (inicio == null || fin == null) {
            throw new ReglaNegocioException("Los parámetros de inicio y fin son obligatorios.");
        }
        
        // 2. Validación cronológica básica
        if (inicio.isAfter(fin) || inicio.isEqual(fin)) {
            throw new ReglaNegocioException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
        
        // 3. Validar jornada institucional (entre 06:00 y 22:00)
        if (inicio.toLocalTime().isBefore(java.time.LocalTime.of(6, 0)) ||
            fin.toLocalTime().isAfter(java.time.LocalTime.of(22, 0))) {
            throw new ReglaNegocioException("El rango horario debe estar entre las 06:00 y las 22:00.");
        }

        // 4. Validar duración permitida (mínimo 1 hora, máximo 4 horas)
        long durationMinutes = Duration.between(inicio, fin).toMinutes();
        if (durationMinutes < 60 || durationMinutes > 240) {
            throw new ReglaNegocioException("La duración del rango debe ser entre 1 y 4 horas.");
        }

        // 5. Validar que el rango empiece y termine en el mismo día
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReglaNegocioException("El rango de reserva debe iniciar y terminar el mismo día.");
        }

        // 6. Consultar a la base de datos usando la lógica NOT EXISTS optimizada en el repositorio
        return ambienteRepository.findAvailableAmbientes(inicio, fin);
    }
}
