package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;

/**
 * Proyeccion generica "agrupado por X": categoria de producto, metodo de pago, concepto
 * de ingreso, disciplina, sucursal, etc. Reutilizada por varios reportes en vez de crear
 * un DTO identico por cada agrupacion.
 */
public record EtiquetaValorDto(Long id, String etiqueta, long cantidad, BigDecimal total) {}
