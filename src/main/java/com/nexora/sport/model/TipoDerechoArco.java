package com.nexora.sport.model;

/**
 * Derecho que ejerce una {@link SolicitudArco} (Ley Federal de Proteccion de Datos
 * Personales en Posesion de los Particulares): los cuatro ARCO mas Revocacion del
 * consentimiento y Limitacion de uso/divulgacion. Igual que en DemoPV.
 */
public enum TipoDerechoArco {
    /** Saber que datos personales se tienen de la persona y para que se usan. */
    ACCESO,
    /** Corregir datos inexactos o incompletos. */
    RECTIFICACION,
    /** Eliminar/dar de baja sus datos, cuando proceda. */
    CANCELACION,
    /** Oponerse a un uso especifico de sus datos. */
    OPOSICION,
    /** Revocar un consentimiento dado previamente. */
    REVOCACION,
    /** Limitar un uso o divulgacion especifica, sin llegar a cancelar. */
    LIMITACION;

    public String etiqueta() {
        return switch (this) {
            case ACCESO -> "Acceso";
            case RECTIFICACION -> "Rectificación";
            case CANCELACION -> "Cancelación";
            case OPOSICION -> "Oposición";
            case REVOCACION -> "Revocación";
            case LIMITACION -> "Limitación";
        };
    }

    /**
     * Los que exponen o alteran datos: exigen identidad verificada (contrasena desde la
     * cuenta, o verificacion explicita del SUPER_ADMIN) antes de poder responderse.
     * Oposicion/Limitacion solo detienen un uso puntual.
     */
    public boolean requiereIdentidadFuerte() {
        return this == ACCESO || this == RECTIFICACION || this == CANCELACION || this == REVOCACION;
    }
}
