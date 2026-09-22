package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.util.List;

public record RetencionResumenDto(
        long elegibles, long retenidos, BigDecimal porcentaje, int ventanaGraciaDias, List<RetencionItemDto> detalle
) {}
