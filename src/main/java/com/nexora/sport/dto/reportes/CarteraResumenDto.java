package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.util.List;

public record CarteraResumenDto(
        BigDecimal totalPorCobrar, long alumnosConSaldo, long membresiasConSaldo, List<CarteraItemDto> detalle
) {}
