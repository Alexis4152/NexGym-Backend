package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado de una caja (homologado con DemoPV#CashRegisterStatusDto): EN USO (corteAbierto) o
 * CERRADA con su entrega pendiente (pendiente) -- exactamente uno de los dos va lleno.
 */
public record CajaRegistradoraEstadoDto(
        Long id,
        String nombre,
        Long sucursalId,
        String sucursalNombre,
        boolean activo,
        CorteAbierto corteAbierto,
        Pendiente pendiente
) {
    /** efectivoEsperado solo lo ve un administrador (a un cajero no le toca y en conteo ciego lo revelaria). */
    public record CorteAbierto(Long corteId, Long usuarioId, String usuarioNombre, LocalDateTime abiertoEn,
                               BigDecimal efectivoEsperado) {}

    /** Lo que recibira quien abra la caja, y por que: lo que entrego el ultimo corte +/- los
     * movimientos que un admin hizo con la caja cerrada. monto null = la caja nunca se ha usado. */
    public record Pendiente(BigDecimal monto, String metodoConteo, String contadoPorNombre, LocalDateTime contadoEn,
                            Long ultimoCorteId, String ultimoCorteUsuarioNombre, LocalDateTime ultimoCorteCerradoEn,
                            BigDecimal ultimoCorteMontoEntregado, String ultimoCorteMetodoConteo,
                            List<MovimientoEfectivoDto> movimientos) {}
}
