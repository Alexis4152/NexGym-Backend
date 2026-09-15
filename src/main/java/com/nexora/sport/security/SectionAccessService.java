package com.nexora.sport.security;

import com.nexora.sport.model.Seccion;
import com.nexora.sport.model.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Bean usado desde {@code @PreAuthorize("@sectionAccess.check('ALUMNOS')")}. */
@Service("sectionAccess")
public class SectionAccessService {

    public boolean check(String seccion) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario usuario)) return false;
        try {
            return usuario.getRol().getSecciones().contains(Seccion.valueOf(seccion));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
