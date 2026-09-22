package com.nexora.sport.dto;

import java.time.LocalDateTime;

public record DocumentoInstructorDto(
        Long id,
        Long disciplinaId,
        String disciplinaNombre,
        String ruta,
        String nombreOriginal,
        LocalDateTime createdAt
) {}
