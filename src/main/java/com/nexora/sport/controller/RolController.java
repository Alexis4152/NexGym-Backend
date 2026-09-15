package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.RolDto;
import com.nexora.sport.dto.RolRequest;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.security.TenantScope;
import com.nexora.sport.service.RolService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("@sectionAccess.check('ROLES')")
public class RolController {

    private final RolService rolService;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public RolController(RolService rolService, CentroRepository centroRepository, TenantScope tenantScope) {
        this.rolService = rolService;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @GetMapping
    public ApiResponse<List<RolDto>> listar(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(rolService.listar(tenantScope.scopeId(actor)));
    }

    @PostMapping
    public ApiResponse<RolDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody RolRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        return ApiResponse.ok("Rol creado", rolService.crear(centroId, centroRepository.getReferenceById(centroId), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RolDto> actualizar(@PathVariable Long id, @Valid @RequestBody RolRequest request) {
        return ApiResponse.ok("Rol actualizado", rolService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        rolService.eliminar(id);
        return ApiResponse.ok("Rol eliminado", null);
    }
}
