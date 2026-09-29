package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RolRequest(
        @NotBlank String nombre,
        @NotEmpty Set<String> secciones,
        /** "ADMIN" u "OPERATIVO" unicamente — un rol creado desde aqui nunca puede ser SUPERVISOR ni SUPER_ADMIN (seccion 23 del encargo). Null = OPERATIVO. */
        String nivel,
        Set<String> permisos
) {}
