package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.util.Set;

public record MembresiaPlanDto(
        Long id,
        String nombre,
        String tipoPlan,
        Integer duracionCantidad,
        String duracionUnidad,
        Integer numeroClasesIncluidas,
        BigDecimal precio,
        boolean accesoCompleto,
        Integer maxDisciplinasSeleccionables,
        boolean permiteAbonos,
        BigDecimal montoMinimoAbono,
        Integer limiteAlumnos,
        long inscritosActuales,
        /** Null cuando el plan no tiene limite (cupo ilimitado); nunca negativo. */
        Integer cupoDisponible,
        boolean activo,
        Set<Long> disciplinaIds,
        Set<String> disciplinaNombres
) {}
