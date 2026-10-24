package com.nexora.sport.model;

/**
 * Estado de una comparacion de efectivo de un corte: la ENTREGA (lo que recibio el cajero al
 * abrir vs lo que debia recibir) o el ARQUEO (lo contado al cerrar vs lo esperado).
 * Homologado con DemoPV#HandoverStatus.
 */
public enum EstadoDiferencia {
    /** No aplica (la caja no tenia entrega previa, o nadie conto al cerrar). */
    NINGUNA,
    /** Cuadro exacto. */
    OK,
    /** No cuadro y nadie lo ha resuelto (lo resuelve un administrador). */
    DIFERENCIA,
    /** Un administrador ya la resolvio; la diferencia nunca se borra, queda con su causa. */
    RESUELTA
}
