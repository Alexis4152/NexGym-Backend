package com.nexora.sport.dto;

/** Disponibilidad de una clase para mostrador (seccion 26 del encargo de jerarquias):
 * a proposito NO trae instructor ni roster -- solo lo minimo para poder decirle a un
 * cliente "en tal sucursal si hay cupo", cruzando sucursales dentro del mismo centro. */
public record ClaseDisponibleDto(
        Long id,
        String sucursalNombre,
        String disciplinaNombre,
        String diaSemana,
        String horaInicio,
        String horaFin,
        int capacidadMaxima,
        long reservados,
        long cupoDisponible
) {}
