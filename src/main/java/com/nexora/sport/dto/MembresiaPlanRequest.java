package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Set;

public record MembresiaPlanRequest(
        @NotBlank String nombre,
        @NotNull String tipoPeriodo,
        Integer duracionDias,
        Integer numeroClasesIncluidas,
        @NotNull @Positive BigDecimal precio,
        boolean multidisciplina,
        boolean accesoCompleto,
        Integer limiteAlumnos,
        Set<Long> disciplinaIds
) {}
