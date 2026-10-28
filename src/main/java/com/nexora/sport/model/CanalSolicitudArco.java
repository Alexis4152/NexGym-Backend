package com.nexora.sport.model;

/** Por donde entro una {@link SolicitudArco}: determina si hace falta confirmar el correo. */
public enum CanalSolicitudArco {
    /** Formulario publico sin sesion: exige confirmar el correo por link antes de que corra el plazo. */
    WEB,
    /** Levantada desde la propia cuenta en sesion ("Mis solicitudes ARCO"). */
    CUENTA,
    /** Registrada por el SUPER_ADMIN para alguien verificado en persona (con o sin cuenta). */
    MOSTRADOR
}
