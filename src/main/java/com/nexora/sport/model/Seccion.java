package com.nexora.sport.model;

/**
 * Catalogo de secciones de la app usadas por el RBAC dinamico (rol -> secciones).
 * Las disciplinas NO viven aqui a proposito: son datos configurables (tabla
 * disciplinas), no un enum de codigo.
 */
public enum Seccion {
    DASHBOARD,
    ALUMNOS,
    MEMBRESIAS,
    CAJA,
    COMPRAS,
    DISCIPLINAS,
    INSTRUCTORES,
    CLASES,
    ASISTENCIA,
    INVENTARIO,
    TIENDA,
    REPORTES,
    USUARIOS,
    ROLES,
    CENTROS
}
