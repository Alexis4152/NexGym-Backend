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
        @NotEmpty(message = "Selecciona al menos una disciplina") Set<Long> disciplinaIds,
        /** Cuenta de login (rol operativo tipico "Entrenador") ligada a esta ficha, para que
         * pueda ver "sus" clases/reservas al iniciar sesion. Null = solo ficha de roster. */
        Long usuarioId,
        /** Sucursales donde esta disponible para dar clases. Null/vacio = todas (no rompe
         * a los instructores existentes al desplegar esto). */
        Set<Long> sucursalIds
) {}
