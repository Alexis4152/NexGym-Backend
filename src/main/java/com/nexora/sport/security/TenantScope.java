package com.nexora.sport.security;

import com.nexora.sport.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Centraliza "sobre que centro puede leer/escribir este actor". Un usuario de
 * centro esta fijo a su propio centro; un SUPER_ADMIN (sin centro propio) debe
 * indicar sobre cual centro quiere actuar via el header {@link #ACTING_CENTRO_HEADER}.
 * Ningun servicio de negocio debe reimplementar esta decision.
 */
@Component
public class TenantScope {

    public static final String ACTING_CENTRO_HEADER = "X-Acting-Centro-Id";
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    public boolean isSuperAdmin(Usuario actor) {
        return SUPER_ADMIN_ROLE.equalsIgnoreCase(actor.getRol().getNombre());
    }

    /** Dueno, Administrador o SUPER_ADMIN: roles con autoridad para autorizar excepciones administrativas puntuales. */
    public boolean isAdminOSuperior(Usuario actor) {
        String rol = actor.getRol().getNombre();
        return isSuperAdmin(actor) || "Dueno".equalsIgnoreCase(rol) || "Administrador".equalsIgnoreCase(rol);
    }

    /** Centro sobre el que este actor puede leer/escribir en la peticion actual. */
    public Long scopeId(Usuario actor) {
        if (actor.getCentro() != null) {
            return actor.getCentro().getId();
        }
        if (isSuperAdmin(actor)) {
            Long acting = actingCentroFromHeader();
            if (acting == null) {
                throw new IllegalStateException("Selecciona un centro para continuar (header " + ACTING_CENTRO_HEADER + ")");
            }
            return acting;
        }
        throw new IllegalStateException("El usuario no tiene un centro asignado");
    }

    private Long actingCentroFromHeader() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();
        String value = request.getHeader(ACTING_CENTRO_HEADER);
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
