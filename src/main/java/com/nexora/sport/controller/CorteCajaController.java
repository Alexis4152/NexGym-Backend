package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.CorteCajaService;
import com.nexora.sport.service.MovimientoEfectivoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cortes-caja")
@PreAuthorize("@sectionAccess.check('TIENDA')")
public class CorteCajaController {

    private final CorteCajaService corteCajaService;
    private final MovimientoEfectivoService movimientoService;

    public CorteCajaController(CorteCajaService corteCajaService, MovimientoEfectivoService movimientoService) {
        this.corteCajaService = corteCajaService;
        this.movimientoService = movimientoService;
    }

    @GetMapping("/abierto")
    public ApiResponse<CorteCajaDto> abierto(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(corteCajaService.abierto(actor));
    }

    @GetMapping
    public ApiResponse<PageResponse<CorteCajaDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                            @RequestParam(required = false) String estado,
                                                            @RequestParam(required = false) Long usuarioId,
                                                            @RequestParam(required = false) Long sucursalId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(corteCajaService.listar(actor, desde, hasta, estado, usuarioId, sucursalId, PageRequest.of(page, size)));
    }

    @GetMapping("/cajeros")
    public ApiResponse<List<CajeroDto>> cajeros(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(corteCajaService.cajeros(actor));
    }

    @GetMapping("/{id}")
    public ApiResponse<CorteCajaDto> obtener(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok(corteCajaService.obtener(actor, id));
    }

    @GetMapping("/{id}/resumen")
    public ApiResponse<CorteCajaResumenDto> resumen(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok(corteCajaService.resumen(actor, id));
    }

    /** Abrir recibiendo una caja: { cajaRegistradoraId, montoContado, deAcuerdo, confirmacionDiferenciaGrande?, nota? }. */
    @PostMapping("/abrir")
    @PreAuthorize("@sectionAccess.check('TIENDA') and @permisoAccess.check('CAJA_ABRIR')")
    public ApiResponse<CorteCajaDto> abrir(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody AbrirCorteRequest request) {
        return ApiResponse.ok("Corte abierto", corteCajaService.abrir(actor, request));
    }

    /** Cerrar con arqueo: { montoContado, notas?, confirmacionDiferenciaGrande? }. */
    @PostMapping("/{id}/cerrar")
    @PreAuthorize("@sectionAccess.check('TIENDA') and @permisoAccess.check('CAJA_CERRAR')")
    public ApiResponse<CorteCajaDto> cerrar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                             @Valid @RequestBody CerrarCorteRequest request) {
        return ApiResponse.ok("Corte cerrado", corteCajaService.cerrar(id, actor, request));
    }

    /** Cierre forzado por un administrador (valida el nivel en el servicio). */
    @PostMapping("/{id}/forzar-cierre")
    public ApiResponse<CorteCajaDto> forzarCierre(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                                   @Valid @RequestBody ForzarCierreCorteRequest request) {
        return ApiResponse.ok("Corte cerrado", corteCajaService.forzarCierre(id, actor, request));
    }

    @GetMapping("/{id}/movimientos")
    public ApiResponse<List<MovimientoEfectivoDto>> movimientos(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok(corteCajaService.movimientos(actor, id));
    }

    /** Movimientos con la caja cerrada que vio este corte al recibir la caja. */
    @GetMapping("/{id}/movimientos-recibidos")
    public ApiResponse<List<MovimientoEfectivoDto>> movimientosRecibidos(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok(corteCajaService.movimientosRecibidos(actor, id));
    }

    @PostMapping("/{id}/movimientos")
    public ApiResponse<MovimientoEfectivoDto> registrarMovimiento(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                                   @Valid @RequestBody MovimientoEfectivoRequest request) {
        return ApiResponse.ok("Movimiento registrado", movimientoService.toDto(movimientoService.registrarEnCorte(id, request, actor)));
    }

    /** Sube un comprobante (ticket de gasto, evidencia de conteo) y devuelve su ruta para el movimiento. */
    @PostMapping(value = "/comprobantes", consumes = "multipart/form-data")
    public ApiResponse<String> subirComprobante(@AuthenticationPrincipal Usuario actor, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(movimientoService.subirComprobante(file, actor));
    }
}
