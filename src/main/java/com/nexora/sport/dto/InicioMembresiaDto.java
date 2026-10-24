package com.nexora.sport.dto;

import java.time.LocalDate;

/**
 * Como arrancaria la vigencia de una membresia dada de alta HOY (ver InicioMembresiaService):
 * se muestra en el formulario antes de confirmar, y es exactamente lo que aplica el backend.
 *  - hayClaseHoy=false: fechaInicio = siguiente dia habil (sin domingos).
 *  - hayClaseHoy=true: arranca hoy SI registra asistencia hoy; si no, en fechaInicio.
 */
public record InicioMembresiaDto(
        boolean hayClaseHoy,
        LocalDate fechaInicio,
        LocalDate fechaInicioAnticipable,
        String claseDisciplina,
        String claseSucursal,
        String claseHoraInicio,
        Long claseLugaresDisponibles,
        String mensaje
) {}
