package com.nexora.sport.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraRequest(
        @NotNull Long proveedorId,
        String estado,
        LocalDate fechaPlaneada,
        String notas,
        @NotEmpty List<ItemRequest> items
) {
    public record ItemRequest(Long articuloId, @NotNull String descripcion, int cantidad, @NotNull BigDecimal costoUnitario) {}
}
