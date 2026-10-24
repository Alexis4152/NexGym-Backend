package com.nexora.sport.service;

import java.math.BigDecimal;

/**
 * Protecciones contra errores de captura al contar efectivo (homologado con
 * DemoPV#CashCountRules):
 *  - Diferencia GRANDE: mas del 10% de lo esperado (minimo $200) -> se pide volver a escribir
 *    el monto para confirmarlo (DiferenciaGrandeException).
 *  - Monto IRRAZONABLE: mas de 10 veces lo esperado + $10,000 (p. ej. un cero de mas) -> se
 *    rechaza de plano.
 */
final class ReglasConteo {

    static final BigDecimal DIFERENCIA_GRANDE_MINIMA = new BigDecimal("200");
    static final BigDecimal DIFERENCIA_GRANDE_PORCENTAJE = new BigDecimal("10");
    static final BigDecimal FACTOR_IRRAZONABLE = new BigDecimal("10");
    static final BigDecimal MARGEN_IRRAZONABLE = new BigDecimal("10000");

    private ReglasConteo() {}

    static boolean esDiferenciaGrande(BigDecimal esperado, BigDecimal contado) {
        BigDecimal base = esperado.max(BigDecimal.ZERO);
        BigDecimal umbral = base.multiply(DIFERENCIA_GRANDE_PORCENTAJE).divide(new BigDecimal("100"))
                .max(DIFERENCIA_GRANDE_MINIMA);
        return contado.subtract(esperado).abs().compareTo(umbral) > 0;
    }

    static boolean esIrrazonable(BigDecimal esperado, BigDecimal contado) {
        BigDecimal limite = esperado.max(BigDecimal.ZERO).multiply(FACTOR_IRRAZONABLE).add(MARGEN_IRRAZONABLE);
        return contado.compareTo(limite) > 0;
    }
}
