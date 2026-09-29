package com.nexora.sport.model;

/**
 * Jerarquia EXPLICITA de un Rol, independiente de su nombre. Antes esto se
 * inferia comparando strings ("Dueno".equals(rol.getNombre())) disperso en
 * TenantScope/UsuarioService — fragil ante renombrar un rol. Ahora un Rol
 * personalizado tambien declara su nivel al crearse.
 *
 * IMPORTANTE: nivel decide JERARQUIA (quien puede administrar a quien), no
 * PERMISOS (que puede operar) ni ALCANCE (que datos puede ver) — esos son
 * Rol.permisos y Usuario.sucursal/centrosAdicionales, conceptos separados.
 */
public enum NivelJerarquia {
    /** Plataforma completa. Unico con centro=null en Rol. */
    SUPER_ADMIN,
    /** Dueno de uno o varios Centros (ver Usuario.centrosAdicionales). */
    SUPERVISOR,
    /** Encargado, alcance por defecto: una Sucursal (ver Usuario.sucursal). */
    ADMIN,
    /** Personal operativo (recepcion, caja, instructor, inventario...). */
    OPERATIVO
}
