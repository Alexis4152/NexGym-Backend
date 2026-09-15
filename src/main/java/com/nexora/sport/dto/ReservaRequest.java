package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservaRequest(
        @NotNull Long claseId,
        @NotNull Long alumnoId,
        @NotNull LocalDate fecha
) {}
