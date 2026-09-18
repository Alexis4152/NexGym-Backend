package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaDto(
        Long id,
        Long corteCajaId,
        String usuarioNombre,
        String clienteNombre,
        String clienteEmail,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal total,
        BigDecimal montoRecibido,
        BigDecimal cambio,
        String metodoPago,
        String tipoTicket,
        String estado,
        String notas,
        LocalDateTime createdAt,
        List<VentaItemDto> items
) {}
