package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;

public record ClaseRequest(
        @NotNull Long sucursalId,
        @NotNull Long disciplinaId,
        Long instructorId,
        @NotNull String diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        Long lugarId,
        @Positive int capacidadMaxima
) {}
