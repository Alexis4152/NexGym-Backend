package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.NotificacionDto;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.NotificacionSchedulerService;
import com.nexora.sport.service.NotificacionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Campana de notificaciones internas del centro (seccion 14 del encargo). */
@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("@sectionAccess.check('NOTIFICACIONES')")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final NotificacionSchedulerService schedulerService;

    public NotificacionController(NotificacionService notificacionService, NotificacionSchedulerService schedulerService) {
        this.notificacionService = notificacionService;
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificacionDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                               @RequestParam(defaultValue = "false") boolean soloNoLeidas,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificacionService.listarBandeja(actor, soloNoLeidas, PageRequest.of(page, size)));
    }

    @GetMapping("/no-leidas/count")
    public ApiResponse<Map<String, Long>> contarNoLeidas(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(Map.of("count", notificacionService.contarNoLeidas(actor)));
    }

    @PostMapping("/{id}/leer")
    public ApiResponse<Void> marcarLeida(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        notificacionService.marcarLeida(actor, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/leer-todas")
    public ApiResponse<Void> marcarTodasLeidas(@AuthenticationPrincipal Usuario actor) {
        notificacionService.marcarTodasLeidas(actor);
        return ApiResponse.ok(null);
    }

    @GetMapping("/alumno/{alumnoId}")
    public ApiResponse<List<NotificacionDto>> historialPorAlumno(@AuthenticationPrincipal Usuario actor, @PathVariable Long alumnoId) {
        return ApiResponse.ok(notificacionService.historialPorAlumno(actor, alumnoId));
    }

    /**
     * Disparo manual de los jobs programados, SOLO para pruebas (evita esperar al cron
     * diario/de 15 min mientras se valida el modulo). Cruza todos los centros igual que
     * el job real, por eso se restringe a SUPER_ADMIN y no a @sectionAccess: cualquier
     * staff de un centro con acceso a NOTIFICACIONES no deberia poder forzar que se
     * generen avisos en centros ajenos.
     */
    @PostMapping("/scheduler/membresias")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> ejecutarSchedulerMembresias() {
        schedulerService.procesarVencimientosMembresias();
        return ApiResponse.ok("Revision de vencimientos de membresia ejecutada", null);
    }

    @PostMapping("/scheduler/recordatorios-clase")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> ejecutarSchedulerRecordatorios() {
        schedulerService.procesarRecordatoriosClase();
        return ApiResponse.ok("Revision de recordatorios de clase ejecutada", null);
    }
}
