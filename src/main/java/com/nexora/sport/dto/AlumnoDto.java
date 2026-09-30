package com.nexora.sport.dto;

import java.time.LocalDate;
import java.util.Set;

public record AlumnoDto(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        String nombre,
        LocalDate fechaNacimiento,
        String telefono,
        String email,
        String contactoEmergenciaNombre,
        String contactoEmergenciaTelefono,
        String fotoUrl,
        String codigoQr,
        String observaciones,
        String estado,
        LocalDate fechaIngreso,
        Set<Long> disciplinaIds,
        Set<String> disciplinaNombres
) {}
