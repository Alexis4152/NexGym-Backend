package com.nexora.sport.security;

import com.nexora.sport.model.Permiso;
import com.nexora.sport.model.Usuario;

/**
 * Efectivo = (Rol.permisos U Usuario.permisosExtra) - Usuario.permisosRevocados
 * (seccion 22 del encargo: excepciones por usuario sin crear un rol nuevo).
 * SUPER_ADMIN siempre tiene todos los permisos, sin excepcion.
 */
public final class PermisoEvaluator {

    private PermisoEvaluator() {}

    public static boolean tiene(Usuario actor, Permiso permiso) {
        if (actor.getRol().getNivel() == com.nexora.sport.model.NivelJerarquia.SUPER_ADMIN) return true;
        if (actor.getPermisosRevocados().contains(permiso)) return false;
        return actor.getRol().getPermisos().contains(permiso) || actor.getPermisosExtra().contains(permiso);
    }
}
