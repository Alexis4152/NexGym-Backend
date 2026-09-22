package com.nexora.sport.dto.reportes;

import java.util.List;

/** bajas = alumnos cuyo Alumno.fechaBaja cae en el periodo (unico evento con timestamp real). */
public record AlumnosResumenDto(long activos, long nuevos, long bajas, List<SeriePuntoDto> nuevosPorMes) {}
