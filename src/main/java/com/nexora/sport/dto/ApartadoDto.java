package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ApartadoDto(
        Long id,
        String clienteNombre,
        String clienteTelefono,
        String clienteEmail,
        String notas,
        String estado,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal total,
        Integer horasVigencia,
        LocalDateTime solicitadoEn,
        LocalDateTime confirmadoEn,
        LocalDateTime expiraEn,
        String confirmadoPorNombre,
        String completadoPorNombre,
        String canceladoPorNombre,
        LocalDateTime canceladoEn,
        String motivoCancelacion,
        Long ventaId,
        List<ApartadoItemDto> items
) {}
