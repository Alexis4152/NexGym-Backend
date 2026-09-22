package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;

/** Indicadores objetivos (seccion 14): sin "mejor/peor instructor", sin calificar. */
public record InstructorAnaliticaDto(
        Long instructorId, String nombre, long clasesImpartidas, long asistenciasAcumuladas,
        BigDecimal promedioAlumnosPorClase, BigDecimal ocupacionPromedio
) {}
