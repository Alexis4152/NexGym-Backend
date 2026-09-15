package com.nexora.sport.dto.auth;

import com.nexora.sport.dto.UsuarioDto;

public record LoginResponse(String accessToken, UsuarioDto usuario) {}
