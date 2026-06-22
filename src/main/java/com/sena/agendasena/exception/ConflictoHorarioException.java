package com.sena.agendasena.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // Esto asegura el código HTTP 409 automáticamente
public class ConflictoHorarioException extends RuntimeException {
    public ConflictoHorarioException(String message) {
        super(message);
    }
}