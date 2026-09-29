package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.UsuarioDto;
import com.nexora.sport.dto.UsuarioRequest;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("@sectionAccess.check('USUARIOS')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UsuarioDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(usuarioService.listar(actor, PageRequest.of(page, size)));
    }

    @PostMapping
    @PreAuthorize("@sectionAccess.check('USUARIOS') and @permisoAccess.check('USUARIOS_CREAR')")
    public ApiResponse<UsuarioDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.ok("Usuario creado, se envio su contrasena temporal por correo", usuarioService.crear(actor, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@sectionAccess.check('USUARIOS') and @permisoAccess.check('USUARIOS_EDITAR')")
    public ApiResponse<UsuarioDto> actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                               @Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.ok("Usuario actualizado", usuarioService.actualizar(actor, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        usuarioService.desactivar(actor, id);
        return ApiResponse.ok("Usuario desactivado", null);
    }

    /** Excepciones de permisos sobre el rol de este usuario (seccion 22 del encargo). Body: {"extra": [...], "revocados": [...]}. */
    @PutMapping("/{id}/permisos")
    public ApiResponse<UsuarioDto> actualizarPermisos(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                       @RequestBody Map<String, Set<String>> body) {
        Set<String> extra = body.getOrDefault("extra", Set.of());
        Set<String> revocados = body.getOrDefault("revocados", Set.of());
        return ApiResponse.ok("Permisos actualizados", usuarioService.actualizarPermisos(actor, id, extra, revocados));
    }
}
