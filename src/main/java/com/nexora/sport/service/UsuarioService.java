package com.nexora.sport.service;

import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.UsuarioDto;
import com.nexora.sport.dto.UsuarioRequest;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.NivelJerarquia;
import com.nexora.sport.model.Permiso;
import com.nexora.sport.model.Rol;
import com.nexora.sport.model.Sucursal;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.RolRepository;
import com.nexora.sport.repository.SucursalRepository;
import com.nexora.sport.repository.UsuarioRepository;
import com.nexora.sport.security.PermisoEvaluator;
import com.nexora.sport.security.TenantScope;
import com.nexora.sport.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CentroRepository centroRepository;
    private final SucursalRepository sucursalRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final TenantScope tenantScope;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                           CentroRepository centroRepository, SucursalRepository sucursalRepository,
                           PasswordEncoder passwordEncoder, MailService mailService, TenantScope tenantScope) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.centroRepository = centroRepository;
        this.sucursalRepository = sucursalRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioDto> listar(Usuario actor, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        return PageResponse.of(usuarioRepository.findByCentroIdAndDeletedAtIsNull(centroId, pageable), this::toDto);
    }

    @Transactional
    public UsuarioDto crear(Usuario actor, UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new FieldConflictException("email", "Ya existe un usuario con ese correo");
        }
        Long centroId = tenantScope.scopeId(actor);
        Rol rol = buscarRolAsignableDelCentro(actor, centroId, request.rolId());
        assertSucursalRequeridaSiAplica(centroId, rol, request.sucursalId());
        Sucursal sucursal = buscarSucursalDelCentro(centroId, request.sucursalId());
        assertPuedeAsignarSucursal(actor, sucursal);
        Set<Sucursal> adicionales = resolverSucursalesAdicionales(centroId, request.sucursalesAdicionalesIds());
        adicionales.forEach(s -> assertPuedeAsignarSucursal(actor, s));

        String tempPassword = PasswordGenerator.generate();
        Usuario usuario = new Usuario();
        usuario.setCentro(centroRepository.getReferenceById(centroId));
        usuario.setSucursal(sucursal);
        usuario.setSucursalesAdicionales(adicionales);
        usuario.setRol(rol);
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(tempPassword));
        usuario.setMustChangePassword(true);
        usuario = usuarioRepository.save(usuario);

        String nombreCentro = usuario.getCentro().getNombre();
        mailService.send(usuario.getEmail(), "Tu acceso a " + nombreCentro + " (NexoraSport)",
                "Se creo una cuenta para ti en " + nombreCentro + ".\n\n" +
                        "Usuario: " + usuario.getEmail() +
                        "\nContrasena temporal: " + tempPassword +
                        "\n\nDeberas cambiarla al iniciar sesion por primera vez.");
        // Si no hay SMTP configurado el correo se descarta silenciosamente (MailService es
        // "best effort"); dejamos la contrasena temporal en el log para no perderla en dev.
        log.info("Usuario creado {} / rol {} — contrasena temporal: {}", usuario.getEmail(), rol.getNombre(), tempPassword);
        return toDto(usuario);
    }

    @Transactional
    public UsuarioDto actualizar(Usuario actor, Long id, UsuarioRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Usuario usuario = buscarDelCentro(centroId, id);
        assertPuedeGestionar(actor, usuario);
        Rol rol = buscarRolAsignableDelCentro(actor, centroId, request.rolId());
        assertSucursalRequeridaSiAplica(centroId, rol, request.sucursalId());
        Sucursal sucursal = buscarSucursalDelCentro(centroId, request.sucursalId());
        assertPuedeAsignarSucursal(actor, sucursal);
        Set<Sucursal> adicionales = resolverSucursalesAdicionales(centroId, request.sucursalesAdicionalesIds());
        adicionales.forEach(s -> assertPuedeAsignarSucursal(actor, s));
        usuario.setNombre(request.nombre());
        usuario.setRol(rol);
        usuario.setSucursal(sucursal);
        usuario.setSucursalesAdicionales(adicionales);
        return toDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desactivar(Usuario actor, Long id) {
        Long centroId = tenantScope.scopeId(actor);
        Usuario usuario = buscarDelCentro(centroId, id);
        if (usuario.getId().equals(actor.getId())) {
            throw new IllegalStateException("No puedes desactivar tu propia cuenta");
        }
        assertPuedeGestionar(actor, usuario);
        usuario.setActivo(false);
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    /**
     * Excepciones de permisos sobre el Rol de un usuario (seccion 22 del encargo):
     * agregar/quitar sin crear un rol nuevo solo para una persona. Un actor nunca
     * puede otorgar un permiso que el mismo no tiene (seccion 23: no escalar).
     */
    @Transactional
    public UsuarioDto actualizarPermisos(Usuario actor, Long id, Set<String> extra, Set<String> revocados) {
        Long centroId = tenantScope.scopeId(actor);
        Usuario usuario = buscarDelCentro(centroId, id);
        assertPuedeGestionar(actor, usuario);

        Set<Permiso> extraPermisos = parsePermisos(extra);
        if (!tenantScope.isSuperAdmin(actor)) {
            for (Permiso p : extraPermisos) {
                if (!PermisoEvaluator.tiene(actor, p)) {
                    throw new IllegalStateException("No puedes otorgar un permiso que tu mismo no tienes: " + p);
                }
            }
        }
        usuario.setPermisosExtra(extraPermisos);
        usuario.setPermisosRevocados(parsePermisos(revocados));
        return toDto(usuarioRepository.save(usuario));
    }

    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    /** Nunca devuelve un usuario de otro centro: evita que un Dueno/Administrador
     * edite/desactive usuarios ajenos adivinando el id (fuga de multi-tenant). */
    private Usuario buscarDelCentro(Long centroId, Long id) {
        Usuario usuario = buscar(id);
        if (usuario.getCentro() == null || !usuario.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        return usuario;
    }

    /** Un Admin/Operativo necesita una sucursal asignada para que su alcance quede
     * realmente acotado (TenantScope.sucursalesPermitidas trata "sin ninguna sucursal"
     * como "ve todas"). Si el centro aun no tiene ninguna sucursal registrada, no se
     * exige (no hay que elegir). */
    private void assertSucursalRequeridaSiAplica(Long centroId, Rol rol, Long sucursalId) {
        boolean requiereSucursal = rol.getNivel() == NivelJerarquia.ADMIN || rol.getNivel() == NivelJerarquia.OPERATIVO;
        if (requiereSucursal && sucursalId == null && sucursalRepository.existsByCentroIdAndActivoTrue(centroId)) {
            throw new IllegalStateException("Selecciona la sucursal a la que estara asignado este usuario");
        }
    }

    /** Un actor solo puede asignar sucursales que EL MISMO administra (seccion 12/23 del
     * encargo): Carlos con Centro+Norte no puede asignarle Sur a un usuario nuevo,
     * aunque Sur exista en el mismo Centro. SUPER_ADMIN/Supervisor (sin restriccion) no
     * tienen este limite -- pueden asignar cualquier sucursal del centro. */
    private void assertPuedeAsignarSucursal(Usuario actor, Sucursal sucursal) {
        if (sucursal == null) return;
        if (!tenantScope.sucursalPermite(actor, sucursal.getId())) {
            throw new IllegalStateException("No administras la sucursal \"" + sucursal.getNombre() + "\", no puedes asignarla");
        }
    }

    private Sucursal buscarSucursalDelCentro(Long centroId, Long sucursalId) {
        if (sucursalId == null) return null;
        Sucursal s = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        if (!s.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Sucursal no encontrada");
        }
        return s;
    }

    /** Sucursales adicionales (p. ej. un gerente a cargo de varias): cada una debe
     * pertenecer al mismo centro, igual que la sucursal "hogar". */
    private Set<Sucursal> resolverSucursalesAdicionales(Long centroId, Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        Set<Sucursal> resueltas = new HashSet<>();
        for (Long id : ids) {
            resueltas.add(buscarSucursalDelCentro(centroId, id));
        }
        return resueltas;
    }

    private Set<Permiso> parsePermisos(Set<String> nombres) {
        if (nombres == null) return Set.of();
        return nombres.stream().map(Permiso::valueOf).collect(Collectors.toSet());
    }

    /** El rol a asignar debe pertenecer al centro del actor (nunca el global SUPER_ADMIN,
     * ni el de otro centro) y su nivel debe ser estrictamente inferior al del actor. */
    private Rol buscarRolAsignableDelCentro(Usuario actor, Long centroId, Long rolId) {
        Rol rol = rolRepository.findById(rolId).orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (rol.getCentro() == null || !rol.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Rol no encontrado");
        }
        if (!tenantScope.isSuperAdmin(actor) && rango(rol.getNivel()) <= rango(actor.getRol().getNivel())) {
            throw new IllegalStateException("No puedes asignar un rol igual o superior al tuyo");
        }
        return rol;
    }

    /** Solo SUPER_ADMIN administra cuentas (incluida la propia): nadie mas puede editar
     * ni ajustar los permisos de su propia cuenta -- evita que Dueno/Admin se auto-otorguen
     * o auto-editen su registro. Fuera de ese caso, sigue aplicando la regla de rango:
     * un actor no puede administrar a otro de su mismo nivel o superior. */
    private void assertPuedeGestionar(Usuario actor, Usuario objetivo) {
        if (tenantScope.isSuperAdmin(actor)) return;
        if (objetivo.getId().equals(actor.getId())) {
            throw new IllegalStateException("No puedes editar tu propia cuenta; solicita el cambio a un SUPER_ADMIN");
        }
        if (rango(objetivo.getRol().getNivel()) <= rango(actor.getRol().getNivel())) {
            throw new IllegalStateException("No puedes administrar un usuario de tu mismo nivel o superior");
        }
    }

    /** El orden de declaracion de NivelJerarquia YA es la jerarquia (SUPER_ADMIN=0 ... OPERATIVO=3):
     * una sola fuente de verdad, nada que mantener sincronizado aparte. */
    private int rango(NivelJerarquia nivel) {
        return nivel.ordinal();
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtenerPerfil(Long id) {
        return toDto(buscar(id));
    }

    public UsuarioDto toDto(Usuario u) {
        Set<Permiso> efectivos = Arrays.stream(Permiso.values())
                .filter(p -> PermisoEvaluator.tiene(u, p))
                .collect(Collectors.toSet());
        return new UsuarioDto(
                u.getId(),
                u.getCentro() != null ? u.getCentro().getId() : null,
                u.getCentro() != null ? u.getCentro().getNombre() : null,
                u.getSucursal() != null ? u.getSucursal().getId() : null,
                u.getSucursal() != null ? u.getSucursal().getNombre() : null,
                u.getSucursalesAdicionales().stream().map(Sucursal::getId).collect(Collectors.toSet()),
                u.getSucursalesAdicionales().stream().map(Sucursal::getNombre).collect(Collectors.toSet()),
                (u.getSucursal() != null ? 1 : 0) + u.getSucursalesAdicionales().size() > 1,
                u.getRol().getId(),
                u.getRol().getNombre(),
                u.getRol().getNivel().name(),
                u.getRol().getSecciones().stream().map(Enum::name).collect(Collectors.toSet()),
                efectivos.stream().map(Enum::name).collect(Collectors.toSet()),
                !u.getCentrosAdicionales().isEmpty(),
                u.getNombre(),
                u.getEmail(),
                u.isActivo(),
                u.isMustChangePassword()
        );
    }
}
