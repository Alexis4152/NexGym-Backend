package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;

/**
 * costoEstimado/margenEstimado usan el costo ACTUAL del articulo (VentaItem no
 * conserva un costo historico, solo precioUnitario) — igual limitacion que DemoPV,
 * documentada explicitamente en ReporteTiendaService.
 */
public record ProductoAnaliticaDto(
        Long articuloId, String nombre, long cantidadVendida, BigDecimal totalVendido,
        BigDecimal costoEstimado, BigDecimal margenEstimado
) {}
