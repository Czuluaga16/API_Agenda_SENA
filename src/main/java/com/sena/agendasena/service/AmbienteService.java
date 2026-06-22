package com.sena.agendasena.service;

import com.sena.agendasena.exception.ReglaNegocioException;
import com.sena.agendasena.model.Ambiente;
import com.sena.agendasena.repository.AmbienteRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AmbienteService {

    private final AmbienteRepository ambienteRepository;

    // Constructor estándar para inyectar AmbienteRepository sin Lombok
    public AmbienteService(AmbienteRepository ambienteRepository) {
        this.ambienteRepository = ambienteRepository;
    }

    public Ambiente registrarAmbiente(Ambiente a) {
        if (a.getNombre() == null || a.getNombre().isBlank()) throw new ReglaNegocioException("Nombre vacío.");
        if (a.getCapacidad() <= 0) throw new ReglaNegocioException("Capacidad debe ser > 0.");
        if (a.getTipo() == null) throw new ReglaNegocioException("Tipo de ambiente requerido.");
        return ambienteRepository.save(a);
    }

    public List<Ambiente> listarAmbientes() {
        return ambienteRepository.findAll();
    }

    public List<Ambiente> listarAmbientesDisponibles(LocalDateTime inicio, LocalDateTime fin) {
        validarRangoHorario(inicio, fin);
        return ambienteRepository.findAvailableAmbientes(inicio, fin);
    }

    private void validarRangoHorario(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) throw new ReglaNegocioException("Parámetros obligatorios.");
        if (inicio.isAfter(fin) || !inicio.toLocalDate().equals(fin.toLocalDate())) 
            throw new ReglaNegocioException("Rango cronológico inválido (mismo día).");
        if (inicio.toLocalTime().isBefore(LocalTime.of(6, 0)) || fin.toLocalTime().isAfter(LocalTime.of(22, 0)))
            throw new ReglaNegocioException("Fuera de jornada institucional (06:00-22:00).");
        long mins = Duration.between(inicio, fin).toMinutes();
        if (mins < 60 || mins > 240) throw new ReglaNegocioException("Duración entre 1 y 4 horas.");
    }
}