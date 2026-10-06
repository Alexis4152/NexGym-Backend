package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.TipoMovimiento;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.CajaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/caja")
@PreAuthorize("@sectionAccess.check('CAJA')")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping("/categorias")
    public ApiResponse<List<CategoriaMovimientoDto>> listarCategorias(@AuthenticationPrincipal Usuario actor,
                                                                        @RequestParam(required = false) String tipo) {
        return ApiResponse.ok(cajaService.listarCategorias(actor, tipo != null ? TipoMovimiento.valueOf(tipo) : null));
    }

    @PostMapping("/categorias")
    public ApiResponse<CategoriaMovimientoDto> crearCategoria(@AuthenticationPrincipal Usuario actor,
                                                                @Valid @RequestBody CategoriaMovimientoRequest request) {
        return ApiResponse.ok("Categoria creada", cajaService.crearCategoria(actor, request));
    }

    @GetMapping("/movimientos")
    public ApiResponse<PageResponse<MovimientoFinancieroDto>> listarMovimientos(
            @AuthenticationPrincipal Usuario actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(cajaService.listarMovimientos(actor, desde, hasta, usuarioId, sucursalId, PageRequest.of(page, size)));
    }

    @PostMapping("/ingresos")
    @PreAuthorize("@sectionAccess.check('CAJA') and @permisoAccess.check('CAJA_MOVIMIENTO')")
    public ApiResponse<MovimientoFinancieroDto> registrarIngreso(@AuthenticationPrincipal Usuario actor,
                                                                   @Valid @RequestBody MovimientoFinancieroRequest request) {
        return ApiResponse.ok("Ingreso registrado", cajaService.registrarMovimiento(actor, TipoMovimiento.INGRESO, request));
    }

    /** multipart (no JSON): el comprobante (PDF/imagen, opcional) viaja en la misma
     * peticion que los datos del egreso -- ver CajaService#registrarMovimiento. Sin
     * comprobante, el egreso nace PENDIENTE de aprobacion. */
    @PostMapping(value = "/egresos", consumes = "multipart/form-data")
    @PreAuthorize("@sectionAccess.check('CAJA') and @permisoAccess.check('CAJA_MOVIMIENTO')")
    public ApiResponse<MovimientoFinancieroDto> registrarEgreso(
            @AuthenticationPrincipal Usuario actor,
            @RequestParam Long categoriaId,
            @RequestParam BigDecimal monto,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long alumnoId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) MultipartFile comprobante) {
        var request = new MovimientoFinancieroRequest(categoriaId, monto, metodoPago, descripcion, fecha, alumnoId, proveedorId);
        return ApiResponse.ok("Egreso registrado", cajaService.registrarMovimiento(actor, TipoMovimiento.EGRESO, request, comprobante));
    }

    /** Bandeja de aprobacion: Dueno ve todos los egresos pendientes del centro; un
     * Encargado solo los de las sucursales que administra (ver CajaService#listarPendientes). */
    @GetMapping("/egresos/pendientes")
    public ApiResponse<PageResponse<MovimientoFinancieroDto>> listarPendientes(@AuthenticationPrincipal Usuario actor,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(cajaService.listarPendientes(actor, PageRequest.of(page, size)));
    }

    @PostMapping("/movimientos/{id}/aprobar")
    @PreAuthorize("@sectionAccess.check('CAJA') and @permisoAccess.check('CAJA_MOVIMIENTO')")
    public ApiResponse<MovimientoFinancieroDto> aprobar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok("Egreso aprobado", cajaService.aprobar(actor, id));
    }

    @PostMapping("/movimientos/{id}/rechazar")
    @PreAuthorize("@sectionAccess.check('CAJA') and @permisoAccess.check('CAJA_MOVIMIENTO')")
    public ApiResponse<MovimientoFinancieroDto> rechazar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                          @Valid @RequestBody RechazarMovimientoRequest request) {
        return ApiResponse.ok("Egreso rechazado", cajaService.rechazar(actor, id, request.motivo()));
    }
}
