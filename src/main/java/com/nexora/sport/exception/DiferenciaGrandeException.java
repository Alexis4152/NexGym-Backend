package com.nexora.sport.exception;

/**
 * Un monto contado con una diferencia muy grande contra lo esperado (ver ReglasConteo): el
 * frontend debe pedir que se vuelva a escribir y reenviar con confirmacionDiferenciaGrande.
 * GlobalExceptionHandler la responde como 400 con data = {"diferenciaGrande": "true"}.
 */
public class DiferenciaGrandeException extends RuntimeException {
    public DiferenciaGrandeException(String message) {
        super(message);
    }
}
