package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.DisciplinaDto;
import com.nexora.sport.dto.DisciplinaRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.DisciplinaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disciplinas")
@PreAuthorize("@sectionAccess.check('DISCIPLINAS')")
public class DisciplinaController {

    private final DisciplinaService disciplinaService;

    public DisciplinaController(DisciplinaService disciplinaService) {
        this.disciplinaService = disciplinaService;
    }

    @GetMapping
    public ApiResponse<PageResponse<DisciplinaDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(disciplinaService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/activas")
    public ApiResponse<List<DisciplinaDto>> listarActivas(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(disciplinaService.listarActivas(actor));
    }

    @PostMapping
    public ApiResponse<DisciplinaDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody DisciplinaRequest request) {
        return ApiResponse.ok("Disciplina creada", disciplinaService.crear(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DisciplinaDto> actualizar(@PathVariable Long id, @Valid @RequestBody DisciplinaRequest request) {
        return ApiResponse.ok("Disciplina actualizada", disciplinaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        disciplinaService.desactivar(id);
        return ApiResponse.ok("Disciplina desactivada", null);
    }
}
