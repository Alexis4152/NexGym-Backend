package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.DocumentoInstructorDto;
import com.nexora.sport.dto.InstructorDto;
import com.nexora.sport.dto.InstructorRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.DocumentoInstructorService;
import com.nexora.sport.service.InstructorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/instructores")
@PreAuthorize("@sectionAccess.check('INSTRUCTORES')")
public class InstructorController {

    private final InstructorService instructorService;
    private final DocumentoInstructorService documentoInstructorService;

    public InstructorController(InstructorService instructorService, DocumentoInstructorService documentoInstructorService) {
        this.instructorService = instructorService;
        this.documentoInstructorService = documentoInstructorService;
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
    public ApiResponse<InstructorDto> actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id, @Valid @RequestBody InstructorRequest request) {
        return ApiResponse.ok("Instructor actualizado", instructorService.actualizar(actor, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        instructorService.desactivar(actor, id);
        return ApiResponse.ok("Instructor desactivado", null);
    }

    @PostMapping("/{id}/foto")
    public ApiResponse<InstructorDto> subirFoto(@AuthenticationPrincipal Usuario actor, @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Foto actualizada", instructorService.subirFoto(actor, id, file));
    }

    @GetMapping("/{id}/documentos")
    public ApiResponse<List<DocumentoInstructorDto>> listarDocumentos(@PathVariable Long id) {
        return ApiResponse.ok(documentoInstructorService.listar(id));
    }

    @PostMapping("/{id}/documentos")
    public ApiResponse<DocumentoInstructorDto> subirDocumento(@PathVariable Long id,
                                                               @RequestParam Long disciplinaId,
                                                               @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Documento subido", documentoInstructorService.subir(id, disciplinaId, file));
    }

    @DeleteMapping("/{id}/documentos/{documentoId}")
    public ApiResponse<Void> eliminarDocumento(@PathVariable Long id, @PathVariable Long documentoId) {
        documentoInstructorService.eliminar(id, documentoId);
        return ApiResponse.ok("Documento eliminado", null);
    }
}
