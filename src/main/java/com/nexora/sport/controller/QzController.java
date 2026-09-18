package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.service.QzSigningService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Certificado/firma para que QZ Tray confie en este backend al imprimir. No expone nada sensible. */
@RestController
@RequestMapping("/api/qz")
public class QzController {

    private final QzSigningService qzSigningService;

    public QzController(QzSigningService qzSigningService) {
        this.qzSigningService = qzSigningService;
    }

    @GetMapping("/certificate")
    public ApiResponse<String> certificate() {
        return ApiResponse.ok(qzSigningService.getCertificate());
    }

    @PostMapping("/sign")
    public ApiResponse<String> sign(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(qzSigningService.sign(body.get("data")));
    }
}
