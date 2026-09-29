package com.nexora.sport.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Un intento de entrega de una {@link Notificacion} por un canal concreto (hoy solo
 * EMAIL; WHATSAPP/PUSH/SMS quedan preparados). Separado de Notificacion para poder
 * tener varios canales por evento sin duplicar el titulo/mensaje, y para trazar
 * intentos/errores sin ensuciar el modelo del "item de campana".
 */
@Entity
@Table(name = "notificacion_envios")
@Getter
@Setter
public class NotificacionEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notificacion_id", nullable = false)
    private Notificacion notificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacion canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE;

    /** Direccion/telefono real usado en este intento (snapshot: si el alumno cambia su email despues, esto no cambia). */
    @Column(length = 150)
    private String destino;

    @Column(nullable = false)
    private int intentos = 0;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(length = 500)
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
