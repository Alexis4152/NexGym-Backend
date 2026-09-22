package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;

public record VentaResumenDto(BigDecimal totalVentas, long numeroVentas, BigDecimal ticketPromedio, long productosVendidos) {}
