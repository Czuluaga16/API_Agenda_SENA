package com.sena.agendasena.controller;

import com.sena.agendasena.model.Ambiente;
import com.sena.agendasena.model.Reserva;
import com.sena.agendasena.service.AmbienteService;
import com.sena.agendasena.service.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ambientes")
public class AmbienteController {

    private final AmbienteService ambienteService;
    private final ReservaService reservaService;

    public AmbienteController(AmbienteService ambienteService, ReservaService reservaService) {
        this.ambienteService = ambienteService;
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<Ambiente> registrarAmbiente(@RequestBody Ambiente ambiente) {
        Ambiente nuevoAmbiente = ambienteService.registrarAmbiente(ambiente);
        return new ResponseEntity<>(nuevoAmbiente, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Ambiente>> listarAmbientes() {
        List<Ambiente> ambientes = ambienteService.listarAmbientes();
        return ResponseEntity.ok(ambientes);
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<Ambiente>> listarAmbientesDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<Ambiente> disponibles = ambienteService.listarAmbientesDisponibles(inicio, fin);
        return ResponseEntity.ok(disponibles);
    }

    @GetMapping("/{id}/reservas")
    public ResponseEntity<List<Reserva>> listarReservasPorAmbienteYFecha(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<Reserva> reservas = reservaService.listarReservasActivasPorAmbienteYFecha(id, fecha);
        return ResponseEntity.ok(reservas);
    }
}
