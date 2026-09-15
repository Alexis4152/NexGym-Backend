package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;

public record AsistenciaRequest(
        @NotNull Long alumnoId,
        Long disciplinaId,
        Long claseId
) {}
