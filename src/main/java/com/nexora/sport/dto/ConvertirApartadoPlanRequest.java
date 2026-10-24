package com.nexora.sport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * POST /api/apartados-plan/{id}/convertir: da de alta la membresia real del apartado y la
 * liga a un alumno. Exactamente uno de alumnoId (alumno que ya existe) o nuevoAlumno (se
 * crea en ese momento con los datos que dejo el cliente, editables por el staff).
 * El anticipo ya cobrado se registra solo como primer pago (no se vuelve a cobrar);
 * montoPagoAdicional es lo que el cliente paga ademas en mostrador (opcional).
 */
public record ConvertirApartadoPlanRequest(
        Long alumnoId,
        @Valid NuevoAlumno nuevoAlumno,
        Set<Long> disciplinaIds,
        LocalDate fechaInicio,
        BigDecimal descuento,
        BigDecimal montoPagoAdicional,
        String metodoPago,
        Long sucursalPreferidaId
) {
    public record NuevoAlumno(
            @NotBlank @Size(max = 150) String nombre,
            String telefono,
            @Email @Size(max = 150) String email,
            LocalDate fechaNacimiento,
            Long sucursalId
    ) {}
}
