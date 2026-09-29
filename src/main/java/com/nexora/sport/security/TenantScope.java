package com.nexora.sport.security;

import com.nexora.sport.model.NivelJerarquia;
import com.nexora.sport.model.Sucursal;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centraliza JERARQUIA + ALCANCE (por centro y por sucursal). Ningun servicio
 * de negocio debe reimplementar estas decisiones ni comparar nombres de rol
 * directamente — todo pasa por aqui, sobre {@link NivelJerarquia} (explicito
 * en Rol.nivel, no inferido del nombre).
 *
 * Centro: un usuario de centro fijo (Usuario.centro) esta atado a el; un
 * SUPER_ADMIN o un SUPERVISOR multi-centro (Usuario.centrosAdicionales no
 * vacio) puede "actuar sobre" otro centro via el header {@link #ACTING_CENTRO_HEADER}.
 * Un dueno de un solo centro (el caso comun) nunca manda ese header y todo
 * funciona exactamente igual que antes de agregar multi-centro.
 *
 * Sucursal: independiente del centro. Dos conceptos distintos (no confundir):
 *  - SUCURSALES AUTORIZADAS ({@link #sucursalesPermitidas}): el conjunto completo
 *    al que un ADMIN/OPERATIVO tiene acceso (su sucursal "hogar" + adicionales).
 *    null = sin restriccion (SUPER_ADMIN/SUPERVISOR, ven todas). Un conjunto VACIO
 *    (no null) significa "no tiene ninguna sucursal asignada": a proposito NO cae
 *    de vuelta a "ve todas" -- un ADMIN/OPERATIVO sin sucursal debe quedar bloqueado
 *    hasta que el dueno le asigne una (ver PrivateRoute/SelectSucursal en el frontend).
 *  - SUCURSAL ACTIVA ({@link #sucursalActivaId}): UNA sola, la que esta usando en
 *    la peticion actual (header {@link #ACTING_SUCURSAL_HEADER}, mismo mecanismo que
 *    {@link #ACTING_CENTRO_HEADER} para Centro). Cambiarla NO modifica sus sucursales
 *    autorizadas, solo el contexto de la peticion.
 */
@Component
public class TenantScope {

    public static final String ACTING_CENTRO_HEADER = "X-Acting-Centro-Id";
    public static final String ACTING_SUCURSAL_HEADER = "X-Acting-Sucursal-Id";

    private final UsuarioRepository usuarioRepository;

    public TenantScope(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Publico para servicios que necesitan iterar directamente una coleccion @Lazy
     * del actor autenticado (p. ej. CentroService#disponiblesPara con
     * centrosAdicionales) sin pasar por un metodo especifico de aqui. Ver {@link #fresh}. */
    public Usuario actorConAsociacionesCargadas(Usuario actor) {
        return fresh(actor);
    }

    /** El Usuario de @AuthenticationPrincipal se cargo en su PROPIA transaccion (al
     * autenticar la peticion) y con spring.jpa.open-in-view=false esa sesion ya esta
     * cerrada para cuando un servicio de negocio (en SU PROPIA transaccion) llega
     * aqui -- sus relaciones @Lazy (sucursal, sucursalesAdicionales, centrosAdicionales)
     * quedan "detached" y revientan con LazyInitializationException al tocarlas
     * (aunque .getId() en un @ManyToOne SI es seguro: el proxy trae el id sin ir a la
     * BD). Recargarlo aqui, dentro de la transaccion activa del metodo que llama,
     * es lo que permite leer esas colecciones/relaciones con seguridad. */
    private Usuario fresh(Usuario actor) {
        return usuarioRepository.findById(actor.getId()).orElse(actor);
    }

    public boolean isSuperAdmin(Usuario actor) {
        return actor.getRol().getNivel() == NivelJerarquia.SUPER_ADMIN;
    }

    public boolean isSupervisor(Usuario actor) {
        return actor.getRol().getNivel() == NivelJerarquia.SUPERVISOR;
    }

    /** SUPER_ADMIN, Supervisor(Dueno) o Admin: niveles con autoridad para autorizar excepciones administrativas puntuales. */
    public boolean isAdminOSuperior(Usuario actor) {
        NivelJerarquia n = actor.getRol().getNivel();
        return n == NivelJerarquia.SUPER_ADMIN || n == NivelJerarquia.SUPERVISOR || n == NivelJerarquia.ADMIN;
    }

    /** SUPER_ADMIN o Supervisor(Dueno) -- NO incluye Admin. Un gerente/encargado de
     * sucursal (nivel ADMIN) no administra Sucursales ni Usuarios de otras sucursales;
     * eso es exclusivo del dueno del negocio o de la plataforma. */
    public boolean isSupervisorOSuperior(Usuario actor) {
        NivelJerarquia n = actor.getRol().getNivel();
        return n == NivelJerarquia.SUPER_ADMIN || n == NivelJerarquia.SUPERVISOR;
    }

    /** Centro sobre el que este actor puede leer/escribir en la peticion actual. */
    public Long scopeId(Usuario actor) {
        Long acting = actingCentroFromHeader();

        if (isSuperAdmin(actor)) {
            if (acting == null) {
                throw new IllegalStateException("Selecciona un centro para continuar (header " + ACTING_CENTRO_HEADER + ")");
            }
            return acting;
        }

        // Supervisor multi-centro "actuando sobre" uno de sus centros adicionales.
        if (acting != null && isSupervisor(actor) && !acting.equals(centroHogarId(actor))) {
            if (!puedeActuarSobreCentro(actor, acting)) {
                throw new IllegalStateException("No administras ese centro");
            }
            return acting;
        }

        if (actor.getCentro() != null) {
            return actor.getCentro().getId();
        }
        throw new IllegalStateException("El usuario no tiene un centro asignado");
    }

    /** Sucursales AUTORIZADAS (ver nota de clase): null = sin restriccion (SUPER_ADMIN/
     * SUPERVISOR). Para ADMIN/OPERATIVO, la union de su sucursal "hogar" + adicionales --
     * puede venir VACIA (sin sucursal asignada todavia), y a proposito no cae a "ve
     * todas": un conjunto vacio bloquea, no abre. */
    public Set<Long> sucursalesPermitidas(Usuario actorPrincipal) {
        if (isSuperAdmin(actorPrincipal) || isSupervisor(actorPrincipal)) return null;
        Usuario actor = fresh(actorPrincipal);
        Set<Long> permitidas = new HashSet<>();
        // Una sucursal desactivada deja de contar, aunque el Usuario aun tenga la
        // relacion guardada -- asi se invalida su acceso/contexto activo sin tener
        // que tocar la asignacion en si (seccion 30 del encargo: "no debe permitir
        // que continue operando ahi"). El historial de esa sucursal no se toca.
        if (actor.getSucursal() != null && actor.getSucursal().isActivo()) {
            permitidas.add(actor.getSucursal().getId());
        }
        permitidas.addAll(actor.getSucursalesAdicionales().stream()
                .filter(Sucursal::isActivo)
                .map(Sucursal::getId).collect(Collectors.toSet()));
        return permitidas;
    }

    /** true si el recurso (su sucursalId, puede ser null) es visible para el alcance actual del actor. */
    public boolean sucursalPermite(Usuario actor, Long sucursalIdDelRecurso) {
        Set<Long> permitidas = sucursalesPermitidas(actor);
        return permitidas == null || permitidas.contains(sucursalIdDelRecurso);
    }

    /** SUCURSAL ACTIVA (ver nota de clase): la UNICA sucursal concreta sobre la que
     * opera esta peticion -- para listar/filtrar por "donde estoy trabajando ahora",
     * no por "todo lo que tengo autorizado". Un SUPER_ADMIN/SUPERVISOR sin header =
     * "Todas las sucursales" (null, vista consolidada); con header = esa una en
     * concreto (debe pertenecer al centro activo). Un ADMIN/OPERATIVO con una sola
     * sucursal autorizada la usa automaticamente sin necesitar el header; con varias,
     * el header es obligatorio (si no llega o no es una de las suyas, se rechaza);
     * sin ninguna, se rechaza siempre ("no tienes sucursal asignada"). */
    public Long sucursalActivaId(Usuario actor) {
        Long acting = actingSucursalFromHeader();
        Set<Long> permitidas = sucursalesPermitidas(actor);

        if (permitidas == null) {
            return acting; // SUPER_ADMIN/SUPERVISOR: null = "todas", o la que pidan explicitamente
        }
        if (permitidas.isEmpty()) {
            throw new IllegalStateException("No tienes ninguna sucursal asignada; contacta a tu administrador");
        }
        if (permitidas.size() == 1) {
            return permitidas.iterator().next();
        }
        if (acting == null || !permitidas.contains(acting)) {
            throw new IllegalStateException("Selecciona una sucursal para continuar (header " + ACTING_SUCURSAL_HEADER + ")");
        }
        return acting;
    }

    public boolean canManageCentro(Usuario actor, Long centroId) {
        if (isSuperAdmin(actor)) return true;
        if (isSupervisor(actor)) return centroId.equals(centroHogarId(actor)) || puedeActuarSobreCentro(actor, centroId);
        return centroId.equals(centroHogarId(actor));
    }

    private Long centroHogarId(Usuario actor) {
        return actor.getCentro() != null ? actor.getCentro().getId() : null;
    }

    private boolean puedeActuarSobreCentro(Usuario actor, Long centroId) {
        return fresh(actor).getCentrosAdicionales().stream().anyMatch(c -> c.getId().equals(centroId));
    }

    private Long actingCentroFromHeader() {
        return parseLongHeader(ACTING_CENTRO_HEADER);
    }

    private Long actingSucursalFromHeader() {
        return parseLongHeader(ACTING_SUCURSAL_HEADER);
    }

    private Long parseLongHeader(String nombre) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();
        String value = request.getHeader(nombre);
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
