package com.nexora.sport.dto.reportes;

import java.util.List;

public record VencimientosResumenDto(
        List<VencimientoItemDto> venceHoy, List<VencimientoItemDto> proximosAVencer,
        List<VencimientoItemDto> vencidos, List<VencimientoItemDto> vencidosSinRenovar
) {}
