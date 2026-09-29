package com.nexora.sport.security;

import com.nexora.sport.model.Permiso;
import com.nexora.sport.model.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Bean usado desde {@code @PreAuthorize("@permisoAccess.check('MEMBRESIAS_CANCELAR')")}. */
@Service("permisoAccess")
public class PermisoAccessService {

    public boolean check(String permiso) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario usuario)) return false;
        try {
            return PermisoEvaluator.tiene(usuario, Permiso.valueOf(permiso));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
