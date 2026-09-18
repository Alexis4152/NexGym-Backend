package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.CentroDto;
import com.nexora.sport.dto.CentroRequest;
import com.nexora.sport.service.ApartadoPdfService;
import com.nexora.sport.service.CentroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centros")
public class CentroController {

    private final CentroService centroService;
    private final ApartadoPdfService apartadoPdfService;

    public CentroController(CentroService centroService, ApartadoPdfService apartadoPdfService) {
        this.centroService = centroService;
        this.apartadoPdfService = apartadoPdfService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<CentroDto>> listar() {
        return ApiResponse.ok(centroService.listar());
    }

    @GetMapping("/{id}")
    public ApiResponse<CentroDto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(centroService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<CentroDto> crear(@Valid @RequestBody CentroRequest request) {
        return ApiResponse.ok("Centro creado", centroService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @sectionAccess.check('CENTROS')")
    public ApiResponse<CentroDto> actualizar(@PathVariable Long id, @Valid @RequestBody CentroRequest request) {
        return ApiResponse.ok("Centro actualizado", centroService.actualizar(id, request));
    }

    @GetMapping("/{id}/apartados-promo.pdf")
    @PreAuthorize("@sectionAccess.check('TIENDA')")
    public ResponseEntity<byte[]> folletoApartados(@PathVariable Long id, @RequestParam String url) {
        byte[] pdf = apartadoPdfService.generarFolleto(centroService.buscar(id), url);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=apartados.pdf")
                .body(pdf);
    }

    @GetMapping("/{id}/apartados-qr.png")
    @PreAuthorize("@sectionAccess.check('TIENDA')")
    public ResponseEntity<byte[]> qrApartados(@PathVariable Long id, @RequestParam String url) {
        byte[] png = apartadoPdfService.generarQr(url);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
