package com.nexora.sport.dto.publico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado de un apartado de productos visto por el CLIENTE desde la tienda publica ("Consultar
 * mi apartado", igual que DemoPV). Solo lo que el cliente necesita para darle seguimiento:
 * nada de quien lo confirmo/cancelo del staff, descuentos internos ni la venta generada.
 * folio = id del apartado (el mismo numero que se le mostro al solicitarlo).
 */
public record PublicApartadoConsultaDto(
        Long folio,
        String estado,
        BigDecimal total,
        LocalDateTime solicitadoEn,
        LocalDateTime expiraEn,
        LocalDateTime canceladoEn,
        String motivoCancelacion,
        List<Item> items
) {
    /** articuloId viaja para que "Editar" pueda reconstruir el carrito con los mismos
     * productos (y la misma sucursal de recoleccion) -- ver PublicApartar.jsx. */
    public record Item(
            Long articuloId,
            String articuloNombre,
            String sucursalNombre,
            int cantidad,
            BigDecimal subtotal
    ) {}
}
