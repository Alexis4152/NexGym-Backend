package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.reportes.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reportes y analitica. "Que paso durante un periodo y por que" (vs. Dashboard, que
 * responde "que esta pasando ahora" — ver ReporteController vs. DashboardController, no
 * se duplica logica: Dashboard sigue calculando sus propios totales del mes en curso).
 * Todos los endpoints requieren la seccion REPORTES (hoy solo la tienen Dueno,
 * Administrador y SUPER_ADMIN via RolService#seedRolesPorDefecto).
 */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("@sectionAccess.check('REPORTES')")
public class ReporteController {

    private final ReporteTiendaService tiendaService;
    private final ReporteFinancieroService financieroService;
    private final ReporteMembresiaService membresiaService;
    private final ReporteAlumnoService alumnoService;
    private final ReporteOperacionService operacionService;
    private final ReportePdfService pdfService;

    public ReporteController(ReporteTiendaService tiendaService, ReporteFinancieroService financieroService,
                              ReporteMembresiaService membresiaService, ReporteAlumnoService alumnoService,
                              ReporteOperacionService operacionService, ReportePdfService pdfService) {
        this.tiendaService = tiendaService;
        this.financieroService = financieroService;
        this.membresiaService = membresiaService;
        this.alumnoService = alumnoService;
        this.operacionService = operacionService;
        this.pdfService = pdfService;
    }

    // ---- Tienda ----

    @GetMapping("/tienda/resumen")
    public ApiResponse<VentaResumenDto> tiendaResumen(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(tiendaService.resumen(actor, from, to));
    }

    @GetMapping("/tienda/resumen-comparativo")
    public ApiResponse<ComparativoDto> tiendaResumenComparativo(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(tiendaService.resumenComparativo(actor, from, to));
    }

    @GetMapping("/tienda/por-dia")
    public ApiResponse<List<SeriePuntoDto>> tiendaPorDia(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(tiendaService.ventasPorDia(actor, from, to));
    }

    @GetMapping("/tienda/por-mes")
    public ApiResponse<List<SeriePuntoDto>> tiendaPorMes(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(tiendaService.ventasPorMes(actor, from, to));
    }

    @GetMapping("/tienda/productos")
    public ApiResponse<List<ProductoAnaliticaDto>> tiendaProductos(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                     @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(tiendaService.topProductos(actor, from, to, limit));
    }

    @GetMapping("/tienda/productos-margen")
    public ApiResponse<List<ProductoAnaliticaDto>> tiendaProductosMargen(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                           @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(tiendaService.topProductosPorMargen(actor, from, to, limit));
    }

    @GetMapping("/tienda/categorias")
    public ApiResponse<List<EtiquetaValorDto>> tiendaCategorias(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(tiendaService.ventasPorCategoria(actor, from, to));
    }

    // ---- Finanzas ----

    @GetMapping("/finanzas/resumen")
    public ApiResponse<FinancieroResumenDto> finanzasResumen(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(financieroService.resumen(actor, from, to));
    }

    @GetMapping("/finanzas/resumen-comparativo")
    public ApiResponse<ComparativoDto> finanzasResumenComparativo(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(financieroService.resumenComparativo(actor, from, to));
    }

    @GetMapping("/finanzas/metodos-pago")
    public ApiResponse<Map<String, List<EtiquetaValorDto>>> finanzasMetodosPago(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(financieroService.metodosDePago(actor, from, to));
    }

    @GetMapping("/finanzas/cartera")
    public ApiResponse<CarteraResumenDto> cartera(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(financieroService.cartera(actor));
    }

    // ---- Membresias ----

    @GetMapping("/membresias/ventas")
    public ApiResponse<MembresiaVentasResumenDto> membresiasVentas(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(membresiaService.ventas(actor, from, to));
    }

    @GetMapping("/membresias/vencimientos")
    public ApiResponse<VencimientosResumenDto> membresiasVencimientos(@AuthenticationPrincipal Usuario actor, @RequestParam(defaultValue = "7") int horizonteDias) {
        return ApiResponse.ok(membresiaService.vencimientos(actor, horizonteDias));
    }

    @GetMapping("/membresias/renovaciones")
    public ApiResponse<RenovacionesResumenDto> membresiasRenovaciones(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(membresiaService.renovaciones(actor, from, to));
    }

    // ---- Alumnos ----

    @GetMapping("/alumnos/resumen")
    public ApiResponse<AlumnosResumenDto> alumnosResumen(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(alumnoService.resumen(actor, from, to));
    }

    @GetMapping("/alumnos/retencion")
    public ApiResponse<RetencionResumenDto> alumnosRetencion(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(alumnoService.retencion(actor, from, to));
    }

    @GetMapping("/alumnos/riesgo-abandono")
    public ApiResponse<List<RiesgoAbandonoItemDto>> alumnosRiesgo(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(alumnoService.riesgoAbandono(actor));
    }

    // ---- Operacion ----

    @GetMapping("/operacion/asistencias")
    public ApiResponse<AsistenciasResumenDto> operacionAsistencias(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(operacionService.asistencias(actor, from, to));
    }

    @GetMapping("/operacion/ocupacion-clases")
    public ApiResponse<List<OcupacionClaseDto>> operacionOcupacion(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                     @RequestParam(required = false) Long sucursalId) {
        return ApiResponse.ok(operacionService.ocupacionClases(actor, from, to, sucursalId));
    }

    @GetMapping("/operacion/instructores")
    public ApiResponse<List<InstructorAnaliticaDto>> operacionInstructores(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(operacionService.instructores(actor, from, to));
    }

    @GetMapping("/operacion/disciplinas")
    public ApiResponse<List<DisciplinaAnaliticaDto>> operacionDisciplinas(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(operacionService.disciplinas(actor, from, to));
    }

    @GetMapping("/operacion/usuarios")
    public ApiResponse<List<OperacionUsuarioDto>> operacionUsuarios(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(operacionService.operacionesPorUsuario(actor, from, to));
    }

    // ---- PDF ----

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal Usuario actor, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = pdfService.generar(actor, from, to);
        String filename = "reporte-" + from + "-a-" + to + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
