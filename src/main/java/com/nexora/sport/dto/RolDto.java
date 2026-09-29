package com.nexora.sport.dto;

import java.util.Set;

public record RolDto(Long id, Long centroId, String nombre, boolean esSistema, boolean activo,
                      Set<String> secciones, String nivel, Set<String> permisos) {}
