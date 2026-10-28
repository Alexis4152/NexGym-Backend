package com.nexora.sport.model;

/** Resultado legal al responder una {@link SolicitudArco}. */
public enum ResultadoArco {
    PROCEDENTE,
    PARCIAL,
    /** Se niega: la solicitud queda RECHAZADA, sin ejecucion. */
    IMPROCEDENTE
}
