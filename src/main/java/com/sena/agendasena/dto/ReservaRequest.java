package com.sena.agendasena.dto;

import java.time.LocalDateTime;

public record ReservaRequest(
    Long ambienteId,
    String nombreInstructor,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin,
    Integer numeroAprendices 
) {}
