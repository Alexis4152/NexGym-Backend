package com.nexora.sport.dto;

public record ProveedorDto(
        Long id, String nombre, String contacto, String telefono, String email, String notas, boolean activo
) {}
