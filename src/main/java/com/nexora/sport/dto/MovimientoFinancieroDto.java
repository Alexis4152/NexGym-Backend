package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
        String proveedorNombre,
        Long sucursalId,
        String sucursalNombre,
        String registradoPorNombre,
        String comprobanteUrl,
        String estadoAprobacion,
        String resueltoPorNombre,
        LocalDateTime resueltoEn,
        String rechazadoMotivo
) {}
