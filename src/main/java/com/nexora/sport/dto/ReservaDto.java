package com.nexora.sport.dto;

import java.time.LocalDate;

public record ReservaDto(
        Long id,
        Long claseId,
        String disciplinaNombre,
        String claseDescripcion,
        Long alumnoId,
        String alumnoNombre,
        LocalDate fecha,
        String estado
) {}
