package com.nexora.sport.service;

import com.nexora.sport.dto.RolDto;
import com.nexora.sport.dto.RolRequest;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Centro;
import com.nexora.sport.model.NivelJerarquia;
import com.nexora.sport.model.Permiso;
import com.nexora.sport.model.Rol;
import com.nexora.sport.model.Seccion;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.RolRepository;
import com.nexora.sport.repository.UsuarioRepository;
import com.nexora.sport.security.PermisoEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nexora.sport.model.Permiso.*;
import static com.nexora.sport.model.Seccion.*;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    public RolService(RolRepository rolRepository, UsuarioRepository usuarioRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    /** Roles visibles para el actor: su mismo nivel y los inferiores, NUNCA uno superior (un
     * Dueño no sabe que existe el rol de plataforma SUPER_ADMIN; un Administrador no ve al
     * Dueño). El SUPER_ADMIN ve todos. Aplica tambien al selector de rol de Usuarios. */
    public List<RolDto> listar(Usuario actor, Long centroId) {
        int rangoActor = actor.getRol().getNivel().ordinal();
        return rolRepository.findByCentroIdOrCentroIsNull(centroId).stream()
                .filter(r -> r.getNivel().ordinal() >= rangoActor)
                .map(this::toDto).toList();
    }

    // Reglas homologadas con DemoPV#RoleService:
    //  - CREAR un rol: solo SUPER_ADMIN (el catalogo de roles es de la plataforma).
    //  - EDITAR secciones/permisos: SUPER_ADMIN, o quien administra roles (el Dueño) sobre los
    //    roles de nivel ESTRICTAMENTE MENOR al suyo -- incluidos los roles de sistema
    //    (Administrador, Recepcion, Entrenador...), que es justo como el Dueño da y quita
    //    permisos a su personal. Nadie edita su propio nivel ni uno superior.
    //  - A un rol de sistema no se le cambia el nombre ni el nivel (solo secciones/permisos).
    //  - Nadie otorga un permiso o una seccion que el mismo no tiene (no escalar privilegios).
    //  - Un rol OPERATIVO nunca recibe la seccion Roles ni permisos de roles (DemoPV: un cajero
    //    jamas puede editar permisos); un rol SUPERVISOR (Dueño) nunca la pierde.

    @Transactional
    public RolDto crear(Usuario actor, Long centroId, Centro centro, RolRequest request) {
        if (!esSuperAdmin(actor)) {
            throw new IllegalStateException("Solo el administrador de la plataforma puede crear roles nuevos");
        }
        if (rolRepository.existsByCentroIdAndNombre(centroId, request.nombre())) {
            throw new FieldConflictException("nombre", "Ya existe un rol con ese nombre");
        }
        Rol rol = new Rol();
        rol.setCentro(centro);
        rol.setNombre(request.nombre().trim());
        rol.setNivel(parseNivelAsignable(request.nivel()));
        rol.setSecciones(sanearSecciones(parseSecciones(request.secciones()), rol.getNivel()));
        rol.setPermisos(sanearPermisos(parsePermisos(request.permisos()), rol.getNivel()));
        return toDto(rolRepository.save(rol));
    }

    @Transactional
    public RolDto actualizar(Usuario actor, Long centroId, Long id, RolRequest request) {
        Rol rol = buscarDelCentro(centroId, id);
        boolean superAdmin = esSuperAdmin(actor);
        if (!superAdmin) assertNivelInferior(actor, rol.getNivel(), "modificar");

        if (rol.isEsSistema()) {
            // Nombre y nivel fijos: el resto del sistema los reconoce por eso.
            if (request.nombre() != null && !request.nombre().trim().equals(rol.getNombre())) {
                throw new IllegalStateException("No se puede renombrar un rol de sistema");
            }
        } else {
            String nombre = request.nombre().trim();
            if (!nombre.equalsIgnoreCase(rol.getNombre()) && rolRepository.existsByCentroIdAndNombre(centroId, nombre)) {
                throw new FieldConflictException("nombre", "Ya existe un rol con ese nombre");
            }
            rol.setNombre(nombre);
            NivelJerarquia nivel = parseNivelAsignable(request.nivel());
            if (!superAdmin) assertNivelInferior(actor, nivel, "asignar");
            rol.setNivel(nivel);
        }

        Set<Seccion> secciones = sanearSecciones(parseSecciones(request.secciones()), rol.getNivel());
        Set<Permiso> permisos = sanearPermisos(parsePermisos(request.permisos()), rol.getNivel());
        if (!superAdmin) {
            // Solo se valida lo que se AGREGA: un rol puede conservar algo que el actor no tiene.
            for (Seccion s : secciones) {
                if (!rol.getSecciones().contains(s) && !actor.getRol().getSecciones().contains(s)) {
                    throw new IllegalStateException("No puedes otorgar la seccion " + s + " porque tu no la tienes");
                }
            }
            for (Permiso p : permisos) {
                if (!rol.getPermisos().contains(p) && !PermisoEvaluator.tiene(actor, p)) {
                    throw new IllegalStateException("No puedes otorgar el permiso " + p + " porque tu no lo tienes");
                }
            }
        }
        rol.setSecciones(secciones);
        rol.setPermisos(permisos);
        return toDto(rolRepository.save(rol));
    }

    @Transactional
    public void eliminar(Usuario actor, Long centroId, Long id) {
        Rol rol = buscarDelCentro(centroId, id);
        if (rol.isEsSistema()) {
            throw new IllegalStateException("No se puede eliminar un rol de sistema");
        }
        if (!esSuperAdmin(actor)) assertNivelInferior(actor, rol.getNivel(), "eliminar");
        if (usuarioRepository.countByRolIdAndDeletedAtIsNull(rol.getId()) > 0) {
            throw new IllegalStateException("No se puede eliminar: hay usuarios con este rol asignado");
        }
        rol.setActivo(false);
        rolRepository.save(rol);
    }

    private static boolean esSuperAdmin(Usuario actor) {
        return actor.getRol().getNivel() == NivelJerarquia.SUPER_ADMIN;
    }

    /** El orden de NivelJerarquia ES la jerarquia (SUPER_ADMIN=0 ... OPERATIVO=3). */
    private static void assertNivelInferior(Usuario actor, NivelJerarquia nivelObjetivo, String accion) {
        if (nivelObjetivo.ordinal() <= actor.getRol().getNivel().ordinal()) {
            throw new IllegalStateException("No puedes " + accion + " un rol de tu mismo nivel o superior");
        }
    }

    private static Set<Seccion> sanearSecciones(Set<Seccion> secciones, NivelJerarquia nivel) {
        Set<Seccion> s = new java.util.HashSet<>(secciones);
        s.remove(CENTROS); // la administracion de centros es solo de la plataforma
        if (nivel == NivelJerarquia.OPERATIVO) s.remove(ROLES);
        if (nivel == NivelJerarquia.SUPERVISOR) s.add(ROLES); // el Dueño nunca se bloquea la pantalla de roles
        return s;
    }

    private static Set<Permiso> sanearPermisos(Set<Permiso> permisos, NivelJerarquia nivel) {
        Set<Permiso> p = new java.util.HashSet<>(permisos);
        if (nivel == NivelJerarquia.OPERATIVO) {
            p.remove(ROLES_VER);
            p.remove(ROLES_ADMINISTRAR);
        }
        if (nivel == NivelJerarquia.SUPERVISOR) {
            p.add(ROLES_VER);
            p.add(ROLES_ADMINISTRAR);
        }
        return p;
    }

    /** Nunca devuelve un rol de otro centro (ni el global SUPER_ADMIN): evita que un
     * Dueno/Administrador edite o borre roles ajenos adivinando el id. */
    private Rol buscarDelCentro(Long centroId, Long id) {
        Rol rol = rolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (rol.getCentro() == null || !rol.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Rol no encontrado");
        }
        return rol;
    }

    private Set<Seccion> parseSecciones(Set<String> nombres) {
        return nombres.stream().map(Seccion::valueOf).collect(Collectors.toSet());
    }

    private Set<Permiso> parsePermisos(Set<String> nombres) {
        if (nombres == null) return Set.of();
        return nombres.stream().map(Permiso::valueOf).collect(Collectors.toSet());
    }

    /** Un rol creado/editado desde la API de un centro NUNCA puede ser SUPERVISOR ni
     * SUPER_ADMIN (seccion 23 del encargo: nadie puede auto-escalarse ni escalar a
     * otros mas alla de lo que la plataforma ya le dio). Esos dos niveles solo existen
     * via el seed automatico (Dueno al crear un Centro) o el DataSeeder de plataforma. */
    private NivelJerarquia parseNivelAsignable(String nivel) {
        if (nivel == null || nivel.isBlank()) return NivelJerarquia.OPERATIVO;
        NivelJerarquia n = NivelJerarquia.valueOf(nivel);
        if (n == NivelJerarquia.SUPERVISOR || n == NivelJerarquia.SUPER_ADMIN) {
            throw new IllegalStateException("No puedes crear un rol de nivel " + n);
        }
        return n;
    }

    /** Crea el paquete de roles por defecto para un centro nuevo: Dueno/Administrador/
     * Recepcion/Entrenador (ya existian) + dos plantillas sugeridas nuevas (Caja/Ventas,
     * Encargado de Inventario, seccion 10-14 del encargo). Todas con nivel + permisos
     * granulares ademas de secciones, para que "ADMIN" deje de significar acceso total. */
    @Transactional
    public void seedRolesPorDefecto(Centro centro) {
        crearRolSistemaSiNoExiste(centro, "Dueno", NivelJerarquia.SUPERVISOR,
                EnumSet.allOf(Seccion.class), EnumSet.allOf(Permiso.class));

        crearRolSistemaSiNoExiste(centro, "Administrador", NivelJerarquia.ADMIN,
                EnumSet.of(DASHBOARD, ALUMNOS, MEMBRESIAS, CAJA, COMPRAS, DISCIPLINAS, INSTRUCTORES,
                        CLASES, ASISTENCIA, INVENTARIO, TIENDA, REPORTES, USUARIOS, NOTIFICACIONES),
                EnumSet.of(ALUMNOS_VER, ALUMNOS_CREAR, ALUMNOS_EDITAR, ALUMNOS_BAJA,
                        MEMBRESIAS_VER, MEMBRESIAS_CREAR, MEMBRESIAS_RENOVAR, MEMBRESIAS_SUSPENDER, MEMBRESIAS_CANCELAR,
                        PAGOS_VER, PAGOS_REGISTRAR, PAGOS_CANCELAR,
                        CAJA_VER, CAJA_ABRIR, CAJA_MOVIMIENTO, CAJA_CERRAR,
                        INVENTARIO_VER, INVENTARIO_CREAR, INVENTARIO_EDITAR, INVENTARIO_ENTRADA, INVENTARIO_AJUSTE_NEGATIVO,
                        VENTAS_VER, VENTAS_CREAR, VENTAS_CANCELAR,
                        APARTADOS_VER, APARTADOS_GESTIONAR, APARTADOS_CANCELAR,
                        CLASES_VER, CLASES_CREAR, CLASES_EDITAR, CLASES_CANCELAR,
                        ASISTENCIAS_VER, ASISTENCIAS_REGISTRAR, INSTRUCTORES_VER, INSTRUCTORES_ADMINISTRAR,
                        REPORTES_OPERATIVOS, REPORTES_FINANCIEROS, USUARIOS_VER, USUARIOS_CREAR, USUARIOS_EDITAR,
                        NOTIFICACIONES_CONFIGURAR));

        crearRolSistemaSiNoExiste(centro, "Recepcion", NivelJerarquia.OPERATIVO,
                EnumSet.of(DASHBOARD, ALUMNOS, MEMBRESIAS, CAJA, CLASES, DISCIPLINAS, ASISTENCIA, INVENTARIO, TIENDA, NOTIFICACIONES),
                // Recepcion vende en el POS (productos y membresias) y por eso abre/cierra
                // su propio corte de caja (igual que DemoPV: cada cajero abre el suyo, ver
                // VentaService#crear). Ve Clases/Instalaciones/Inventario/Planes en modo
                // lectura: no tiene CLASES_CREAR/EDITAR/CANCELAR, INSTALACIONES_ADMINISTRAR,
                // INVENTARIO_CREAR/EDITAR/ENTRADA/AJUSTE_NEGATIVO ni MEMBRESIAS_PLANES_ADMINISTRAR.
                EnumSet.of(ALUMNOS_VER, ALUMNOS_CREAR, ALUMNOS_EDITAR,
                        MEMBRESIAS_VER, MEMBRESIAS_CREAR, MEMBRESIAS_RENOVAR, PAGOS_VER, PAGOS_REGISTRAR,
                        APARTADOS_VER, CLASES_VER, ASISTENCIAS_VER, ASISTENCIAS_REGISTRAR,
                        INVENTARIO_VER, VENTAS_VER, VENTAS_CREAR, CAJA_VER, CAJA_ABRIR, CAJA_CERRAR));

        // NOTIFICACIONES aqui NO abre el broadcast general de Recepcion/Encargado/Dueno
        // (ver NotificacionService#esPersonalOperativo): un Entrenador solo vera sus
        // propios avisos (INSTRUCTOR_*, ver destinatarioTipo=INSTRUCTOR) mas alla de
        // tener esta seccion, que solo habilita la pantalla/endpoint de notificaciones.
        crearRolSistemaSiNoExiste(centro, "Entrenador", NivelJerarquia.OPERATIVO,
                EnumSet.of(CLASES, ASISTENCIA, ALUMNOS, NOTIFICACIONES),
                EnumSet.of(CLASES_VER, ASISTENCIAS_VER, ASISTENCIAS_REGISTRAR, ALUMNOS_VER));

        crearRolSistemaSiNoExiste(centro, "Caja / Ventas", NivelJerarquia.OPERATIVO,
                EnumSet.of(DASHBOARD, CAJA, TIENDA, INVENTARIO, NOTIFICACIONES),
                EnumSet.of(CAJA_VER, CAJA_ABRIR, CAJA_MOVIMIENTO, CAJA_CERRAR,
                        VENTAS_VER, VENTAS_CREAR, INVENTARIO_VER, APARTADOS_VER, APARTADOS_GESTIONAR));

        crearRolSistemaSiNoExiste(centro, "Encargado de Inventario", NivelJerarquia.OPERATIVO,
                EnumSet.of(DASHBOARD, INVENTARIO, NOTIFICACIONES),
                EnumSet.of(INVENTARIO_VER, INVENTARIO_CREAR, INVENTARIO_EDITAR, INVENTARIO_ENTRADA));
    }

    private void crearRolSistemaSiNoExiste(Centro centro, String nombre, NivelJerarquia nivel, Set<Seccion> secciones, Set<Permiso> permisos) {
        if (rolRepository.existsByCentroIdAndNombre(centro.getId(), nombre)) return;
        Rol rol = new Rol();
        rol.setCentro(centro);
        rol.setNombre(nombre);
        rol.setEsSistema(true);
        rol.setNivel(nivel);
        rol.setSecciones(secciones);
        rol.setPermisos(permisos);
        rolRepository.save(rol);
    }

    public RolDto toDto(Rol r) {
        return new RolDto(
                r.getId(),
                r.getCentro() != null ? r.getCentro().getId() : null,
                r.getNombre(),
                r.isEsSistema(),
                r.isActivo(),
                r.getSecciones().stream().map(Enum::name).collect(Collectors.toSet()),
                r.getNivel().name(),
                r.getPermisos().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}
