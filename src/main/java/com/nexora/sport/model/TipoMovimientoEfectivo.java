package com.nexora.sport.model;

/**
 * Tipo de un {@link MovimientoEfectivo}: dinero que entra o sale del cajon fisico y que NO es
 * una venta ni un pago de membresia. Homologado con DemoPV#CashMovementType.
 *
 * OJO: se declaran todos los valores (incluidos los de faltantes/reembolsos) desde el inicio,
 * porque Hibernate crea un CHECK en la columna con los valores que existan al crear la tabla.
 */
public enum TipoMovimientoEfectivo {
    /** Gasto pagado con efectivo de la caja (tambien genera su egreso en el libro de Caja). */
    GASTO(true),
    /** Retiro de efectivo (al dueño, al banco, a caja fuerte). Solo administradores. */
    RETIRO(true),
    /** Pago de un reembolso a quien puso dinero de su bolsa. */
    REEMBOLSO(true),
    /** Alguien puso dinero de su bolsa en la caja: el centro se lo debe (genera un reembolso). */
    APORTE(false),
    /** Entrada de cambio o fondo extra que no se reembolsa. */
    FONDO(false),
    /** Pago o abono de un faltante que entra a la caja. */
    PAGO_FALTANTE(false),
    /** Prestamo a otra caja (salida de esta). */
    PRESTAMO_SALIDA(true),
    /** Prestamo recibido de otra caja (entrada a esta). */
    PRESTAMO_ENTRADA(false);

    private final boolean salida;

    TipoMovimientoEfectivo(boolean salida) {
        this.salida = salida;
    }

    public boolean isSalida() {
        return salida;
    }
}
