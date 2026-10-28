package com.nexora.sport.model;

/** Como se verifico la identidad de quien pide una {@link SolicitudArco}. */
public enum MetodoIdentidadArco {
    /** Volvio a escribir la contrasena de su cuenta en sesion. */
    CONTRASENA,
    /** Identificacion mostrada en persona. */
    EN_PERSONA,
    /** Confirmada por telefono. */
    TELEFONO,
    /** Otro medio, explicado en la nota. */
    OTRO
}
