package com.sena.agendasena.controller;

import com.sena.agendasena.dto.OcupacionReporteDto;
import com.sena.agendasena.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReservaService reservaService;

    public ReporteController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping("/ocupacion")
    public ResponseEntity<List<OcupacionReporteDto>> obtenerReporteOcupacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<OcupacionReporteDto> reporte = reservaService.generarReporteOcupacion(fecha);
        return ResponseEntity.ok(reporte);
    }
}
