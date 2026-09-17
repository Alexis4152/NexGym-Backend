package com.nexora.sport.dto;

public record DisciplinaDto(
        Long id,
        String nombre,
        String descripcion,
        String icono,
        String color,
        String modalidad,
        Integer limiteAlumnos,
        boolean requiereInstalacion,
        boolean activo
) {}
