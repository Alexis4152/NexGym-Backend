package com.nexora.sport.model;

/**
 * A quien va dirigida la notificacion. ALUMNO/INSTRUCTOR no tienen portal propio hoy
 * (no inician sesion en NexoraSport), asi que para ellos el unico canal real es EMAIL;
 * la fila en {@link Notificacion} igual se conserva como historial/auditoria.
 * CENTRO_ADMIN es un aviso interno visible para todo el staff del centro con acceso
 * a la seccion NOTIFICACIONES (no se dirige a un Usuario especifico).
 */
public enum TipoDestinatario {
    ALUMNO,
    INSTRUCTOR,
    CENTRO_ADMIN
}
