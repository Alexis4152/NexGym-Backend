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
    public ApiResponse<UsuarioDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.ok("Usuario creado, se envio su contrasena temporal por correo", usuarioService.crear(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UsuarioDto> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return ApiResponse.ok("Usuario actualizado", usuarioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        usuarioService.desactivar(id);
        return ApiResponse.ok("Usuario desactivado", null);
    }
}
