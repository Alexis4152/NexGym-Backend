package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraDto(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        String estado,
        LocalDate fechaPlaneada,
        LocalDate fechaRealizada,
        BigDecimal total,
        String notas,
        List<CompraItemDto> items
) {
    public record CompraItemDto(Long id, Long articuloId, String descripcion, int cantidad, BigDecimal costoUnitario) {}
}
