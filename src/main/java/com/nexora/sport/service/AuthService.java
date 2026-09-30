package com.nexora.sport.service;

import com.nexora.sport.dto.auth.LoginRequest;
import com.nexora.sport.dto.auth.LoginResponse;
import com.nexora.sport.model.PasswordResetToken;
import com.nexora.sport.model.RefreshToken;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.PasswordResetTokenRepository;
import com.nexora.sport.repository.RefreshTokenRepository;
import com.nexora.sport.repository.UsuarioRepository;
import com.nexora.sport.security.JwtTokenProvider;
import com.nexora.sport.util.PasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final UsuarioService usuarioService;

    @Value("${app.jwt.refresh-token-expiration-hours}")
    private long refreshHours;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder,
                        MailService mailService, UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.usuarioService = usuarioService;
    }

    public record LoginResult(LoginResponse response, String refreshToken) {}

    @Transactional
    public LoginResult login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Usuario usuario = usuarioRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new IllegalStateException("Credenciales invalidas"));
        String accessToken = jwtTokenProvider.generateAccessToken(usuario.getEmail());
        String refreshToken = createRefreshToken(usuario);
        return new LoginResult(new LoginResponse(accessToken, usuarioService.toDto(usuario)), refreshToken);
    }

    private String createRefreshToken(Usuario usuario) {
        RefreshToken rt = new RefreshToken();
        rt.setUsuario(usuario);
        rt.setToken(PasswordGenerator.generateToken());
        rt.setExpiresAt(LocalDateTime.now().plusHours(refreshHours));
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }

    @Transactional
    public LoginResponse refresh(String rawToken) {
        RefreshToken rt = refreshTokenRepository.findByTokenAndRevokedFalse(rawToken)
                .orElseThrow(() -> new IllegalStateException("Sesion expirada, inicia sesion de nuevo"));
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Sesion expirada, inicia sesion de nuevo");
        }
        Usuario usuario = rt.getUsuario();
        String accessToken = jwtTokenProvider.generateAccessToken(usuario.getEmail());
        return new LoginResponse(accessToken, usuarioService.toDto(usuario));
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(rawToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void forgotPassword(String email) {
        usuarioRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(usuario -> {
            passwordResetTokenRepository.invalidateAllForUser(usuario.getId());
            PasswordResetToken token = new PasswordResetToken();
            token.setUsuario(usuario);
            token.setToken(PasswordGenerator.generateToken());
            token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(token);
            // Con acentos reales (a diferencia del resto de textos internos de la app):
            // un cuerpo sin ningun acento en español es lo bastante corto y ambiguo para
            // que el detector de idioma de Gmail lo confunda con ingles ("Parece que este
            // mensaje esta en ingles"), como le paso a un usuario real con este correo.
            String enlace = frontendUrl + "/reset-password?token=" + token.getToken();
            String cuerpo = "Recibimos una solicitud para restablecer tu contraseña en NexoraSport.\n\n" +
                    "Da clic en este enlace para continuar (válido 30 minutos):\n" + enlace + "\n\n" +
                    "Si el enlace no abre, entra a " + frontendUrl + "/reset-password y pega ahí este código:\n" +
                    token.getToken() + "\n\n" +
                    "Si tú no solicitaste este cambio, ignora este correo; tu contraseña sigue igual.";
            mailService.send(usuario.getEmail(), "Recupera tu contraseña - NexoraSport", cuerpo);
        });
        // Respuesta generica sin importar si el correo existe, para no filtrar informacion.
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenAndUsedFalse(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Codigo invalido o expirado"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Codigo invalido o expirado");
        }
        Usuario usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuario.setMustChangePassword(false);
        usuarioRepository.save(usuario);
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        refreshTokenRepository.revokeAllForUser(usuario.getId());
    }

    @Transactional
    public void changePassword(Usuario usuario, String newPassword) {
        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuario.setMustChangePassword(false);
        usuarioRepository.save(usuario);
        refreshTokenRepository.revokeAllForUser(usuario.getId());
    }
}
