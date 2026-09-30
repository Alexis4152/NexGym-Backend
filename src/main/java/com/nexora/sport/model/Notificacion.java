package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * El "item de campana": registro persistente de un evento que le importa a alguien.
 * Existe siempre (sirve de historial/auditoria, seccion 23 del encargo), independiente
 * de si ademas se disparo un correo (ver {@link NotificacionEnvio}). destinatarioTipo
 * decide como se interpreta: ALUMNO/INSTRUCTOR usan alumno/instructor; CENTRO_ADMIN no
 * apunta a un Usuario especifico, es visible para todo el staff con acceso a la seccion.
 */
@Entity
@Table(name = "notificaciones")
@Getter
@Setter
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_id", nullable = false)
    private Centro centro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinatario_tipo", nullable = false, length = 20)
    private TipoDestinatario destinatarioTipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    /** Solo aplica a CENTRO_ADMIN: a que Seccion pertenece el evento (INVENTARIO, CLASES,
     * MEMBRESIAS, CAJA...). Null = visible para cualquiera con acceso a NOTIFICACIONES (el
     * comportamiento de antes); no-null = solo visible para quien ademas tenga esa Seccion
     * en su rol (ver NotificacionService#listarBandeja). Asi Recepcion/Encargado/Dueno ven
     * subconjuntos distintos de la misma bandeja sin necesitar un TipoDestinatario nuevo
     * por cada rol. */
    @Enumerated(EnumType.STRING)
    @Column(name = "seccion_objetivo", length = 30)
    private Seccion seccionObjetivo;

    /** Solo aplica a CENTRO_ADMIN: si el evento es propio de una sucursal (ej. "nueva
     * clase"), null = centro-wide (ej. stock, que hoy no se lleva por sucursal). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    /** Tipo de entidad relacionada (p.ej. "MEMBRESIA", "RESERVA", "CLASE", "ALUMNO") para armar el enlace/accion en el frontend. */
    @Column(name = "entidad_tipo", length = 30)
    private String entidadTipo;

    @Column(name = "entidad_id")
    private Long entidadId;

    /**
     * Llave de idempotencia (seccion 9): si no es null, un proceso programado que corre
     * dos veces no debe generar dos filas para el mismo evento/destinatario/fecha. Unica
     * a nivel BD (ademas del chequeo en NotificacionService) como ultima defensa ante carreras.
     */
    @Column(name = "dedupe_key", unique = true, length = 200)
    private String dedupeKey;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "leida_en")
    private LocalDateTime leidaEn;

    /**
     * Snapshot de Centro.notificacionesInternoActivo al momento de crearse: controla si
     * aparece en la campana, sin perder el registro para historial/auditoria si el centro
     * tenia el canal interno apagado.
     */
    @Column(name = "interno_visible", nullable = false)
    private boolean internoVisible = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
