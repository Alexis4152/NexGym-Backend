package com.nexora.sport.dto.publico;

import java.math.BigDecimal;

public record PublicArticuloDto(Long id, String nombre, String categoriaNombre, BigDecimal precioVenta, String imagenUrl) {}
