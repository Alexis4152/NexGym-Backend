package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.UsuarioDto;
import com.nexora.sport.dto.auth.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.AuthService;
import com.nexora.sport.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @Value("${app.jwt.refresh-cookie-name}")
    private String cookieName;
    @Value("${app.jwt.refresh-token-expiration-hours}")
    private long refreshHours;
    @Value("${app.jwt.refresh-cookie-secure}")
    private boolean cookieSecure;
    @Value("${app.jwt.refresh-cookie-samesite}")
    private String cookieSameSite;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header("Set-Cookie", buildCookie(result.refreshToken()).toString())
                .body(ApiResponse.ok(result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request) {
        String token = extraerCookie(request);
        if (token == null) return ResponseEntity.status(401).body(ApiResponse.error("No hay sesion activa"));
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(token)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = extraerCookie(request);
        if (token != null) authService.logout(token);
        return ResponseEntity.ok()
                .header("Set-Cookie", buildExpiredCookie().toString())
                .body(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioDto>> me(@AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.toDto(actor)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.ok("Si el correo existe, recibiras instrucciones", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("Contrasena actualizada", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal Usuario actor,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(actor, request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("Contrasena actualizada", null));
    }

    private ResponseCookie buildCookie(String token) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(refreshHours * 3600)
                .build();
    }

    private ResponseCookie buildExpiredCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true).secure(cookieSecure).sameSite(cookieSameSite).path("/api/auth").maxAge(0).build();
    }

    private String extraerCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
