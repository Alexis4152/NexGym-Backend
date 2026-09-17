package com.nexora.sport.service;

import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.UsuarioDto;
import com.nexora.sport.dto.UsuarioRequest;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.Rol;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.RolRepository;
import com.nexora.sport.repository.UsuarioRepository;
import com.nexora.sport.security.TenantScope;
import com.nexora.sport.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CentroRepository centroRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final TenantScope tenantScope;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                           CentroRepository centroRepository, PasswordEncoder passwordEncoder,
                           MailService mailService, TenantScope tenantScope) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.centroRepository = centroRepository;
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
        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        String tempPassword = PasswordGenerator.generate();
        Usuario usuario = new Usuario();
        usuario.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        usuario.setRol(rol);
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(tempPassword));
        usuario.setMustChangePassword(true);
        usuario = usuarioRepository.save(usuario);

        mailService.send(usuario.getEmail(), "Tu cuenta en NexoraSport",
                "Se creo una cuenta para ti.\n\nCorreo: " + usuario.getEmail() +
                        "\nContrasena temporal: " + tempPassword +
                        "\n\nDeberas cambiarla al iniciar sesion por primera vez.");
        // Si no hay SMTP configurado el correo se descarta silenciosamente (MailService es
        // "best effort"); dejamos la contrasena temporal en el log para no perderla en dev.
        log.info("Usuario creado {} / rol {} — contrasena temporal: {}", usuario.getEmail(), rol.getNombre(), tempPassword);
        return toDto(usuario);
    }

    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscar(id);
        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        usuario.setNombre(request.nombre());
        usuario.setRol(rol);
        return toDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = buscar(id);
        usuario.setActivo(false);
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtenerPerfil(Long id) {
        return toDto(buscar(id));
    }

    public UsuarioDto toDto(Usuario u) {
        return new UsuarioDto(
                u.getId(),
                u.getCentro() != null ? u.getCentro().getId() : null,
                u.getCentro() != null ? u.getCentro().getNombre() : null,
                u.getRol().getId(),
                u.getRol().getNombre(),
                u.getRol().getSecciones().stream().map(Enum::name).collect(Collectors.toSet()),
                u.getNombre(),
                u.getEmail(),
                u.isActivo(),
                u.isMustChangePassword()
        );
    }
}
