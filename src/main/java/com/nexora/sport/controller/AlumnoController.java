package com.nexora.sport.controller;

import com.nexora.sport.dto.AlumnoDto;
import com.nexora.sport.dto.AlumnoRequest;
import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.AlumnoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/alumnos")
@PreAuthorize("@sectionAccess.check('ALUMNOS')")
public class AlumnoController {

    private final AlumnoService alumnoService;

    public AlumnoController(AlumnoService alumnoService) {
        this.alumnoService = alumnoService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AlumnoDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                         @RequestParam(required = false) String q,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(alumnoService.listar(actor, q, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<AlumnoDto> obtener(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok(alumnoService.obtener(actor, id));
    }

    /** Para Asistencia/POS/formularios de busqueda: un lector de QR "escribe" el token
     * en el campo enfocado + Enter, igual que un lector de codigo de barras. */
    @GetMapping("/por-qr/{codigoQr}")
    public ApiResponse<AlumnoDto> buscarPorQr(@AuthenticationPrincipal Usuario actor, @PathVariable String codigoQr) {
        return ApiResponse.ok(alumnoService.buscarPorQr(actor, codigoQr));
    }

    @PostMapping("/{id}/regenerar-qr")
    @PreAuthorize("@sectionAccess.check('ALUMNOS') and @permisoAccess.check('ALUMNOS_EDITAR')")
    public ApiResponse<AlumnoDto> regenerarQr(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok("QR regenerado", alumnoService.regenerarQr(actor, id));
    }

    @PostMapping
    @PreAuthorize("@sectionAccess.check('ALUMNOS') and @permisoAccess.check('ALUMNOS_CREAR')")
    public ApiResponse<AlumnoDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody AlumnoRequest request) {
        return ApiResponse.ok("Alumno registrado", alumnoService.crear(actor, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@sectionAccess.check('ALUMNOS') and @permisoAccess.check('ALUMNOS_EDITAR')")
    public ApiResponse<AlumnoDto> actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id, @Valid @RequestBody AlumnoRequest request) {
        return ApiResponse.ok("Alumno actualizado", alumnoService.actualizar(actor, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@sectionAccess.check('ALUMNOS') and @permisoAccess.check('ALUMNOS_BAJA')")
    public ApiResponse<Void> desactivar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        alumnoService.desactivar(actor, id);
        return ApiResponse.ok("Alumno dado de baja", null);
    }

    @PostMapping("/{id}/foto")
    @PreAuthorize("@sectionAccess.check('ALUMNOS') and @permisoAccess.check('ALUMNOS_EDITAR')")
    public ApiResponse<AlumnoDto> subirFoto(@AuthenticationPrincipal Usuario actor, @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Foto actualizada", alumnoService.subirFoto(actor, id, file));
    }
}
