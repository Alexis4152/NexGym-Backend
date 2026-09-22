package com.nexora.sport.dto.reportes;

import java.util.List;

public record RenovacionesResumenDto(
        long total, long mismoPlan, long cambioPlan, long cambioDisciplina,
        long anticipadas, long tardias, List<RenovacionItemDto> detalle
) {}
