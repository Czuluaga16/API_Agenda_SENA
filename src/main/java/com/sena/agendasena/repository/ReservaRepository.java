package com.sena.agendasena.repository;

import com.sena.agendasena.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.ambiente.id = :ambienteId " +
           "AND r.estado = 'ACTIVA' AND r.fechaHoraInicio < :fin AND r.fechaHoraFin > :inicio")
    long countOverlappingReservations(Long ambienteId, LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.nombreInstructor = :nombreInstructor " +
           "AND r.estado = 'ACTIVA' AND r.fechaHoraInicio BETWEEN :startOfDay AND :endOfDay")
    long countInstructorReservationsOnDay(String nombreInstructor, LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT r FROM Reserva r WHERE r.ambiente.id = :ambienteId " +
           "AND r.estado = 'ACTIVA' AND r.fechaHoraInicio BETWEEN :startOfDay AND :endOfDay")
    List<Reserva> findActiveReservationsByAmbienteAndDate(Long ambienteId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT r FROM Reserva r WHERE r.estado IN ('ACTIVA', 'FINALIZADA') " +
           "AND r.fechaHoraInicio BETWEEN :startOfDay AND :endOfDay")
    List<Reserva> findReservationsForReport(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
