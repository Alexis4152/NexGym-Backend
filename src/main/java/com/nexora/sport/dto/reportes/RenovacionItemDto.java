package com.nexora.sport.dto.reportes;

import java.time.LocalDate;

public record RenovacionItemDto(
        Long membresiaId, Long alumnoId, String alumnoNombre, String planAnteriorNombre, String planNuevoNombre,
        LocalDate fechaInicio, boolean mismoPlan, boolean cambioDisciplina, boolean anticipada, boolean tardia
) {}
