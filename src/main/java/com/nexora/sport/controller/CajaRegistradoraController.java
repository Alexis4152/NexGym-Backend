package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.CajaRegistradoraService;
import com.nexora.sport.service.MovimientoEfectivoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cajas registradoras (cajones fisicos) por sucursal. Alta/edicion y mover efectivo con la caja
 * cerrada: solo administradores de esa sucursal (lo valida el servicio). */
@RestController
@RequestMapping("/api/cajas-registradoras")
@PreAuthorize("@sectionAccess.check('TIENDA')")
public class CajaRegistradoraController {

    private final CajaRegistradoraService cajaService;
    private final MovimientoEfectivoService movimientoService;

    public CajaRegistradoraController(CajaRegistradoraService cajaService, MovimientoEfectivoService movimientoService) {
        this.cajaService = cajaService;
        this.movimientoService = movimientoService;
    }

    /** Cajas visibles con su estado: en uso, o cerrada con lo que recibira el siguiente turno. */
    @GetMapping
    public ApiResponse<List<CajaRegistradoraEstadoDto>> listar(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(cajaService.listarEstado(actor));
    }

    @PostMapping
    public ApiResponse<CajaRegistradoraEstadoDto> crear(@AuthenticationPrincipal Usuario actor,
                                                        @Valid @RequestBody CajaRegistradoraRequest request) {
        return ApiResponse.ok("Caja creada", cajaService.crear(request, actor));
    }

    @PutMapping("/{id}")
    public ApiResponse<CajaRegistradoraEstadoDto> actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                             @Valid @RequestBody CajaRegistradoraRequest request) {
        return ApiResponse.ok("Caja actualizada", cajaService.actualizar(id, request, actor));
    }

    /** Retirar o agregar efectivo con la caja cerrada (entre turnos). */
    @PostMapping("/{id}/movimientos")
    public ApiResponse<MovimientoEfectivoDto> movimientoConCajaCerrada(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                                       @Valid @RequestBody MovimientoEfectivoRequest request) {
        return ApiResponse.ok("Movimiento registrado",
                movimientoService.toDto(movimientoService.registrarConCajaCerrada(id, request, actor)));
    }

    /** Personal del centro, para "quien retiro el dinero" / "quien reporto el conteo". */
    @GetMapping("/personas")
    public ApiResponse<List<CajeroDto>> personas(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(cajaService.personas(actor));
    }
}
