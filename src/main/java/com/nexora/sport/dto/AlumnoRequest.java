package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record AlumnoRequest(
        @NotBlank @Size(max = 150) String nombre,
        LocalDate fechaNacimiento,
        String telefono,
        String email,
        String contactoEmergenciaNombre,
        String contactoEmergenciaTelefono,
        @Size(max = 1000) String observaciones,
        String estado,
        Set<Long> disciplinaIds,
        /** Sucursal donde se inscribe. Null = sin asignar (visible centro-wide). Obligatorio si quien lo crea esta acotado a una sucursal. */
        Long sucursalId
) {}
