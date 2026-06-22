package com.sena.agendasena.repository;

import com.sena.agendasena.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT COUNT(r) FROM Reserva r " +
           "WHERE r.ambiente.id = :ambienteId " +
           "AND r.estado = 'ACTIVA' " +
           "AND r.fechaHoraInicio < :fin " + 
           "AND r.fechaHoraFin > :inicio")
    long countOverlappingReservations(@Param("ambienteId") Long ambienteId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(r) FROM Reserva r " +
           "WHERE r.nombreInstructor = :nombreInstructor " +
           "AND r.estado = 'ACTIVA' " +
           "AND r.fechaHoraInicio >= :startOfDay " +
           "AND r.fechaHoraInicio <= :endOfDay")
    long countInstructorReservationsOnDay(@Param("nombreInstructor") String nombreInstructor,
                                          @Param("startOfDay") LocalDateTime startOfDay,
                                          @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT r FROM Reserva r " +
           "WHERE r.ambiente.id = :ambienteId " +
           "AND r.estado = 'ACTIVA' " +
           "AND r.fechaHoraInicio >= :startOfDay " +
           "AND r.fechaHoraInicio <= :endOfDay")
    List<Reserva> findActiveReservationsByAmbienteAndDate(@Param("ambienteId") Long ambienteId,
                                                          @Param("startOfDay") LocalDateTime startOfDay,
                                                          @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT r FROM Reserva r " +
           "WHERE (r.estado = 'ACTIVA' OR r.estado = 'FINALIZADA') " +
           "AND r.fechaHoraInicio >= :startOfDay " +
           "AND r.fechaHoraInicio <= :endOfDay")
    List<Reserva> findReservationsForReport(@Param("startOfDay") LocalDateTime startOfDay,
                                            @Param("endOfDay") LocalDateTime endOfDay);
}
