package com.sena.agendasena.dto;

public record OcupacionReporteDto(
    Long ambienteId,
    String nombreAmbiente,
    double horasReservadas,
    double porcentajeOcupacion
) {}
