package com.nexora.sport.dto;

/** Alumno registrado a una clase (lista de solo lectura, ver ClaseService#alumnosDeClase). */
public record AlumnoClaseDto(
        Long id,
        String nombre,
        String fotoUrl,
        String telefono,
        String email,
        String estado
) {}
