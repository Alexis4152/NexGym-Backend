package com.nexora.sport.dto;

import java.time.LocalTime;

public record ClaseDto(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        Long disciplinaId,
        String disciplinaNombre,
        Long instructorId,
        String instructorNombre,
        String diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long lugarId,
        String lugarNombre,
        String lugarDireccion,
        int capacidadMaxima,
        boolean activo
) {}
