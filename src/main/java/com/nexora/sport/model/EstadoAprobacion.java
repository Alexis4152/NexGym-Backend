package com.nexora.sport.model;

/** Estado de aprobacion de un egreso (ver MovimientoFinanciero#estadoAprobacion).
 * Los ingresos siempre nacen APROBADO; un egreso sin comprobante nace PENDIENTE
 * y no cuenta en sumas/reportes hasta que Dueno o Encargado lo resuelva. */
public enum EstadoAprobacion {
    PENDIENTE,
    APROBADO,
    RECHAZADO
}
