package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.InstructorDto;
import com.nexora.sport.dto.InstructorRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.InstructorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instructores")
@PreAuthorize("@sectionAccess.check('INSTRUCTORES')")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @GetMapping
    public ApiResponse<PageResponse<InstructorDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                             @RequestParam(required = false) String q,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(instructorService.listar(actor, q, PageRequest.of(page, size)));
    }

    @PostMapping
    public ApiResponse<InstructorDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody InstructorRequest request) {
        return ApiResponse.ok("Instructor creado", instructorService.crear(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<InstructorDto> actualizar(@PathVariable Long id, @Valid @RequestBody InstructorRequest request) {
        return ApiResponse.ok("Instructor actualizado", instructorService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        instructorService.desactivar(id);
        return ApiResponse.ok("Instructor desactivado", null);
    }
}
