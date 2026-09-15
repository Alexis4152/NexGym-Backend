package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal ingresosMes,
        BigDecimal egresosMes,
        BigDecimal balanceMes,
        long alumnosActivos,
        long membresiasVencidas,
        long membresiasPorVencer,
        long asistenciasHoy,
        List<ArticuloInventarioDto> stockBajo,
        List<MembresiaDto> proximasAVencer
) {}
