package com.sena.agendasena.repository;

import com.sena.agendasena.model.Ambiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AmbienteRepository extends JpaRepository<Ambiente, Long> {

    // Cambiado de NOT IN a NOT EXISTS por robustez, rendimiento y claridad para el motor de base de datos.
    // Busca los ambientes activos tales que NO exista ninguna reserva ACTIVA que se cruce en el horario solicitado.
    @Query("SELECT a FROM Ambiente a WHERE a.activo = true AND NOT EXISTS (" +
           "SELECT r FROM Reserva r " +
           "WHERE r.ambiente = a " +
           "AND r.estado = 'ACTIVA' " +
           "AND r.fechaHoraInicio < :fin " +
           "AND r.fechaHoraFin > :inicio)")
    List<Ambiente> findAvailableAmbientes(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
