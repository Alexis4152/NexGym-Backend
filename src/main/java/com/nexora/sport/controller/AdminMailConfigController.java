package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.MailConfigDto;
import com.nexora.sport.dto.MailConfigRequest;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.MailConfigService;
import com.nexora.sport.service.MailService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Cuenta SMTP global de la plataforma: infraestructura, no algo por centro -- solo SUPER_ADMIN. */
@RestController
@RequestMapping("/api/admin/mail-config")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminMailConfigController {

    private final MailConfigService mailConfigService;
    private final MailService mailService;

    public AdminMailConfigController(MailConfigService mailConfigService, MailService mailService) {
        this.mailConfigService = mailConfigService;
        this.mailService = mailService;
    }

    @GetMapping
    public ApiResponse<MailConfigDto> get() {
        return ApiResponse.ok(mailConfigService.get());
    }

    @PutMapping
    public ApiResponse<MailConfigDto> update(@Valid @RequestBody MailConfigRequest request,
                                             @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Configuracion de correo actualizada", mailConfigService.update(request, actor));
    }

    /** Manda un correo de prueba (sincrono) al propio SUPER_ADMIN con la configuracion guardada:
     * si falla, el error real del servidor SMTP llega a la pantalla en vez de perderse en el log. */
    @PostMapping("/test")
    public ApiResponse<Void> test(@AuthenticationPrincipal Usuario actor) {
        try {
            mailService.sendSync(actor.getEmail(), "Correo de prueba - NexoraSport",
                    "Si estás leyendo esto, la configuración de correo de NexoraSport funciona correctamente.");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo de prueba: " + e.getMessage());
        }
        return ApiResponse.ok("Correo de prueba enviado a " + actor.getEmail(), null);
    }
}
