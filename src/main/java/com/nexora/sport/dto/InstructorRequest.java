package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record InstructorRequest(
        @NotBlank String nombre,
        String telefono,
        String email,
        String especialidad,
        Set<Long> disciplinaIds
) {}
