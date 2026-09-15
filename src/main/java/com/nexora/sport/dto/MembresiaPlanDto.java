package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.util.Set;

public record MembresiaPlanDto(
        Long id,
        String nombre,
        String tipoPeriodo,
        Integer duracionDias,
        Integer numeroClasesIncluidas,
        BigDecimal precio,
        boolean multidisciplina,
        boolean accesoCompleto,
        Integer limiteAlumnos,
        boolean activo,
        Set<Long> disciplinaIds,
        Set<String> disciplinaNombres
) {}
