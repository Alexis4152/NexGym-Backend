package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;

/** "Operaciones por usuario" (seccion 14): staff administrativo, NO instructores. */
public record OperacionUsuarioDto(
        Long usuarioId, String nombre, long ventasRegistradas, BigDecimal totalVentas,
        long pagosRegistrados, BigDecimal totalPagos
) {}
