package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MembresiaDto(
        Long id,
        Long alumnoId,
        String alumnoNombre,
        Long planId,
        String planNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer clasesRestantes,
        BigDecimal precioFinal,
        String estado,
        long diasParaVencer
) {}
