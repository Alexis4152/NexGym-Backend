package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.CentroDto;
import com.nexora.sport.dto.CentroRequest;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.security.TenantScope;
import com.nexora.sport.service.ApartadoPdfService;
import com.nexora.sport.service.CentroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/centros")
public class CentroController {

    private final CentroService centroService;
    private final ApartadoPdfService apartadoPdfService;
    private final TenantScope tenantScope;

    public CentroController(CentroService centroService, ApartadoPdfService apartadoPdfService, TenantScope tenantScope) {
        this.centroService = centroService;
        this.apartadoPdfService = apartadoPdfService;
        this.tenantScope = tenantScope;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<CentroDto>> listar() {
        return ApiResponse.ok(centroService.listar());
    }

    /**
     * Centros que este actor puede seleccionar/administrar: SUPER_ADMIN -> todos;
     * SUPERVISOR -> su centro "hogar" + centrosAdicionales (multi-centro, seccion 4-5 del
     * encargo); cualquier otro -> solo el suyo (lista de 1, el frontend no muestra selector).
     * Reutiliza la MISMA pantalla SelectCentro.jsx que ya usaba SUPER_ADMIN.
     */
    @GetMapping("/disponibles")
    public ApiResponse<List<CentroDto>> disponibles(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(centroService.disponiblesPara(actor));
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
    public ApiResponse<CentroDto> actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id, @Valid @RequestBody CentroRequest request) {
        assertPuedeAdministrar(actor, id);
        return ApiResponse.ok("Centro actualizado", centroService.actualizar(id, request));
    }

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @sectionAccess.check('CENTROS')")
    public ApiResponse<CentroDto> subirLogo(@AuthenticationPrincipal Usuario actor, @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        assertPuedeAdministrar(actor, id);
        return ApiResponse.ok("Logo actualizado", centroService.subirLogo(id, file));
    }

    /** Nunca deja que un Dueno edite/vea la configuracion de un centro ajeno adivinando el id. */
    private void assertPuedeAdministrar(Usuario actor, Long centroId) {
        if (!tenantScope.canManageCentro(actor, centroId)) {
            throw new com.nexora.sport.exception.ResourceNotFoundException("Centro no encontrado");
        }
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
