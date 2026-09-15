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
    public ApiResponse<AlumnoDto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(alumnoService.obtener(id));
    }

    @PostMapping
    public ApiResponse<AlumnoDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody AlumnoRequest request) {
        return ApiResponse.ok("Alumno registrado", alumnoService.crear(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AlumnoDto> actualizar(@PathVariable Long id, @Valid @RequestBody AlumnoRequest request) {
        return ApiResponse.ok("Alumno actualizado", alumnoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        alumnoService.desactivar(id);
        return ApiResponse.ok("Alumno dado de baja", null);
    }
}
