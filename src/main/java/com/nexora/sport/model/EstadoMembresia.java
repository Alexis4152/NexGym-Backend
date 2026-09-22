package com.nexora.sport.model;

/**
 * Estado OPERATIVO de la membresia unicamente (no financiero, no indicadores). AGOTADA
 * y "proxima a vencer"/"saldo pendiente"/"pago vencido" a proposito NO son valores de
 * este enum: se calculan (ver MembresiaService#toDto) para no terminar con
 * combinaciones como ACTIVA_CON_ADEUDO.
 */
public enum EstadoMembresia {
    /** fechaInicio > hoy: contratada pero su vigencia todavia no arranca (ver renovacion anticipada). */
    PENDIENTE,
    ACTIVA,
    /** Bloqueo temporal manual (lesion, viaje...), con motivo obligatorio. No extiende fechaFin en este MVP. */
    SUSPENDIDA,
    /** Manual, permanente, con motivo obligatorio. */
    CANCELADA,
    /** Calculado/actualizado por lote cuando fechaFin ya paso. */
    VENCIDA
}
