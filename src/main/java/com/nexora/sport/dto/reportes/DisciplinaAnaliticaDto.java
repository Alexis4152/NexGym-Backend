package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;

/**
 * ingresosAsociados NO es ingreso exclusivo de la disciplina: es la suma de precioFinal
 * de TODAS las membresias vigentes que incluyen esta disciplina (una membresia
 * multidisciplina suma completa a cada una de sus disciplinas). Se expone asi, con esta
 * etiqueta, para no inventar una regla de prorrateo que no existe (seccion 11 del encargo).
 */
public record DisciplinaAnaliticaDto(
        Long disciplinaId, String nombre, long alumnosActivos, long membresiasRelacionadas,
        long asistencias, long clasesImpartidas, BigDecimal ingresosAsociadosNoExclusivos
) {}
