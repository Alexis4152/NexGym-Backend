package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;

/** ocupacion = asistieron / cupo (seccion 13). "reservados" se muestra aparte, sin mezclarse en la formula. */
public record OcupacionClaseDto(
        Long claseId, String disciplinaNombre, String instructorNombre, String sucursalNombre, String horario,
        int cupo, long reservados, long asistieron, BigDecimal porcentajeOcupacion
) {}
