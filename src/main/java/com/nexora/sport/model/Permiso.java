package com.nexora.sport.model;

/**
 * Catalogo de permisos GRANULARES, por accion, encima de {@link Seccion} (que
 * solo decide si un modulo es visible/accesible en absoluto). Seccion sigue
 * respondiendo "¿puede entrar a Membresias?"; Permiso responde "¿puede
 * cancelar una membresia, o solo verla y renovarla?".
 *
 * No todo en el sistema necesita un Permiso — acciones que son puramente de
 * jerarquia estructural (p.ej. "solo un nivel ADMIN+ puede abrir mas de un
 * corte de caja el mismo dia", "autorizar exceder el cupo de una instalacion")
 * se quedan como chequeos de NivelJerarquia via TenantScope, no se
 * convirtieron en Permiso para no crear permisos que nunca se le negarian a
 * nadie de ese nivel de todas formas.
 */
public enum Permiso {
    ALUMNOS_VER, ALUMNOS_CREAR, ALUMNOS_EDITAR, ALUMNOS_BAJA,

    MEMBRESIAS_VER, MEMBRESIAS_CREAR, MEMBRESIAS_RENOVAR, MEMBRESIAS_SUSPENDER, MEMBRESIAS_CANCELAR,

    PAGOS_VER, PAGOS_REGISTRAR, PAGOS_CANCELAR,

    CAJA_VER, CAJA_ABRIR, CAJA_MOVIMIENTO, CAJA_CERRAR,

    INVENTARIO_VER, INVENTARIO_CREAR, INVENTARIO_EDITAR, INVENTARIO_ENTRADA, INVENTARIO_AJUSTE_NEGATIVO,

    VENTAS_VER, VENTAS_CREAR, VENTAS_CANCELAR,

    APARTADOS_VER, APARTADOS_GESTIONAR, APARTADOS_CANCELAR,

    CLASES_VER, CLASES_CREAR, CLASES_EDITAR, CLASES_CANCELAR,

    ASISTENCIAS_VER, ASISTENCIAS_REGISTRAR,

    INSTRUCTORES_VER, INSTRUCTORES_ADMINISTRAR,

    REPORTES_OPERATIVOS, REPORTES_FINANCIEROS,

    USUARIOS_VER, USUARIOS_CREAR, USUARIOS_EDITAR,

    ROLES_VER, ROLES_ADMINISTRAR,

    CONFIGURACION_VER, CONFIGURACION_EDITAR,

    NOTIFICACIONES_CONFIGURAR
}
