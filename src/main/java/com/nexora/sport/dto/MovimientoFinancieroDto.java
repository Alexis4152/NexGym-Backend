package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoFinancieroDto(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        String tipo,
        BigDecimal monto,
        String metodoPago,
        String descripcion,
        LocalDate fecha,
        Long alumnoId,
        String alumnoNombre,
        Long proveedorId,
        String proveedorNombre
) {}
