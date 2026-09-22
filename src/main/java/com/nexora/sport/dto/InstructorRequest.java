package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record InstructorRequest(
        @NotBlank String nombre,
        String telefono,
        String email,
        @Size(max = 200) String especialidad,
        @NotEmpty(message = "Selecciona al menos una disciplina") Set<Long> disciplinaIds
) {}
