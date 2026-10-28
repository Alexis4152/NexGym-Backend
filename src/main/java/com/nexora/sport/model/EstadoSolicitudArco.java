package com.nexora.sport.model;

/**
 * Ciclo de vida de una {@link SolicitudArco}. Flujo normal: PENDIENTE_CORREO (solo canal
 * WEB) -> PENDIENTE -> [INFO_SOLICITADA -> PENDIENTE] -> APROBADA -> COMPLETADA.
 * Terminales sin ejecucion: RECHAZADA y NO_PRESENTADA (no respondio la informacion a tiempo).
 */
public enum EstadoSolicitudArco {
    /** Canal WEB: falta que el titular confirme su correo con el link; el plazo aun no corre. */
    PENDIENTE_CORREO,
    /** Recibida formalmente: corre el plazo legal de respuesta. */
    PENDIENTE,
    /** Se le pidio mas informacion al titular. */
    INFO_SOLICITADA,
    /** Resuelta a su favor (total o parcial); falta ejecutar. */
    APROBADA,
    /** Resuelta, ejecutada y cerrada. */
    COMPLETADA,
    /** Resuelta en contra (improcedente). */
    RECHAZADA,
    /** El titular no respondio la informacion pedida dentro del plazo. */
    NO_PRESENTADA;

    public boolean abierta() {
        return this == PENDIENTE || this == INFO_SOLICITADA || this == APROBADA;
    }

    public boolean cerrada() {
        return this == COMPLETADA || this == RECHAZADA || this == NO_PRESENTADA;
    }
}
