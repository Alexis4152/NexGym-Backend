package com.nexora.sport.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AsistenciaDto(
        Long id,
        Long alumnoId,
        String alumnoNombre,
        Long disciplinaId,
        String disciplinaNombre,
        Long claseId,
        LocalDate fecha,
        LocalTime hora
) {}
