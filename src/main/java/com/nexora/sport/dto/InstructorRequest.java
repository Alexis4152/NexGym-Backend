package com.nexora.sport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record InstructorRequest(
        @NotBlank String nombre,
        String telefono,
        /** Correo de ACCESO: es su usuario de login (institucional o personal). Al guardar se
         * crea/sincroniza la cuenta ligada a la ficha (ver InstructorService#vincularCuenta). */
        @NotBlank(message = "El correo de acceso es obligatorio")
        @Email(message = "Captura un correo valido") @Size(max = 150) String email,
        /** Correo personal de contacto, opcional (cuando el de acceso es el institucional). */
        @Email(message = "Captura un correo valido") @Size(max = 150) String emailPersonal,
        @Size(max = 200) String especialidad,
        @NotEmpty(message = "Selecciona al menos una disciplina") Set<Long> disciplinaIds,
        /** Sucursales donde esta disponible para dar clases. Null/vacio = todas (no rompe
         * a los instructores existentes al desplegar esto). */
        Set<Long> sucursalIds
) {}
