package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.VentaDto;
import com.nexora.sport.dto.VentaRequest;
import com.nexora.sport.model.EstadoVenta;
import com.nexora.sport.model.MetodoPago;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ventas")
@PreAuthorize("@sectionAccess.check('TIENDA')")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public ApiResponse<PageResponse<VentaDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                        @RequestParam(required = false) String cliente,
                                                        @RequestParam(required = false) String metodoPago,
                                                        @RequestParam(required = false) String estado,
                                                        @RequestParam(required = false) Long usuarioId,
                                                        @RequestParam(required = false) Long sucursalId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        MetodoPago metodo = (metodoPago != null && !metodoPago.isBlank()) ? MetodoPago.valueOf(metodoPago) : null;
        EstadoVenta estadoVenta = (estado != null && !estado.isBlank()) ? EstadoVenta.valueOf(estado) : null;
        return ApiResponse.ok(ventaService.listar(actor, desde, hasta, cliente, metodo, estadoVenta, usuarioId, sucursalId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<VentaDto> obtener(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok(ventaService.obtener(actor, id));
    }

    @GetMapping("/{id}/ticket-escpos")
    public ApiResponse<String> ticketEscPos(@PathVariable Long id) {
        return ApiResponse.ok(ventaService.ticketEscPos(id));
    }

    @PostMapping
    public ApiResponse<VentaDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody VentaRequest request) {
        return ApiResponse.ok("Venta registrada", ventaService.crear(actor, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<VentaDto> cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Venta cancelada", ventaService.cancelar(id, actor));
    }
}
