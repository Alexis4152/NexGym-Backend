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

    /** Ver la lista de roles (solo lectura) tambien la necesita quien gestiona Usuarios,
     * para poblar el selector de rol al crear/editar -- no solo quien administra Roles
     * en si. Por eso este metodo reemplaza el @PreAuthorize de clase para aceptar
     * cualquiera de las dos secciones (crear/editar/eliminar siguen exclusivos de ROLES). */
    @GetMapping
    @PreAuthorize("@sectionAccess.check('ROLES') or @sectionAccess.check('USUARIOS')")
    public ApiResponse<List<RolDto>> listar(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(rolService.listar(actor, tenantScope.scopeId(actor)));
    }

    /** Crear roles nuevos: solo SUPER_ADMIN (homologado con DemoPV#RoleController#create). */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<RolDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody RolRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        return ApiResponse.ok("Rol creado", rolService.crear(actor, centroId, centroRepository.getReferenceById(centroId), request));
    }

    /** Dar/quitar secciones y permisos a un rol de nivel inferior al del actor (el servicio
     * valida el nivel y que no se otorgue nada que el actor no tenga). */
    @PutMapping("/{id}")
    @PreAuthorize("@sectionAccess.check('ROLES') and @permisoAccess.check('ROLES_ADMINISTRAR')")
    public ApiResponse<RolDto> actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                          @Valid @RequestBody RolRequest request) {
        return ApiResponse.ok("Rol actualizado", rolService.actualizar(actor, tenantScope.scopeId(actor), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@sectionAccess.check('ROLES') and @permisoAccess.check('ROLES_ADMINISTRAR')")
    public ApiResponse<Void> eliminar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        rolService.eliminar(actor, tenantScope.scopeId(actor), id);
        return ApiResponse.ok("Rol eliminado", null);
    }
}
