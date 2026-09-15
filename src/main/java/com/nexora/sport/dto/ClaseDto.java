package com.nexora.sport.dto;

import java.time.LocalTime;

public record ClaseDto(
        Long id,
        Long disciplinaId,
        String disciplinaNombre,
        Long instructorId,
        String instructorNombre,
        String diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String lugar,
        int capacidadMaxima,
        boolean activo
) {}
